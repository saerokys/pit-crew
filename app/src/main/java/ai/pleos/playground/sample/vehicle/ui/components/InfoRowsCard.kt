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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** [InfoRowsCard]의 한 행: 아이콘, 작은 라벨, 굵은 값. */
data class InfoRow(val icon: ImageVector, val label: String, val value: String, val note: String? = null)

/** 제목과 행 목록을 구분선으로 나눠 보여 주는 카드(예: SC-221 "말할 내용"). */
@Composable
fun InfoRowsCard(
    title: String,
    rows: List<InfoRow>,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    val colors = PitCrewTheme.colors
    PitCrewCard(modifier = modifier) {
        Text(title, style = PitCrewTheme.typography.title, color = colors.textPrimary)
        Spacer(Modifier.height(16.dp))
        Divider()
        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(row.icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(PitCrewDimens.iconSize))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(row.label, style = PitCrewTheme.typography.caption, color = colors.textSecondary)
                    Text(row.value, style = PitCrewTheme.typography.cardTitle, color = colors.textPrimary)
                    if (row.note != null) Text(row.note, style = PitCrewTheme.typography.caption, color = colors.textSecondary)
                }
            }
            if (index < rows.lastIndex) Divider()
        }
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(PitCrewDimens.cardBorder)
            .background(PitCrewTheme.colors.divider)
    )
}
