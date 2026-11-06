package ai.pleos.playground.sample.vehicle.navi

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log

/**
 * NaviHelper 연결 상태(SC-280).
 * - [IDLE]: 연결하지 않음(SC-280 밖), [CONNECTING]: 연결 중, [READY]: 길안내 요청 가능, [UNAVAILABLE]: 연결 실패
 */
enum class NaviState { IDLE, CONNECTING, READY, UNAVAILABLE }

/**
 * 길안내 요청 결과(SC-280). `requestRoute()`는 반환값이 없어 이벤트로만 알 수 있다.
 * - [NONE]: 요청 전, [REQUESTING]: 요청했고 이벤트를 기다림, [STARTED]: 길안내 시작 이벤트가 옴,
 * - [ERROR]: 오류 이벤트나 요청 실패, [UNCONFIRMED]: 제한 시간 안에 이벤트가 오지 않음
 */
enum class RouteResult { NONE, REQUESTING, STARTED, ERROR, UNCONFIRMED }

interface NaviCallback {
    fun onNaviState(state: NaviState)
    fun onRouteResult(result: RouteResult)
}

/**
 * NaviHelper SDK를 감싼 클래스.
 * - [connect]는 SC-280에 들어올 때만, [release]는 화면을 떠날 때 등에 부른다. 연결은 `initialize(onConnected, onFailed)`로 하고
 *   (SDK가 내부에서 최대 30번, 0.5초 간격으로 재시도하고 모두 실패하면 onFailed), 리스너는 연결 성공 뒤 한 번만 등록한다.
 * - [requestRoute]는 사용자가 [길안내 시작]을 눌렀을 때만 부른다. 모든 SDK 호출은 예외를 잡는다(권한이 없으면 SecurityException).
 * - 로그에는 이벤트 종류, 오류 코드 이름, 시간만 남기고 목적지 이름과 좌표는 남기지 않는다.
 * 모든 메서드는 메인 스레드에서 부른다(SDK 콜백은 다른 스레드라 메인으로 옮겨 처리한다).
 */
class NaviRouter(context: Context, private val callback: NaviCallback) {

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val tag = "PitCrewNavi"

    private var helper: NaviHelper? = null
    private var listenerAdded = false
    private var bornAt = 0L

    var state: NaviState = NaviState.IDLE
        private set
    var result: RouteResult = RouteResult.NONE
        private set

    private val resultWatchdog = Runnable {
        Log.i(tag, "route result timeout @${elapsed()}ms")
        setResult(RouteResult.UNCONFIRMED)
    }

    private fun elapsed() = SystemClock.elapsedRealtime() - bornAt

    private fun setState(newState: NaviState) {
        if (state == newState) return
        state = newState
        callback.onNaviState(newState)
    }

    private fun setResult(newResult: RouteResult) {
        if (result == newResult) return
        result = newResult
        if (newResult != RouteResult.REQUESTING) main.removeCallbacks(resultWatchdog)
        callback.onRouteResult(newResult)
    }

    private val listener = object : NaviHelperEventListener {
        override fun onRouteStarted(routeStartInfo: RouteStartInfo) {
            main.post { handleEvent("onRouteStarted", RouteResult.STARTED) }
        }

        override fun onRoutingCompleted() {
            main.post { handleEvent("onRoutingCompleted", RouteResult.STARTED) }
        }

        override fun onError(errorCode: NaviErrorCode) {
            main.post { handleEvent("onError ${errorCode.name}", RouteResult.ERROR) }
        }
    }

    private fun handleEvent(name: String, newResult: RouteResult) {
        Log.i(tag, "$name @${elapsed()}ms")
        // 요청하지 않은 이벤트(다른 앱의 길안내 등)는 무시한다.
        if (result == RouteResult.REQUESTING || result == RouteResult.UNCONFIRMED) setResult(newResult)
    }

    /** SC-280에 들어올 때. 이미 연결 중이거나 연결됐으면 아무것도 하지 않는다. */
    fun connect() {
        if (helper != null) return
        setResult(RouteResult.NONE)
        setState(NaviState.CONNECTING)
        try {
            bornAt = SystemClock.elapsedRealtime()
            val instance = NaviHelper(appContext)
            helper = instance
            instance.initialize(
                onConnected = { main.post { handleConnected() } },
                onFailed = { throwable -> main.post { handleFailed(throwable.javaClass.simpleName) } }
            )
        } catch (e: Exception) {
            Log.w(tag, "create/initialize failed: ${e.javaClass.simpleName}")
            makeUnavailable()
        }
    }

    private fun handleConnected() {
        val instance = helper ?: return
        if (state != NaviState.CONNECTING) return
        Log.i(tag, "connected @${elapsed()}ms")
        try {
            // 연결 성공 뒤에 리스너를 한 번만 등록한다.
            if (!listenerAdded) {
                instance.addListener(listener)
                listenerAdded = true
            }
        } catch (e: Exception) {
            Log.w(tag, "addListener failed: ${e.javaClass.simpleName}")
            makeUnavailable()
            return
        }
        setState(NaviState.READY)
    }

    private fun handleFailed(reason: String) {
        if (helper == null) return
        Log.i(tag, "connect failed $reason @${elapsed()}ms")
        makeUnavailable()
    }

    /** 사용자가 [길안내 시작]([다시 시도])을 눌렀을 때. 연결되지 않았으면 부르지 않는다. */
    fun requestRoute(place: DemoPlace) {
        val instance = helper
        if (instance == null || state != NaviState.READY || result == RouteResult.REQUESTING) return
        try {
            setResult(RouteResult.REQUESTING)
            main.removeCallbacks(resultWatchdog)
            main.postDelayed(resultWatchdog, RESULT_TIMEOUT_MS)
            instance.requestRoute(
                RouteInfo(
                    latitude = place.latitude,
                    longitude = place.longitude,
                    poiName = place.name,
                    poiId = place.id,
                    address = "시연용 예시 장소",
                    poiSubId = "0",
                    poiProvider = "PitCrew-demo"
                )
            )
            Log.i(tag, "requestRoute issued @${elapsed()}ms")
        } catch (e: Exception) {
            Log.w(tag, "requestRoute failed: ${e.javaClass.simpleName}")
            setResult(RouteResult.ERROR)
        }
    }

    private fun makeUnavailable() {
        releaseInstance()
        setState(NaviState.UNAVAILABLE)
    }

    private fun releaseInstance() {
        main.removeCallbacks(resultWatchdog)
        val instance = helper
        helper = null
        val hadListener = listenerAdded
        listenerAdded = false
        if (instance == null) return
        try {
            if (hadListener) instance.removeListener(listener)
        } catch (e: Exception) {
        }
        try {
            instance.release()
            Log.i(tag, "released")
        } catch (e: Exception) {
            Log.w(tag, "release failed: ${e.javaClass.simpleName}")
        }
    }

    /**
     * 화면을 떠날 때, 홈 도착, 동의 초기화, 뷰모델 정리에서: removeListener -> release 하고 처음 상태로 돌린다.
     * 다시 쓰면 새 인스턴스를 만들어 연결한다.
     */
    fun release() {
        releaseInstance()
        setResult(RouteResult.NONE)
        setState(NaviState.IDLE)
    }

    private companion object {
        /** 길안내 시작 이벤트를 기다리는 시간(SDK의 요청은 반환값이 없어 이벤트로만 안다). */
        const val RESULT_TIMEOUT_MS = 10_000L
    }
}
