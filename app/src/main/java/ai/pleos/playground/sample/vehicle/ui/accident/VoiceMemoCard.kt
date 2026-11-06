package ai.pleos.playground.sample.vehicle.ui.accident

import ai.pleos.playground.sample.vehicle.stt.VoiceState
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewButton
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewCard
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewIcons
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** SC-230 음성 카드에 보여 줄 값. [voiceOn]은 SC-205 음성 스위치, [saved]는 세션에 확정한 메모, [draft]는 아직 저장하지 않은 인식 결과. */
data class VoiceMemoUi(
    val voiceOn: Boolean,
    val state: VoiceState,
    val draft: String?,
    val saved: String?,
    /** [draft]가 100자 제한으로 잘렸을 때만 참(확인 화면에 "100자까지만 저장돼요"). */
    val cut: Boolean = false,
    /** 방금 못 알아들었을 때 참(대기 상태에 "잘 듣지 못했어요. 다시 말해 주세요."). 엔진 오류(카드 비활성)에는 쓰지 않는다. */
    val notHeard: Boolean = false
)

/** SC-230 음성 카드의 버튼 동작. */
class VoiceMemoActions(
    val onSpeak: () -> Unit,
    val onStop: () -> Unit,
    val onSave: () -> Unit,
    val onRetry: () -> Unit
)

/**
 * SC-230 음성 메모 카드(SPEC): [말하기] → 듣는 중 → 인식 결과 확인 후 [저장] [다시 말하기].
 * 음성 스위치가 꺼져 있거나 사용할 수 없으면 회색 점선 비활성과 "음성 사용이 꺼져 있거나 사용할 수 없어요"를 보여 준다(사고 모드는 계속 진행된다).
 * 채운 버튼은 하단 바의 [다음] 하나뿐이라 이 카드의 버튼은 모두 기본 버튼이다.
 * 메모는 사고 기록에 들어가지 않는다. 말하기 전에 "상대방 이름·번호·연락처는 말하지 마세요"를 보여 준다.
 */
@Composable
fun VoiceMemoCard(ui: VoiceMemoUi, actions: VoiceMemoActions, modifier: Modifier = Modifier) {
    val colors = PitCrewTheme.colors
    val disabled = !ui.voiceOn || ui.state == VoiceState.UNAVAILABLE
    PitCrewCard(modifier = modifier, dashed = disabled) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    PitCrewIcons.Mic,
                    contentDescription = null,
                    tint = if (disabled) colors.textDisabled else colors.accent,
                    modifier = Modifier.size(PitCrewDimens.iconSize)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    AccidentTexts.VOICE_TITLE,
                    style = PitCrewTheme.typography.cardTitle,
                    color = if (disabled) colors.textDisabled else colors.textPrimary
                )
            }
            Spacer(Modifier.height(16.dp))
            if (disabled) {
                Text(AccidentTexts.VOICE_UNAVAILABLE, style = PitCrewTheme.typography.body, color = colors.textDisabled)
                Spacer(Modifier.weight(1f))
                PitCrewButton(
                    text = AccidentTexts.VOICE_SPEAK,
                    onClick = {},
                    enabled = false,
                    leadingIcon = PitCrewIcons.Mic,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // 본문(스크롤): 안내, 인식 결과 또는 저장한 메모, 말하기 전 안내
                Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                    when {
                        ui.draft != null -> {
                            Text(AccidentTexts.VOICE_REVIEW, style = PitCrewTheme.typography.caption, color = colors.textSecondary)
                            Spacer(Modifier.height(12.dp))
                            Text(ui.draft, style = PitCrewTheme.typography.body, color = colors.textPrimary)
                            if (ui.cut) {
                                Spacer(Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(PitCrewIcons.Info, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(PitCrewDimens.iconSize))
                                    Spacer(Modifier.width(12.dp))
                                    Text(AccidentTexts.VOICE_CUT, style = PitCrewTheme.typography.caption, color = colors.textSecondary)
                                }
                            }
                        }
                        ui.saved != null -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(PitCrewIcons.CheckCircle, contentDescription = null, tint = colors.statusOk, modifier = Modifier.size(PitCrewDimens.iconSize))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    AccidentTexts.VOICE_SAVED,
                                    style = PitCrewTheme.typography.body.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(ui.saved, style = PitCrewTheme.typography.body, color = colors.textPrimary)
                        }
                        else -> Text(AccidentTexts.VOICE_HINT, style = PitCrewTheme.typography.caption, color = colors.textSecondary)
                    }
                    if (ui.draft == null && ui.notHeard) {
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(PitCrewIcons.Info, contentDescription = null, tint = colors.accent, modifier = Modifier.size(PitCrewDimens.iconSize))
                            Spacer(Modifier.width(12.dp))
                            Text(AccidentTexts.VOICE_NOT_HEARD, style = PitCrewTheme.typography.caption.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                        }
                    }
                    if (ui.draft == null) {
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(PitCrewIcons.Warning, contentDescription = null, tint = colors.statusWarn, modifier = Modifier.size(PitCrewDimens.iconSize))
                            Spacer(Modifier.width(12.dp))
                            Text(AccidentTexts.VOICE_WARNING, style = PitCrewTheme.typography.caption, color = colors.textPrimary)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                when {
                    ui.draft != null -> {
                        PitCrewButton(
                            text = AccidentTexts.VOICE_SAVE,
                            onClick = actions.onSave,
                            leadingIcon = PitCrewIcons.Check,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(PitCrewDimens.touchGap))
                        PitCrewButton(
                            text = AccidentTexts.VOICE_RETRY,
                            onClick = actions.onRetry,
                            leadingIcon = PitCrewIcons.Mic,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    ui.state == VoiceState.PREPARING -> PitCrewButton(
                        text = AccidentTexts.VOICE_PREPARING,
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ui.state == VoiceState.LISTENING -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(PitCrewIcons.Mic, contentDescription = null, tint = colors.accent, modifier = Modifier.size(PitCrewDimens.iconSize))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                AccidentTexts.VOICE_LISTENING,
                                style = PitCrewTheme.typography.body.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                        }
                        Spacer(Modifier.height(PitCrewDimens.touchGap))
                        PitCrewButton(
                            text = AccidentTexts.VOICE_STOP,
                            onClick = actions.onStop,
                            leadingIcon = PitCrewIcons.Stop,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> PitCrewButton(
                        text = if (ui.saved != null) AccidentTexts.VOICE_RETRY else AccidentTexts.VOICE_SPEAK,
                        onClick = actions.onSpeak,
                        leadingIcon = PitCrewIcons.Mic,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
