package ai.pleos.playground.sample.vehicle.tts

import ai.pleos.playground.sample.vehicle.session.AccidentSession
import ai.pleos.playground.sample.vehicle.session.InjuryAnswer
import ai.pleos.playground.sample.vehicle.ui.nav.PitCrewRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechTextsTest {

    private val session = AccidentSession(startedAtMillis = 0L, injury = InjuryAnswer.YES)

    @Test
    fun onlyTheFiveGuidanceScreensHaveReadingText() {
        PitCrewRoute.entries.forEach { route ->
            val text = SpeechTexts.forRoute(route, session)
            if (route in SpeechTexts.speechRoutes) assertNotNull(route.name, text) else assertNull(route.name, text)
        }
        assertEquals(5, SpeechTexts.speechRoutes.size)
    }

    @Test
    fun emergencyNumbersAreReadAsWordsButNotInScreenText() {
        val text = SpeechTexts.forRoute(PitCrewRoute.EMERGENCY, session)!!
        assertTrue(text.contains("일일구"))
        assertTrue(text.contains("일일이"))
        assertFalse(text.contains("119"))
        assertFalse(text.contains("112"))
    }

    @Test
    fun emergencyTextDoesNotReadThePlaceRow() {
        val text = SpeechTexts.forRoute(PitCrewRoute.EMERGENCY, session)!!
        assertFalse(text.contains("사고 장소"))
        assertFalse(text.contains("OpenStreetMap"))
    }

    @Test
    fun everyReadingEndsWithTheDisclaimerExceptTheInjuryQuestion() {
        PitCrewRoute.entries.filter { it in SpeechTexts.speechRoutes && it != PitCrewRoute.INJURY }.forEach {
            assertTrue(it.name, SpeechTexts.forRoute(it, session)!!.endsWith("조언이 아니에요."))
        }
    }
}
