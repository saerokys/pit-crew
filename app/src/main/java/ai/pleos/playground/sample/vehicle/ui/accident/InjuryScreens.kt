package ai.pleos.playground.sample.vehicle.ui.accident

import ai.pleos.playground.sample.vehicle.session.AccidentSession
import ai.pleos.playground.sample.vehicle.session.InjuryAnswer
import ai.pleos.playground.sample.vehicle.ui.summary.OSM_CREDIT
import ai.pleos.playground.sample.vehicle.ui.summary.addressText
import ai.pleos.playground.sample.vehicle.ui.summary.placeStatusText
import ai.pleos.playground.sample.vehicle.ui.components.AccidentStepScaffold
import ai.pleos.playground.sample.vehicle.ui.components.ChoiceTile
import ai.pleos.playground.sample.vehicle.ui.components.EmergencyBanner
import ai.pleos.playground.sample.vehicle.ui.components.InfoRow
import ai.pleos.playground.sample.vehicle.ui.components.InfoRowsCard
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewIcons
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp

/**
 * SC-220 부상 확인. 세 선택 버튼으로만 이동한다([다음] 없음).
 * 선택하면 응답을 세션에 저장하고 [onAnswer]가 이동한다. 이미 고른 응답은 선택된 모양으로 보인다.
 */
@Composable
fun InjuryScreen(
    session: AccidentSession?,
    onAnswer: (InjuryAnswer) -> Unit,
    onExitRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PitCrewTheme.colors
    val current = session?.injury ?: InjuryAnswer.NOT_ANSWERED
    AccidentStepScaffold(
        currentStep = 3,
        title = AccidentTexts.INJURY_TITLE,
        onExitRequest = onExitRequest,
        modifier = modifier
    ) {
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth().height(PitCrewDimens.choiceTileHeight),
            horizontalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)
        ) {
            ChoiceTile(
                label = "없어요",
                icon = PitCrewIcons.CheckCircle,
                accent = colors.statusOk,
                selected = current == InjuryAnswer.NONE,
                onClick = { onAnswer(InjuryAnswer.NONE) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            ChoiceTile(
                label = "있어요",
                icon = PitCrewIcons.Warning,
                accent = colors.statusUrgentText,
                selected = current == InjuryAnswer.YES,
                onClick = { onAnswer(InjuryAnswer.YES) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            ChoiceTile(
                label = "잘 모르겠어요",
                icon = PitCrewIcons.HelpCircle,
                accent = colors.statusWarn,
                selected = current == InjuryAnswer.UNSURE,
                onClick = { onAnswer(InjuryAnswer.UNSURE) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(PitCrewDimens.captionRowHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(PitCrewIcons.Phone, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(PitCrewDimens.iconSize))
            Spacer(Modifier.width(16.dp))
            Text(
                AccidentTexts.INJURY_HINT,
                style = PitCrewTheme.typography.caption,
                color = colors.textSecondary
            )
        }
    }
}

/**
 * SC-221 긴급 안내. 앱은 전화를 걸지 않는다(안내만). 진행 목록과 [사고 모드 나가기]는 두지 않는다.
 * 문구는 초안이며 공식 안내와 대조 전이다(PROGRESS.md 참고).
 */
@Composable
fun EmergencyScreen(session: AccidentSession?, modifier: Modifier = Modifier) {
    val colors = PitCrewTheme.colors
    val injuryText = AccidentTexts.injurySpeakValue(session?.injury)
    Column(modifier = modifier.fillMaxSize()) {
        EmergencyBanner(text = AccidentTexts.EMERGENCY_BANNER)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(
                    start = PitCrewDimens.safeMargin,
                    end = PitCrewDimens.safeMargin,
                    top = PitCrewDimens.stepScreenTopGap,
                    bottom = PitCrewDimens.stepScreenBottomGap
                ),
            horizontalArrangement = Arrangement.spacedBy(PitCrewDimens.stepColumnGap)
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Icon(PitCrewIcons.Phone, contentDescription = null, tint = colors.statusUrgentText, modifier = Modifier.size(PitCrewDimens.heroIconSize))
                Spacer(Modifier.height(PitCrewDimens.touchGap))
                Text(AccidentTexts.EMERGENCY_CALL, style = PitCrewTheme.typography.heroTitle, color = colors.textPrimary)
                Spacer(Modifier.height(PitCrewDimens.touchGap))
                Text(
                    AccidentTexts.EMERGENCY_NO_DIAL,
                    style = PitCrewTheme.typography.caption,
                    color = colors.textSecondary
                )
                Spacer(Modifier.height(16.dp))
                Text(AccidentTexts.EMERGENCY_POLICE, style = PitCrewTheme.typography.body, color = colors.textPrimary)
                Spacer(Modifier.height(16.dp))
                Text(
                    AccidentTexts.DISCLAIMER,
                    style = PitCrewTheme.typography.caption,
                    color = colors.textSecondary
                )
            }
            Box(
                modifier = Modifier.width(PitCrewDimens.emergencyCardWidth).fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                InfoRowsCard(
                    title = AccidentTexts.SPEAK_CARD_TITLE,
                    iconTint = colors.statusUrgentText,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    rows = listOf(
                        InfoRow(PitCrewIcons.Car, AccidentTexts.SPEAK_KIND_LABEL, AccidentTexts.SPEAK_KIND_VALUE),
                        // 주소가 있을 때만 "말로 설명하세요" 문장을 주소로 바꾼다. 없으면 기존 문장에 상태 한 줄만 덧붙인다.
                        addressText(session)?.let { address ->
                            InfoRow(PitCrewIcons.Pin, "사고 장소", address, "참고용 주소예요 · $OSM_CREDIT")
                        } ?: InfoRow(
                            PitCrewIcons.Pin,
                            "사고 장소",
                            listOfNotNull("사고 장소를 말로 설명하세요. 도로 이름이나 이정표를 말하면 좋아요.", placeStatusText(session)).joinToString("\n")
                        ),
                        InfoRow(PitCrewIcons.Person, AccidentTexts.SPEAK_INJURY_LABEL, injuryText)
                    )
                )
            }
        }
    }
}
