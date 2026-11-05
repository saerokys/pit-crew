package ai.pleos.playground.sample.vehicle.ui.precheck

import ai.pleos.playground.sample.vehicle.ui.components.BadgeStatus
import ai.pleos.playground.sample.vehicle.vehicle.SupportState
import ai.pleos.playground.sample.vehicle.vehicle.VehicleSnapshot
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue

/**
 * SC-300 출발 전 점검의 판정. 화면과 분리해 규칙만 모았다(읽은 값 -> 항목별 결과 -> 요약).
 * - 열림 기준은 값이 0보다 클 때이다(문 Hood·Rear 포함, 창문은 0~10 중 0보다 크면 열림).
 * - 방향지시등이 왼쪽/오른쪽으로 켜져 있으면 "확인 필요"이다.
 * - 읽을 수 없는 항목은 "확인 불가"이며 확인 필요 건수에 세지 않는다.
 * - 배터리는 값만 보여 주고 어떤 판단도 하지 않는다.
 */
enum class PreCheckKind { DOOR, WINDOW, BATTERY, TURN_SIGNAL }

/**
 * 카드 하나의 결과. [badge]가 null이면 배지를 두지 않는다(배터리 값).
 * [needsAttention]은 "확인 필요"에 세는 항목, [unreadable]은 "확인 불가" 항목이다.
 * [compact]가 참이면 값 줄이 길어 작은 글자로 보여 준다.
 */
data class PreCheckItem(
    val kind: PreCheckKind,
    val label: String,
    val value: String,
    val badge: BadgeStatus?,
    val hint: String? = null,
    val needsAttention: Boolean = false,
    val unreadable: Boolean = false,
    val compact: Boolean = false
)

data class PreCheckResult(
    val items: List<PreCheckItem>,
    val attentionCount: Int,
    val summaryStatus: BadgeStatus,
    val summaryText: String
)

private fun unreadableItem(kind: PreCheckKind, label: String, state: SupportState) = PreCheckItem(
    kind = kind,
    label = label,
    value = if (state == SupportState.UNSUPPORTED) "이 차량에서는 지원하지 않아요" else "확인하지 못했어요",
    badge = BadgeStatus.UNKNOWN,
    unreadable = true,
    compact = true
)

/** 문, 창문처럼 영역별 값이 있는 항목. 읽지 못한 영역이 있어도 닫혔다고 말하지 않는다. */
private fun areaItem(
    kind: PreCheckKind,
    label: String,
    state: SupportState,
    positions: Collection<Int?>,
    closeHint: String
): PreCheckItem {
    if (state != SupportState.READABLE) return unreadableItem(kind, label, state)
    if (positions.isEmpty() || positions.all { it == null }) return unreadableItem(kind, label, SupportState.UNKNOWN)
    val open = positions.count { (it ?: 0) > 0 }
    val failed = positions.count { it == null }
    return when {
        open > 0 -> PreCheckItem(
            kind, label,
            value = if (failed > 0) "열림 ${open}개 · 일부 확인 불가" else "열림 ${open}개",
            badge = BadgeStatus.WARN,
            hint = closeHint,
            needsAttention = true,
            compact = failed > 0
        )
        failed > 0 -> PreCheckItem(kind, label, "일부 확인 불가", BadgeStatus.UNKNOWN, unreadable = true, compact = true)
        else -> PreCheckItem(kind, label, "모두 닫힘", BadgeStatus.OK)
    }
}

private fun batteryItem(snapshot: VehicleSnapshot?): PreCheckItem {
    val reading = snapshot?.battery
    val value = reading?.value
    return if (reading?.state == SupportState.READABLE && value != null) {
        PreCheckItem(PreCheckKind.BATTERY, "배터리", "$value%", badge = null)
    } else {
        unreadableItem(PreCheckKind.BATTERY, "배터리", reading?.state ?: SupportState.UNKNOWN)
    }
}

private fun turnSignalItem(snapshot: VehicleSnapshot?): PreCheckItem {
    val reading = snapshot?.turnSignal
    val value = reading?.value
    if (reading?.state != SupportState.READABLE || value == null) {
        return unreadableItem(PreCheckKind.TURN_SIGNAL, "방향지시등", reading?.state ?: SupportState.UNKNOWN)
    }
    return when (value) {
        TurnSignalValue.OFF -> PreCheckItem(PreCheckKind.TURN_SIGNAL, "방향지시등", "꺼짐", BadgeStatus.OK)
        TurnSignalValue.LEFT -> turnOn("왼쪽 켜짐")
        TurnSignalValue.RIGHT -> turnOn("오른쪽 켜짐")
    }
}

private fun turnOn(text: String) = PreCheckItem(
    PreCheckKind.TURN_SIGNAL, "방향지시등", text, BadgeStatus.WARN,
    hint = "방향지시등을 끄고 다시 확인해 주세요", needsAttention = true
)

/** [snapshot]이 null이면(읽기 실패) 모든 항목을 "확인 불가"로 본다. */
fun evaluatePreCheck(snapshot: VehicleSnapshot?): PreCheckResult {
    val items = listOf(
        areaItem(
            PreCheckKind.DOOR, "문",
            snapshot?.doorsState ?: SupportState.UNKNOWN, snapshot?.doors?.values.orEmpty(),
            "문을 닫고 다시 확인해 주세요"
        ),
        areaItem(
            PreCheckKind.WINDOW, "창문",
            snapshot?.windowsState ?: SupportState.UNKNOWN, snapshot?.windows?.values.orEmpty(),
            "창문을 닫고 다시 확인해 주세요"
        ),
        batteryItem(snapshot),
        turnSignalItem(snapshot)
    )
    val attention = items.count { it.needsAttention }
    val unreadable = items.count { it.unreadable }
    val (status, text) = when {
        attention > 0 -> BadgeStatus.WARN to "확인 필요 ${attention}건"
        unreadable == items.size -> BadgeStatus.UNKNOWN to "확인 불가"
        unreadable > 0 -> BadgeStatus.UNKNOWN to "일부 확인 불가"
        else -> BadgeStatus.OK to "이상 없음"
    }
    return PreCheckResult(items, attention, status, text)
}
