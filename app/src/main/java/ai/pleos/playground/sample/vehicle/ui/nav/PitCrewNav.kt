package ai.pleos.playground.sample.vehicle.ui.nav

import ai.pleos.playground.sample.vehicle.ui.components.PitCrewBottomBar
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewButton
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewTopBar
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 화면 이동 틀. 실제 화면은 Phase 2 이후에 route마다 채운다.
 * step은 SPEC 1.3의 진행률(SC-200 1/7 ... SC-260 7/7, SC-221은 3/7)이다.
 */
enum class PitCrewRoute(val screenId: String, val title: String, val step: Int?) {
    HOME("SC-100", "홈", null),
    STOP_CHECK("SC-200", "정차 확인", 1),
    SAFETY("SC-210", "안전 확보", 2),
    INJURY("SC-220", "부상 확인", 3),
    EMERGENCY("SC-221", "긴급 안내", 3),
    RECORD("SC-230", "현장 기록 안내", 4),
    OTHER_PARTY("SC-240", "상대방 정보 확인", 5),
    VEHICLE_STATE("SC-250", "차량 상태 기록", 6),
    SUMMARY("SC-260", "접수 요약", 7)
}

/**
 * 틀 확인용 직선 순서. SC-221 분기(부상 있어요/모르겠어요)는 Phase 3에서 만든다.
 */
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
class PitCrewNavigator(start: PitCrewRoute = PitCrewRoute.HOME) {
    private val stack = mutableStateListOf(start)
    val current: PitCrewRoute get() = stack.last()
    val canPop: Boolean get() = stack.size > 1
    fun push(route: PitCrewRoute) { stack.add(route) }
    fun pop(): Boolean = if (canPop) { stack.removeAt(stack.lastIndex); true } else false
    fun next(): PitCrewRoute? {
        val index = AccidentFlow.indexOf(current)
        return if (index < 0) null else AccidentFlow.getOrNull(index + 1)
    }
}

/** 홈과 사고 모드 흐름의 틀. [onExit]은 홈에서 뒤로 갈 때(앱 밖으로 나가는 대신 개발 메뉴로) 부른다. */
@Composable
fun PitCrewNavHost(onExit: () -> Unit, modifier: Modifier = Modifier) {
    val navigator = remember { PitCrewNavigator() }
    BackHandler { if (!navigator.pop()) onExit() }
    val route = navigator.current

    Column(modifier = modifier.fillMaxSize().background(PitCrewTheme.colors.background)) {
        PitCrewTopBar(step = route.step)
        Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
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
                if (route == PitCrewRoute.HOME) {
                    // 틀 확인용 버튼. 실제 홈(SC-100)은 Phase 2에서 만든다.
                    PitCrewButton(
                        text = "사고가 났어요",
                        onClick = { navigator.push(PitCrewRoute.STOP_CHECK) },
                        modifier = Modifier.width(PitCrewDimens.nextButtonWidth)
                    )
                }
            }
        }
        if (route != PitCrewRoute.HOME) {
            PitCrewBottomBar(
                onPrevious = { navigator.pop() },
                onNext = {
                    val next = navigator.next()
                    if (next != null) navigator.push(next) else onExit()
                },
                nextLabel = if (navigator.next() == null) "마치기" else "다음"
            )
        }
    }
}
