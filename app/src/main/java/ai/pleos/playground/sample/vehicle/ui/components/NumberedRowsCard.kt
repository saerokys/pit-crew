package ai.pleos.playground.sample.vehicle.ui.components

import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** [NumberedRowsCard]의 한 행: 번호, 라벨, 값 여러 줄. */
data class NumberedRow(val label: String, val lines: List<String>)

/**
 * 번호 행 카드(예: SC-260 "보험사에 말할 순서"). 번호 원은 accent 테두리, 라벨 28, 값은 36 Bold로 줄마다 보여 준다.
 * 내용이 넘치면 카드 안에서 세로로 스크롤된다.
 */
@Composable
fun NumberedRowsCard(rows: List<NumberedRow>, modifier: Modifier = Modifier) {
    val colors = PitCrewTheme.colors
    PitCrewCard(modifier = modifier, padding = 24.dp) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            rows.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(PitCrewDimens.numberCircle)
                            .border(BorderStroke(PitCrewDimens.cardBorder, colors.accent), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text((index + 1).toString(), style = PitCrewTheme.typography.cardTitle, color = colors.textPrimary)
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        row.label,
                        style = PitCrewTheme.typography.caption,
                        color = colors.textSecondary,
                        modifier = Modifier.width(150.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        row.lines.forEach { line ->
                            Text(line, style = PitCrewTheme.typography.cardTitle, color = colors.textPrimary)
                        }
                    }
                }
                if (index < rows.lastIndex) {
                    Box(Modifier.fillMaxWidth().height(PitCrewDimens.cardBorder).background(colors.divider))
                }
            }
        }
    }
}
