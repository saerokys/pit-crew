package ai.pleos.playground.sample.vehicle.ui.accident

import ai.pleos.playground.sample.vehicle.session.InjuryAnswer

/**
 * 안내 화면(SC-210, 220, 221, 230, 240)의 문구를 한 곳에 둔다.
 * 화면 글자와 [소리로 듣기]가 읽는 문장이 같은 상수에서 나오므로 서로 어긋나지 않는다(새 문장은 만들지 않는다).
 */
object AccidentTexts {
    const val DISCLAIMER = "참고용 안내이며 법률·의료 조언이 아니에요."

    // SC-210
    const val SAFETY_TITLE = "먼저 안전을 확보하세요"
    const val SAFETY_WHY_TITLE = "왜 필요한가요?"
    const val SAFETY_WHY_BODY = "비상등은 뒤따르는 차에 사고를 알려 줘요. 도로 위에 머물면 2차 사고 위험이 커서, 안전한 곳으로 피해야 해요."

    // SC-220
    const val INJURY_TITLE = "다친 사람이 있나요?"
    const val INJURY_HINT = "있거나 모르겠으면 119 안내를 먼저 보여 드려요"

    // SC-221
    const val EMERGENCY_BANNER = "119에 먼저 연락하세요"
    const val EMERGENCY_CALL = "전화로 119를 눌러 주세요"
    const val EMERGENCY_NO_DIAL = "이 앱은 전화를 대신 걸지 않아요. 직접 걸어 주세요."
    const val EMERGENCY_POLICE = "경찰 신고는 112예요."
    const val SPEAK_CARD_TITLE = "말할 내용"
    const val SPEAK_KIND_LABEL = "사고 종류"
    const val SPEAK_KIND_VALUE = "교통사고라고 말해요"
    const val SPEAK_INJURY_LABEL = "다친 사람"

    /** SC-221 "다친 사람" 행의 값. 있어요면 "있음…", 그 밖(잘 모르겠어요 등)은 "모름…". */
    fun injurySpeakValue(injury: InjuryAnswer?): String = when (injury) {
        InjuryAnswer.YES -> "있음. 몇 명인지, 어떤 상태인지 말해 주세요."
        else -> "모름. 보이는 대로 말해 주세요."
    }

    // SC-230
    const val RECORD_TITLE = "현장을 기록해 두세요"
    const val RECORD_HINT_PHONE = "휴대폰으로 촬영해 두세요"
    const val RECORD_HINT_LATER = "나중에 보험사에 보여 줄 수 있어요"
    const val RECORD_NOT_SAVED = "사진은 앱에 저장되지 않아요"

    // SC-240
    const val OTHER_TITLE = "상대방 정보를 확인하세요"
    const val OTHER_NOTE_TITLE = "알아 두세요"
    const val OTHER_NOTE_BODY = "이 앱은 상대방 정보를 입력받거나 저장하지 않아요. 확인한 것만 체크해 두세요."
}
