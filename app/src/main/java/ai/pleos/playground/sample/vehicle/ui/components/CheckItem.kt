package ai.pleos.playground.sample.vehicle.ui.components

import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewShapes
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * 체크 항목. 한 줄 전체가 터치 영역(SC-210 시안: 행 높이 144, 글자 40 Bold).
 * 미체크: surface 배경, divider 테두리의 빈 체크박스.
 * 체크: accent 테두리, accent로 채운 체크박스와 체크 표시.
 */
@Composable
fun CheckItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PitCrewTheme.colors
    val boxShape = RoundedCornerShape(PitCrewDimens.checkboxCorner)
    val line = if (checked) colors.accent else colors.divider
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = PitCrewDimens.checkItemMinHeight)
            .background(colors.surface, PitCrewShapes.card)
            .border(BorderStroke(PitCrewDimens.cardBorder, line), PitCrewShapes.card)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(horizontal = 32.dp, vertical = 40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(PitCrewDimens.checkboxSize)
                .background(if (checked) colors.accent else colors.surface, boxShape)
                .border(BorderStroke(PitCrewDimens.cardBorder, line), boxShape),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    PitCrewIcons.Check,
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(PitCrewDimens.iconSize)
                )
            }
        }
        Spacer(Modifier.width(24.dp))
        Text(text, style = PitCrewTheme.typography.checkItem, color = colors.textPrimary)
    }
}
