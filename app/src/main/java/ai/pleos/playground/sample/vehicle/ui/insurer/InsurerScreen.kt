package ai.pleos.playground.sample.vehicle.ui.insurer

import ai.pleos.playground.sample.vehicle.insurer.InsurerItem
import ai.pleos.playground.sample.vehicle.insurer.InsurerPackage
import ai.pleos.playground.sample.vehicle.insurer.InsurerSendState
import ai.pleos.playground.sample.vehicle.session.AccidentSession
import ai.pleos.playground.sample.vehicle.session.formatClock
import ai.pleos.playground.sample.vehicle.ui.components.CheckItem
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewButton
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewCard
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewIcons
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** SC-290 화면 문구. 모든 글자는 28 이상이다. */
object InsurerTexts {
    const val TITLE = "보험사로 보낼 기록이에요"
    const val BADGE = "시연용 가상 보험사"
    const val STEP_SEND = "전송"
    const val STEP_RECEIVE = "접수"
    const val ITEMS_TITLE = "전달할 항목"
    const val PREVIEW_TITLE = "보낼 내용 미리보기"
    const val NOT_INCLUDED = "상대방 정보와 사진은 포함되지 않아요"
    const val VOICE_WARNING = "상대방 이름·번호·연락처가 있으면 이 항목을 끄세요"
    const val SEND = "보내기"
    const val SENDING = "보내는 중…"
    const val RESEND = "다시 보내기"
    const val SENT = "보냈어요"
    const val NONE_SELECTED = "보낼 항목을 하나 이상 고르세요"
    const val FAILED = "전송하지 못했어요"
    const val NOT_REAL = "실제 보험사로 전달되지 않았어요"
    const val DEMO_NUMBER = "시연용 번호예요. 실제 접수 번호가 아니에요"
    const val NO_NETWORK = "이 전송은 시연용이라 네트워크를 쓰지 않아요"
    const val RECEIPT_TITLE = "접수 번호"
    const val LOG_TITLE = "전송 로그"
    const val DISCLAIMER = "참고용 기록이며 법적 효력을 보증하지 않아요. 법률·의료 조언이 아니에요."
}

private fun iconFor(item: InsurerItem): ImageVector = when (item) {
    InsurerItem.TIME -> PitCrewIcons.Clock
    InsurerItem.PLACE -> PitCrewIcons.Pin
    InsurerItem.WEATHER -> PitCrewIcons.Sun
    InsurerItem.VEHICLE -> PitCrewIcons.Car
    InsurerItem.CHECKS -> PitCrewIcons.Clipboard
    InsurerItem.VOICE -> PitCrewIcons.Mic
}

/**
 * SC-290 사고 기록 전달(시연용 가상 보험사). 전송은 앱 안 모의 처리이고 외부 호출이 없다.
 * 왼쪽: 전달할 항목 체크(값이 없는 항목은 회색 선택 불가), 가운데: 보낼 내용 미리보기(음성 메모는 내용 전체),
 * 오른쪽: 안내, 접수 번호와 전송 로그, [보내기]. 사용자가 [보내기]를 눌러야만 접수한다.
 * 채운 버튼은 한 화면에 1개: 보내기 전에는 [보내기], 보낸 뒤에는 하단 바의 [다음]이다.
 */
@Composable
fun InsurerScreen(
    session: AccidentSession?,
    unchecked: Set<InsurerItem>,
    sendState: InsurerSendState,
    lastSent: Set<InsurerItem>,
    onToggle: (InsurerItem) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PitCrewTheme.colors
    val receipts = session?.insurerReceipts.orEmpty()
    val available = InsurerPackage.available(session)
    val selected = InsurerPackage.selected(session, unchecked)
    val sentOnce = receipts.isNotEmpty()

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
            Text(
                InsurerTexts.TITLE,
                style = PitCrewTheme.typography.screenTitle,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            StepIndicator(current = if (sentOnce) 2 else 1)
            Spacer(Modifier.width(PitCrewDimens.touchGap))
            DemoBadge()
        }
        Spacer(Modifier.height(PitCrewDimens.touchGap))
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PitCrewDimens.stepColumnGap)
        ) {
            // 왼쪽: 전달할 항목
            Column(modifier = Modifier.width(LEFT_WIDTH).fillMaxHeight()) {
                Text(
                    InsurerTexts.ITEMS_TITLE,
                    style = PitCrewTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                    color = colors.textSecondary
                )
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)
                ) {
                    InsurerItem.entries.forEach { item ->
                        val on = item in available
                        CheckItem(
                            text = item.label,
                            checked = on && item !in unchecked,
                            onCheckedChange = { onToggle(item) },
                            dense = true,
                            enabled = on,
                            note = if (on) null else InsurerPackage.unavailableNote(item)
                        )
                    }
                }
            }
            // 가운데: 보낼 내용 미리보기
            PitCrewCard(modifier = Modifier.weight(1f).fillMaxHeight(), padding = 24.dp) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Text(
                        "${InsurerTexts.PREVIEW_TITLE}(${selected.size}건)",
                        style = PitCrewTheme.typography.cardTitle,
                        color = colors.textPrimary
                    )
                    InsurerItem.entries.filter { it in selected }.forEach { item ->
                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.fillMaxWidth().height(PitCrewDimens.cardBorder).background(colors.divider))
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(iconFor(item), contentDescription = null, tint = colors.accent, modifier = Modifier.size(PitCrewDimens.iconSize))
                            Spacer(Modifier.width(12.dp))
                            Text(item.label, style = PitCrewTheme.typography.caption, color = colors.textSecondary)
                        }
                        Text(
                            InsurerPackage.preview(item, session).orEmpty(),
                            style = PitCrewTheme.typography.body.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        if (item == InsurerItem.VOICE) {
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(PitCrewIcons.Warning, contentDescription = null, tint = colors.statusWarn, modifier = Modifier.size(PitCrewDimens.iconSize))
                                Spacer(Modifier.width(12.dp))
                                Text(InsurerTexts.VOICE_WARNING, style = PitCrewTheme.typography.caption, color = colors.textPrimary)
                            }
                        }
                    }
                }
            }
            // 오른쪽: 안내, 접수 번호와 로그, 보내기
            Column(modifier = Modifier.width(RIGHT_WIDTH).fillMaxHeight()) {
                PitCrewCard(modifier = Modifier.fillMaxWidth(), dashed = true, padding = 20.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(PitCrewIcons.Info, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(PitCrewDimens.iconSize))
                        Spacer(Modifier.width(12.dp))
                        Text(InsurerTexts.NOT_INCLUDED, style = PitCrewTheme.typography.caption, color = colors.textPrimary)
                    }
                }
                Spacer(Modifier.height(PitCrewDimens.touchGap))
                Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                    if (sentOnce) ReceiptBlock(session = session)
                }
                Spacer(Modifier.height(12.dp))
                when {
                    sendState == InsurerSendState.FAILED -> NoticeLine(PitCrewIcons.Warning, colors.statusWarn, InsurerTexts.FAILED, bold = true)
                    selected.isEmpty() -> NoticeLine(PitCrewIcons.Info, colors.textSecondary, InsurerTexts.NONE_SELECTED, bold = false)
                }
                Spacer(Modifier.height(12.dp))
                val sameAsSent = sendState == InsurerSendState.SENT && selected == lastSent
                val label = when {
                    sendState == InsurerSendState.SENDING -> InsurerTexts.SENDING
                    sameAsSent -> InsurerTexts.SENT
                    sendState == InsurerSendState.FAILED || sentOnce -> InsurerTexts.RESEND
                    else -> InsurerTexts.SEND
                }
                val enabled = sendState != InsurerSendState.SENDING && selected.isNotEmpty() && !sameAsSent
                PitCrewButton(
                    text = label,
                    onClick = onSend,
                    enabled = enabled,
                    // 채운 버튼은 한 화면에 1개: 한 번 보낸 뒤에는 하단 바 [다음]이 채운 버튼이다.
                    filled = !sentOnce,
                    leadingIcon = PitCrewIcons.Send,
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
                Spacer(Modifier.height(12.dp))
                // 네트워크 스위치와 상관없이 항상 보인다.
                Text(InsurerTexts.NO_NETWORK, style = PitCrewTheme.typography.caption, color = colors.textSecondary)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth().height(PitCrewDimens.iconSize + 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(PitCrewIcons.Info, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(PitCrewDimens.iconSize))
            Spacer(Modifier.width(16.dp))
            Text(InsurerTexts.DISCLAIMER, style = PitCrewTheme.typography.caption, color = colors.textSecondary)
        }
    }
}

private val LEFT_WIDTH = 440.dp
private val RIGHT_WIDTH = 500.dp

/** 접수 번호(가장 최근)와 안내, 전송 로그(항목 이름과 개수만, 최근이 위). */
@Composable
private fun ReceiptBlock(session: AccidentSession?) {
    val colors = PitCrewTheme.colors
    val receipts = session?.insurerReceipts.orEmpty()
    val last = receipts.lastOrNull() ?: return
    PitCrewCard(modifier = Modifier.fillMaxWidth(), padding = 20.dp) {
        Text(InsurerTexts.RECEIPT_TITLE, style = PitCrewTheme.typography.caption, color = colors.textSecondary)
        Text(last.number, style = PitCrewTheme.typography.cardTitle, color = colors.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text(InsurerTexts.DEMO_NUMBER, style = PitCrewTheme.typography.caption, color = colors.textPrimary)
        Spacer(Modifier.height(8.dp))
        NoticeLine(PitCrewIcons.Info, colors.textSecondary, InsurerTexts.NOT_REAL, bold = true)
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(PitCrewDimens.cardBorder).background(colors.divider))
        Spacer(Modifier.height(12.dp))
        Text(InsurerTexts.LOG_TITLE, style = PitCrewTheme.typography.caption.copy(fontWeight = FontWeight.Bold), color = colors.textSecondary)
        receipts.asReversed().forEach { receipt ->
            Spacer(Modifier.height(8.dp))
            Text(
                "${receipt.number} · ${formatClock(receipt.sentAtMillis)} · ${receipt.itemNames.size}건",
                style = PitCrewTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )
            Text(receipt.itemNames.joinToString(", "), style = PitCrewTheme.typography.caption, color = colors.textSecondary)
        }
    }
}

@Composable
private fun NoticeLine(icon: ImageVector, tint: androidx.compose.ui.graphics.Color, text: String, bold: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(PitCrewDimens.iconSize))
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            style = PitCrewTheme.typography.caption.copy(fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal),
            color = PitCrewTheme.colors.textPrimary
        )
    }
}

/** 오른쪽 위 "시연용 가상 보험사" 배지. surface 배경, accent 테두리 2, 알약(상단 바의 가상 차량 값 배지와 같은 모양). */
@Composable
private fun DemoBadge() {
    val colors = PitCrewTheme.colors
    Row(
        modifier = Modifier
            .height(PitCrewDimens.badgeHeight)
            .background(colors.surface, PitCrewShapes.pill)
            .border(BorderStroke(PitCrewDimens.cardBorder, colors.accent), PitCrewShapes.pill)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(PitCrewIcons.Building, contentDescription = null, tint = colors.accent, modifier = Modifier.size(PitCrewDimens.iconSize))
        Spacer(Modifier.width(12.dp))
        Text(InsurerTexts.BADGE, style = PitCrewTheme.typography.badge, color = colors.textPrimary)
    }
}

/** "1 전송 — 2 접수" 2단계 표시(시안의 ③ 연결은 만들지 않는다). 현재 단계까지 accent. */
@Composable
private fun StepIndicator(current: Int) {
    val colors = PitCrewTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepDot(1, InsurerTexts.STEP_SEND, reached = current >= 1)
        Box(Modifier.width(48.dp).height(PitCrewDimens.cardBorder).background(if (current >= 2) colors.accent else colors.divider))
        StepDot(2, InsurerTexts.STEP_RECEIVE, reached = current >= 2)
    }
}

@Composable
private fun StepDot(number: Int, label: String, reached: Boolean) {
    val colors = PitCrewTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
        Box(
            modifier = Modifier
                .size(PitCrewDimens.iconSize)
                .background(if (reached) colors.accent else colors.surface, PitCrewShapes.pill)
                .border(BorderStroke(PitCrewDimens.cardBorder, if (reached) colors.accent else colors.divider), PitCrewShapes.pill),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$number",
                style = PitCrewTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                color = if (reached) colors.onAccent else colors.textSecondary
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = PitCrewTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
            color = if (reached) colors.textPrimary else colors.textSecondary
        )
    }
}
