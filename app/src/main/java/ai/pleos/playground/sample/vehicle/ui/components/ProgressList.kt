package ai.pleos.playground.sample.vehicle.ui.components

import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewShapes
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** 사고 모드 7단계 이름(시안의 짧은 이름. 5단계는 앱이 연락을 대신하는 것처럼 읽히지 않게 "상대방 확인"). */
val AccidentStepLabels = listOf(
    "정차 확인",
    "안전 확보",
    "부상 확인",
    "상황 기록",
    "상대방 확인",
    "차량 상태",
    "마무리"
)

/**
 * 진행 목록(7단계). 완료는 accent 원 + 체크, 현재는 accent 원 + 번호 + 강조 행, 대기는 빈 원 + 번호.
 * 색만으로 구분하지 않고 아이콘/번호/굵기를 함께 쓴다. 터치 대상이 아니다.
 */
@Composable
fun ProgressList(currentStep: Int, modifier: Modifier = Modifier) {
    val colors = PitCrewTheme.colors
    Column(
        modifier = modifier
            .width(PitCrewDimens.progressListWidth)
            .background(colors.surface, PitCrewShapes.card)
            .border(BorderStroke(PitCrewDimens.cardBorder, colors.divider), PitCrewShapes.card)
            .padding(PitCrewDimens.progressListPadding),
        verticalArrangement = Arrangement.spacedBy(PitCrewDimens.progressRowGap)
    ) {
        AccidentStepLabels.forEachIndexed { index, label ->
            val number = index + 1
            ProgressRow(number = number, label = label, state = when {
                number < currentStep -> StepState.DONE
                number == currentStep -> StepState.CURRENT
                else -> StepState.WAITING
            })
        }
    }
}

private enum class StepState(val description: String) {
    DONE("완료"),
    CURRENT("현재"),
    WAITING("대기")
}

@Composable
private fun ProgressRow(number: Int, label: String, state: StepState) {
    val colors = PitCrewTheme.colors
    val current = state == StepState.CURRENT
    var rowModifier = Modifier
        .fillMaxWidth()
        .height(PitCrewDimens.progressRowHeight)
        .semantics { contentDescription = "${number}단계 $label ${state.description}" }
    if (current) {
        rowModifier = rowModifier
            .background(colors.surfaceSelected, PitCrewShapes.card)
            .border(BorderStroke(PitCrewDimens.cardBorder, colors.accent), PitCrewShapes.card)
    }
    Row(
        modifier = rowModifier.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val circle = Modifier.size(PitCrewDimens.progressCircle)
        when (state) {
            StepState.DONE -> Box(circle.background(colors.accent, CircleShape), contentAlignment = Alignment.Center) {
                Icon(PitCrewIcons.Check, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(32.dp))
            }
            StepState.CURRENT -> Box(circle.background(colors.accent, CircleShape), contentAlignment = Alignment.Center) {
                Text(number.toString(), style = PitCrewTheme.typography.caption.copy(fontWeight = FontWeight.Bold), color = colors.onAccent)
            }
            StepState.WAITING -> Box(
                circle.border(BorderStroke(PitCrewDimens.cardBorder, colors.divider), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(number.toString(), style = PitCrewTheme.typography.caption.copy(fontWeight = FontWeight.Bold), color = colors.textSecondary)
            }
        }
        Spacer(Modifier.width(16.dp))
        Text(
            label,
            style = if (current) PitCrewTheme.typography.caption.copy(fontWeight = FontWeight.Bold) else PitCrewTheme.typography.caption,
            color = when (state) {
                StepState.CURRENT -> colors.textPrimary
                StepState.DONE -> colors.textSecondary
                StepState.WAITING -> colors.textDisabled
            }
        )
    }
}
