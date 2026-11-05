package ai.pleos.playground.sample.vehicle.ui.components

import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewShapes
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class BadgeStatus(val defaultLabel: String, val icon: ImageVector) {
    OK("정상", PitCrewIcons.CheckCircle),
    WARN("주의", PitCrewIcons.Warning),
    URGENT("긴급", PitCrewIcons.ErrorCircle),
    UNKNOWN("확인 불가", PitCrewIcons.HelpCircle)
}

/** 상태 배지(알약, 높이 72). 색만으로 구분하지 않고 항상 아이콘과 글자를 함께 쓴다. */
@Composable
fun StatusBadge(
    status: BadgeStatus,
    modifier: Modifier = Modifier,
    label: String = status.defaultLabel
) {
    val c = PitCrewTheme.colors
    val bg = when (status) {
        BadgeStatus.OK -> c.statusOk
        BadgeStatus.WARN -> c.statusWarn
        BadgeStatus.URGENT -> c.statusUrgent
        BadgeStatus.UNKNOWN -> c.statusUnknownBg
    }
    val fg = when (status) {
        BadgeStatus.OK, BadgeStatus.WARN -> c.onAccent
        BadgeStatus.URGENT -> c.textPrimary
        BadgeStatus.UNKNOWN -> c.statusUnknownText
    }
    Row(
        modifier = modifier
            .height(PitCrewDimens.badgeHeight)
            .background(bg, PitCrewShapes.pill)
            .padding(start = 12.dp, end = 28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(status.icon, contentDescription = null, tint = fg, modifier = Modifier.size(PitCrewDimens.iconSize))
        Spacer(Modifier.width(8.dp))
        Text(label, style = PitCrewTheme.typography.badge, color = fg)
    }
}
