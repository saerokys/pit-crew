package ai.pleos.playground.sample.vehicle.stt

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import java.util.Locale

/**
 * 음성 메모 인식 상태(SC-230 음성 카드).
 * - [IDLE]: 대기([말하기]를 누를 수 있음), [PREPARING]: 준비 중(초기화, 시작 대기), [LISTENING]: 듣는 중
 * - [UNAVAILABLE]: 초기화 실패, 한국어·엔진 미지원, 권한 오류. 앱 실행 동안 음성 카드를 비활성으로 둔다.
 */
enum class VoiceState { IDLE, PREPARING, LISTENING, UNAVAILABLE }

/** 인식 결과를 받는 쪽(뷰모델). 모든 콜백은 메인 스레드에서 온다. */
interface VoiceMemoCallback {
    fun onState(state: VoiceState)

    /** 최종 결과(공백 정리, 최대 [VoiceMemoText.MAX_CHARS]자). 비어 있으면 오지 않는다. [cut]은 글자 수 제한으로 잘렸는지. */
    fun onFinalText(text: String, cut: Boolean)

    /** 못 알아들었음(결과 없음, 시간 초과, 비치명 오류)으로 대기로 돌아갈 때. 엔진·마이크·권한·언어 오류와 초기화 실패(사용할 수 없음)에는 오지 않는다. */
    fun onNoResult()
}

/**
 * SpeechToText(`Mode.ON_DEVICE`, 한국어)로 음성 메모를 한 번 듣는다.
 * - 사용자가 [말하기]를 눌렀을 때만 [start]가 불린다. 음성 스위치가 꺼져 있으면 호출하는 쪽이 부르지 않고, 이 클래스는 SDK 객체를 만들지 않는다.
 * - 시도마다 새 인스턴스를 만들고 `initialize` 성공 뒤에 리스너를 등록한다(리스너는 시도당 한 번). 끝나면(결과, 오류, 취소) 바로 해제한다.
 * - 해제 순서는 stop -> removeListener -> release 이다. 모든 SDK 호출은 예외를 잡고, 실패해도 앱은 계속 진행된다.
 * - 오디오는 받지도 저장하지도 않는다(SDK가 돌려주는 텍스트만 쓴다). 로그에는 이벤트 이름, 시간, 글자 수만 남기고 메모 내용은 남기지 않는다.
 * 모든 메서드는 메인 스레드에서 부른다(SDK 콜백은 다른 스레드라 메인으로 옮겨 처리한다).
 */
class VoiceMemoRecognizer(context: Context, private val callback: VoiceMemoCallback) {

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val tag = "PitCrewSTT"

    private var stt: SpeechToText? = null
    private var listenerAdded = false
    private var unavailable = false
    private var lastPartial = ""
    private var bornAt = 0L

    var state: VoiceState = VoiceState.IDLE
        private set

    private val startWatchdog = Runnable {
        Log.i(tag, "start timeout @${elapsed()}ms")
        finishAttempt(VoiceState.IDLE)
        callback.onNoResult()
    }
    private val listenWatchdog = Runnable {
        Log.i(tag, "listen timeout @${elapsed()}ms")
        // 너무 오래 듣고 있으면 멈추고, 그때까지 들은 중간 결과가 있으면 그것을 결과로 쓴다.
        val partial = lastPartial
        finishAttempt(VoiceState.IDLE)
        deliver(partial)
    }

    private fun elapsed() = SystemClock.elapsedRealtime() - bornAt

    private fun set(newState: VoiceState) {
        if (state == newState) return
        state = newState
        callback.onState(newState)
    }

    private val resultListener = object : ResultListener {
        // onReady: 서비스가 마이크를 열고 말을 받을 준비가 됨(에뮬레이터 확인) -> "듣는 중".
        // onStartedRecognition: 말소리가 감지됨(조용하면 오지 않음, 에뮬레이터 확인).
        override fun onReady() { main.post { handleReady() } }
        override fun onStartedRecognition() { main.post { Log.i(tag, "onStartedRecognition @${elapsed()}ms") } }
        override fun onUpdated(stt: String, completed: Boolean) {
            // 내용은 로그에 남기지 않는다(글자 수만).
            main.post { handleUpdated(stt, completed) }
        }
        override fun onUpdatedEpdData(on: Long, off: Long) = Unit
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onEndedRecognition() { main.post { handleEnded() } }
        override fun onError(errorType: ErrorType) { main.post { handleError(errorType) } }
    }

    /** 사용자가 [말하기]를 눌렀을 때. 이미 준비 중이거나 듣는 중이면 무시한다. */
    fun start() {
        if (unavailable) {
            set(VoiceState.UNAVAILABLE)
            return
        }
        if (state == VoiceState.PREPARING || state == VoiceState.LISTENING) return
        lastPartial = ""
        set(VoiceState.PREPARING)
        main.removeCallbacks(startWatchdog)
        main.postDelayed(startWatchdog, START_TIMEOUT_MS)
        createAndInitialize()
    }

    private fun createAndInitialize() {
        try {
            bornAt = SystemClock.elapsedRealtime()
            val instance = SpeechToText(appContext, Mode.ON_DEVICE)
            stt = instance
            instance.initialize(object : OnInitializeListener {
                override fun onSuccess() { main.post { handleInitSuccess() } }
                override fun onFailure(errorType: ErrorType) { main.post { handleInitFailure(errorType) } }
            })
        } catch (e: Exception) {
            Log.w(tag, "create/initialize failed: ${e.javaClass.simpleName}")
            makeUnavailable()
        }
    }

    private fun handleInitSuccess() {
        val instance = stt ?: return
        if (state != VoiceState.PREPARING) return
        Log.i(tag, "init success @${elapsed()}ms")
        try {
            // 초기화 성공 뒤에 리스너를 등록한다(TTS에서 순서 오류가 있었다).
            instance.addListener(resultListener)
            listenerAdded = true
            instance.setLocale(Locale.KOREA)
            instance.request()
            Log.i(tag, "request issued @${elapsed()}ms")
        } catch (e: Exception) {
            Log.w(tag, "listener/locale/request failed: ${e.javaClass.simpleName}")
            makeUnavailable()
        }
    }

    /** 듣는 중에 사용자가 [멈추기]를 눌렀을 때. 그때까지 들은 결과가 있으면 그것을 결과로 쓰고, 없으면 대기로 돌아간다. */
    fun stopListening() {
        if (state != VoiceState.LISTENING) return
        Log.i(tag, "stop by user len=${lastPartial.length} @${elapsed()}ms")
        val partial = lastPartial
        finishAttempt(VoiceState.IDLE)
        deliver(partial)
    }

    private fun handleInitFailure(errorType: ErrorType) {
        Log.i(tag, "init failure ${errorType.name} @${elapsed()}ms")
        makeUnavailable()
    }

    private fun handleReady() {
        if (state != VoiceState.PREPARING) return
        Log.i(tag, "onReady @${elapsed()}ms")
        main.removeCallbacks(startWatchdog)
        main.removeCallbacks(listenWatchdog)
        main.postDelayed(listenWatchdog, LISTEN_MAX_MS)
        set(VoiceState.LISTENING)
    }

    private fun handleUpdated(text: String, completed: Boolean) {
        if (state != VoiceState.LISTENING && state != VoiceState.PREPARING) return
        Log.i(tag, "onUpdated completed=$completed len=${text.length} @${elapsed()}ms")
        lastPartial = text
        if (completed) {
            finishAttempt(VoiceState.IDLE)
            deliver(text)
        }
    }

    private fun handleEnded() {
        if (state != VoiceState.LISTENING && state != VoiceState.PREPARING) return
        Log.i(tag, "onEndedRecognition len=${lastPartial.length} @${elapsed()}ms")
        // 최종 결과 없이 끝났으면 마지막 중간 결과를 쓴다(없으면 아무것도 안 함: 다시 말하기).
        val partial = lastPartial
        finishAttempt(VoiceState.IDLE)
        deliver(partial)
    }

    private fun handleError(errorType: ErrorType) {
        Log.i(tag, "onError ${errorType.name} @${elapsed()}ms")
        when (errorType) {
            // 엔진, 환경, 권한 문제: 앱 실행 동안 쓸 수 없음
            ErrorType.UNAVAILABLE_LANGUAGE,
            ErrorType.NOT_SUPPORTED_API,
            ErrorType.ON_DEVICE_ENGINE_ERROR,
            ErrorType.PERMISSION_DENIED,
            ErrorType.UNAUTHENTICATED,
            ErrorType.MIC_ERROR,
            ErrorType.AUDIO_ERROR -> makeUnavailable()
            // 그 밖의 오류: 이번 시도만 끝내고 다시 말할 수 있게 둔다("잘 듣지 못했어요")
            else -> {
                finishAttempt(VoiceState.IDLE)
                callback.onNoResult()
            }
        }
    }

    /** 결과를 정리해 전달한다. 앞뒤 공백을 지우고 최대 [VoiceMemoText.MAX_CHARS]자로 자르며, 비어 있으면 결과 대신 "못 알아들음"을 알린다. */
    private fun deliver(raw: String) {
        val text = VoiceMemoText.clean(raw)
        if (text.isEmpty()) {
            Log.i(tag, "no text")
            callback.onNoResult()
            return
        }
        val cut = VoiceMemoText.wasCut(raw)
        Log.i(tag, "final len=${text.length} cut=$cut")
        callback.onFinalText(text, cut)
    }

    /** 이번 시도를 끝낸다: stop -> removeListener -> release 하고 [newState]로 바꾼다. */
    private fun finishAttempt(newState: VoiceState) {
        main.removeCallbacks(startWatchdog)
        main.removeCallbacks(listenWatchdog)
        releaseInstance()
        set(if (unavailable) VoiceState.UNAVAILABLE else newState)
    }

    private fun makeUnavailable() {
        unavailable = true
        main.removeCallbacks(startWatchdog)
        main.removeCallbacks(listenWatchdog)
        releaseInstance()
        set(VoiceState.UNAVAILABLE)
    }

    private fun releaseInstance() {
        val instance = stt
        stt = null
        val hadListener = listenerAdded
        listenerAdded = false
        lastPartial = ""
        if (instance == null) return
        try {
            instance.stop()
        } catch (e: Exception) {
        }
        try {
            if (hadListener) instance.removeListener(resultListener)
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
     * 듣기를 멈추고 SDK를 해제한다(화면 이동, 앱이 가려질 때, 홈 도착, 동의 초기화, 뷰모델 정리).
     * 다시 쓰면 새 인스턴스를 만들어 초기화한다. [forgetUnavailable]이 참이면 "사용할 수 없음"(초기화 실패)도 잊는다.
     */
    fun release(forgetUnavailable: Boolean = false) {
        main.removeCallbacks(startWatchdog)
        main.removeCallbacks(listenWatchdog)
        releaseInstance()
        if (forgetUnavailable) unavailable = false
        set(if (unavailable) VoiceState.UNAVAILABLE else VoiceState.IDLE)
    }

    companion object {
        private const val START_TIMEOUT_MS = 10_000L
        private const val LISTEN_MAX_MS = 30_000L
    }
}
