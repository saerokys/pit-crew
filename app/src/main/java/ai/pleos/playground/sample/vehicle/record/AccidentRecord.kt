package ai.pleos.playground.sample.vehicle.record

import ai.pleos.playground.sample.vehicle.session.AccidentSession
import ai.pleos.playground.sample.vehicle.session.AllChecklists
import ai.pleos.playground.sample.vehicle.ui.summary.AddressUi
import ai.pleos.playground.sample.vehicle.ui.summary.LocationUi
import ai.pleos.playground.sample.vehicle.ui.summary.WeatherUi
import ai.pleos.playground.sample.vehicle.ui.summary.OSM_CREDIT
import ai.pleos.playground.sample.vehicle.ui.summary.addressText
import ai.pleos.playground.sample.vehicle.ui.summary.addressUi
import ai.pleos.playground.sample.vehicle.ui.summary.locationUi
import ai.pleos.playground.sample.vehicle.ui.summary.weatherUi
import ai.pleos.playground.sample.vehicle.vehicle.Reading
import ai.pleos.playground.sample.vehicle.vehicle.VehicleSnapshot

/**
 * SC-270 [기록 저장]으로 기기에 남기는 사고 기록 패키지(SPEC 1.2, PLAN Phase 10).
 * 항목은 사고 시각, 위치·주소, 날씨, 차량 상태, 안내 이행이다(날씨는 형식 버전 2부터).
 * 좌표, 부상 응답, 상대방 정보, 사진, 차대번호는 세션에서도 가져오지 않고 이 형식에 칸이 없다.
 * 음성 메모는 사고 기록에 넣지 않는다(PROGRESS 결정 기록, 세션 메모리에만 둔다).
 *
 * 순수 Kotlin 값(Map, List, 숫자, 문자열, null)으로 만들어 JVM 단위 테스트로 확인하고,
 * JSON 변환은 [RecordStore]가 한다.
 */
object AccidentRecord {
    /** 저장 형식 번호. 내용 항목이 아니라 형식이 바뀔 때를 위한 값이다. */
    const val FORMAT_VERSION = 2

    const val KEY_VERSION = "version"
    const val KEY_STARTED_AT = "startedAt"
    const val KEY_LOCATION = "location"
    const val KEY_WEATHER = "weather"
    const val KEY_VEHICLE = "vehicle"
    const val KEY_CHECKS = "checks"

    /** 위치 항목 값. 위치를 허용하지 않았으면 NOT_USED, 읽지 못했으면 UNAVAILABLE, 읽었으면 CONFIRMED. */
    const val LOCATION_NOT_USED = "NOT_USED"
    const val LOCATION_UNAVAILABLE = "UNAVAILABLE"
    const val LOCATION_CONFIRMED = "CONFIRMED"

    /** 주소 항목 값. 조회하지 않았으면 NONE, 조회에 실패했으면 UNAVAILABLE, 성공했으면 OK(이때만 주소 문자열과 출처가 있다). */
    const val ADDRESS_NONE = "NONE"
    const val ADDRESS_UNAVAILABLE = "UNAVAILABLE"
    const val ADDRESS_OK = "OK"

    /** 날씨 항목 값. 쓰지 않았으면 NOT_USED, 읽지 못했거나 조회 전이면 UNAVAILABLE, 성공했으면 OK(이때만 조건과 온도가 있다). */
    const val WEATHER_NOT_USED = "NOT_USED"
    const val WEATHER_UNAVAILABLE = "UNAVAILABLE"
    const val WEATHER_OK = "OK"

    /** 세션에서 저장할 값만 뽑는다. */
    fun from(session: AccidentSession): Map<String, Any?> = linkedMapOf(
        KEY_VERSION to FORMAT_VERSION,
        KEY_STARTED_AT to session.startedAtMillis,
        KEY_LOCATION to locationPart(session),
        KEY_WEATHER to weatherPart(session),
        KEY_VEHICLE to session.vehicle?.let(::vehiclePart),
        KEY_CHECKS to checksPart(session)
    )

    private fun locationPart(session: AccidentSession): Map<String, Any?> {
        val location = when (locationUi(session)) {
            null -> LOCATION_NOT_USED
            LocationUi.OK -> LOCATION_CONFIRMED
            else -> LOCATION_UNAVAILABLE // 읽지 못함, 또는 읽는 중에 저장함
        }
        val addressState = if (location != LOCATION_CONFIRMED) {
            ADDRESS_NONE
        } else {
            when (addressUi(session)) {
                null -> ADDRESS_NONE
                AddressUi.OK -> ADDRESS_OK
                else -> ADDRESS_UNAVAILABLE
            }
        }
        val address = if (addressState == ADDRESS_OK) addressText(session) else null
        return linkedMapOf(
            "status" to location,
            "addressStatus" to addressState,
            "address" to address,
            "addressCredit" to if (address != null) OSM_CREDIT else null
        )
    }

    private fun weatherPart(session: AccidentSession): Map<String, Any?> {
        val ui = weatherUi(session)
        val status = when (ui) {
            null -> WEATHER_NOT_USED
            WeatherUi.OK -> WEATHER_OK
            else -> WEATHER_UNAVAILABLE
        }
        val ok = status == WEATHER_OK
        return linkedMapOf(
            "status" to status,
            "condition" to if (ok) session.weather?.condition else null,
            "temperatureC" to if (ok) session.weather?.temperatureC else null
        )
    }

    private fun vehiclePart(snapshot: VehicleSnapshot): Map<String, Any?> = linkedMapOf(
        "readAt" to snapshot.readAtMillis,
        "doors" to snapshot.doors.mapKeys { it.key.name },
        "windows" to snapshot.windows.mapKeys { it.key.name },
        "battery" to snapshot.battery.value,
        "odometer" to snapshot.odometer.value?.let(::floatToDouble),
        "turnSignal" to snapshot.turnSignal.value?.name
    )

    /** Float를 JSON에 넣으면 12.3f가 12.300000190734863이 되므로 글자로 바꿔 다시 읽는다. 유한하지 않은 값은 null. */
    private fun floatToDouble(value: Float): Double? =
        if (value.isNaN() || value.isInfinite()) null else value.toString().toDouble()

    /** 항목 id별 체크 여부(체크하지 않은 항목은 false = "확인 안 함"). 12개 모두 쓴다. */
    private fun checksPart(session: AccidentSession): Map<String, Boolean> =
        linkedMapOf<String, Boolean>().apply {
            AllChecklists.forEach { section -> section.items.forEach { put(it.id, session.isChecked(it.id)) } }
        }
}

/** SC-270 요약 한 줄에 쓰는 숫자. SC-260과 같은 기준(체크 항목 12개, 차량 기능 5개)이다. */
object EndingSummary {
    const val VEHICLE_TOTAL = 5

    val checkTotal: Int get() = AllChecklists.sumOf { it.items.size }

    fun checkedCount(session: AccidentSession?): Int =
        AllChecklists.sumOf { section -> section.items.count { session?.isChecked(it.id) == true } }

    /** 문, 창문, 배터리, 주행 거리, 방향지시등 중 값을 하나라도 읽은 기능의 수. */
    fun vehicleReadCount(snapshot: VehicleSnapshot?): Int {
        if (snapshot == null) return 0
        fun <T> Reading<T>.has() = value != null
        return listOf(
            snapshot.doors.values.any { it != null },
            snapshot.windows.values.any { it != null },
            snapshot.battery.has(),
            snapshot.odometer.has(),
            snapshot.turnSignal.has()
        ).count { it }
    }
}
