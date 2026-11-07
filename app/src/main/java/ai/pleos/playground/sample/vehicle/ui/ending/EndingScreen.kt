package ai.pleos.playground.sample.vehicle.ui.ending

import ai.pleos.playground.sample.vehicle.record.EndingSummary
import ai.pleos.playground.sample.vehicle.record.SaveState
import ai.pleos.playground.sample.vehicle.session.AccidentSession
import ai.pleos.playground.sample.vehicle.session.formatClock
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewButton
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewIcons
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** 안내 문구(SPEC·시안 원문). SC-520(홈 → 설정·정보 → 저장 기록 삭제)에서 언제든 삭제할 수 있다. */
private const val KEEP_NOTICE = "저장하면 이 기기에만 보관되고 언제든 삭제할 수 있어요"
private const val SAVED_TEXT = "저장했어요"
private const val FAILED_TEXT = "저장하지 못했어요"
private const val OVERWRITE_TEXT = "이전에 저장한 기록은 바뀌어요"

/** SC-260과 같은 문구(법률·의료 조언이 아니라는 표시 포함). */
private const val DISCLAIMER = "참고용 기록이며 법적 효력을 보증하지 않아요. 법률·의료 조언이 아니에요."

/**
 * SC-270 종료와 기록 처리. 두 버튼을 누르기 전에는 아무것도 저장하지 않는다(기본은 보관하지 않음).
 * [기록 저장]은 기록 패키지를 기기 내부 저장소에 한 건 저장하고, 저장하면 같은 화면에서 "저장했어요"와 [홈으로]만 남긴다.
 * [저장하지 않고 종료]는 저장 없이 홈으로 간다(홈에 도착하면 세션이 버려진다).
 * 시안의 [다음 장소 안내], [출발 전 점검하러 가기]는 만들지 않았다(PROGRESS 4-2).
 */
@Composable
fun EndingScreen(
    session: AccidentSession?,
    saveState: SaveState,
    hasSavedRecord: Boolean,
    onSave: () -> Unit,
    onDiscardAndExit: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PitCrewTheme.colors
    val time = session?.let { formatClock(it.startedAtMillis) } ?: "확인 불가"
    val summary = "기록 시각 $time · 차량 상태 ${EndingSummary.vehicleReadCount(session?.vehicle)}/${EndingSummary.VEHICLE_TOTAL}" +
        " · 안내 이행 ${EndingSummary.checkedCount(session)}/${EndingSummary.checkTotal}"
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = PitCrewDimens.safeMargin,
                end = PitCrewDimens.safeMargin,
                top = PitCrewDimens.stepScreenTopGap,
                bottom = PitCrewDimens.stepScreenBottomGap
            )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                PitCrewIcons.CheckCircle,
                contentDescription = null,
                tint = colors.statusOk,
                modifier = Modifier.size(PitCrewDimens.endingTitleIcon)
            )
            Spacer(Modifier.width(32.dp))
            Text("사고 대응을 마쳤어요", style = PitCrewTheme.typography.screenTitle, color = colors.textPrimary)
        }
        Spacer(Modifier.height(16.dp))
        Text(summary, style = PitCrewTheme.typography.heroSub, color = colors.textSecondary)

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)
        ) {
            if (saveState == SaveState.SAVED) {
                // 저장한 뒤에는 [홈으로] 하나만 둔다.
                PitCrewButton(
                    text = "홈으로",
                    onClick = onHome,
                    filled = true,
                    modifier = Modifier.weight(1f).height(PitCrewDimens.endingButtonHeight)
                )
                Spacer(Modifier.weight(1f))
            } else {
                PitCrewButton(
                    text = "기록 저장",
                    onClick = onSave,
                    filled = true,
                    enabled = session != null && saveState != SaveState.SAVING,
                    leadingIcon = PitCrewIcons.Save,
                    modifier = Modifier.weight(1f).height(PitCrewDimens.endingButtonHeight)
                )
                PitCrewButton(
                    text = "저장하지 않고 종료",
                    onClick = onDiscardAndExit,
                    leadingIcon = PitCrewIcons.Exit,
                    modifier = Modifier.weight(1f).height(PitCrewDimens.endingButtonHeight)
                )
            }
        }

        Spacer(Modifier.height(PitCrewDimens.touchGap))
        NoticeRow(PitCrewIcons.ShieldCheck, colors.textSecondary, KEEP_NOTICE)
        // 저장 결과나 덮어쓰기 안내가 들어갈 자리(없을 때도 높이를 비워 두어 화면이 밀리지 않게 한다).
        StatusSlot {
            when {
                saveState == SaveState.SAVED -> StatusLine(PitCrewIcons.CheckCircle, colors.statusOk, SAVED_TEXT)
                saveState == SaveState.FAILED -> StatusLine(PitCrewIcons.Warning, colors.statusWarn, FAILED_TEXT)
                hasSavedRecord -> NoticeRow(PitCrewIcons.Info, colors.textSecondary, OVERWRITE_TEXT)
            }
        }
        NoticeRow(PitCrewIcons.Info, colors.textSecondary, DISCLAIMER)
    }
}

/** 높이를 고정한 자리(내용이 없어도 76dp를 차지한다). */
@Composable
private fun StatusSlot(content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().height(PitCrewDimens.captionRowHeight), verticalArrangement = Arrangement.Center) {
        content()
    }
}

@Composable
private fun NoticeRow(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().height(PitCrewDimens.captionRowHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(PitCrewDimens.iconSize))
        Spacer(Modifier.width(16.dp))
        Text(text, style = PitCrewTheme.typography.caption, color = PitCrewTheme.colors.textSecondary)
    }
}

@Composable
private fun StatusLine(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(PitCrewDimens.iconSize))
        Spacer(Modifier.width(16.dp))
        Text(
            text,
            style = PitCrewTheme.typography.body.copy(fontWeight = FontWeight.Bold),
            color = PitCrewTheme.colors.textPrimary
        )
    }
}
