package ai.pleos.playground.sample.vehicle.ui.status

import ai.pleos.playground.sample.vehicle.session.formatClock
import ai.pleos.playground.sample.vehicle.ui.components.BadgeStatus
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewIcons
import ai.pleos.playground.sample.vehicle.ui.components.StatusRow
import ai.pleos.playground.sample.vehicle.ui.components.StatusRowsCard
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import ai.pleos.playground.sample.vehicle.vehicle.SupportState
import ai.pleos.playground.sample.vehicle.vehicle.VehicleFeature
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private fun badgeFor(state: SupportState): BadgeStatus = when (state) {
    SupportState.READABLE -> BadgeStatus.OK
    SupportState.UNSUPPORTED, SupportState.UNKNOWN -> BadgeStatus.UNKNOWN
}

/** 행 이름은 시안대로 짧게("문 열림" 대신 "문"). */
private fun rowIcon(feature: VehicleFeature) = when (feature) {
    VehicleFeature.DOOR -> PitCrewIcons.DoorCard
    VehicleFeature.WINDOW -> PitCrewIcons.WindowCard
    VehicleFeature.BATTERY -> PitCrewIcons.Battery
    VehicleFeature.ODOMETER -> PitCrewIcons.Gauge
    VehicleFeature.TURN_SIGNAL -> PitCrewIcons.TurnSignal
}

/**
 * SC-400 차량 연동 상태. SC-000이 확인한 지원 여부(홈과 같은 [featureSupport])를 5행으로 보여 준다.
 * 값은 읽지 않는다. 위치, 음성, 길안내 행은 해당 Phase에서 추가한다.
 */
@Composable
fun LinkStatusScreen(
    featureSupport: Map<VehicleFeature, SupportState>,
    checkedAtMillis: Long?,
    modifier: Modifier = Modifier
) {
    val colors = PitCrewTheme.colors
    val rows = VehicleFeature.entries.map { feature ->
        val state = featureSupport[feature] ?: SupportState.UNKNOWN
        StatusRow(rowIcon(feature), feature.title, badgeFor(state), state.label)
    }
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
                "이 차량에서 읽을 수 있는 항목",
                style = PitCrewTheme.typography.screenTitle,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                "마지막 확인 ${checkedAtMillis?.let { formatClock(it) } ?: "확인 불가"}",
                style = PitCrewTheme.typography.badge,
                color = colors.textPrimary
            )
        }
        Spacer(Modifier.height(PitCrewDimens.touchGap))
        StatusRowsCard(title = "차량 값", rows = rows, modifier = Modifier.weight(1f).fillMaxWidth())
        Row(
            modifier = Modifier.fillMaxWidth().height(PitCrewDimens.iconSize + 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(PitCrewIcons.Info, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(PitCrewDimens.iconSize))
            Spacer(Modifier.width(16.dp))
            Text(
                "읽을 수 없는 값은 숨기지 않고 확인 불가로 보여 드려요",
                style = PitCrewTheme.typography.caption,
                color = colors.textSecondary
            )
        }
    }
}
