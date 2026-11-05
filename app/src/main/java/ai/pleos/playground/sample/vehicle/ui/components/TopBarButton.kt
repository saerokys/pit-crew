package ai.pleos.playground.sample.vehicle.ui.components

import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewShapes
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * 상단 바 오른쪽의 작은 버튼(예: [소리로 듣기]). 높이 96 이상, 글자 28 Bold.
 * 비활성은 점선 테두리와 textDisabled이다(아이콘과 글자가 그대로 있어 색만으로 구분하지 않는다).
 */
@Composable
fun TopBarButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = PitCrewTheme.colors
    val content = if (enabled) colors.textPrimary else colors.textDisabled
    var m = modifier
        .widthIn(min = PitCrewDimens.topBarButtonMinWidth)
        .heightIn(min = PitCrewDimens.touchMin)
        .background(if (enabled) colors.surface else colors.background, PitCrewShapes.button)
    m = if (enabled) {
        m.border(BorderStroke(PitCrewDimens.cardBorder, colors.accent), PitCrewShapes.button)
            .clickable(role = Role.Button, onClick = onClick)
    } else {
        m.dashedBorder(colors.divider, PitCrewDimens.cardBorder, PitCrewDimens.cornerRadius)
    }
    Row(
        modifier = m.padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(PitCrewDimens.iconSize))
        Spacer(Modifier.width(8.dp))
        Text(text, style = PitCrewTheme.typography.badge, color = content)
    }
}
