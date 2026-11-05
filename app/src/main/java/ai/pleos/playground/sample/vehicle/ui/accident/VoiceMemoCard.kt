package ai.pleos.playground.sample.vehicle.ui.accident

import ai.pleos.playground.sample.vehicle.ui.components.PitCrewButton
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewCard
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewIcons
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * SC-230 음성 메모 카드. Phase 11 전에는 회색 점선 비활성이다(음성 인식은 아직 만들지 않았다).
 * Phase 11에서 이 자리에 [말하기] -> 듣는 중 -> 인식 결과 확인 -> [저장] [다시 말하기]를 넣는다.
 */
@Composable
fun VoiceMemoCard(modifier: Modifier = Modifier) {
    val colors = PitCrewTheme.colors
    PitCrewCard(modifier = modifier, dashed = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PitCrewIcons.Mic, contentDescription = null, tint = colors.textDisabled, modifier = Modifier.size(PitCrewDimens.iconSize))
            Spacer(Modifier.width(12.dp))
            Text("말로 메모 남기기", style = PitCrewTheme.typography.cardTitle, color = colors.textDisabled)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "음성 사용이 꺼져 있거나 사용할 수 없어요",
            style = PitCrewTheme.typography.body,
            color = colors.textDisabled
        )
        Spacer(Modifier.height(PitCrewDimens.cardPadding))
        PitCrewButton(
            text = "말하기",
            onClick = {},
            enabled = false,
            leadingIcon = PitCrewIcons.Mic,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
