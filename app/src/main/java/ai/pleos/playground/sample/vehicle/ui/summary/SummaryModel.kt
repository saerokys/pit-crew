package ai.pleos.playground.sample.vehicle.ui.summary

import ai.pleos.playground.sample.vehicle.address.AddressStatus
import ai.pleos.playground.sample.vehicle.location.LocationStatus
import ai.pleos.playground.sample.vehicle.session.AccidentSession
import ai.pleos.playground.sample.vehicle.session.InjuryAnswer
import ai.pleos.playground.sample.vehicle.session.OtherPartyChecklist
import ai.pleos.playground.sample.vehicle.session.formatClock
import ai.pleos.playground.sample.vehicle.ui.vehicle.formatNumber
import ai.pleos.playground.sample.vehicle.vehicle.AreaNames
import ai.pleos.playground.sample.vehicle.vehicle.Reading
import ai.pleos.playground.sample.vehicle.vehicle.SupportState
import ai.pleos.playground.sample.vehicle.vehicle.VehicleSnapshot
import ai.pleos.playground.sample.vehicle.weather.WeatherFormat
import ai.pleos.playground.sample.vehicle.weather.WeatherStatus

/**
 * SC-260 요약에 쓰는 문장을 세션에서 만든다(화면 표시용, 저장·전송하지 않음).
 * 위치, 사진, 상대방 정보, 차대번호는 세션에 없고 여기서도 다루지 않는다.
 */

/** 줄바꿈이 영역 이름 중간이 아니라 쉼표 뒤에서만 되도록 이름 안의 공백을 줄바꿈 없는 공백으로 바꾼다. */
private fun noBreak(name: String): String = name.replace(' ', ' ')

fun injuryText(answer: InjuryAnswer): String = when (answer) {
    InjuryAnswer.NONE -> "없음"
    InjuryAnswer.YES -> "있음. 몇 명인지, 어떤 상태인지 말해 주세요."
    InjuryAnswer.UNSURE -> "모름. 보이는 대로 말해 주세요."
    InjuryAnswer.NOT_ANSWERED -> "응답 안 함"
}

/**
 * 열린 문/창문을 이름으로 모두 나열한다. 열림 기준은 값이 0보다 클 때이다.
 * 기능을 읽지 못했으면 "확인 불가", 일부 영역만 못 읽었으면 그 사실을 덧붙인다.
 */
fun <K> openAreasText(
    label: String,
    state: SupportState?,
    positions: Map<K, Int?>,
    names: Map<K, String>
): String {
    if (state != SupportState.READABLE || positions.isEmpty() || positions.values.all { it == null }) {
        return "$label: 확인 불가"
    }
    val open = names.keys.filter { (positions[it] ?: 0) > 0 }
    val failed = positions.values.count { it == null }
    val body = if (open.isEmpty()) {
        if (failed > 0) "확인된 곳 없음" else "없음"
    } else {
        open.joinToString(", ") { noBreak(names.getValue(it)) }
    }
    return if (failed > 0) "$label: $body (일부 확인 불가)" else "$label: $body"
}

private fun <T> valueText(label: String, reading: Reading<T>?, format: (T) -> String): String {
    val value = reading?.value
    return if (reading?.state == SupportState.READABLE && value != null) "$label: ${format(value)}" else "$label: 확인 불가"
}

/**
 * 차량 상태 요약 5줄(열린 문, 열린 창문, 배터리, 주행 거리, 읽은 시각). 읽은 값이 없으면 "확인 불가".
 * "읽은 시각"은 차량 값을 읽은 시각(SC-250의 기록 시각과 같음)이며, 사고 모드를 시작한 시각과 다르다.
 */
fun vehicleLines(snapshot: VehicleSnapshot?): List<String> = listOf(
    openAreasText("열린 문", snapshot?.doorsState, snapshot?.doors.orEmpty(), AreaNames.door),
    openAreasText("열린 창문", snapshot?.windowsState, snapshot?.windows.orEmpty(), AreaNames.window),
    valueText("배터리", snapshot?.battery) { "$it%" },
    valueText("주행 거리", snapshot?.odometer) { "${formatNumber(it)}km" },
    if (snapshot != null) "읽은 시각 ${formatClock(snapshot.readAtMillis)}" else "읽은 시각 확인 불가"
)

/** 주소 출처 표기(Nominatim 정책). 주소가 보이는 화면마다 함께 보여 준다. */
const val OSM_CREDIT = "© OpenStreetMap contributors"

/** 위치 표시 상태. null이면 위치를 허용하지 않았으므로 아무것도 보이지 않는다. 좌표는 어디에도 보이지 않는다. */
enum class LocationUi { PENDING, OK, UNAVAILABLE }

fun locationUi(session: AccidentSession?): LocationUi? {
    if (session == null || !session.locationRequested) return null
    val status = session.locationStatus ?: return LocationUi.PENDING
    return if (status == LocationStatus.OK) LocationUi.OK else LocationUi.UNAVAILABLE
}

/** 주소 표시 상태. null이면 주소를 조회하지 않는 경우(위치 또는 네트워크 꺼짐, 위치 확인 불가)이다. */
enum class AddressUi { PENDING, OK, FAILED }

fun addressUi(session: AccidentSession?): AddressUi? {
    if (session == null || !session.addressRequested || locationUi(session) != LocationUi.OK) return null
    val address = session.address ?: return AddressUi.PENDING
    return if (address.status == AddressStatus.OK && !address.text.isNullOrBlank()) AddressUi.OK else AddressUi.FAILED
}

/**
 * 날씨 카드 표시 상태. null이면 카드를 보이지 않는다(위치를 껐거나 네트워크를 껐을 때).
 * 위치와 네트워크를 모두 켠 경우: 조회 전·위치를 읽는 중이면 PENDING, 값이 오면 OK, 위치를 읽지 못했거나 조회에 실패하면 FAILED.
 */
enum class WeatherUi { PENDING, OK, FAILED }

fun weatherUi(session: AccidentSession?): WeatherUi? {
    if (session == null || !session.weatherRequested || !session.locationRequested) return null
    val weather = session.weather
    if (weather != null) {
        return if (weather.status == WeatherStatus.OK && WeatherFormat.displayText(weather) != null) WeatherUi.OK else WeatherUi.FAILED
    }
    return when (locationUi(session)) {
        LocationUi.UNAVAILABLE -> WeatherUi.FAILED // 좌표가 없어 조회하지 않는다
        else -> WeatherUi.PENDING
    }
}

/** 날씨 한 줄 "맑음 18°C"(조회에 성공했을 때만). */
fun weatherText(session: AccidentSession?): String? =
    if (weatherUi(session) == WeatherUi.OK) WeatherFormat.displayText(session?.weather) else null

/** 날씨 출처 표기(CC BY 4.0). 날씨 값이 보이는 화면마다 함께 보여 준다. */
const val WEATHER_CREDIT = "Weather data by Open-Meteo.com"

/** 주소 문자열(조회에 성공했을 때만). */
fun addressText(session: AccidentSession?): String? =
    if (addressUi(session) == AddressUi.OK) session?.address?.text else null

/**
 * SC-221, SC-260에서 기존 "사고 장소를 말로 설명하세요" 문장 아래에 덧붙이는 상태 한 줄.
 * 위치를 허용하지 않았거나 주소가 보이는 경우는 null. [summary]가 참이면 SC-260 형식("위치: 확인됨")이다.
 */
fun placeStatusText(session: AccidentSession?, summary: Boolean = false): String? {
    val prefix = if (summary) "위치: " else "위치 "
    return when (locationUi(session)) {
        null -> null
        LocationUi.PENDING -> "${prefix}확인 중이에요"
        LocationUi.UNAVAILABLE -> "${prefix}확인 불가"
        LocationUi.OK -> when (addressUi(session)) {
            null -> "${prefix}확인됨"
            AddressUi.PENDING -> "주소를 찾는 중이에요"
            AddressUi.OK -> null
            AddressUi.FAILED -> if (summary) "위치: 확인됨, 주소 확인 불가" else "위치 확인됨 · 주소 확인 불가"
        }
    }
}

/** 상대방 확인(SC-240) 체크 개수 요약. 상대방 정보 자체는 저장하지 않으므로 개수만 보여 준다. */
fun otherPartyText(session: AccidentSession?): String {
    val total = OtherPartyChecklist.items.size
    val checked = OtherPartyChecklist.items.count { session?.isChecked(it.id) == true }
    return "확인함 ${checked}개, 확인 안 함 ${total - checked}개"
}
