package ai.pleos.playground.sample.vehicle.session

import ai.pleos.playground.sample.vehicle.vehicle.VehicleSnapshot
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
    val injury: InjuryAnswer = InjuryAnswer.NOT_ANSWERED,
    /** SC-200에서 읽은 차량 상태(SC-250에서 다시 읽으면 갱신). 읽기 전이면 null. */
    val vehicle: VehicleSnapshot? = null
) {
    fun isChecked(id: String): Boolean = checks[id] == true
    fun withChecked(id: String, checked: Boolean): AccidentSession = copy(checks = checks + (id to checked))
    fun withInjury(answer: InjuryAnswer): AccidentSession = copy(injury = answer)
    fun withVehicle(snapshot: VehicleSnapshot): AccidentSession = copy(vehicle = snapshot)
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

/** SC-230 체크 항목(SPEC): 휴대폰으로 촬영할 것. 앱은 사진을 찍거나 저장하지 않는다. */
object RecordChecklist {
    val items = listOf(
        ChecklistItemDef("SC-230:vehicles_overview", "두 차량의 전체 모습"),
        ChecklistItemDef("SC-230:damage_closeup", "파손 부위 가까이"),
        ChecklistItemDef("SC-230:plate", "상대 차량 번호판"),
        ChecklistItemDef("SC-230:road_signals", "도로·신호·차선 상황"),
        ChecklistItemDef("SC-230:surroundings", "사고 장소 주변")
    )
}

/** SC-240 체크 항목(SPEC). 체크 여부만 세션에 두고 상대방 정보는 입력받거나 저장하지 않는다. */
object OtherPartyChecklist {
    val items = listOf(
        ChecklistItemDef("SC-240:plate_number", "상대 차량 번호 확인"),
        ChecklistItemDef("SC-240:contact", "연락처 교환"),
        ChecklistItemDef("SC-240:insurer", "상대 보험사 확인")
    )
}

/** SC-260 요약이 화면별로 "확인함/확인 안 함"을 보여 줄 때 쓰는 체크 목록 묶음. */
data class ChecklistSection(val screenId: String, val title: String, val items: List<ChecklistItemDef>)

val AllChecklists = listOf(
    ChecklistSection("SC-210", "안전 확보", SafetyChecklist.items),
    ChecklistSection("SC-230", "현장 기록", RecordChecklist.items),
    ChecklistSection("SC-240", "상대방 확인", OtherPartyChecklist.items)
)

fun formatClock(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
