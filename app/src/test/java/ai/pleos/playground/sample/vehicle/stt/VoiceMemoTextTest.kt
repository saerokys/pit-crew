package ai.pleos.playground.sample.vehicle.stt

import ai.pleos.playground.sample.vehicle.record.AccidentRecord
import ai.pleos.playground.sample.vehicle.session.AccidentSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 음성 메모 글자 규칙과 "사고 기록에 넣지 않는다"는 결정 확인. 값은 모두 지어낸 글자이며 실제 메모가 아니다. */
class VoiceMemoTextTest {

    @Test
    fun maxIsOneHundred() {
        assertEquals(100, VoiceMemoText.MAX_CHARS)
    }

    @Test
    fun trimsAndCollapsesWhitespace() {
        assertEquals("가나 다라 마바", VoiceMemoText.clean("  가나   다라\n마바\t"))
    }

    @Test
    fun blankBecomesEmpty() {
        assertEquals("", VoiceMemoText.clean(""))
        assertEquals("", VoiceMemoText.clean("   \n\t "))
    }

    @Test
    fun keepsExactlyOneHundred() {
        val text = "가".repeat(100)
        assertEquals(text, VoiceMemoText.clean(text))
    }

    @Test
    fun cutsOverOneHundredAtOneHundred() {
        val cut = VoiceMemoText.clean("가".repeat(101))
        assertEquals(100, cut.length)
        assertEquals(100, VoiceMemoText.clean("나".repeat(250)).length)
    }

    @Test
    fun wasCutOnlyWhenOverOneHundredAfterNormalizing() {
        assertFalse(VoiceMemoText.wasCut(""))
        assertFalse(VoiceMemoText.wasCut("가".repeat(100)))
        // 공백을 정리한 글자로 센다: 100자 + 앞뒤 공백·연속 공백은 잘림이 아니다.
        assertFalse(VoiceMemoText.wasCut("  " + "가".repeat(100) + "  \n"))
        assertFalse(VoiceMemoText.wasCut("가   나\n\n다"))
        assertTrue(VoiceMemoText.wasCut("가".repeat(101)))
        assertTrue(VoiceMemoText.wasCut("가나다라 ".repeat(30)))
    }

    @Test
    fun wasCutMatchesCleanLength() {
        listOf("가".repeat(99), "가".repeat(100), "가".repeat(101), "가 ".repeat(80)).forEach {
            assertEquals("입력 길이 ${it.length}", VoiceMemoText.wasCut(it), VoiceMemoText.clean(it).length < it.replace(Regex("\\s+"), " ").trim().length)
        }
    }

    @Test
    fun trailingSpaceAfterCutIsRemoved() {
        // 99번째 글자 다음이 공백이면 자른 뒤 끝의 공백은 지운다.
        val cut = VoiceMemoText.clean("가".repeat(99) + " 나다라")
        assertEquals("가".repeat(99), cut)
    }

    @Test
    fun voiceMemoIsNotInTheSavedRecord() {
        val memo = "가나다라 지어낸 메모"
        val session = AccidentSession(startedAtMillis = 1L).withVoiceMemo(memo)
        assertEquals(memo, session.voiceMemo)
        val record = AccidentRecord.from(session)
        // 키에도 값에도 메모가 없다(결정 A: 세션 메모리에만 두고 사고 기록에는 넣지 않는다).
        assertFalse(record.keys.any { it.contains("memo", ignoreCase = true) || it.contains("voice", ignoreCase = true) })
        assertFalse(record.toString().contains("지어낸 메모"))
        assertEquals(setOf("version", "startedAt", "location", "weather", "vehicle", "checks"), record.keys)
    }

    @Test
    fun newSessionHasNoMemoAndReplacingKeepsOne() {
        val first = AccidentSession(startedAtMillis = 1L)
        assertNull(first.voiceMemo)
        val replaced = first.withVoiceMemo("하나").withVoiceMemo("둘")
        assertEquals("둘", replaced.voiceMemo)
        assertTrue(replaced.voiceMemo!!.length <= VoiceMemoText.MAX_CHARS)
    }
}
