package ai.pleos.playground.sample.vehicle.ui.accident

import ai.pleos.playground.sample.vehicle.session.AccidentSession
import ai.pleos.playground.sample.vehicle.session.SafetyChecklist
import ai.pleos.playground.sample.vehicle.session.formatClock
import ai.pleos.playground.sample.vehicle.ui.components.AccidentStepScaffold
import ai.pleos.playground.sample.vehicle.ui.components.CheckItem
import ai.pleos.playground.sample.vehicle.ui.components.InfoCard
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewCard
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewIcons
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewShapes
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * SC-200 정차 확인. 정차 여부는 속도·기어를 읽지 않고 사용자 확인으로만 판단한다(규칙 7).
 * 배치는 SC-200.png, 문구와 동작은 SPEC. [session]이 있을 때만 시작 시각을 보여 준다.
 */
@Composable
fun StopCheckScreen(
    session: AccidentSession?,
    consentLine: String,
    onStopped: () -> Unit,
    onNotYet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PitCrewTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = PitCrewDimens.safeMargin)
    ) {
        Spacer(Modifier.height(PitCrewDimens.stepScreenTopGap))
        Text("안전한 곳에 차를 세우셨나요?", style = PitCrewTheme.typography.screenTitle, color = colors.textPrimary)
        Spacer(Modifier.height(PitCrewDimens.stepColumnGap))
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)
        ) {
            // 채운 포인트 카드는 이 화면에서 이것 하나뿐이다.
            Column(
                modifier = Modifier
                    .weight(1.35f)
                    .fillMaxHeight()
                    .background(colors.accent, PitCrewShapes.card)
                    .clickable(role = Role.Button, onClick = onStopped)
                    .padding(PitCrewDimens.homeCardPadding),
                verticalArrangement = Arrangement.Bottom
            ) {
                Icon(PitCrewIcons.CheckCircle, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(PitCrewDimens.heroIconSize))
                Spacer(Modifier.height(PitCrewDimens.touchGap))
                Text("네, 세웠어요", style = PitCrewTheme.typography.heroTitle, color = colors.onAccent)
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)
            ) {
                PitCrewCard(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    onClick = onNotYet,
                    padding = PitCrewDimens.homeCardPadding
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
                        Icon(PitCrewIcons.ArrowsRight, contentDescription = null, tint = colors.accent, modifier = Modifier.size(PitCrewDimens.heroIconSize / 2))
                        Spacer(Modifier.height(PitCrewDimens.touchGap))
                        Text("아직이에요", style = PitCrewTheme.typography.title, color = colors.textPrimary)
                    }
                }
                // 시안의 상시 경고 카드(문구는 시안 그대로)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 168.dp)
                        .background(colors.surfaceSelected, PitCrewShapes.card)
                        .border(BorderStroke(PitCrewDimens.cardBorder, colors.divider), PitCrewShapes.card)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(PitCrewIcons.Warning, contentDescription = null, tint = colors.accent, modifier = Modifier.size(PitCrewDimens.iconSize))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("아직 움직이고 있다면", style = PitCrewTheme.typography.caption, color = colors.textSecondary)
                        Text(
                            "안전한 곳에 차를 세운 뒤 다시 시작하세요",
                            style = PitCrewTheme.typography.body.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(PitCrewDimens.captionRowHeight),
            contentAlignment = Alignment.CenterStart
        ) {
            // 시작 시각은 세션이 있을 때만, 동의 상태(SC-205에서 정한 값)는 항상 한 줄로 보여 준다.
            Text(
                listOfNotNull(session?.let { "사고 모드 시작 ${formatClock(it.startedAtMillis)}" }, consentLine).joinToString(" · "),
                style = PitCrewTheme.typography.caption,
                color = colors.textSecondary
            )
        }
    }
}

/** SC-210 안전 확보(체크리스트 템플릿). 체크는 세션에 저장되어 [이전]으로 갔다 와도 유지된다. */
@Composable
fun SafetyCheckScreen(
    session: AccidentSession?,
    onCheckedChange: (id: String, checked: Boolean) -> Unit,
    onExitRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    AccidentStepScaffold(
        currentStep = 2,
        title = AccidentTexts.SAFETY_TITLE,
        onExitRequest = onExitRequest,
        modifier = modifier,
        right = {
            InfoCard(
                title = AccidentTexts.SAFETY_WHY_TITLE,
                body = AccidentTexts.SAFETY_WHY_BODY,
                note = AccidentTexts.DISCLAIMER
            )
        }
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomStart) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)
            ) {
                SafetyChecklist.items.forEach { item ->
                    CheckItem(
                        text = item.text,
                        checked = session?.isChecked(item.id) == true,
                        onCheckedChange = { onCheckedChange(item.id, it) }
                    )
                }
            }
        }
    }
}
