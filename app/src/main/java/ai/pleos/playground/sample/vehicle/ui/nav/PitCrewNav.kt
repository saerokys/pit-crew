package ai.pleos.playground.sample.vehicle.ui.nav

import ai.pleos.playground.sample.vehicle.ui.components.PitCrewBottomBar
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewButton
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewTopBar
import ai.pleos.playground.sample.vehicle.ui.home.HomeScreen
import ai.pleos.playground.sample.vehicle.ui.home.LinkSummary
import ai.pleos.playground.sample.vehicle.ui.home.StartupScreen
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 화면 이동 틀. 실제 화면은 Phase 3 이후에 route마다 채운다.
 * step은 SPEC 1.3의 진행률(SC-200 1/7 ... SC-260 7/7, SC-221은 3/7)이다.
 */
enum class PitCrewRoute(val screenId: String, val title: String, val step: Int?) {
    STARTUP("SC-000", "앱 시작", null),
    HOME("SC-100", "홈", null),
    STOP_CHECK("SC-200", "정차 확인", 1),
    SAFETY("SC-210", "안전 확보", 2),
    INJURY("SC-220", "부상 확인", 3),
    EMERGENCY("SC-221", "긴급 안내", 3),
    RECORD("SC-230", "현장 기록 안내", 4),
    OTHER_PARTY("SC-240", "상대방 정보 확인", 5),
    VEHICLE_STATE("SC-250", "차량 상태 기록", 6),
    SUMMARY("SC-260", "접수 요약", 7),
    PRE_DEPARTURE("SC-300", "출발 전 점검", null),
    VEHICLE_LINK("SC-400", "차량 연동 상태", null),
    SETTINGS("SC-500", "설정·정보", null)
}

/** 틀 확인용 직선 순서. SC-221 분기(부상 있어요/모르겠어요)는 Phase 3에서 만든다. */
private val AccidentFlow = listOf(
    PitCrewRoute.STOP_CHECK,
    PitCrewRoute.SAFETY,
    PitCrewRoute.INJURY,
    PitCrewRoute.RECORD,
    PitCrewRoute.OTHER_PARTY,
    PitCrewRoute.VEHICLE_STATE,
    PitCrewRoute.SUMMARY
)

/** 경로 스택. push로 들어가고 pop으로 직전 화면에 돌아간다. */
class PitCrewNavigator(start: PitCrewRoute) {
    private val stack = mutableStateListOf(start)
    val current: PitCrewRoute get() = stack.last()
    val canPop: Boolean get() = stack.size > 1
    fun push(route: PitCrewRoute) { stack.add(route) }
    fun pop(): Boolean = if (canPop) { stack.removeAt(stack.lastIndex); true } else false
    /** 시작 화면(SC-000)을 홈으로 바꾼다. 홈에서 뒤로 가도 SC-000으로 돌아오지 않는다. */
    fun replaceWithHome() { stack.clear(); stack.add(PitCrewRoute.HOME) }
    fun next(): PitCrewRoute? {
        val index = AccidentFlow.indexOf(current)
        return if (index < 0) null else AccidentFlow.getOrNull(index + 1)
    }
}

/**
 * 앱의 Pit Crew 화면 흐름: SC-000 -> SC-100 홈 -> (사고 모드 틀 | 자리표시 화면).
 * [topBarTrailing]은 상단 바 오른쪽 칸이다(디버그 빌드의 개발자 도구 버튼).
 * 홈에서 뒤로 가면 앱이 종료되는 시스템 기본 동작을 그대로 쓴다.
 */
@Composable
fun PitCrewNavHost(
    startupDone: Boolean,
    summary: LinkSummary,
    modifier: Modifier = Modifier,
    topBarTrailing: (@Composable () -> Unit)? = null
) {
    val navigator = remember { PitCrewNavigator(if (startupDone) PitCrewRoute.HOME else PitCrewRoute.STARTUP) }
    LaunchedEffect(startupDone) {
        if (startupDone && navigator.current == PitCrewRoute.STARTUP) navigator.replaceWithHome()
    }
    val route = navigator.current
    // SC-000에서는 뒤로 가기를 무시한다. 그 밖에는 직전 화면으로, 홈에서는 시스템 기본(앱 종료).
    BackHandler(enabled = route == PitCrewRoute.STARTUP || navigator.canPop) {
        if (route != PitCrewRoute.STARTUP) navigator.pop()
    }

    Column(modifier = modifier.fillMaxSize().background(PitCrewTheme.colors.background)) {
        val showSubtitle = route == PitCrewRoute.STARTUP || route == PitCrewRoute.HOME
        PitCrewTopBar(
            step = route.step,
            subtitle = if (showSubtitle) "사고 직후 정차부터 마무리까지 안내" else null,
            trailing = topBarTrailing
        )
        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            when (route) {
                PitCrewRoute.STARTUP -> StartupScreen()
                PitCrewRoute.HOME -> HomeScreen(
                    summary = summary,
                    onAccident = { navigator.push(PitCrewRoute.STOP_CHECK) },
                    onPreDeparture = { navigator.push(PitCrewRoute.PRE_DEPARTURE) },
                    onVehicleLink = { navigator.push(PitCrewRoute.VEHICLE_LINK) },
                    onSettings = { navigator.push(PitCrewRoute.SETTINGS) }
                )
                else -> PlaceholderScreen(route, onHome = { navigator.replaceWithHome() })
            }
        }
        if (route.step != null) {
            PitCrewBottomBar(
                onPrevious = { navigator.pop() },
                onNext = {
                    val next = navigator.next()
                    if (next != null) navigator.push(next) else navigator.replaceWithHome()
                },
                nextLabel = if (navigator.next() == null) "마치기" else "다음"
            )
        }
    }
}

/** 실제 화면이 아직 없는 route의 자리표시 화면. 흐름 밖 화면은 [홈으로]로 돌아간다. */
@Composable
private fun PlaceholderScreen(route: PitCrewRoute, onHome: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)
        ) {
            Text(
                "${route.screenId} ${route.title}",
                style = PitCrewTheme.typography.title,
                color = PitCrewTheme.colors.textPrimary
            )
            Text(
                "자리표시 화면 (실제 화면은 이후 Phase)",
                style = PitCrewTheme.typography.caption,
                color = PitCrewTheme.colors.textSecondary
            )
            if (route.step == null) {
                PitCrewButton(text = "홈으로", onClick = onHome, modifier = Modifier.width(PitCrewDimens.prevButtonWidth))
            }
        }
    }
}
