package ai.pleos.playground.sample.vehicle.session

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 사고 모드 세션(SPEC 1.2). 메모리에만 두고 파일·저장소에 쓰지 않는다.
 * 사고 모드를 나가거나 끝내면(홈에 도착하면) 버린다.
 * 3-a에서 쓰는 값: 시작 시각, 체크 현황. 부상 응답 등은 이후 단계에서 추가한다.
 */
data class AccidentSession(
    val startedAtMillis: Long,
    /** 항목 id -> 체크 여부. 값이 없으면 "확인 안 함". */
    val checks: Map<String, Boolean> = emptyMap(),
    /** SC-220 부상 응답. 선택하지 않고 나가면 "응답 안 함". */
    val injury: InjuryAnswer = InjuryAnswer.NOT_ANSWERED
) {
    fun isChecked(id: String): Boolean = checks[id] == true
    fun withChecked(id: String, checked: Boolean): AccidentSession = copy(checks = checks + (id to checked))
    fun withInjury(answer: InjuryAnswer): AccidentSession = copy(injury = answer)
}

/** SPEC 1.2의 부상 응답: 없음 / 있음 / 모름 / 응답 안 함. */
enum class InjuryAnswer(val label: String) {
    NONE("없음"),
    YES("있음"),
    UNSURE("모름"),
    NOT_ANSWERED("응답 안 함")
}

/** 체크 항목 하나. id는 세션과 SC-260 요약이 같이 쓴다. */
data class ChecklistItemDef(val id: String, val text: String)

/** SC-210 체크 항목(SPEC). */
object SafetyChecklist {
    val items = listOf(
        ChecklistItemDef("SC-210:hazard_lights", "비상등 켜기"),
        ChecklistItemDef("SC-210:move_car", "경미한 사고이고 차를 옮길 수 있다면 안전한 곳으로 이동"),
        ChecklistItemDef("SC-210:move_passengers", "동승자는 안전한 곳으로 이동"),
        ChecklistItemDef("SC-210:check_traffic", "도로 위에서 차 밖으로 나갈 때는 주변 차량 확인")
    )
}

fun formatClock(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
