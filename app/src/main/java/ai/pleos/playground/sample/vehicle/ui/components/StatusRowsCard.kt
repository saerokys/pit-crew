package ai.pleos.playground.sample.vehicle.ui.components

import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** [StatusRowsCard]의 한 행: 아이콘 + 이름 + 상태 배지(아이콘과 글자). */
data class StatusRow(val icon: ImageVector, val label: String, val status: BadgeStatus, val statusLabel: String)

/** 카드 제목 아래에 "아이콘 + 이름 ... 상태 배지" 행을 세로로 나열하고 행 사이에 divider를 둔다(SC-400). */
@Composable
fun StatusRowsCard(title: String, rows: List<StatusRow>, modifier: Modifier = Modifier) {
    val colors = PitCrewTheme.colors
    PitCrewCard(modifier = modifier, padding = 24.dp) {
        Text(
            title,
            style = PitCrewTheme.typography.badge,
            color = colors.textSecondary
        )
        Spacer(Modifier.height(8.dp))
        Column {
            rows.forEachIndexed { index, row ->
                if (index > 0) Box(Modifier.fillMaxWidth().height(PitCrewDimens.cardBorder).background(colors.divider))
                Row(
                    modifier = Modifier.fillMaxWidth().height(PitCrewDimens.statusRowHeight),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(row.icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(PitCrewDimens.iconSize))
                    Spacer(Modifier.width(16.dp))
                    Text(
                        row.label,
                        style = PitCrewTheme.typography.checkItem,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatusBadge(row.status, label = row.statusLabel)
                }
            }
        }
    }
}
