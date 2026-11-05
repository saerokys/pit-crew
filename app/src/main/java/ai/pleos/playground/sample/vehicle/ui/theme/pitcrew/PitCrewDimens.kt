package ai.pleos.playground.sample.vehicle.ui.theme.pitcrew

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** docs/design/tokens.md 1절·4절의 크기와 모양. 1px = 1dp. */
object PitCrewDimens {
    val safeMargin = 48.dp
    val touchMin = 96.dp          // 버튼, 체크 항목 한 줄
    val touchGap = 24.dp          // 터치 영역 사이
    val buttonHeight = 96.dp
    val cornerRadius = 20.dp
    val focusBorder = 4.dp
    val cardBorder = 2.dp
    val cardPadding = 32.dp
    val topBarHeight = 128.dp
    val bottomBarHeight = 128.dp
    val barVerticalMargin = 16.dp // 바 안의 버튼 위아래 여백 (128 - 96) / 2
    val checkboxSize = 64.dp
    val checkboxCorner = 12.dp    // tokens.md에 없음. SC-210 시안에서 눈으로 읽은 값(확인 필요)
    val prevButtonWidth = 320.dp  // SC-210 하단 바에서 읽음
    val nextButtonWidth = 640.dp  // SC-210 하단 바에서 읽음
    val iconSize = 48.dp
    val badgeHeight = 72.dp
    val topBarBadgeHeight = 56.dp
    val stepSegmentWidth = 48.dp
    val stepSegmentHeight = 16.dp
    val stepSegmentGap = 8.dp
    // 홈(SC-100) 시안에서 읽은 값
    val homeCardPadding = 48.dp
    val heroIconSize = 120.dp
    val menuIconSize = 64.dp
    val homeStatusTopGap = 32.dp
    val homeHintHeight = 84.dp
}

object PitCrewShapes {
    val card = RoundedCornerShape(PitCrewDimens.cornerRadius)
    val button = RoundedCornerShape(PitCrewDimens.cornerRadius)
    val pill = CircleShape
}
