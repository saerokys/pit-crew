package ai.pleos.playground.sample.vehicle.ui.accident

import ai.pleos.playground.sample.vehicle.session.AccidentSession
import ai.pleos.playground.sample.vehicle.session.ChecklistItemDef
import ai.pleos.playground.sample.vehicle.session.OtherPartyChecklist
import ai.pleos.playground.sample.vehicle.session.RecordChecklist
import ai.pleos.playground.sample.vehicle.ui.components.AccidentStepScaffold
import ai.pleos.playground.sample.vehicle.ui.components.CheckItem
import ai.pleos.playground.sample.vehicle.ui.components.InfoCard
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewIcons
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

private const val DISCLAIMER = "참고용 안내이며 법률·의료 조언이 아니에요."

/** 체크 항목 목록(아래쪽 정렬, 넘치면 스크롤). 체크는 세션에 저장되어 [이전]으로 갔다 와도 유지된다. */
@Composable
private fun Checklist(
    items: List<ChecklistItemDef>,
    session: AccidentSession?,
    onCheckedChange: (String, Boolean) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomStart) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)
        ) {
            items.forEach { item ->
                CheckItem(
                    text = item.text,
                    checked = session?.isChecked(item.id) == true,
                    onCheckedChange = { onCheckedChange(item.id, it) },
                    compact = compact
                )
            }
        }
    }
}

/**
 * SC-230 현장 기록 안내. 앱은 사진을 찍거나 저장하지 않는다(카메라 호출, 사진 저장 코드 없음).
 * 체크 5개는 사용자가 휴대폰으로 직접 촬영했는지 표시하는 용도다.
 */
@Composable
fun RecordScreen(
    session: AccidentSession?,
    onCheckedChange: (String, Boolean) -> Unit,
    onExitRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PitCrewTheme.colors
    AccidentStepScaffold(
        currentStep = 4,
        title = "현장을 기록해 두세요",
        onExitRequest = onExitRequest,
        modifier = modifier,
        right = {
            VoiceMemoCard()
            Spacer(Modifier.height(PitCrewDimens.touchGap))
            Text(DISCLAIMER, style = PitCrewTheme.typography.caption, color = colors.textSecondary)
        }
    ) {
        Text("휴대폰으로 촬영해 두세요", style = PitCrewTheme.typography.caption, color = colors.textSecondary)
        Text("나중에 보험사에 보여 줄 수 있어요", style = PitCrewTheme.typography.caption, color = colors.textSecondary)
        Checklist(
            items = RecordChecklist.items,
            session = session,
            onCheckedChange = onCheckedChange,
            compact = true,
            modifier = Modifier.weight(1f)
        )
        Row(
            modifier = Modifier.fillMaxWidth().height(PitCrewDimens.iconSize + 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(PitCrewIcons.Camera, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(PitCrewDimens.iconSize))
            Spacer(Modifier.width(16.dp))
            Text("사진은 앱에 저장되지 않아요", style = PitCrewTheme.typography.caption, color = colors.textSecondary)
        }
    }
}

/**
 * SC-240 상대방 정보 확인. 입력창이 없고, 앱은 상대방 정보를 입력받거나 저장하지 않는다.
 * 체크 여부만 세션에 둔다.
 */
@Composable
fun OtherPartyScreen(
    session: AccidentSession?,
    onCheckedChange: (String, Boolean) -> Unit,
    onExitRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    AccidentStepScaffold(
        currentStep = 5,
        title = "상대방 정보를 확인하세요",
        onExitRequest = onExitRequest,
        modifier = modifier,
        right = {
            InfoCard(
                title = "알아 두세요",
                body = "이 앱은 상대방 정보를 입력받거나 저장하지 않아요. 확인한 것만 체크해 두세요.",
                note = DISCLAIMER,
                icon = PitCrewIcons.Info
            )
        }
    ) {
        Checklist(
            items = OtherPartyChecklist.items,
            session = session,
            onCheckedChange = onCheckedChange,
            compact = false,
            modifier = Modifier.weight(1f)
        )
    }
}
