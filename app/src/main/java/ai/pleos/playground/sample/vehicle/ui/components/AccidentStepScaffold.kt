package ai.pleos.playground.sample.vehicle.ui.components

import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 사고 모드 단계 화면(SC-210 템플릿)의 본문 틀. 상단 바와 하단 바는 바깥(PitCrewNavHost)이 그린다.
 * 왼쪽: 진행 목록 + [사고 모드 나가기], 가운데: 제목 + [content], 오른쪽(선택): [right].
 * SC-220, 230, 240이 같은 틀을 쓴다.
 */
@Composable
fun AccidentStepScaffold(
    currentStep: Int,
    title: String,
    onExitRequest: () -> Unit,
    modifier: Modifier = Modifier,
    right: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = PitCrewDimens.safeMargin,
                end = PitCrewDimens.safeMargin,
                top = PitCrewDimens.stepScreenTopGap,
                bottom = PitCrewDimens.stepScreenBottomGap
            ),
        horizontalArrangement = Arrangement.spacedBy(PitCrewDimens.stepColumnGap)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)) {
            ProgressList(currentStep = currentStep)
            PitCrewButton(
                text = "사고 모드 나가기",
                onClick = onExitRequest,
                modifier = Modifier.width(PitCrewDimens.progressListWidth)
            )
        }
        Column(modifier = Modifier.weight(1f).fillMaxSize()) {
            Text(title, style = PitCrewTheme.typography.screenTitle, color = PitCrewTheme.colors.textPrimary)
            content()
        }
        if (right != null) {
            Column(modifier = Modifier.width(PitCrewDimens.infoCardWidth).fillMaxSize()) { right() }
        }
    }
}
