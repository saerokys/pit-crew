package ai.pleos.playground.sample.vehicle.ui.summary

import ai.pleos.playground.sample.vehicle.session.AccidentSession
import ai.pleos.playground.sample.vehicle.session.AllChecklists
import ai.pleos.playground.sample.vehicle.session.InjuryAnswer
import ai.pleos.playground.sample.vehicle.session.formatClock
import ai.pleos.playground.sample.vehicle.ui.components.NumberedRow
import ai.pleos.playground.sample.vehicle.ui.components.NumberedRowsCard
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewCard
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewIcons
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val DISCLAIMER = "참고용 기록이며 법적 효력을 보증하지 않아요. 법률·의료 조언이 아니에요."

/**
 * SC-260 접수 요약. 화면 표시만 한다(저장, 외부 전송 없음). [기록 저장]과 [보험사로 전달]은 Phase 10, 12에서 연결한다.
 * 왼쪽: 보험사에 말할 순서(번호 5행). 오른쪽: 기록 시각과 체크 현황(확인함/확인 안 함).
 * 위치, 사진, 상대방 정보, 차대번호는 세션에 없고 표시하지 않는다.
 */
@Composable
fun SummaryScreen(session: AccidentSession?, modifier: Modifier = Modifier) {
    val colors = PitCrewTheme.colors
    val rows = listOf(
        NumberedRow("무슨 일", listOf("교통사고가 났어요")),
        // 주소가 있으면 주소와 출처 표기로 바꾸고, 없으면 기존 문장에 위치 상태 한 줄만 덧붙인다.
        addressText(session)?.let { address ->
            NumberedRow("사고 장소", listOf(address), OSM_CREDIT)
        } ?: NumberedRow(
            "사고 장소",
            listOfNotNull("사고 장소를 말로 설명하세요. 도로 이름이나 이정표를 말하면 좋아요.", placeStatusText(session, summary = true))
        ),
        NumberedRow("다친 사람", listOf(injuryText(session?.injury ?: InjuryAnswer.NOT_ANSWERED))),
        NumberedRow("차량 상태", vehicleLines(session?.vehicle)),
        NumberedRow("상대방 확인", listOf(otherPartyText(session)))
    )
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
        Text("보험사에 이렇게 말하세요", style = PitCrewTheme.typography.screenTitle, color = colors.textPrimary)
        Spacer(Modifier.height(PitCrewDimens.touchGap))
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PitCrewDimens.stepColumnGap)
        ) {
            NumberedRowsCard(rows = rows, modifier = Modifier.weight(1f).fillMaxHeight())
            RecordStatusCard(session = session, modifier = Modifier.width(PitCrewDimens.summaryChecklistWidth).fillMaxHeight())
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(PitCrewDimens.iconSize + 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(PitCrewIcons.Info, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(PitCrewDimens.iconSize))
            Spacer(Modifier.width(16.dp))
            Text(DISCLAIMER, style = PitCrewTheme.typography.caption, color = colors.textSecondary)
        }
    }
}

/** 오른쪽 카드: 기록 시각과 안내, 화면별 체크 현황. 넘치면 카드 안에서 스크롤된다. */
@Composable
private fun RecordStatusCard(session: AccidentSession?, modifier: Modifier = Modifier) {
    val colors = PitCrewTheme.colors
    PitCrewCard(modifier = modifier, padding = 24.dp) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                "기록 시각 ${session?.let { formatClock(it.startedAtMillis) } ?: "확인 불가"}",
                style = PitCrewTheme.typography.cardTitle,
                color = colors.textPrimary
            )
            Text(
                "사고 모드를 시작한 시각이에요. 실제 사고 시각과 다를 수 있어요.",
                style = PitCrewTheme.typography.caption,
                color = colors.textSecondary
            )
            // 항목이 많아 아래로 스크롤되므로, 화면별 확인 개수는 먼저 한눈에 보이게 맨 위에 둔다.
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(PitCrewDimens.cardBorder).background(colors.divider))
            Spacer(Modifier.height(12.dp))
            AllChecklists.forEach { section ->
                val checked = section.items.count { session?.isChecked(it.id) == true }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(
                        section.title,
                        style = PitCrewTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "확인함 ${checked}/${section.items.size}",
                        style = PitCrewTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                }
            }
            AllChecklists.forEach { section ->
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().height(PitCrewDimens.cardBorder).background(colors.divider))
                Spacer(Modifier.height(12.dp))
                Text(
                    section.title,
                    style = PitCrewTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                    color = colors.textSecondary
                )
                section.items.forEach { item ->
                    val isChecked = session?.isChecked(item.id) == true
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isChecked) PitCrewIcons.CheckCircle else PitCrewIcons.MinusCircle,
                            contentDescription = null,
                            tint = if (isChecked) colors.accent else colors.textSecondary,
                            modifier = Modifier.size(PitCrewDimens.iconSize)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            item.text,
                            style = PitCrewTheme.typography.caption,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isChecked) "확인함" else "확인 안 함",
                            style = PitCrewTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                            color = if (isChecked) colors.textPrimary else colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
