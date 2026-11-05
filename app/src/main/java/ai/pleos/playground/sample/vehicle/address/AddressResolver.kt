package ai.pleos.playground.sample.vehicle.address

import ai.pleos.playground.sample.vehicle.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONException
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.HttpsURLConnection

/** 주소 조회 결과 종류. OK가 아니면 화면에는 "주소 확인 불가"로 보인다. */
enum class AddressStatus {
    OK,
    /** 서버가 이 좌표의 주소를 찾지 못함, 또는 쓸 수 있는 주소 항목이 없음 */
    NO_ADDRESS,
    /** 3초 안에 응답이 없음 */
    TIMEOUT,
    /** 네트워크가 끊겼거나 서버에 연결할 수 없음 */
    NETWORK,
    /** HTTP 오류, 응답 해석 실패, 그 밖의 예외 */
    ERROR
}

/** 주소 한 줄과 상태. 좌표는 담지 않는다(세션에는 이 값만 둔다). */
data class AddressResult(val status: AddressStatus, val text: String? = null)

/**
 * 주소 문장 만들기와 좌표 반올림(Android 의존 없는 순수 함수라 JVM 단위 테스트로 확인한다).
 */
object AddressFormat {
    /** 서버로 보내는 좌표는 소수점 4자리(약 11m)로 반올림한다. */
    fun roundCoordinate(value: Double): String = String.format(java.util.Locale.US, "%.4f", value)

    private fun Map<String, String>.first(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key -> this[key]?.trim()?.takeIf { it.isNotEmpty() } }

    /**
     * Nominatim `address` 항목에서 "시/도 구 도로명 번호" 순서의 한 줄을 만든다.
     * 상호(name), 우편번호, 나라는 쓰지 않는다. 도로명이나 동네 이름이 없으면 null(주소 없음).
     */
    fun compose(address: Map<String, String>): String? {
        val road = address.first("road", "pedestrian", "footway", "path")
        val area = address.first("suburb", "quarter", "neighbourhood", "village", "town", "hamlet")
        if (road == null && area == null) return null
        val parts = mutableListOf<String>()
        fun add(value: String?) {
            if (value != null && parts.lastOrNull() != value) parts.add(value)
        }
        add(address.first("province", "state", "region"))
        add(address.first("county"))
        add(address.first("city"))
        add(address.first("borough", "city_district", "district"))
        if (road == null) add(area) else {
            add(road)
            add(address.first("house_number"))
        }
        return parts.joinToString(" ").takeIf { it.isNotBlank() }
    }
}

/**
 * Nominatim(공용 서버) 역지오코딩으로 좌표를 주소 한 줄로 바꾼다. 사고 모드 시작 때 세션당 1회만 부른다.
 * - HTTPS만 쓴다. 리디렉션은 따라가지 않는다. 실패해도 재시도하지 않는다(정책 준수).
 * - User-Agent는 앱 이름과 버전뿐이다(이메일, 키, 기기 식별자 없음).
 * - 좌표는 호출에만 쓰고 로그, 문서, 세션에 남기지 않는다. 요청 URL, 응답 본문, 예외 메시지를 로그에 쓰지 않는다.
 * - 응답의 name(상호 등)은 버리고 행정 구역과 도로 항목만 쓴다.
 * - 3초 제한(연결 3초, 읽기 3초, 전체 3초).
 */
class AddressResolver(private val timeoutMs: Long = 3_000L) {

    suspend fun resolve(latitude: Double, longitude: Double): AddressResult {
        if (!latitude.isFinite() || !longitude.isFinite() || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            return AddressResult(AddressStatus.ERROR)
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
                AddressResult(AddressStatus.TIMEOUT)
            } else {
                result
            }
        }
    }

    private fun request(latitude: Double, longitude: Double, holder: AtomicReference<HttpsURLConnection?>): AddressResult {
        var connection: HttpsURLConnection? = null
        return try {
            val url = URL(
                "https://$HOST/reverse?format=jsonv2" +
                    "&lat=${AddressFormat.roundCoordinate(latitude)}&lon=${AddressFormat.roundCoordinate(longitude)}" +
                    "&zoom=18&addressdetails=1&accept-language=ko"
            )
            if (url.protocol != "https") return AddressResult(AddressStatus.ERROR)
            connection = (url.openConnection() as HttpsURLConnection).also { holder.set(it) }
            connection.connectTimeout = 3_000
            connection.readTimeout = 3_000
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept-Language", "ko")
            if (connection.responseCode != 200) return AddressResult(AddressStatus.ERROR)
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            if (json.has("error")) return AddressResult(AddressStatus.NO_ADDRESS)
            val address = json.optJSONObject("address") ?: return AddressResult(AddressStatus.NO_ADDRESS)
            val fields = address.keys().asSequence().associateWith { address.optString(it) }
            val text = AddressFormat.compose(fields)
            if (text == null) AddressResult(AddressStatus.NO_ADDRESS) else AddressResult(AddressStatus.OK, text)
        } catch (e: SocketTimeoutException) {
            AddressResult(AddressStatus.TIMEOUT)
        } catch (e: java.io.IOException) {
            AddressResult(AddressStatus.NETWORK)
        } catch (e: JSONException) {
            AddressResult(AddressStatus.ERROR)
        } catch (e: Exception) {
            AddressResult(AddressStatus.ERROR)
        } finally {
            try {
                connection?.disconnect()
            } catch (e: Exception) {
            }
        }
    }

    private companion object {
        const val HOST = "nominatim.openstreetmap.org"
        val USER_AGENT = "PitCrew/${BuildConfig.VERSION_NAME} (Android Automotive prototype)"
    }
}
