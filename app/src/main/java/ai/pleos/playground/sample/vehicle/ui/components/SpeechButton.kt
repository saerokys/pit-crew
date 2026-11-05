package ai.pleos.playground.sample.vehicle.ui.components

import ai.pleos.playground.sample.vehicle.tts.SpeechState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 상단 바의 [소리로 듣기] 버튼(SC-210, 220, 221, 230, 240에서만 둔다).
 * 상태는 아이콘과 글자로 구분한다: 소리로 듣기 / 준비 중… / 멈추기 / 소리 안내 불가(비활성).
 * [voiceOn]이 꺼져 있으면(SC-205 음성 스위치) 회색 비활성이다.
 */
@Composable
fun SpeechButton(state: SpeechState, voiceOn: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    when {
        !voiceOn -> TopBarButton("소리로 듣기", PitCrewIcons.SpeakerOn, onClick = {}, modifier = modifier, enabled = false)
        state == SpeechState.PREPARING -> TopBarButton("준비 중…", PitCrewIcons.Refresh, onClick = {}, modifier = modifier, enabled = false)
        state == SpeechState.SPEAKING -> TopBarButton("멈추기", PitCrewIcons.Stop, onClick = onClick, modifier = modifier)
        state == SpeechState.SCREEN_ERROR || state == SpeechState.UNAVAILABLE ->
            TopBarButton("소리 안내 불가", PitCrewIcons.SpeakerOff, onClick = {}, modifier = modifier, enabled = false)
        else -> TopBarButton("소리로 듣기", PitCrewIcons.SpeakerOn, onClick = onClick, modifier = modifier)
    }
}
