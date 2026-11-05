package ai.pleos.playground.sample.vehicle.ui.components

import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewShapes
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/**
 * 카드: surface 배경, 모서리 20, divider 테두리 2, 안쪽 여백 32.
 * [dashed] = 사용자가 허용한 경우만 보이는 카드(위치, 날씨)의 점선 테두리.
 * [onClick]이 있으면 카드 전체가 터치 영역이다(높이 96 이상은 호출하는 쪽이 지킨다).
 */
@Composable
fun PitCrewCard(
    modifier: Modifier = Modifier,
    dashed: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = PitCrewTheme.colors
    var m = modifier.background(colors.surface, PitCrewShapes.card)
    m = if (dashed) {
        m.dashedBorder(colors.divider, PitCrewDimens.cardBorder, PitCrewDimens.cornerRadius)
    } else {
        m.border(BorderStroke(PitCrewDimens.cardBorder, colors.divider), PitCrewShapes.card)
    }
    if (onClick != null) m = m.clickable(role = Role.Button, onClick = onClick)
    Column(modifier = m.padding(PitCrewDimens.cardPadding)) { content() }
}

/** 카드 안의 제목(textPrimary, Bold)과 설명(textSecondary). */
@Composable
fun PitCrewCardText(title: String, description: String? = null) {
    Text(title, style = PitCrewTheme.typography.title, color = PitCrewTheme.colors.textPrimary)
    if (description != null) {
        Text(description, style = PitCrewTheme.typography.caption, color = PitCrewTheme.colors.textSecondary)
    }
}
