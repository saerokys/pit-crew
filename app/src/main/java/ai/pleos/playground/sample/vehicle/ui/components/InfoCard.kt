package ai.pleos.playground.sample.vehicle.ui.components

import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * 설명 카드(예: "왜 필요한가요?"). 아이콘, 제목(Bold), 본문, 그리고 "참고용" 표시([note])를 보여 준다.
 * 사고 모드 단계 화면이 오른쪽 열에 공통으로 쓴다.
 */
@Composable
fun InfoCard(
    title: String,
    body: String,
    note: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = PitCrewIcons.HelpCircle
) {
    val colors = PitCrewTheme.colors
    PitCrewCard(modifier = modifier) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(PitCrewDimens.infoIconSize))
        Spacer(Modifier.height(16.dp))
        Text(title, style = PitCrewTheme.typography.cardTitle, color = colors.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text(body, style = PitCrewTheme.typography.body, color = colors.textSecondary)
        Spacer(Modifier.height(12.dp))
        Text(note, style = PitCrewTheme.typography.caption, color = colors.textSecondary)
    }
}
