package ai.pleos.playground.sample.vehicle.insurer

import ai.pleos.playground.sample.vehicle.record.AccidentRecord
import ai.pleos.playground.sample.vehicle.session.AccidentSession
import ai.pleos.playground.sample.vehicle.session.AllChecklists
import ai.pleos.playground.sample.vehicle.session.formatClock
import ai.pleos.playground.sample.vehicle.ui.summary.addressText
import ai.pleos.playground.sample.vehicle.ui.summary.vehicleLines
import ai.pleos.playground.sample.vehicle.ui.summary.weatherText

/**
 * SC-290 전달 항목. SPEC의 6개뿐이다(새 항목을 만들지 않는다).
 * 부상 응답, 상대방 정보, 사진, 차대번호, 좌표는 칸 자체가 없다(규칙 12).
 */
enum class InsurerItem(val label: String) {
    TIME("사고 시각"),
    PLACE("위치와 주소"),
    WEATHER("날씨"),
    VEHICLE("차량 상태"),
    CHECKS("안내 이행 기록"),
    VOICE("음성 메모")
}

/** SC-290 [보내기] 진행 상태. */
enum class InsurerSendState { IDLE, SENDING, SENT, FAILED }

/**
 * 시연용 접수 한 건의 전송 로그(세션 메모리에만 둔다, 홈으로 가면 사라진다).
 * 접수 번호, 보낸 시각, 보낸 항목 이름만 두고 값(주소, 날씨, 메모)은 넣지 않는다.
 */
data class InsurerReceipt(val number: String, val sentAtMillis: Long, val itemNames: List<String>)

/**
 * SC-290 전송 내용을 세션에서 만든다(Android 의존 없는 순수 함수라 JVM 단위 테스트로 확인한다).
 * 전송은 앱 안 모의 처리라 이 내용은 앱 밖으로 나가지 않는다(외부 호출 없음).
 */
object InsurerPackage {

    /** 지금 세션에서 고를 수 있는 항목. 값이 없는 항목은 고를 수 없다(회색). */
    fun available(session: AccidentSession?): Set<InsurerItem> {
        if (session == null) return emptySet()
        return InsurerItem.entries.filter { item ->
            when (item) {
                InsurerItem.TIME -> true
                InsurerItem.PLACE -> addressText(session) != null
                InsurerItem.WEATHER -> weatherText(session) != null
                InsurerItem.VEHICLE -> session.vehicle?.hasAnyValue() == true
                InsurerItem.CHECKS -> true
                InsurerItem.VOICE -> !session.voiceMemo.isNullOrBlank()
            }
        }.toSet()
    }

    /** 사용자가 끄지 않은 고를 수 있는 항목(기본은 고를 수 있는 항목 모두 켜짐). */
    fun selected(session: AccidentSession?, unchecked: Set<InsurerItem>): Set<InsurerItem> =
        available(session) - unchecked

    /** 고를 수 없는 항목 옆에 보여 주는 이유. */
    fun unavailableNote(item: InsurerItem): String = when (item) {
        InsurerItem.PLACE -> "주소가 없어요"
        InsurerItem.WEATHER -> "날씨가 없어요"
        InsurerItem.VEHICLE -> "읽은 값이 없어요"
        InsurerItem.VOICE -> "저장한 메모가 없어요"
        else -> ""
    }

    /** 미리보기에 보여 줄 값(전송 내용과 같은 값). 고를 수 없으면 null. */
    fun preview(item: InsurerItem, session: AccidentSession?): String? {
        if (session == null || item !in available(session)) return null
        return when (item) {
            InsurerItem.TIME -> formatClock(session.startedAtMillis)
            InsurerItem.PLACE -> addressText(session)
            InsurerItem.WEATHER -> weatherText(session)
            InsurerItem.VEHICLE -> vehicleLines(session.vehicle).take(4).joinToString(", ")
            InsurerItem.CHECKS -> checksText(session)
            InsurerItem.VOICE -> session.voiceMemo
        }
    }

    /** "체크한 N/12"와 체크한 항목 이름(앞 3개, 나머지는 "외 N개"). */
    private fun checksText(session: AccidentSession): String {
        val items = AllChecklists.flatMap { it.items }
        val checked = items.filter { session.isChecked(it.id) }
        val head = "체크한 ${checked.size}/${items.size}"
        if (checked.isEmpty()) return head
        val names = checked.take(3).joinToString(", ") { it.text }
        val more = if (checked.size > 3) " 외 ${checked.size - 3}개" else ""
        return "$head · $names$more"
    }

    /**
     * 전송 내용. 고를 수 있고 [selected]에 든 항목만 담는다. 좌표, 부상 응답, 상대방 정보, 사진, 차대번호는 칸이 없다.
     * 값은 [AccidentRecord]와 같은 형식(영역별 차량 값, 체크 id별 여부)을 재사용한다.
     */
    fun payload(session: AccidentSession, selected: Set<InsurerItem>): Map<String, Any?> {
        val use = selected intersect available(session)
        val record = AccidentRecord.from(session)
        val out = linkedMapOf<String, Any?>()
        InsurerItem.entries.filter { it in use }.forEach { item ->
            when (item) {
                InsurerItem.TIME -> out["time"] = session.startedAtMillis
                InsurerItem.PLACE -> out["place"] = addressText(session)
                InsurerItem.WEATHER -> out["weather"] = linkedMapOf(
                    "condition" to session.weather?.condition,
                    "temperatureC" to session.weather?.temperatureC
                )
                InsurerItem.VEHICLE -> out["vehicle"] = record[AccidentRecord.KEY_VEHICLE]
                InsurerItem.CHECKS -> out["checks"] = record[AccidentRecord.KEY_CHECKS]
                InsurerItem.VOICE -> out["voiceMemo"] = session.voiceMemo
            }
        }
        return out
    }

    /**
     * 시연용 접수 번호 "MOCK-시각4자리-무작위4자리". 사고 시각, 위치, 사용자 식별 값을 쓰지 않아 개인을 식별하지 않으며,
     * 실제 접수 번호와 섞이지 않게 "MOCK-"로 시작한다.
     */
    fun receiptNumber(nowMillis: Long, random: Int): String {
        val time = Math.floorMod(nowMillis / 1000, 10_000L).toString().padStart(4, '0')
        val rand = Math.floorMod(random, 10_000).toString().padStart(4, '0')
        return "MOCK-$time-$rand"
    }

    /**
     * 모의 접수: 전송 내용을 만들어 확인한 뒤(앱 밖으로 나가지 않음) 접수 한 건을 돌려준다.
     * 로그에는 항목 이름만 남긴다(값 제외).
     */
    fun receipt(session: AccidentSession, selected: Set<InsurerItem>, nowMillis: Long, random: Int): InsurerReceipt {
        val use = InsurerItem.entries.filter { it in (selected intersect available(session)) }
        payload(session, selected) // 보낼 내용을 만들어 본다(앱 안에서만 쓰고 버린다)
        return InsurerReceipt(receiptNumber(nowMillis, random), nowMillis, use.map { it.label })
    }
}
