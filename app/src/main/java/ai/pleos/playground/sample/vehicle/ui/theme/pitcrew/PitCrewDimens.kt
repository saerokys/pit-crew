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
    // 사고 모드 단계 화면(SC-200, SC-210) 시안에서 읽은 값
    val stepScreenTopGap = 40.dp
    val stepScreenBottomGap = 32.dp
    val stepColumnGap = 32.dp
    val progressListWidth = 400.dp
    val progressListPadding = 18.dp
    val progressRowHeight = 80.dp
    val progressRowGap = 12.dp
    val progressCircle = 48.dp
    val checkItemMinHeight = 144.dp
    val checkItemCompactMinHeight = 112.dp // 항목이 5개인 SC-230용(예외)
    val infoCardWidth = 460.dp
    val infoIconSize = 56.dp
    val topBarButtonMinWidth = 248.dp
    val captionRowHeight = 76.dp
    // SC-220, SC-221 시안에서 읽은 값
    val choiceTileHeight = 458.dp
    val choiceTileBorder = 4.dp
    val emergencyBannerHeight = 128.dp
    val emergencyCardWidth = 640.dp
    // SC-250 시안에서 읽은 값
    val carPanelWidth = 480.dp
    // SC-260 시안에서 읽은 값
    val numberCircle = 64.dp
    val summaryChecklistWidth = 560.dp // 시안은 한 줄 카드였고 2열 구성으로 바꿔 정한 값
    // SC-300, SC-400 시안에서 읽은 값
    val summaryBadgeHeight = 96.dp // SC-300 오른쪽 위 요약 배지
    val statusRowHeight = 121.dp   // SC-400 행 높이(약 121)
    val homeButtonWidth = 400.dp   // SC-300 하단 바 [홈으로]
    val recheckButtonWidth = 520.dp // SC-300 하단 바 [다시 확인]
    // SC-205 시안에서 읽은 값
    val consentRowHeight = 134.dp
    val switchWidth = 136.dp
    val switchHeight = 76.dp
    val switchKnob = 60.dp
    // SC-270 시안에서 읽은 값
    val endingButtonHeight = 200.dp
    val endingTitleIcon = 96.dp
}

object PitCrewShapes {
    val card = RoundedCornerShape(PitCrewDimens.cornerRadius)
    val button = RoundedCornerShape(PitCrewDimens.cornerRadius)
    val pill = CircleShape
}
