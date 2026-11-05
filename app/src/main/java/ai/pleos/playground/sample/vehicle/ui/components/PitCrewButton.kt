package ai.pleos.playground.sample.vehicle.ui.components

import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewShapes
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * 큰 버튼. 높이 96 이상, 글자 36 Bold.
 *
 * 상태(tokens.md 5절): 기본 / 눌림 / 포커스 / 비활성 / 선택됨.
 * [filled] = 채운 주요 버튼(accent 배경, onAccent 글자). **한 화면에 1개만** 쓴다.
 */
@Composable
fun PitCrewButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    filled: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null
) {
    val colors = PitCrewTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()

    val container = when {
        !enabled -> colors.background
        filled -> colors.accent
        pressed -> colors.surfaceSelected
        else -> colors.surface
    }
    val contentColor = when {
        !enabled -> colors.textDisabled
        filled -> colors.onAccent
        else -> colors.textPrimary
    }
    val borderWidth = if (focused && enabled) PitCrewDimens.focusBorder else PitCrewDimens.cardBorder
    val borderColor = when {
        focused && enabled -> colors.textPrimary
        selected -> colors.accent
        else -> colors.divider
    }

    var chrome = modifier
        .heightIn(min = PitCrewDimens.buttonHeight)
        .alpha(if (filled && pressed) 0.85f else 1f)
        .background(container, PitCrewShapes.button)
    chrome = if (!enabled) {
        chrome.dashedBorder(colors.divider, PitCrewDimens.cardBorder, PitCrewDimens.cornerRadius)
    } else if (filled && !focused) {
        chrome
    } else {
        chrome.border(BorderStroke(borderWidth, borderColor), PitCrewShapes.button)
    }

    Row(
        modifier = chrome
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 32.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val leading = if (selected && leadingIcon == null) PitCrewIcons.Check else leadingIcon
        if (leading != null) {
            Icon(leading, contentDescription = null, tint = contentColor, modifier = Modifier.size(PitCrewDimens.iconSize))
            Spacer(Modifier.width(12.dp))
        }
        Text(text = text, style = PitCrewTheme.typography.button, color = contentColor)
        if (trailingIcon != null) {
            Spacer(Modifier.width(12.dp))
            Icon(trailingIcon, contentDescription = null, tint = contentColor, modifier = Modifier.size(PitCrewDimens.iconSize))
        }
    }
}
