package ai.pleos.playground.sample.vehicle.ui.precheck

import ai.pleos.playground.sample.vehicle.ui.components.BadgeStatus
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewIcons
import ai.pleos.playground.sample.vehicle.ui.components.StatusBadge
import ai.pleos.playground.sample.vehicle.ui.components.StatusCard
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import ai.pleos.playground.sample.vehicle.vehicle.VehicleSnapshot
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * SC-300 출발 전 점검. 들어오면 차량 값을 한 번 읽고([onCheck]) [다시 확인]으로 다시 읽는다.
 * 사고 모드 세션과 관계없다(세션을 만들거나 바꾸지 않는다). 읽기 전용이다.
 */
@Composable
fun PreDepartureScreen(
    snapshot: VehicleSnapshot?,
    isChecking: Boolean,
    onCheck: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PitCrewTheme.colors
    LaunchedEffect(Unit) { onCheck() }

    val checking = snapshot == null && isChecking
    val result = evaluatePreCheck(snapshot)

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
                "출발 전 점검",
                style = PitCrewTheme.typography.screenTitle,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            StatusBadge(
                status = if (checking) BadgeStatus.UNKNOWN else result.summaryStatus,
                label = if (checking) "확인 중…" else result.summaryText,
                height = PitCrewDimens.summaryBadgeHeight
            )
        }
        Spacer(Modifier.height(PitCrewDimens.touchGap))
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)
        ) {
            result.items.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)
                ) {
                    pair.forEach { item ->
                        PreCheckCard(item, checking, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

@Composable
private fun PreCheckCard(item: PreCheckItem, checking: Boolean, modifier: Modifier) {
    val icon = when (item.kind) {
        PreCheckKind.DOOR -> PitCrewIcons.DoorCard
        PreCheckKind.WINDOW -> PitCrewIcons.WindowCard
        PreCheckKind.BATTERY -> PitCrewIcons.Battery
        PreCheckKind.TURN_SIGNAL -> PitCrewIcons.TurnSignal
    }
    if (checking) {
        StatusCard(icon = icon, label = item.label, value = "확인 중이에요", modifier = modifier, isMessage = true)
    } else {
        StatusCard(
            icon = icon,
            label = item.label,
            value = item.value,
            modifier = modifier,
            badge = item.badge,
            isMessage = item.compact,
            largeValue = true,
            highlight = item.needsAttention,
            hint = item.hint
        )
    }
}
