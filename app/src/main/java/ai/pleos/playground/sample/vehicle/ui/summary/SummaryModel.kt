package ai.pleos.playground.sample.vehicle.ui.summary

import ai.pleos.playground.sample.vehicle.session.AccidentSession
import ai.pleos.playground.sample.vehicle.session.InjuryAnswer
import ai.pleos.playground.sample.vehicle.session.OtherPartyChecklist
import ai.pleos.playground.sample.vehicle.session.formatClock
import ai.pleos.playground.sample.vehicle.ui.vehicle.formatNumber
import ai.pleos.playground.sample.vehicle.vehicle.AreaNames
import ai.pleos.playground.sample.vehicle.vehicle.Reading
import ai.pleos.playground.sample.vehicle.vehicle.SupportState
import ai.pleos.playground.sample.vehicle.vehicle.VehicleSnapshot

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

/** 상대방 확인(SC-240) 체크 개수 요약. 상대방 정보 자체는 저장하지 않으므로 개수만 보여 준다. */
fun otherPartyText(session: AccidentSession?): String {
    val total = OtherPartyChecklist.items.size
    val checked = OtherPartyChecklist.items.count { session?.isChecked(it.id) == true }
    return "확인함 ${checked}개, 확인 안 함 ${total - checked}개"
}
