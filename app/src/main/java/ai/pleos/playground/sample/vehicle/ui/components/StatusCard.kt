package ai.pleos.playground.sample.vehicle.ui.components

import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * 상태 카드: 아이콘 + 라벨 + (선택) 상태 배지 + 값 한 줄.
 * 값이 없거나 오류면 호출하는 쪽이 회색 "확인 불가" 배지와 안내 문구를 넘긴다(아이콘+글자, 색만으로 구분하지 않음).
 * [isMessage]가 참이면 값 줄을 조금 작은 글자(36 Bold)로 보여 준다(안내 문구용).
 */
@Composable
fun StatusCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    badge: BadgeStatus? = null,
    isMessage: Boolean = false
) {
    val colors = PitCrewTheme.colors
    PitCrewCard(modifier = modifier, padding = 24.dp) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(PitCrewDimens.iconSize))
                Spacer(Modifier.width(12.dp))
                Text(label, style = PitCrewTheme.typography.caption, color = colors.textSecondary, modifier = Modifier.weight(1f))
                if (badge != null) StatusBadge(badge)
            }
            Text(
                value,
                style = if (isMessage) PitCrewTheme.typography.cardTitle else PitCrewTheme.typography.title,
                color = colors.textPrimary
            )
        }
    }
}
