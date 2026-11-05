package ai.pleos.playground.sample.vehicle.tts

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import java.util.Locale

/**
 * [소리로 듣기] 버튼의 상태.
 * - [IDLE]: 누를 수 있음, [PREPARING]: 준비 중(초기화·시작 대기), [SPEAKING]: 읽는 중(다시 누르면 멈춤)
 * - [SCREEN_ERROR]: 재생 오류. 그 화면에서만 "소리 안내 불가"(화면을 옮기면 [clearScreenError]로 풀림)
 * - [UNAVAILABLE]: 초기화 실패, 한국어·엔진 미지원, 권한 오류. 앱 실행 동안 "소리 안내 불가"
 */
enum class SpeechState { IDLE, PREPARING, SPEAKING, SCREEN_ERROR, UNAVAILABLE }

/**
 * TextToSpeech(`Mode.ON_DEVICE`, 한국어) 한 인스턴스를 필요할 때 만들어 쓴다.
 * - 사용자가 버튼을 눌렀을 때만 [speak]가 불린다(자동 재생 없음). 음성 스위치가 꺼져 있으면 호출하는 쪽이 부르지 않는다.
 * - 초기화는 한 번만, 이벤트 리스너도 한 번만 등록한다(초기화 후 등록). 말하는 중에는 새 [speak]를 받지 않는다.
 * - 모든 SDK 호출은 예외를 잡는다. 실패해도 앱은 계속 진행되고 상태만 바뀐다.
 * - 로그에는 이벤트 이름과 시간만 남기고 읽은 문장은 남기지 않는다.
 * 모든 메서드는 메인 스레드에서 부른다(SDK 콜백은 다른 스레드라 메인으로 옮겨 처리한다).
 */
class SpeechPlayer(context: Context, private val onChange: (SpeechState) -> Unit) {

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val tag = "PitCrewTTS"

    private var tts: TextToSpeech? = null
    private var initialized = false
    private var listenerAdded = false
    private var pendingText: String? = null
    private var speakIssued = false
    private var ignoreStopUntil = 0L
    private var engineUnavailable = false
    private var bornAt = 0L

    var state: SpeechState = SpeechState.IDLE
        private set

    private val startWatchdog = Runnable {
        Log.i(tag, "start timeout")
        failThisScreen()
    }
    private val playWatchdog = Runnable {
        Log.i(tag, "play timeout")
        failThisScreen()
    }

    private fun elapsed() = SystemClock.elapsedRealtime() - bornAt

    private fun set(newState: SpeechState) {
        if (state == newState) return
        state = newState
        onChange(newState)
    }

    private val listener = object : EventListener {
        override fun onReady() = Unit
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onStart() = main.post { handleStart() }.let { }
        override fun onDone() = main.post { handleDone() }.let { }
        override fun onStop() = main.post { handleStop() }.let { }
        override fun onError(errorType: ErrorType) = main.post { handleError(errorType) }.let { }
    }

    /** 사용자가 [소리로 듣기]를 눌렀을 때. [text]는 화면에 있는 문구를 이어 붙인 읽기 문장이다. */
    fun speak(text: String) {
        if (engineUnavailable) {
            set(SpeechState.UNAVAILABLE)
            return
        }
        if (state == SpeechState.PREPARING || state == SpeechState.SPEAKING || state == SpeechState.SCREEN_ERROR) return
        if (text.isBlank()) return
        pendingText = text
        speakIssued = false
        set(SpeechState.PREPARING)
        main.removeCallbacks(startWatchdog)
        main.postDelayed(startWatchdog, START_TIMEOUT_MS)
        if (tts == null) {
            createAndInitialize()
        } else if (initialized) {
            issueSpeak()
        }
        // 초기화 중이면 onSuccess에서 이어서 말한다.
    }

    private fun createAndInitialize() {
        try {
            bornAt = SystemClock.elapsedRealtime()
            val instance = TextToSpeech(appContext, Mode.ON_DEVICE)
            tts = instance
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
        val instance = tts ?: return
        Log.i(tag, "init success @${elapsed()}ms")
        initialized = true
        try {
            if (!listenerAdded) {
                instance.addEventListener(listener)
                listenerAdded = true
            }
            instance.setLocale(Locale.KOREA)
        } catch (e: Exception) {
            Log.w(tag, "listener/locale failed: ${e.javaClass.simpleName}")
            makeUnavailable()
            return
        }
        if (state == SpeechState.PREPARING) issueSpeak()
    }

    private fun handleInitFailure(errorType: ErrorType) {
        Log.i(tag, "init failure ${errorType.name} @${elapsed()}ms")
        makeUnavailable()
    }

    private fun issueSpeak() {
        val instance = tts ?: return
        val text = pendingText ?: return
        pendingText = null
        try {
            speakIssued = true
            instance.speak(text, true)
            Log.i(tag, "speak issued len=${text.length}")
        } catch (e: Exception) {
            Log.w(tag, "speak failed: ${e.javaClass.simpleName}")
            speakIssued = false
            failThisScreen()
        }
    }

    private fun handleStart() {
        if (state != SpeechState.PREPARING) return
        Log.i(tag, "onStart @${elapsed()}ms")
        main.removeCallbacks(startWatchdog)
        main.removeCallbacks(playWatchdog)
        main.postDelayed(playWatchdog, PLAY_TIMEOUT_MS)
        set(SpeechState.SPEAKING)
    }

    private fun handleDone() {
        if (state != SpeechState.SPEAKING && state != SpeechState.PREPARING) return
        Log.i(tag, "onDone @${elapsed()}ms")
        speakIssued = false
        cancelWatchdogs()
        set(SpeechState.IDLE)
    }

    private fun handleStop() {
        // 우리가 stop()을 부른 직후에 오는 onStop은 이미 처리했으므로 무시한다(바로 다시 누른 새 재생을 끊지 않게).
        if (SystemClock.elapsedRealtime() < ignoreStopUntil) return
        if (state != SpeechState.SPEAKING && state != SpeechState.PREPARING) return
        Log.i(tag, "onStop @${elapsed()}ms")
        speakIssued = false
        cancelWatchdogs()
        set(SpeechState.IDLE)
    }

    private fun handleError(errorType: ErrorType) {
        Log.i(tag, "onError ${errorType.name} @${elapsed()}ms")
        when (errorType) {
            // 엔진이나 환경 문제: 앱 실행 동안 쓸 수 없음
            ErrorType.UNAVAILABLE_LANGUAGE,
            ErrorType.NOT_SUPPORTED_API,
            ErrorType.ON_DEVICE_ENGINE_ERROR,
            ErrorType.PERMISSION_DENIED,
            ErrorType.UNAUTHENTICATED -> makeUnavailable()
            // 재생 오류: 그 화면에서만
            else -> failThisScreen()
        }
    }

    /** 사용자가 다시 눌렀거나 화면을 떠날 때. 읽는 중이면 멈추고 대기 중인 요청도 지운다. */
    fun stop() {
        pendingText = null
        cancelWatchdogs()
        if (speakIssued) {
            Log.i(tag, "stop requested")
            ignoreStopUntil = SystemClock.elapsedRealtime() + IGNORE_STOP_MS
            speakIssued = false
            try {
                tts?.stop()
            } catch (e: Exception) {
                Log.w(tag, "stop failed: ${e.javaClass.simpleName}")
            }
        }
        if (state == SpeechState.PREPARING || state == SpeechState.SPEAKING) set(SpeechState.IDLE)
    }

    /** 화면을 옮겼을 때: 재생 오류로 "소리 안내 불가"가 된 상태를 풀어 준다(초기화 실패는 풀지 않는다). */
    fun clearScreenError() {
        if (state == SpeechState.SCREEN_ERROR) set(SpeechState.IDLE)
    }

    private fun failThisScreen() {
        cancelWatchdogs()
        pendingText = null
        if (speakIssued) {
            ignoreStopUntil = SystemClock.elapsedRealtime() + IGNORE_STOP_MS
            speakIssued = false
            try {
                tts?.stop()
            } catch (e: Exception) {
            }
        }
        set(SpeechState.SCREEN_ERROR)
    }

    private fun makeUnavailable() {
        engineUnavailable = true
        cancelWatchdogs()
        pendingText = null
        speakIssued = false
        releaseInstance()
        set(SpeechState.UNAVAILABLE)
    }

    private fun cancelWatchdogs() {
        main.removeCallbacks(startWatchdog)
        main.removeCallbacks(playWatchdog)
    }

    private fun releaseInstance() {
        val instance = tts
        tts = null
        initialized = false
        val hadListener = listenerAdded
        listenerAdded = false
        if (instance == null) return
        try {
            if (hadListener) instance.removeEventListener(listener)
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
     * 읽기를 멈추고 SDK를 해제한다(사고 모드를 끝내고 홈에 도착했을 때, 뷰모델 정리 때).
     * 다시 쓰면 새 인스턴스를 만들어 초기화한다. [forgetUnavailable]이 참이면 "소리 안내 불가"(초기화 실패)도 잊는다.
     */
    fun release(forgetUnavailable: Boolean = false) {
        stop()
        releaseInstance()
        if (forgetUnavailable) engineUnavailable = false
        set(if (engineUnavailable) SpeechState.UNAVAILABLE else SpeechState.IDLE)
    }

    private companion object {
        const val START_TIMEOUT_MS = 3_000L
        const val PLAY_TIMEOUT_MS = 60_000L
        const val IGNORE_STOP_MS = 500L
    }
}
