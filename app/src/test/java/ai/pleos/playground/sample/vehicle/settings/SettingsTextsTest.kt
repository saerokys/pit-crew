package ai.pleos.playground.sample.vehicle.settings

import ai.pleos.playground.sample.vehicle.ui.settings.SettingsTexts
import ai.pleos.playground.sample.vehicle.ui.settings.versionOrUnknown
import org.junit.Assert.assertEquals
import org.junit.Test

/** SC-500 앱 정보의 버전 글자 확인(읽지 못한 값은 "확인 불가"). */
class SettingsTextsTest {

    @Test
    fun realVersionsAreShownAsIs() {
        assertEquals("2.3.22", versionOrUnknown("2.3.22"))
        assertEquals("2.3.2", versionOrUnknown(" 2.3.2 "))
    }

    @Test
    fun unreadVersionsAreUnknown() {
        assertEquals(SettingsTexts.UNKNOWN, versionOrUnknown(null))
        assertEquals(SettingsTexts.UNKNOWN, versionOrUnknown(""))
        assertEquals(SettingsTexts.UNKNOWN, versionOrUnknown("unknown"))
        assertEquals(SettingsTexts.UNKNOWN, versionOrUnknown("not installed"))
    }

    @Test
    fun consentSummaryStillSaysAllOffByDefault() {
        assertEquals("위치 꺼짐 · 음성 꺼짐 · 네트워크 꺼짐", Consent().summaryLine())
    }
}
