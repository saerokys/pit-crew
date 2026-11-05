package ai.pleos.playground.sample.vehicle.ui.components

import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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

/**
 * 긴급 안내 배너(SC-221). statusUrgent 배경에 흰 글자(titleUrgent), 아이콘과 글자를 함께 쓴다.
 * 상단 바 바로 아래 전체 폭에 놓는다.
 */
@Composable
fun EmergencyBanner(text: String, modifier: Modifier = Modifier) {
    val colors = PitCrewTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PitCrewDimens.emergencyBannerHeight)
            .background(colors.statusUrgent)
            .padding(horizontal = PitCrewDimens.safeMargin),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(PitCrewIcons.AlertOctagon, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(64.dp))
        Spacer(Modifier.width(24.dp))
        Text(text, style = PitCrewTheme.typography.titleUrgent, color = colors.textPrimary)
    }
}
