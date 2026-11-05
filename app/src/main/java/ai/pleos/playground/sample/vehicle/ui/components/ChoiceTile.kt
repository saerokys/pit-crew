package ai.pleos.playground.sample.vehicle.ui.components

import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewShapes
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 큰 선택 타일(SC-220의 없어요/있어요/잘 모르겠어요). 색 테두리 4, 큰 아이콘, 큰 글자.
 * [accent]는 테두리, 아이콘, 글자 색이다(tokens의 상태 색만 넘긴다).
 * 선택된 타일은 배경이 surfaceSelected로 바뀌고 "✓ 선택됨" 글자가 붙는다(색만으로 구분하지 않음).
 * 한 번 누르면 바로 다음으로 가는 선택 화면용이라 채운 버튼이 아니다.
 */
@Composable
fun ChoiceTile(
    label: String,
    icon: ImageVector,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PitCrewTheme.colors
    Column(
        modifier = modifier
            .background(if (selected) colors.surfaceSelected else colors.surface, PitCrewShapes.card)
            .border(BorderStroke(PitCrewDimens.choiceTileBorder, accent), PitCrewShapes.card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(PitCrewDimens.touchGap),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(104.dp))
        Spacer(Modifier.height(PitCrewDimens.touchGap))
        Text(label, style = PitCrewTheme.typography.tileLabel, color = accent)
        if (selected) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PitCrewIcons.Check, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(PitCrewDimens.iconSize))
                Spacer(Modifier.width(8.dp))
                Text(
                    "선택됨",
                    style = PitCrewTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            }
        }
    }
}
