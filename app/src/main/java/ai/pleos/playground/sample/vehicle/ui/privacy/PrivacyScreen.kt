package ai.pleos.playground.sample.vehicle.ui.privacy

import ai.pleos.playground.sample.vehicle.ui.components.PitCrewCard
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewIcons
import ai.pleos.playground.sample.vehicle.ui.summary.OSM_CREDIT
import ai.pleos.playground.sample.vehicle.ui.summary.WEATHER_CREDIT
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * SC-510 문구. 모든 글자는 28 이상이다. 원칙 문구는 SPEC SC-510 줄의 말을 그대로 나눠 쓴 것이고,
 * 출처·안내 문구는 PROGRESS 4절과 사용자가 정한 문장이다(새로 지은 문장 없음).
 */
object PrivacyTexts {
    const val TITLE = "개인정보 처리 원칙"
    const val NOT_COLLECTED = "수집하지 않는 것: 사진, 차대번호, 상대방 연락처"
    const val NOT_COLLECTED_NOTE = "위치·음성은 허용한 경우만"
    const val EXTERNAL = "외부 전송은 사용자가 켠 경우만"
    const val KEEP = "보관은 사용자 선택"
    const val DELETE = "언제든 삭제 가능"
    const val SOURCES_TITLE = "출처와 안내"
    const val WEATHER_LICENSE = "CC BY 4.0"
    const val OSM_LICENSE = "ODbL 1.0 라이선스"
    const val WEATHER_LOG = "날씨를 조회하면 서버 로그에 좌표가 최대 90일 남을 수 있어요"
    const val DEMO_INSURER = "시연용 가상 보험사예요. 실제 보험사로 전달되지 않아요"

    /** 하단 바 가운데 문구. */
    const val DISCLAIMER = "참고용이며 법률·의료 조언이 아니에요"

    /** 화면에 나오는 문구 전체(검토와 단위 테스트용). */
    val ALL: List<String> = listOf(
        TITLE, NOT_COLLECTED, NOT_COLLECTED_NOTE, EXTERNAL, KEEP, DELETE, SOURCES_TITLE,
        WEATHER_CREDIT, WEATHER_LICENSE, WEATHER_LOG, OSM_CREDIT, OSM_LICENSE, DEMO_INSURER, DISCLAIMER
    )
}

/**
 * SC-510 개인정보 처리 원칙. 정적 화면이며 입력이 없다. 카드 한 장 안에 원칙(SPEC)과 출처·안내를 함께 두고
 * 한 화면에 안 들어가면 카드 안에서 스크롤한다. "참고용" 문구는 하단 바 가운데에 둔다(PitCrewNav).
 */
@Composable
fun PrivacyScreen(modifier: Modifier = Modifier) {
    val colors = PitCrewTheme.colors
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
        Text(PrivacyTexts.TITLE, style = PitCrewTheme.typography.screenTitle, color = colors.textPrimary)
        Spacer(Modifier.height(PitCrewDimens.touchGap))
        PitCrewCard(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                // 원칙(SPEC SC-510)
                PrivacyLine(PitCrewIcons.ShieldCheck, PrivacyTexts.NOT_COLLECTED, note = PrivacyTexts.NOT_COLLECTED_NOTE)
                PrivacyLine(PitCrewIcons.ShieldCheck, PrivacyTexts.EXTERNAL)
                PrivacyLine(PitCrewIcons.ShieldCheck, PrivacyTexts.KEEP)
                PrivacyLine(PitCrewIcons.ShieldCheck, PrivacyTexts.DELETE)
                Divider()
                // 출처와 안내
                Text(
                    PrivacyTexts.SOURCES_TITLE,
                    style = PitCrewTheme.typography.cardTitle,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
                PrivacyLine(PitCrewIcons.Sun, WEATHER_CREDIT, note = PrivacyTexts.WEATHER_LICENSE)
                PrivacyLine(PitCrewIcons.Info, PrivacyTexts.WEATHER_LOG)
                PrivacyLine(PitCrewIcons.Pin, OSM_CREDIT, note = PrivacyTexts.OSM_LICENSE)
                PrivacyLine(PitCrewIcons.Building, PrivacyTexts.DEMO_INSURER)
            }
        }
    }
}

/** 아이콘 48 + 글자 한 줄(32). [note]가 있으면 아래에 28 textSecondary로 붙인다. 아이콘과 글자를 항상 함께 쓴다. */
@Composable
private fun PrivacyLine(icon: ImageVector, text: String, note: String? = null) {
    val colors = PitCrewTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(PitCrewDimens.iconSize))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(text, style = PitCrewTheme.typography.body.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
            if (note != null) Text(note, style = PitCrewTheme.typography.caption, color = colors.textSecondary)
        }
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .height(PitCrewDimens.cardBorder)
            .background(PitCrewTheme.colors.divider)
    )
}
