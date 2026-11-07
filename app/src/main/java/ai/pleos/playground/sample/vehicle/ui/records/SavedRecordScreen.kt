package ai.pleos.playground.sample.vehicle.ui.records

import ai.pleos.playground.sample.vehicle.record.SavedRecordSummary
import ai.pleos.playground.sample.vehicle.record.SavedRecordUi
import ai.pleos.playground.sample.vehicle.record.SavedState
import ai.pleos.playground.sample.vehicle.ui.components.DialogButton
import ai.pleos.playground.sample.vehicle.ui.components.InfoRow
import ai.pleos.playground.sample.vehicle.ui.components.InfoRowsCard
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewButton
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewCard
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewDialog
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewIcons
import ai.pleos.playground.sample.vehicle.ui.summary.WEATHER_CREDIT
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** SC-520 문구. 모든 글자는 28 이상이다. 위치·주소·날씨 줄의 값 문구는 구현 판단이다(PROGRESS 4-2). */
object SavedRecordTexts {
    const val TITLE = "저장 기록"
    const val NONE = "보관한 기록이 없어요"
    const val UNREADABLE = "기록을 읽지 못했어요"
    const val DELETE = "삭제"
    const val CANCEL = "취소"
    const val DELETE_FAILED = "삭제하지 못했어요"
    const val DIALOG_TITLE = "기록을 삭제할까요?"
    const val DIALOG_BODY = "삭제하면 되돌릴 수 없어요"
    const val LABEL_LOCATION = "위치"
    const val LABEL_ADDRESS = "주소"
    const val LABEL_WEATHER = "날씨"
    const val VALUE_OK = "확인됨"
    const val VALUE_UNAVAILABLE = "확인 불가"
    const val VALUE_NOT_USED = "쓰지 않음"
    const val VALUE_NO_ADDRESS = "없음"

    /** 하단 바 가운데 문구(SC-260, 270과 같은 문장). */
    const val DISCLAIMER = "참고용 기록이며 법적 효력을 보증하지 않아요. 법률·의료 조언이 아니에요."
}

/**
 * SC-520 저장 기록. SC-270에서 저장한 최근 1건의 요약(SC-270 요약 한 줄, 위치·주소·날씨 한 줄씩)을 보여 주고,
 * [삭제]는 확인 대화상자를 거쳐서만 지운다. 기록이 없으면 "보관한 기록이 없어요", 읽지 못하면 "기록을 읽지 못했어요"와
 * [삭제]를 보여 준다. 좌표는 기록에 없고 화면에도 없다. 채운 버튼은 쓰지 않는다(삭제는 기본 버튼).
 */
@Composable
fun SavedRecordScreen(
    state: SavedRecordUi,
    deleteFailed: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PitCrewTheme.colors
    var showDialog by remember { mutableStateOf(false) }
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
        Text(SavedRecordTexts.TITLE, style = PitCrewTheme.typography.screenTitle, color = colors.textPrimary)
        Spacer(Modifier.height(PitCrewDimens.touchGap))
        Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            when (state) {
                SavedRecordUi.Loading -> Unit
                SavedRecordUi.None -> MessageCard(PitCrewIcons.Info, colors.textSecondary, SavedRecordTexts.NONE)
                SavedRecordUi.Unreadable -> MessageCard(PitCrewIcons.Warning, colors.statusWarn, SavedRecordTexts.UNREADABLE)
                is SavedRecordUi.Found -> InfoRowsCard(
                    title = state.summary.summaryLine(),
                    rows = rowsFor(state.summary),
                    iconTint = colors.accent,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        // 기록이 있거나 읽지 못했을 때만 [삭제]를 둔다. 실패하면 오른쪽에 한 줄을 보인다.
        if (state is SavedRecordUi.Found || state == SavedRecordUi.Unreadable) {
            Spacer(Modifier.height(PitCrewDimens.touchGap))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PitCrewButton(
                    text = SavedRecordTexts.DELETE,
                    onClick = { showDialog = true },
                    modifier = Modifier.width(PitCrewDimens.recheckButtonWidth).height(PitCrewDimens.touchMin)
                )
                if (deleteFailed) {
                    Spacer(Modifier.width(PitCrewDimens.touchGap))
                    Icon(PitCrewIcons.Warning, contentDescription = null, tint = colors.statusWarn, modifier = Modifier.size(PitCrewDimens.iconSize))
                    Spacer(Modifier.width(16.dp))
                    Text(
                        SavedRecordTexts.DELETE_FAILED,
                        style = PitCrewTheme.typography.body.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                }
            }
        }
    }
    if (showDialog) {
        PitCrewDialog(
            title = SavedRecordTexts.DIALOG_TITLE,
            body = SavedRecordTexts.DIALOG_BODY,
            buttons = listOf(
                DialogButton(SavedRecordTexts.DELETE) { showDialog = false; onDelete() },
                DialogButton(SavedRecordTexts.CANCEL) { showDialog = false }
            ),
            onDismiss = { showDialog = false }
        )
    }
}

/** 위치·주소·날씨 한 줄씩. 주소에는 출처, 날씨에는 출처를 값 아래 줄로 붙인다. 좌표 행은 없다. */
private fun rowsFor(summary: SavedRecordSummary): List<InfoRow> = listOf(
    InfoRow(PitCrewIcons.Pin, SavedRecordTexts.LABEL_LOCATION, stateText(summary.location)),
    InfoRow(
        PitCrewIcons.Pin,
        SavedRecordTexts.LABEL_ADDRESS,
        when (summary.address) {
            SavedState.OK -> summary.addressText.orEmpty()
            SavedState.UNAVAILABLE -> SavedRecordTexts.VALUE_UNAVAILABLE
            SavedState.NOT_USED -> SavedRecordTexts.VALUE_NO_ADDRESS
        },
        note = summary.addressCredit
    ),
    InfoRow(
        PitCrewIcons.Sun,
        SavedRecordTexts.LABEL_WEATHER,
        if (summary.weather == SavedState.OK) summary.weatherText.orEmpty() else stateText(summary.weather),
        note = if (summary.weather == SavedState.OK) WEATHER_CREDIT else null
    )
)

private fun stateText(state: SavedState): String = when (state) {
    SavedState.OK -> SavedRecordTexts.VALUE_OK
    SavedState.UNAVAILABLE -> SavedRecordTexts.VALUE_UNAVAILABLE
    SavedState.NOT_USED -> SavedRecordTexts.VALUE_NOT_USED
}

/** 안내 한 줄 카드(기록 없음, 읽지 못함). 아이콘과 글자를 함께 쓴다. */
@Composable
private fun MessageCard(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color, text: String) {
    PitCrewCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(PitCrewDimens.iconSize))
            Spacer(Modifier.width(16.dp))
            Text(text, style = PitCrewTheme.typography.title, color = PitCrewTheme.colors.textPrimary)
        }
    }
}
