package ai.pleos.playground.sample.vehicle.ui.components

import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewShapes
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * 스위치 그림(136×76 알약 + 지름 60 손잡이). 꺼짐은 divider 바탕에 textSecondary 손잡이,
 * 켜짐은 accent 바탕에 흰 손잡이, 쓸 수 없으면 회색(statusUnknownBg)과 textDisabled 손잡이이다.
 * 눌림 처리는 바깥 [PitCrewSwitchRow]가 한다(행 전체가 터치 영역).
 */
@Composable
fun PitCrewSwitch(checked: Boolean, enabled: Boolean = true, modifier: Modifier = Modifier) {
    val colors = PitCrewTheme.colors
    val track = when {
        !enabled -> colors.statusUnknownBg
        checked -> colors.accent
        else -> colors.divider
    }
    val knob = when {
        !enabled -> colors.textDisabled
        checked -> colors.textPrimary
        else -> colors.textSecondary
    }
    val padding = (PitCrewDimens.switchHeight - PitCrewDimens.switchKnob) / 2
    val travel = PitCrewDimens.switchWidth - PitCrewDimens.switchKnob - padding * 2
    val offsetX by animateDpAsState(if (checked) padding + travel else padding, label = "switch")
    Box(
        modifier = modifier
            .size(PitCrewDimens.switchWidth, PitCrewDimens.switchHeight)
            .background(track, PitCrewShapes.pill)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = offsetX)
                .size(PitCrewDimens.switchKnob)
                .background(knob, PitCrewShapes.pill)
        )
    }
}

/**
 * 스위치 행(SC-205): 아이콘, 제목(Bold), 설명, 오른쪽에 "켜짐/꺼짐" 글자와 스위치. 행 전체가 터치 영역(높이 134).
 * [available]이 false면 점선 테두리 회색 행으로 바꾸고 [stateLabel]에 "사용 불가"를 보여 준다(누를 수 없음).
 * 켜짐 여부는 색만이 아니라 글자로도 알린다.
 */
@Composable
fun PitCrewSwitchRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    available: Boolean = true
) {
    val colors = PitCrewTheme.colors
    val stateLabel = when {
        !available -> "사용 불가"
        checked -> "켜짐"
        else -> "꺼짐"
    }
    val titleColor = if (available) colors.textPrimary else colors.textDisabled
    val descColor = if (available) colors.textSecondary else colors.textDisabled
    val rowModifier = modifier.height(PitCrewDimens.consentRowHeight)
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(PitCrewDimens.consentRowHeight)
                .then(
                    if (available) Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
                    else Modifier
                )
                .padding(horizontal = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (available) colors.accent else colors.textDisabled, modifier = Modifier.size(PitCrewDimens.iconSize))
            Spacer(Modifier.width(32.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = PitCrewTheme.typography.checkItem, color = titleColor)
                Text(description, style = PitCrewTheme.typography.caption, color = descColor)
            }
            Text(
                stateLabel,
                style = PitCrewTheme.typography.body.copy(fontWeight = FontWeight.Bold),
                color = if (available) colors.textPrimary else colors.textDisabled,
                maxLines = 1,
                softWrap = false
            )
            Spacer(Modifier.width(24.dp))
            PitCrewSwitch(checked = checked && available, enabled = available)
        }
    }
    PitCrewCard(modifier = rowModifier, padding = 0.dp, dashed = !available) { content() }
}
