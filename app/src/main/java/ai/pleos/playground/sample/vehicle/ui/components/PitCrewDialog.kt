package ai.pleos.playground.sample.vehicle.ui.components

import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** 대화상자 버튼. 채운 버튼은 쓰지 않는다(뒤 화면에 이미 채운 버튼이 있을 수 있다). */
data class DialogButton(val text: String, val onClick: () -> Unit)

/**
 * 확인 대화상자. 카드 모양(surface, divider 테두리)에 큰 버튼 1~2개.
 * 바깥을 누르거나 뒤로 가기를 누르면 [onDismiss]를 부른다.
 */
@Composable
fun PitCrewDialog(
    title: String,
    body: String,
    buttons: List<DialogButton>,
    onDismiss: () -> Unit,
    note: String? = null
) {
    val colors = PitCrewTheme.colors
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        PitCrewCard(modifier = Modifier.width(880.dp)) {
            Text(title, style = PitCrewTheme.typography.title, color = colors.textPrimary)
            Spacer(Modifier.height(16.dp))
            Text(body, style = PitCrewTheme.typography.body, color = colors.textSecondary)
            if (note != null) {
                Text(note, style = PitCrewTheme.typography.caption, color = colors.textSecondary)
            }
            Spacer(Modifier.height(PitCrewDimens.cardPadding))
            Row(horizontalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)) {
                buttons.forEach { button ->
                    PitCrewButton(
                        text = button.text,
                        onClick = button.onClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
