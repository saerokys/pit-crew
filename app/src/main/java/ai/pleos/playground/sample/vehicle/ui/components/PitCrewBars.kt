package ai.pleos.playground.sample.vehicle.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** 단계 막대. total칸 중 current칸까지 accent, 나머지 divider 색. */
@Composable
fun StepBar(current: Int, modifier: Modifier = Modifier, total: Int = 7) {
    val colors = PitCrewTheme.colors
    Row(
        modifier = modifier.semantics { contentDescription = "단계 $current/$total" },
        horizontalArrangement = Arrangement.spacedBy(PitCrewDimens.stepSegmentGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .size(PitCrewDimens.stepSegmentWidth, PitCrewDimens.stepSegmentHeight)
                    .background(if (index < current) colors.accent else colors.divider, PitCrewShapes.pill)
            )
        }
    }
}

/** 상단 바의 "가상 차량 값" 배지: surface 배경, accent 테두리 2, 알약. 터치 대상이 아니다. */
@Composable
fun VirtualValueBadge(modifier: Modifier = Modifier) {
    val colors = PitCrewTheme.colors
    Box(
        modifier = modifier
            .height(PitCrewDimens.topBarBadgeHeight)
            .background(colors.surface, PitCrewShapes.pill)
            .border(BorderStroke(PitCrewDimens.cardBorder, colors.accent), PitCrewShapes.pill)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("가상 차량 값", style = PitCrewTheme.typography.badge, color = colors.textSecondary)
    }
}

/**
 * 상단 바(높이 128, bar 색): 앱 아이콘, "Pit Crew", (선택) 설명, (사고 모드만) 단계 막대와 "단계 n/7", 가상 차량 값 배지.
 * [trailing]은 배지 오른쪽 자리(예: SC-210의 소리로 듣기 버튼)이다.
 */
@Composable
fun PitCrewTopBar(
    modifier: Modifier = Modifier,
    step: Int? = null,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = PitCrewTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PitCrewDimens.topBarHeight)
            .background(colors.bar)
            .padding(horizontal = PitCrewDimens.safeMargin),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            PitCrewIcons.Car,
            contentDescription = null,
            tint = colors.textPrimary,
            modifier = Modifier.size(PitCrewDimens.iconSize)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text("Pit Crew", style = PitCrewTheme.typography.title, color = colors.textPrimary)
            if (subtitle != null) {
                Text(subtitle, style = PitCrewTheme.typography.caption, color = colors.textSecondary)
            }
        }
        if (step != null) {
            Spacer(Modifier.width(PitCrewDimens.safeMargin))
            StepBar(current = step)
            Spacer(Modifier.width(16.dp))
            Text(
                "단계 $step/7",
                style = PitCrewTheme.typography.body.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )
        }
        Spacer(Modifier.weight(1f))
        VirtualValueBadge()
        if (trailing != null) {
            Spacer(Modifier.width(PitCrewDimens.touchGap))
            trailing()
        }
    }
}

/**
 * 하단 바(높이 128, bar 색): 왼쪽 [이전](기본 버튼), 오른쪽 가장 큰 [다음](채운 주요 버튼).
 * [onPrevious]가 null이면 [이전]을, [onNext]가 null이면 [다음]을 그리지 않는다(선택 버튼으로만 이동하는 화면).
 * 채운 버튼은 이 [다음] 하나뿐이다([nextFilled]가 거짓이면 기본 버튼, SC-290에서 보내기 전).
 * [extraLabel]이 있으면 [다음] 왼쪽에 기본 버튼을 하나 더 둔다(SC-260의 [보험사로 전달]).
 * [centerText]가 있으면 버튼 사이 빈 칸 가운데에 안내 글자(글자 28, textSecondary)를 둔다(SC-280의 "참고용" 문구).
 * 기본값은 null이라 쓰지 않는 화면은 모양이 그대로다. 두 줄까지 들어간다.
 */
@Composable
fun PitCrewBottomBar(
    onNext: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onPrevious: (() -> Unit)? = null,
    nextLabel: String = "다음",
    previousLabel: String = "이전",
    previousIcon: ImageVector? = PitCrewIcons.ChevronLeft,
    previousWidth: Dp = PitCrewDimens.prevButtonWidth,
    nextIcon: ImageVector? = PitCrewIcons.ChevronRight,
    nextWidth: Dp = PitCrewDimens.nextButtonWidth,
    nextEnabled: Boolean = true,
    nextFilled: Boolean = true,
    extraLabel: String? = null,
    onExtra: (() -> Unit)? = null,
    extraIcon: ImageVector? = null,
    extraWidth: Dp = PitCrewDimens.recheckButtonWidth,
    centerText: String? = null,
    centerIcon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PitCrewDimens.bottomBarHeight)
            .background(PitCrewTheme.colors.bar)
            .padding(horizontal = PitCrewDimens.safeMargin, vertical = PitCrewDimens.barVerticalMargin),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onPrevious != null) {
            PitCrewButton(
                text = previousLabel,
                onClick = onPrevious,
                leadingIcon = previousIcon,
                modifier = Modifier.width(previousWidth)
            )
        }
        if (centerText != null) {
            Row(
                modifier = Modifier.weight(1f).padding(horizontal = PitCrewDimens.touchGap),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (centerIcon != null) {
                    Icon(centerIcon, contentDescription = null, tint = PitCrewTheme.colors.textSecondary, modifier = Modifier.size(PitCrewDimens.iconSize))
                    Spacer(Modifier.width(16.dp))
                }
                Text(centerText, style = PitCrewTheme.typography.caption, color = PitCrewTheme.colors.textSecondary, modifier = Modifier.weight(1f, fill = false))
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
        if (extraLabel != null && onExtra != null) {
            // 기본 버튼(채운 버튼은 [다음] 하나뿐). 터치 영역 높이 96, 오른쪽 [다음]과 간격 24.
            PitCrewButton(
                text = extraLabel,
                onClick = onExtra,
                leadingIcon = extraIcon,
                modifier = Modifier.width(extraWidth)
            )
            Spacer(Modifier.width(PitCrewDimens.touchGap))
        }
        if (onNext != null) {
            PitCrewButton(
                text = nextLabel,
                onClick = onNext,
                filled = nextFilled,
                enabled = nextEnabled,
                trailingIcon = nextIcon,
                modifier = Modifier.width(nextWidth)
            )
        }
    }
}
