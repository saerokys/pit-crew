package ai.pleos.playground.sample.vehicle.record

import ai.pleos.playground.sample.vehicle.session.formatClock
import ai.pleos.playground.sample.vehicle.ui.summary.OSM_CREDIT

/** SC-520 화면 상태. 기록 내용은 [Found]의 요약에만 있다. */
sealed interface SavedRecordUi {
    data object Loading : SavedRecordUi
    data object None : SavedRecordUi
    data object Unreadable : SavedRecordUi
    data class Found(val summary: SavedRecordSummary) : SavedRecordUi
}

/** 위치·주소·날씨 한 줄의 상태. OK는 값이 있고, UNAVAILABLE은 확인하지 못했고, NOT_USED는 쓰지 않았거나 없다. */
enum class SavedState { OK, UNAVAILABLE, NOT_USED }

/**
 * SC-520에 보여 줄 저장 기록 요약(SC-270 요약 한 줄과 위치·주소·날씨 한 줄씩).
 * 저장 형식([AccidentRecord])에 있는 항목만 읽는다. 좌표, 상대방 정보, 사진, 차대번호, 음성 메모, 부상 응답은
 * 형식에 칸이 없으므로 여기에도 없다. 순수 Kotlin이라 JVM 단위 테스트로 확인한다.
 */
data class SavedRecordSummary(
    val startedAtMillis: Long,
    val vehicleRead: Int,
    val checked: Int,
    val location: SavedState,
    val address: SavedState,
    val addressText: String?,
    val addressCredit: String?,
    val weather: SavedState,
    val weatherText: String?
) {
    /** SC-270 요약 한 줄과 같은 형식: "기록 시각 HH:mm · 차량 상태 N/5 · 안내 이행 N/12". */
    fun summaryLine(): String =
        "기록 시각 ${formatClock(startedAtMillis)} · 차량 상태 $vehicleRead/${EndingSummary.VEHICLE_TOTAL}" +
            " · 안내 이행 $checked/${EndingSummary.checkTotal}"

    companion object {
        /** 저장한 기록(Map)에서 요약을 만든다. 기록 시각이 없거나 숫자가 아니면 읽을 수 없는 기록으로 보고 null. */
        fun from(record: Map<String, Any?>): SavedRecordSummary? {
            val startedAt = (record[AccidentRecord.KEY_STARTED_AT] as? Number)?.toLong() ?: return null
            val location = record[AccidentRecord.KEY_LOCATION] as? Map<*, *>
            val weather = record[AccidentRecord.KEY_WEATHER] as? Map<*, *>

            val locationState = when (location?.get("status")) {
                AccidentRecord.LOCATION_CONFIRMED -> SavedState.OK
                AccidentRecord.LOCATION_UNAVAILABLE -> SavedState.UNAVAILABLE
                else -> SavedState.NOT_USED
            }
            val addressValue = (location?.get("address") as? String)?.takeIf { it.isNotBlank() }
            val addressState = when {
                location?.get("addressStatus") == AccidentRecord.ADDRESS_OK && addressValue != null -> SavedState.OK
                location?.get("addressStatus") == AccidentRecord.ADDRESS_UNAVAILABLE -> SavedState.UNAVAILABLE
                else -> SavedState.NOT_USED
            }
            val weatherText = weatherText(weather)
            val weatherState = when {
                weather?.get("status") == AccidentRecord.WEATHER_OK && weatherText != null -> SavedState.OK
                weather?.get("status") == AccidentRecord.WEATHER_UNAVAILABLE -> SavedState.UNAVAILABLE
                else -> SavedState.NOT_USED
            }
            return SavedRecordSummary(
                startedAtMillis = startedAt,
                vehicleRead = vehicleReadCount(record[AccidentRecord.KEY_VEHICLE] as? Map<*, *>),
                checked = (record[AccidentRecord.KEY_CHECKS] as? Map<*, *>)?.values?.count { it == true } ?: 0,
                location = locationState,
                address = addressState,
                addressText = if (addressState == SavedState.OK) addressValue else null,
                addressCredit = if (addressState == SavedState.OK) {
                    (location?.get("addressCredit") as? String)?.takeIf { it.isNotBlank() } ?: OSM_CREDIT
                } else {
                    null
                },
                weather = weatherState,
                weatherText = if (weatherState == SavedState.OK) weatherText else null
            )
        }

        /** 문, 창문, 배터리, 주행 거리, 방향지시등 중 값이 있는 기능의 수(SC-270 요약과 같은 기준). */
        private fun vehicleReadCount(vehicle: Map<*, *>?): Int {
            if (vehicle == null) return 0
            fun anyValue(key: String) = (vehicle[key] as? Map<*, *>)?.values?.any { it != null } == true
            return listOf(
                anyValue("doors"),
                anyValue("windows"),
                vehicle["battery"] != null,
                vehicle["odometer"] != null,
                vehicle["turnSignal"] != null
            ).count { it }
        }

        /** "맑음 11°C" 형식(저장한 조건과 온도가 모두 있을 때만). 온도는 JSON에서 정수나 실수로 올 수 있다. */
        private fun weatherText(weather: Map<*, *>?): String? {
            if (weather == null) return null
            val condition = (weather["condition"] as? String)?.takeIf { it.isNotBlank() } ?: return null
            val temperature = weather["temperatureC"] as? Number ?: return null
            val value = temperature.toDouble()
            val text = if (value == Math.rint(value)) value.toLong().toString() else value.toString()
            return "$condition ${text}°C"
        }
    }
}
