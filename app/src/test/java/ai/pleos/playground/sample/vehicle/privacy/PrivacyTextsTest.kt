package ai.pleos.playground.sample.vehicle.privacy

import ai.pleos.playground.sample.vehicle.ui.privacy.PrivacyTexts
import ai.pleos.playground.sample.vehicle.ui.summary.OSM_CREDIT
import ai.pleos.playground.sample.vehicle.ui.summary.WEATHER_CREDIT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SC-510 문구 확인: 출처 표기가 다른 화면과 같은 글자이고, 실제 연계로 읽히는 말이 없다. */
class PrivacyTextsTest {

    @Test
    fun creditsAreTheSameAsOtherScreens() {
        assertEquals("Weather data by Open-Meteo.com", WEATHER_CREDIT)
        assertEquals("© OpenStreetMap contributors", OSM_CREDIT)
        assertTrue(PrivacyTexts.ALL.contains(WEATHER_CREDIT))
        assertTrue(PrivacyTexts.ALL.contains(OSM_CREDIT))
        assertEquals("CC BY 4.0", PrivacyTexts.WEATHER_LICENSE)
        assertEquals("ODbL 1.0 라이선스", PrivacyTexts.OSM_LICENSE)
        assertTrue(PrivacyTexts.ALL.contains(PrivacyTexts.OSM_LICENSE))
    }

    @Test
    fun principlesUseSpecWording() {
        assertEquals("외부 전송은 사용자가 켠 경우만", PrivacyTexts.EXTERNAL)
        assertEquals("보관은 사용자 선택", PrivacyTexts.KEEP)
        assertEquals("언제든 삭제 가능", PrivacyTexts.DELETE)
        assertEquals("위치·음성은 허용한 경우만", PrivacyTexts.NOT_COLLECTED_NOTE)
        assertTrue(PrivacyTexts.NOT_COLLECTED.contains("사진"))
        assertTrue(PrivacyTexts.NOT_COLLECTED.contains("차대번호"))
        assertTrue(PrivacyTexts.NOT_COLLECTED.contains("상대방 연락처"))
    }

    @Test
    fun demoInsurerAndDisclaimerAreStated() {
        assertEquals("시연용 가상 보험사예요. 실제 보험사로 전달되지 않아요", PrivacyTexts.DEMO_INSURER)
        assertEquals("참고용이며 법률·의료 조언이 아니에요", PrivacyTexts.DISCLAIMER)
        assertEquals("날씨를 조회하면 서버 로그에 좌표가 최대 90일 남을 수 있어요", PrivacyTexts.WEATHER_LOG)
    }

    @Test
    fun noTextClaimsRealInsurerOrLegalJudgment() {
        val banned = listOf("과실", "책임 없", "보상 받", "접수되었", "실제 접수")
        PrivacyTexts.ALL.forEach { line -> banned.forEach { word -> assertFalse("$line / $word", line.contains(word)) } }
    }
}
