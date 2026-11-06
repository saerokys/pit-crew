package ai.pleos.playground.sample.vehicle.weather

import ai.pleos.playground.sample.vehicle.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONException
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.net.URL
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.HttpsURLConnection
import kotlin.math.roundToInt

/** 날씨 조회 결과 종류. OK가 아니면 화면에는 회색 "확인 불가"로 보인다. */
enum class WeatherStatus {
    OK,
    /** 응답은 왔지만 온도나 날씨 코드가 없거나, 표에 없는 날씨 코드 */
    NO_DATA,
    /** 3초 안에 응답이 없음 */
    TIMEOUT,
    /** 네트워크가 끊겼거나 서버에 연결할 수 없음 */
    NETWORK,
    /** HTTP 오류, 응답 해석 실패, 그 밖의 예외 */
    ERROR
}

/** 날씨 한 줄(조건과 온도)과 상태. 좌표는 담지 않는다(세션에는 이 값만 둔다). */
data class WeatherResult(val status: WeatherStatus, val condition: String? = null, val temperatureC: Int? = null)

/**
 * 좌표 반올림, 날씨 코드 변환, 표시 문장 만들기(Android 의존 없는 순수 함수라 JVM 단위 테스트로 확인한다).
 * 날씨 코드는 Open-Meteo 문서의 WMO 코드 표에 있는 것만 한국어로 바꾸고, 표에 없는 코드는 NO_DATA(확인 불가)로 한다.
 */
object WeatherFormat {
    /** 서버로 보내는 좌표는 소수점 2자리(약 1km)로 반올림한다. 서버 예보 격자가 1km보다 거칠어 더 정밀할 필요가 없다. */
    fun roundCoordinate(value: Double): String = String.format(Locale.US, "%.2f", value)

    private val conditions: Map<Int, String> = mapOf(
        0 to "맑음", 1 to "대체로 맑음", 2 to "구름 조금", 3 to "흐림",
        45 to "안개", 48 to "안개(서리)",
        51 to "약한 이슬비", 53 to "이슬비", 55 to "강한 이슬비", 56 to "어는 이슬비", 57 to "어는 이슬비",
        61 to "약한 비", 63 to "비", 65 to "강한 비", 66 to "어는 비", 67 to "어는 비",
        71 to "약한 눈", 73 to "눈", 75 to "강한 눈", 77 to "싸락눈",
        80 to "약한 소나기", 81 to "소나기", 82 to "강한 소나기",
        85 to "눈 소나기", 86 to "눈 소나기",
        95 to "뇌우", 96 to "우박 뇌우", 97 to "강한 뇌우", 99 to "우박 뇌우"
    )

    /** 표에 있는 코드의 한국어 문구. 없으면 null. */
    fun condition(code: Int?): String? = code?.let { conditions[it] }

    /** 온도와 날씨 코드로 결과를 만든다. 둘 중 하나라도 없거나 코드가 표에 없으면 NO_DATA. */
    fun describe(temperature: Double?, code: Int?): WeatherResult {
        val text = condition(code)
        if (text == null || temperature == null || !temperature.isFinite()) return WeatherResult(WeatherStatus.NO_DATA)
        return WeatherResult(WeatherStatus.OK, text, temperature.roundToInt())
    }

    /** 화면과 기록에 쓰는 한 줄: "맑음 18°C". 값이 없으면 null. */
    fun displayText(result: WeatherResult?): String? {
        if (result?.status != WeatherStatus.OK) return null
        val text = result.condition ?: return null
        val temperature = result.temperatureC ?: return null
        return "$text ${temperature}°C"
    }
}

/**
 * Open-Meteo(키 없는 무료 API)로 좌표의 현재 날씨(온도, 날씨 코드)를 한 번 조회한다. 사고 모드 시작 때 세션당 1회만 부른다.
 * - HTTPS만 쓴다. 리디렉션은 따라가지 않고, 캐시를 쓰지 않고, 실패해도 재시도하지 않는다.
 * - 좌표는 소수점 2자리로 반올림해 호출에만 쓰고 로그, 문서, 세션에 남기지 않는다. 요청 URL, 응답 본문, 예외 메시지를 로그에 쓰지 않는다.
 * - User-Agent는 앱 이름과 버전뿐이다(이메일, 키, 기기 식별자 없음).
 * - 3초 제한(연결 3초, 읽기 3초, 전체 3초).
 * - 화면에 날씨를 보여 줄 때는 출처 "Weather data by Open-Meteo.com"를 함께 보여 준다(CC BY 4.0).
 */
class WeatherResolver(private val timeoutMs: Long = 3_000L) {

    suspend fun resolve(latitude: Double, longitude: Double): WeatherResult {
        if (!latitude.isFinite() || !longitude.isFinite() || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            return WeatherResult(WeatherStatus.ERROR)
        }
        val connection = AtomicReference<HttpsURLConnection?>()
        return coroutineScope {
            val work = async(Dispatchers.IO) { request(latitude, longitude, connection) }
            val result = withTimeoutOrNull(timeoutMs) { work.await() }
            if (result == null) {
                // 시간 초과: 연결을 끊어 막힌 읽기를 풀고 결과는 버린다.
                try {
                    connection.get()?.disconnect()
                } catch (e: Exception) {
                }
                work.cancel()
                WeatherResult(WeatherStatus.TIMEOUT)
            } else {
                result
            }
        }
    }

    private fun request(latitude: Double, longitude: Double, holder: AtomicReference<HttpsURLConnection?>): WeatherResult {
        var connection: HttpsURLConnection? = null
        return try {
            val url = URL(
                "https://$HOST/v1/forecast" +
                    "?latitude=${WeatherFormat.roundCoordinate(latitude)}&longitude=${WeatherFormat.roundCoordinate(longitude)}" +
                    "&current=temperature_2m,weather_code"
            )
            if (url.protocol != "https") return WeatherResult(WeatherStatus.ERROR)
            connection = (url.openConnection() as HttpsURLConnection).also { holder.set(it) }
            connection.connectTimeout = 3_000
            connection.readTimeout = 3_000
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", USER_AGENT)
            if (connection.responseCode != 200) return WeatherResult(WeatherStatus.ERROR)
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val current = JSONObject(body).optJSONObject("current") ?: return WeatherResult(WeatherStatus.NO_DATA)
            val temperature = if (current.has("temperature_2m") && !current.isNull("temperature_2m")) current.optDouble("temperature_2m") else null
            val code = if (current.has("weather_code") && !current.isNull("weather_code")) current.optInt("weather_code", -1).takeIf { it >= 0 } else null
            WeatherFormat.describe(temperature?.takeIf { !it.isNaN() }, code)
        } catch (e: SocketTimeoutException) {
            WeatherResult(WeatherStatus.TIMEOUT)
        } catch (e: java.io.IOException) {
            WeatherResult(WeatherStatus.NETWORK)
        } catch (e: JSONException) {
            WeatherResult(WeatherStatus.ERROR)
        } catch (e: Exception) {
            WeatherResult(WeatherStatus.ERROR)
        } finally {
            try {
                connection?.disconnect()
            } catch (e: Exception) {
            }
        }
    }

    private companion object {
        const val HOST = "api.open-meteo.com"
        val USER_AGENT = "PitCrew/${BuildConfig.VERSION_NAME} (Android Automotive prototype)"
    }
}
