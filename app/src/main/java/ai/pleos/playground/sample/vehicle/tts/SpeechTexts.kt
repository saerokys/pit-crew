package ai.pleos.playground.sample.vehicle.tts

import ai.pleos.playground.sample.vehicle.session.AccidentSession
import ai.pleos.playground.sample.vehicle.session.OtherPartyChecklist
import ai.pleos.playground.sample.vehicle.session.RecordChecklist
import ai.pleos.playground.sample.vehicle.session.SafetyChecklist
import ai.pleos.playground.sample.vehicle.ui.accident.AccidentTexts
import ai.pleos.playground.sample.vehicle.ui.nav.PitCrewRoute

/**
 * [소리로 듣기]가 읽는 문장. 화면에 이미 있는 문구([AccidentTexts], 체크 항목)를 이어 붙인 것뿐이고 새 문장은 없다.
 * 읽지 않는 것: 주소, 좌표, 위치 상태 줄(SC-221의 "사고 장소" 행), 음성 메모 카드, 버튼 이름.
 * 법률·과실 판단 문구는 없고 "참고용" 문장은 화면에 있는 대로 읽는다.
 */
object SpeechTexts {

    /** [소리로 듣기]를 보여 주는 화면. 이 밖의 모든 화면에서는 버튼을 숨긴다. */
    val speechRoutes: Set<PitCrewRoute> = setOf(
        PitCrewRoute.SAFETY,       // SC-210
        PitCrewRoute.INJURY,       // SC-220
        PitCrewRoute.EMERGENCY,    // SC-221
        PitCrewRoute.RECORD,       // SC-230
        PitCrewRoute.OTHER_PARTY   // SC-240
    )

    /** 그 화면에서 읽을 문장. 버튼을 보이지 않는 화면이면 null. */
    fun forRoute(route: PitCrewRoute, session: AccidentSession?): String? = when (route) {
        PitCrewRoute.SAFETY -> join(
            AccidentTexts.SAFETY_TITLE,
            *SafetyChecklist.items.map { it.text }.toTypedArray(),
            AccidentTexts.SAFETY_WHY_BODY,
            AccidentTexts.DISCLAIMER
        )
        PitCrewRoute.INJURY -> join(AccidentTexts.INJURY_TITLE, AccidentTexts.INJURY_HINT)
        PitCrewRoute.EMERGENCY -> join(
            AccidentTexts.EMERGENCY_BANNER,
            AccidentTexts.EMERGENCY_CALL,
            AccidentTexts.EMERGENCY_NO_DIAL,
            AccidentTexts.EMERGENCY_POLICE,
            AccidentTexts.SPEAK_CARD_TITLE,
            "${AccidentTexts.SPEAK_KIND_LABEL}. ${AccidentTexts.SPEAK_KIND_VALUE}",
            "${AccidentTexts.SPEAK_INJURY_LABEL}. ${AccidentTexts.injurySpeakValue(session?.injury)}",
            AccidentTexts.DISCLAIMER
        )
        PitCrewRoute.RECORD -> join(
            AccidentTexts.RECORD_TITLE,
            AccidentTexts.RECORD_HINT_PHONE,
            AccidentTexts.RECORD_HINT_LATER,
            *RecordChecklist.items.map { it.text }.toTypedArray(),
            AccidentTexts.RECORD_NOT_SAVED,
            AccidentTexts.DISCLAIMER
        )
        PitCrewRoute.OTHER_PARTY -> join(
            AccidentTexts.OTHER_TITLE,
            *OtherPartyChecklist.items.map { it.text }.toTypedArray(),
            AccidentTexts.OTHER_NOTE_BODY,
            AccidentTexts.DISCLAIMER
        )
        else -> null
    }

    /** 문장 사이에 마침표를 보장해 한 번에 이어 읽게 하고, 긴급 번호는 숫자 그대로 읽히지 않게 소리 나는 대로 바꾼다. */
    private fun join(vararg parts: String): String =
        parts.joinToString(" ") { sentence(it) }.let(::spoken)

    private fun sentence(text: String): String =
        if (text.endsWith(".") || text.endsWith("?")) text else "$text."

    /** 읽기 문장에만 쓰는 발음: 119 -> 일일구, 112 -> 일일이(화면 글자는 그대로). */
    internal fun spoken(text: String): String = text.replace("119", "일일구").replace("112", "일일이")
}
