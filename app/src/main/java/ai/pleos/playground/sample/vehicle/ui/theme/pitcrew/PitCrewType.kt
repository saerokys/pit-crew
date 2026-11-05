package ai.pleos.playground.sample.vehicle.ui.theme.pitcrew

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * docs/design/tokens.md 3절의 글자. 28sp 미만은 정의하지 않는다.
 * 글꼴은 시스템 기본(글꼴 파일 추가 안 함). 자간 -1%는 -0.01em.
 */
@Immutable
class PitCrewTypography(
    val title: TextStyle,
    val titleUrgent: TextStyle,
    val body: TextStyle,
    val caption: TextStyle,
    val button: TextStyle,
    val badge: TextStyle,
    val heroTitle: TextStyle,
    val heroSub: TextStyle,
    val screenTitle: TextStyle,
    val checkItem: TextStyle,
    val cardTitle: TextStyle,
    val tileLabel: TextStyle
)

private val tracking = (-0.01).em // 자간 -1%

val PitCrewDefaultTypography = PitCrewTypography(
    title = TextStyle(fontSize = 40.sp, lineHeight = 52.sp, fontWeight = FontWeight.Bold, letterSpacing = tracking),
    titleUrgent = TextStyle(fontSize = 72.sp, lineHeight = 84.sp, fontWeight = FontWeight.Bold, letterSpacing = tracking),
    body = TextStyle(fontSize = 32.sp, lineHeight = 45.sp, fontWeight = FontWeight.Normal, letterSpacing = tracking),
    caption = TextStyle(fontSize = 28.sp, lineHeight = 40.sp, fontWeight = FontWeight.Normal, letterSpacing = tracking),
    button = TextStyle(fontSize = 36.sp, lineHeight = 45.sp, fontWeight = FontWeight.Bold, letterSpacing = tracking),
    badge = TextStyle(fontSize = 28.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = tracking),
    heroTitle = TextStyle(fontSize = 80.sp, lineHeight = 96.sp, fontWeight = FontWeight.Bold, letterSpacing = tracking),
    heroSub = TextStyle(fontSize = 36.sp, lineHeight = 48.sp, fontWeight = FontWeight.Normal, letterSpacing = tracking),
    screenTitle = TextStyle(fontSize = 56.sp, lineHeight = 68.sp, fontWeight = FontWeight.Bold, letterSpacing = tracking),
    checkItem = TextStyle(fontSize = 40.sp, lineHeight = 52.sp, fontWeight = FontWeight.Bold, letterSpacing = tracking),
    cardTitle = TextStyle(fontSize = 36.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold, letterSpacing = tracking),
    tileLabel = TextStyle(fontSize = 48.sp, lineHeight = 60.sp, fontWeight = FontWeight.Bold, letterSpacing = tracking)
)
