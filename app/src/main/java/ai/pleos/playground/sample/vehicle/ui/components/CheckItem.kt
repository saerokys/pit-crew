package ai.pleos.playground.sample.vehicle.ui.components

import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewShapes
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
 * [compact]는 항목이 5개인 SC-230용: 행 최소 높이 112, 글자 36 Bold.
 * 미체크: surface 배경, divider 테두리의 빈 체크박스.
 * 체크: accent 테두리, accent로 채운 체크박스와 체크 표시.
 * [dense]는 항목이 6개인 SC-290용: 행 최소 높이 96(터치 영역 최소), 글자 36 Bold.
 * [dense]이면서 [note]가 있으면 위아래 여백을 4로 줄여 행 높이가 96이 되게 한다.
 * [enabled]가 거짓이면 고를 수 없는 항목(회색 글자, 체크 불가)이고 [note]에 이유를 보여 준다.
 */
@Composable
fun CheckItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    dense: Boolean = false,
    enabled: Boolean = true,
    note: String? = null
) {
    val colors = PitCrewTheme.colors
    val boxShape = RoundedCornerShape(PitCrewDimens.checkboxCorner)
    val line = if (checked && enabled) colors.accent else colors.divider
    val minHeight = when {
        dense -> PitCrewDimens.touchMin
        compact -> PitCrewDimens.checkItemCompactMinHeight
        else -> PitCrewDimens.checkItemMinHeight
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .background(colors.surface, PitCrewShapes.card)
            .border(BorderStroke(PitCrewDimens.cardBorder, line), PitCrewShapes.card)
            .toggleable(enabled = enabled, value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            // dense에 note(고를 수 없는 이유)가 붙으면 두 줄이 되므로 위아래 여백을 4로 줄여 행 높이를 96으로 맞춘다.
            .padding(horizontal = 32.dp, vertical = if (dense) (if (note != null) 4.dp else 16.dp) else if (compact) 24.dp else 40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(PitCrewDimens.checkboxSize)
                .background(if (checked && enabled) colors.accent else colors.surface, boxShape)
                .border(BorderStroke(PitCrewDimens.cardBorder, line), boxShape),
            contentAlignment = Alignment.Center
        ) {
            if (checked && enabled) {
                Icon(
                    PitCrewIcons.Check,
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(PitCrewDimens.iconSize)
                )
            }
        }
        Spacer(Modifier.width(24.dp))
        Column {
            Text(
                text,
                style = if (compact || dense) PitCrewTheme.typography.cardTitle else PitCrewTheme.typography.checkItem,
                color = if (enabled) colors.textPrimary else colors.textDisabled
            )
            if (note != null) {
                Text(note, style = PitCrewTheme.typography.caption, color = colors.textDisabled)
            }
        }
    }
}
