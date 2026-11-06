package ai.pleos.playground.sample.vehicle.ui.destination

import ai.pleos.playground.sample.vehicle.navi.DemoPlace
import ai.pleos.playground.sample.vehicle.navi.DemoPlaces
import ai.pleos.playground.sample.vehicle.navi.DestinationCategory
import ai.pleos.playground.sample.vehicle.navi.NaviState
import ai.pleos.playground.sample.vehicle.navi.RouteResult
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewButton
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewCard
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewIcons
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewShapes
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** SC-280 화면 문구. 모든 글자는 28 이상이다. [DISCLAIMER]는 하단 바 가운데에 쓴다(PitCrewNav). */
object DestinationTexts {
    const val TITLE = "어디로 갈까요?"
    const val CANDIDATES = "예시 후보"
    const val EXAMPLE_NOTICE = "시연용 예시 장소예요. 실제 추천이 아니에요"
    const val HOSPITAL_EMERGENCY = "응급 상황이면 119에 먼저 연락하세요"
    const val START = "길안내 시작"
    const val RETRY = "다시 시도"
    const val CONNECTING = "연결 중…"
    const val REQUESTING = "요청 중…"
    const val START_HINT = "[길안내 시작]을 눌러야만 길안내가 시작돼요"
    const val UNAVAILABLE = "길안내를 쓸 수 없어요"
    const val ERROR = "길안내를 시작하지 못했어요"
    const val UNCONFIRMED = "길안내가 시작됐는지 확인하지 못했어요"
    const val UNCONFIRMED_HINT = "내비게이션 앱이 열려 있는지 확인해 주세요."
    const val STARTED = "길안내를 시작했어요"
    const val NO_CANDIDATES = "후보를 찾지 못했어요"
    const val DISCLAIMER = "참고용 안내예요. 길안내는 직접 확인하고 판단하세요. 법률·의료 조언이 아니에요."
    const val SELECTED = "선택됨"
}

private fun iconFor(category: DestinationCategory): ImageVector = when (category) {
    DestinationCategory.SAFE -> PitCrewIcons.ShieldCheck
    DestinationCategory.GARAGE -> PitCrewIcons.Wrench
    DestinationCategory.HOSPITAL -> PitCrewIcons.PlusBox
}

/**
 * SC-280 다음 장소 안내. 카드 3개(안전한 곳으로 이동 / 정비소 / 병원)를 고르면 시연용 예시 후보를 보여 주고,
 * [길안내 시작]을 눌러야만 NaviHelper로 목적지를 전달한다. 후보는 앱에 넣은 상수라 거리는 보여 주지 않고(사용자 위치를 쓰지 않음)
 * "실제 추천이 아니에요"를 항상 보인다. 입력창은 없다(규칙 14). 채운 버튼은 [길안내 시작] 하나뿐이다.
 */
@Composable
fun DestinationScreen(
    category: DestinationCategory,
    placeId: String?,
    naviState: NaviState,
    routeResult: RouteResult,
    onSelectCategory: (DestinationCategory) -> Unit,
    onSelectPlace: (String) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PitCrewTheme.colors
    val places = DemoPlaces.forCategory(category)
    val selected = DemoPlaces.find(category, placeId)

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
        Text(DestinationTexts.TITLE, style = PitCrewTheme.typography.screenTitle, color = colors.textPrimary)
        Spacer(Modifier.height(PitCrewDimens.touchGap))
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PitCrewDimens.stepColumnGap)
        ) {
            // 왼쪽: 목적지 유형 카드 3개
            Column(
                modifier = Modifier.width(CATEGORY_WIDTH).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)
            ) {
                DestinationCategory.entries.forEach { item ->
                    CategoryCard(
                        category = item,
                        selected = item == category,
                        onClick = { onSelectCategory(item) },
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }
            }
            // 오른쪽: 예시 후보, 안내, [길안내 시작]
            PitCrewCard(modifier = Modifier.weight(1f).fillMaxHeight(), padding = 24.dp) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 스크롤은 쓰지 않는다. 후보 3개, 119, 상태 줄, 버튼이 한 화면에 모두 들어가야 한다.
                    Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        // 제목과 "시연용 예시 장소예요…"를 한 줄에 둔다(세로 공간을 아끼려고). 좁으면 안내 쪽이 줄바꿈된다.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${DestinationTexts.CANDIDATES} ${places.size}곳",
                                style = PitCrewTheme.typography.cardTitle,
                                color = colors.textPrimary
                            )
                            Spacer(Modifier.width(PitCrewDimens.touchGap))
                            Box(modifier = Modifier.weight(1f)) {
                                NoticeLine(PitCrewIcons.Info, colors.textSecondary, DestinationTexts.EXAMPLE_NOTICE, bold = true)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        if (places.isEmpty()) {
                            NoticeLine(PitCrewIcons.Warning, colors.statusWarn, DestinationTexts.NO_CANDIDATES, bold = true)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)) {
                                places.forEach { place ->
                                    PlaceRow(place = place, selected = place.id == selected?.id, onClick = { onSelectPlace(place.id) })
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // 병원을 골랐을 때 항상 보이는 줄(스크롤 밖에 둔다).
                    if (category == DestinationCategory.HOSPITAL) {
                        NoticeLine(PitCrewIcons.Phone, colors.statusUrgentText, DestinationTexts.HOSPITAL_EMERGENCY, bold = true)
                        Spacer(Modifier.height(12.dp))
                    }
                    StatusLine(naviState = naviState, routeResult = routeResult)
                    val label = when {
                        naviState == NaviState.CONNECTING || naviState == NaviState.IDLE -> DestinationTexts.CONNECTING
                        routeResult == RouteResult.REQUESTING -> DestinationTexts.REQUESTING
                        routeResult == RouteResult.ERROR || routeResult == RouteResult.UNCONFIRMED -> DestinationTexts.RETRY
                        else -> DestinationTexts.START
                    }
                    val enabled = naviState == NaviState.READY && selected != null && routeResult != RouteResult.REQUESTING
                    PitCrewButton(
                        text = label,
                        onClick = onStart,
                        enabled = enabled,
                        filled = true,
                        leadingIcon = PitCrewIcons.Send,
                        modifier = Modifier.fillMaxWidth().height(PitCrewDimens.touchMin)
                    )
                    // "[길안내 시작]을 눌러야만…"는 상태 줄이 없을 때만 보인다(상태 줄이 있으면 이미 눌렀다는 뜻이라 중복).
                    if (!hasStatusLine(naviState, routeResult)) {
                        Spacer(Modifier.height(8.dp))
                        NoticeLine(PitCrewIcons.Info, colors.textSecondary, DestinationTexts.START_HINT, bold = false)
                    }
                }
            }
        }
    }
}

private val CATEGORY_WIDTH = 560.dp

/** 상태 줄이 보이는지(연결 실패, 오류, 확인 못 함, 시작함). */
private fun hasStatusLine(naviState: NaviState, routeResult: RouteResult): Boolean =
    naviState == NaviState.UNAVAILABLE ||
        routeResult == RouteResult.ERROR || routeResult == RouteResult.UNCONFIRMED || routeResult == RouteResult.STARTED

/** 연결·요청 결과 한 줄. 연결 중과 요청 전에는 보이지 않는다. 아이콘과 글자를 함께 쓴다. */
@Composable
private fun StatusLine(naviState: NaviState, routeResult: RouteResult) {
    val colors = PitCrewTheme.colors
    val line: Triple<ImageVector, Color, String>? = when {
        naviState == NaviState.UNAVAILABLE -> Triple(PitCrewIcons.Warning, colors.statusWarn, DestinationTexts.UNAVAILABLE)
        routeResult == RouteResult.ERROR -> Triple(PitCrewIcons.Warning, colors.statusWarn, DestinationTexts.ERROR)
        routeResult == RouteResult.UNCONFIRMED -> Triple(PitCrewIcons.Info, colors.textSecondary, DestinationTexts.UNCONFIRMED)
        routeResult == RouteResult.STARTED -> Triple(PitCrewIcons.CheckCircle, colors.statusOk, DestinationTexts.STARTED)
        else -> null
    }
    if (line != null) {
        NoticeLine(line.first, line.second, line.third, bold = true)
        Spacer(Modifier.height(12.dp))
        // 대기가 끝났는데 이벤트가 오지 않은 경우: 내비 앱 화면이 열려 있지 않으면 요청이 버려진다(PROGRESS 5절).
        if (routeResult == RouteResult.UNCONFIRMED && naviState != NaviState.UNAVAILABLE) {
            NoticeLine(PitCrewIcons.Info, colors.textSecondary, DestinationTexts.UNCONFIRMED_HINT, bold = false)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun NoticeLine(icon: ImageVector, tint: Color, text: String, bold: Boolean) {
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

/** 목적지 유형 카드. 선택되면 accent 테두리와 surfaceSelected 배경, "✓ 선택됨"(색만으로 구분하지 않음). */
@Composable
private fun CategoryCard(category: DestinationCategory, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = PitCrewTheme.colors
    Row(
        modifier = modifier
            .background(if (selected) colors.surfaceSelected else colors.surface, PitCrewShapes.card)
            .border(BorderStroke(if (selected) PitCrewDimens.choiceTileBorder else PitCrewDimens.cardBorder, if (selected) colors.accent else colors.divider), PitCrewShapes.card)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(iconFor(category), contentDescription = null, tint = colors.accent, modifier = Modifier.size(PitCrewDimens.menuIconSize))
        Spacer(Modifier.width(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(category.title, style = PitCrewTheme.typography.title, color = colors.textPrimary)
            Text(category.subtitle, style = PitCrewTheme.typography.caption, color = colors.textSecondary)
        }
        if (selected) {
            Icon(PitCrewIcons.Check, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(PitCrewDimens.iconSize))
            Spacer(Modifier.width(8.dp))
            Text(
                DestinationTexts.SELECTED,
                style = PitCrewTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )
        }
    }
}

/** 후보 한 줄(터치 영역 96 이상). 거리는 보여 주지 않는다. */
@Composable
private fun PlaceRow(place: DemoPlace, selected: Boolean, onClick: () -> Unit) {
    val colors = PitCrewTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = PitCrewDimens.touchMin)
            .background(if (selected) colors.surfaceSelected else colors.surface, PitCrewShapes.card)
            .border(BorderStroke(PitCrewDimens.cardBorder, if (selected) colors.accent else colors.divider), PitCrewShapes.card)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (selected) PitCrewIcons.CheckCircle else PitCrewIcons.MinusCircle,
            contentDescription = null,
            tint = if (selected) colors.accent else colors.textSecondary,
            modifier = Modifier.size(PitCrewDimens.iconSize)
        )
        Spacer(Modifier.width(16.dp))
        Text(place.name, style = PitCrewTheme.typography.cardTitle, color = colors.textPrimary)
    }
}
