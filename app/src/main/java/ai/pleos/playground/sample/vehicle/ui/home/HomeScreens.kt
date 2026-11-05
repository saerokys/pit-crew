package ai.pleos.playground.sample.vehicle.ui.home

import ai.pleos.playground.sample.vehicle.ui.components.BadgeStatus
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewCard
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewIcons
import ai.pleos.playground.sample.vehicle.ui.components.StatusBadge
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewShapes
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import ai.pleos.playground.sample.vehicle.vehicle.SupportState
import ai.pleos.playground.sample.vehicle.vehicle.VehicleFeature
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** 홈의 연동 상태 한 줄(배지 + 문구). 0개와 초기화 실패는 같은 "확인 불가"로 본다. */
data class LinkSummary(
    val status: BadgeStatus,
    val text: String,
    val note: String? = null
)

fun summarizeLink(initialized: Boolean, support: Map<VehicleFeature, SupportState>): LinkSummary {
    val total = VehicleFeature.entries.size
    val readable = if (initialized) support.values.count { it == SupportState.READABLE } else 0
    return when {
        readable == 0 -> LinkSummary(
            status = BadgeStatus.UNKNOWN,
            text = "확인 불가",
            note = "차량 정보를 읽을 수 없어요. 체크리스트는 그대로 쓸 수 있어요"
        )
        readable == total -> LinkSummary(BadgeStatus.OK, "차량 정보 ${total}개 중 ${readable}개를 읽을 수 있어요")
        else -> LinkSummary(BadgeStatus.WARN, "차량 정보 ${total}개 중 ${readable}개를 읽을 수 있어요")
    }
}

/** SC-000: 앱 시작과 차량 연동 확인 중 화면. 끝나면 바로 홈으로 간다. */
@Composable
fun StartupScreen(modifier: Modifier = Modifier) {
    val colors = PitCrewTheme.colors
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(96.dp),
            color = colors.accent,
            trackColor = colors.divider,
            strokeWidth = 8.dp
        )
        Spacer(Modifier.height(PitCrewDimens.touchGap))
        Text("차량 연동을 확인하고 있어요", style = PitCrewTheme.typography.body, color = colors.textPrimary)
        Text("잠시만 기다려 주세요", style = PitCrewTheme.typography.caption, color = colors.textSecondary)
    }
}

/** SC-100: 홈. 배치는 시안(SC-100.png), 문구와 동작은 SPEC. */
@Composable
fun HomeScreen(
    summary: LinkSummary,
    onAccident: () -> Unit,
    onPreDeparture: () -> Unit,
    onVehicleLink: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PitCrewTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = PitCrewDimens.safeMargin)
    ) {
        Spacer(Modifier.height(PitCrewDimens.homeStatusTopGap))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)
        ) {
            StatusBadge(summary.status, label = summary.text)
            summary.note?.let {
                Text(it, style = PitCrewTheme.typography.caption, color = colors.textSecondary)
            }
        }
        Spacer(Modifier.height(PitCrewDimens.touchGap))
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)
        ) {
            // 채운 포인트 카드는 이 화면에서 이것 하나뿐이다.
            AccidentTile(onClick = onAccident, modifier = Modifier.weight(1.5f).fillMaxHeight())
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)
            ) {
                MenuTile("출발 전 점검", PitCrewIcons.Clipboard, onPreDeparture, Modifier.weight(1f).fillMaxWidth())
                MenuTile("차량 연동 상태", PitCrewIcons.Car, onVehicleLink, Modifier.weight(1f).fillMaxWidth())
                MenuTile("설정·정보", PitCrewIcons.Sliders, onSettings, Modifier.weight(1f).fillMaxWidth())
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(PitCrewDimens.homeHintHeight),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "차를 안전하게 세운 뒤에 사용하세요",
                style = PitCrewTheme.typography.caption,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun AccidentTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = PitCrewTheme.colors
    Column(
        modifier = modifier
            .background(colors.accent, PitCrewShapes.card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(PitCrewDimens.homeCardPadding),
        verticalArrangement = Arrangement.Bottom
    ) {
        Icon(
            PitCrewIcons.Warning,
            contentDescription = null,
            tint = colors.onAccent,
            modifier = Modifier.size(PitCrewDimens.heroIconSize)
        )
        Spacer(Modifier.height(PitCrewDimens.touchGap))
        Text("사고가 났어요", style = PitCrewTheme.typography.heroTitle, color = colors.onAccent)
        Text("7단계로 차근차근 안내해요", style = PitCrewTheme.typography.heroSub, color = colors.onAccent)
    }
}

@Composable
private fun MenuTile(title: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = PitCrewTheme.colors
    PitCrewCard(modifier = modifier, onClick = onClick, padding = PitCrewDimens.homeCardPadding) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(PitCrewDimens.menuIconSize))
            Spacer(Modifier.width(PitCrewDimens.homeCardPadding / 2))
            Text(
                title,
                style = PitCrewTheme.typography.title,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                PitCrewIcons.ChevronRight,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(PitCrewDimens.iconSize)
            )
        }
    }
}
