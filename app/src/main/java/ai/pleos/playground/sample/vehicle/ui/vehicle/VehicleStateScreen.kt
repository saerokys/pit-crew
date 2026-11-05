package ai.pleos.playground.sample.vehicle.ui.vehicle

import ai.pleos.playground.sample.vehicle.session.AccidentSession
import ai.pleos.playground.sample.vehicle.session.formatClock
import ai.pleos.playground.sample.vehicle.ui.components.BadgeStatus
import ai.pleos.playground.sample.vehicle.ui.components.CarTopView
import ai.pleos.playground.sample.vehicle.ui.components.DoorMark
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewCard
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewIcons
import ai.pleos.playground.sample.vehicle.ui.components.StatusBadge
import ai.pleos.playground.sample.vehicle.ui.components.StatusCard
import ai.pleos.playground.sample.vehicle.ui.components.TopBarButton
import ai.pleos.playground.sample.vehicle.ui.summary.LocationUi
import ai.pleos.playground.sample.vehicle.ui.summary.locationUi
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import ai.pleos.playground.sample.vehicle.vehicle.AreaNames
import ai.pleos.playground.sample.vehicle.vehicle.Reading
import ai.pleos.playground.sample.vehicle.vehicle.SupportState
import ai.pleos.playground.sample.vehicle.vehicle.VehicleSnapshot
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat

/** 카드 하나에 보여 줄 내용. [badge]가 null이면 배지를 두지 않는다(값만 표시). */
private data class CardInfo(val value: String, val badge: BadgeStatus? = null, val isMessage: Boolean = false)

private val DoorNames = AreaNames.door

/** 문과 창문의 열림 기준: 0이면 닫힘, 0보다 크면 열림(정도는 보여 주지 않는다). */
private fun isOpen(position: Int?): Boolean = (position ?: 0) > 0

private fun messageFor(state: SupportState): CardInfo = when (state) {
    SupportState.UNSUPPORTED -> CardInfo("이 차량에서는 지원하지 않아요", BadgeStatus.UNKNOWN, isMessage = true)
    else -> CardInfo("확인하지 못했어요", BadgeStatus.UNKNOWN, isMessage = true)
}

/** 문, 창문처럼 영역별 값이 있는 기능의 카드 내용. 읽지 못한 영역이 있으면 닫혔다고 말하지 않는다. */
private fun areaInfo(state: SupportState, positions: Collection<Int?>): CardInfo {
    if (state != SupportState.READABLE) return messageFor(state)
    if (positions.isEmpty() || positions.all { it == null }) return messageFor(SupportState.UNKNOWN)
    val open = positions.count { isOpen(it) }
    val failed = positions.count { it == null }
    return when {
        failed > 0 -> CardInfo(
            if (open > 0) "열림 ${open}개 · 일부 확인 불가" else "일부 확인 불가",
            BadgeStatus.UNKNOWN,
            isMessage = true
        )
        open > 0 -> CardInfo("열림 ${open}개", BadgeStatus.WARN)
        else -> CardInfo("모두 닫힘", BadgeStatus.OK)
    }
}

private fun <T> valueInfo(reading: Reading<T>, format: (T) -> String): CardInfo {
    val value = reading.value
    return if (reading.state == SupportState.READABLE && value != null) CardInfo(format(value)) else messageFor(reading.state)
}

internal fun formatNumber(value: Float): String =
    NumberFormat.getInstance().apply { maximumFractionDigits = 1 }.format(value)

private fun turnSignalText(value: TurnSignalValue): String = when (value) {
    TurnSignalValue.OFF -> "꺼짐"
    TurnSignalValue.LEFT -> "왼쪽 켜짐"
    TurnSignalValue.RIGHT -> "오른쪽 켜짐"
}

/** 그림에 쓸 영역별 표시. 지원하지 않거나 읽지 못한 영역은 UNKNOWN(점선). */
private fun doorMarks(snapshot: VehicleSnapshot?): Map<DoorValue, DoorMark> =
    DoorValue.entries.associateWith { area ->
        if (snapshot == null || snapshot.doorsState != SupportState.READABLE || !snapshot.doors.containsKey(area)) {
            DoorMark.UNKNOWN
        } else when (val position = snapshot.doors[area]) {
            null -> DoorMark.UNKNOWN
            else -> if (isOpen(position)) DoorMark.OPEN else DoorMark.CLOSED
        }
    }

private fun openDoorLabel(snapshot: VehicleSnapshot?): String? {
    val open = DoorValue.entries.filter { snapshot?.doors?.get(it).let(::isOpen) && snapshot?.doorsState == SupportState.READABLE }
    return when {
        open.isEmpty() -> null
        open.size == 1 -> "${DoorNames.getValue(open.first())} 열림"
        else -> "${DoorNames.getValue(open.first())} 외 ${open.size - 1}곳 열림"
    }
}

/**
 * SC-250 차량 상태 기록. 읽기 전용(get 호출만, 리스너 없음)이다. 값은 SC-200에서 읽은 것을 보여 주고 [다시 읽기]로 갱신한다.
 * 위치, 날씨 카드는 Phase 7~8, 11에서 만든다(이번에는 보이지 않는다).
 */
@Composable
fun VehicleStateScreen(
    session: AccidentSession?,
    isReading: Boolean,
    onReread: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PitCrewTheme.colors
    val snapshot = session?.vehicle

    // 들어왔는데 보여 줄 값이 없고 읽는 중도 아니면 한 번 읽는다.
    LaunchedEffect(Unit) {
        if (snapshot == null && !isReading) onReread()
    }

    val title = when {
        snapshot == null && isReading -> "차량 상태를 읽고 있어요"
        snapshot == null || !snapshot.hasAnyValue() -> "차량 상태를 확인하지 못했어요"
        else -> "차량 상태를 기록했어요"
    }
    val doors = if (snapshot == null) CardInfo("읽는 중이에요", isMessage = true)
    else areaInfo(snapshot.doorsState, snapshot.doors.values)
    val windows = if (snapshot == null) CardInfo("읽는 중이에요", isMessage = true)
    else areaInfo(snapshot.windowsState, snapshot.windows.values)
    val battery = if (snapshot == null) CardInfo("읽는 중이에요", isMessage = true)
    else valueInfo(snapshot.battery) { "$it%" }
    val odometer = if (snapshot == null) CardInfo("읽는 중이에요", isMessage = true)
    else valueInfo(snapshot.odometer) { "${formatNumber(it)}km" }
    val turn = if (snapshot == null) CardInfo("읽는 중이에요", isMessage = true)
    else valueInfo(snapshot.turnSignal, ::turnSignalText)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = PitCrewDimens.safeMargin,
                end = PitCrewDimens.safeMargin,
                top = PitCrewDimens.stepScreenTopGap,
                bottom = PitCrewDimens.stepScreenBottomGap
            )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = PitCrewTheme.typography.screenTitle,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                if (isReading) "읽는 중…" else snapshot?.let { "기록 시각 ${formatClock(it.readAtMillis)}" }.orEmpty(),
                style = PitCrewTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )
            Spacer(Modifier.width(PitCrewDimens.touchGap))
            TopBarButton(
                text = "다시 읽기",
                icon = PitCrewIcons.Refresh,
                onClick = onReread,
                enabled = !isReading
            )
        }
        Spacer(Modifier.height(PitCrewDimens.touchGap))
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)
        ) {
            // 왼쪽: 차량 윗면 그림과 열린 곳 알림
            PitCrewCard(modifier = Modifier.width(PitCrewDimens.carPanelWidth).fillMaxHeight(), padding = 16.dp) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CarTopView(marks = doorMarks(snapshot), modifier = Modifier.fillMaxSize())
                    openDoorLabel(snapshot)?.let { label ->
                        StatusBadge(BadgeStatus.WARN, label = label, modifier = Modifier.align(Alignment.BottomStart))
                    }
                }
            }
            // 오른쪽: 상태 카드 5개
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)
            ) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)
                ) {
                    InfoStatusCard(PitCrewIcons.DoorCard, "문", doors, Modifier.weight(1f).fillMaxHeight())
                    InfoStatusCard(PitCrewIcons.WindowCard, "창문", windows, Modifier.weight(1f).fillMaxHeight())
                }
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)
                ) {
                    InfoStatusCard(PitCrewIcons.Battery, "배터리", battery, Modifier.weight(1f).fillMaxHeight())
                    InfoStatusCard(PitCrewIcons.Gauge, "주행 거리", odometer, Modifier.weight(1f).fillMaxHeight())
                }
                InfoStatusCard(PitCrewIcons.TurnSignal, "방향지시등", turn, Modifier.weight(1f).fillMaxWidth())
                // 위치를 허용한 경우에만 보이는 점선 카드(좌표와 주소는 보이지 않고 상태만, 주소는 Phase 8)
                locationUi(session)?.let { ui ->
                    val info = when (ui) {
                        LocationUi.PENDING -> CardInfo("위치 확인 중이에요", isMessage = true)
                        LocationUi.OK -> CardInfo("위치 확인됨", isMessage = true)
                        LocationUi.UNAVAILABLE -> CardInfo("위치를 확인하지 못했어요", BadgeStatus.UNKNOWN, isMessage = true)
                    }
                    StatusCard(
                        icon = PitCrewIcons.Pin,
                        label = "사고 위치(주소)",
                        value = info.value,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        badge = info.badge,
                        isMessage = true,
                        dashed = true
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoStatusCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    info: CardInfo,
    modifier: Modifier
) {
    StatusCard(icon = icon, label = label, value = info.value, modifier = modifier, badge = info.badge, isMessage = info.isMessage)
}
