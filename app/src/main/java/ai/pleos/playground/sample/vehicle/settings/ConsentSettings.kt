package ai.pleos.playground.sample.vehicle.settings

import android.content.Context

/**
 * SC-205 동의 상태. 저장하는 값은 스위치 3개의 켜짐/꺼짐과 "이미 정했는지"뿐이다(좌표, 주소 등은 저장하지 않는다).
 * 기본값은 모두 꺼짐이다.
 */
data class Consent(
    val location: Boolean = false,
    val voice: Boolean = false,
    val network: Boolean = false,
    /** SC-205에서 [다음]을 눌러 정했는지. 정했으면 사고 모드 진입 때 SC-205를 건너뛴다. */
    val decided: Boolean = false
) {
    /** SC-200 아래에 보여 주는 한 줄. */
    fun summaryLine(): String =
        "위치 ${onOff(location)} · 음성 ${onOff(voice)} · 네트워크 ${onOff(network)}"

    private fun onOff(value: Boolean) = if (value) "켜짐" else "꺼짐"
}

/** SharedPreferences 한 파일에 [Consent]를 읽고 쓴다. 저장소 오류가 나도 기본값(모두 꺼짐)으로 동작한다. */
class ConsentStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(): Consent = try {
        Consent(
            location = prefs.getBoolean(KEY_LOCATION, false),
            voice = prefs.getBoolean(KEY_VOICE, false),
            network = prefs.getBoolean(KEY_NETWORK, false),
            decided = prefs.getBoolean(KEY_DECIDED, false)
        )
    } catch (e: Exception) {
        Consent()
    }

    fun save(consent: Consent) {
        try {
            prefs.edit()
                .putBoolean(KEY_LOCATION, consent.location)
                .putBoolean(KEY_VOICE, consent.voice)
                .putBoolean(KEY_NETWORK, consent.network)
                .putBoolean(KEY_DECIDED, consent.decided)
                .apply()
        } catch (e: Exception) {
            // 저장 실패는 무시한다(다음 실행에서 다시 묻는다)
        }
    }

    /** 디버그 빌드의 [동의 초기화]용. 동의 설정만 지우고 권한은 건드리지 않는다. */
    fun clear() {
        try {
            prefs.edit().clear().apply()
        } catch (e: Exception) {
        }
    }

    private companion object {
        const val FILE = "pitcrew_consent"
        const val KEY_LOCATION = "location"
        const val KEY_VOICE = "voice"
        const val KEY_NETWORK = "network"
        const val KEY_DECIDED = "decided"
    }
}
