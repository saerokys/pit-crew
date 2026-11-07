package ai.pleos.playground.sample.vehicle.record

import ai.pleos.playground.sample.vehicle.session.AccidentSession
import ai.pleos.playground.sample.vehicle.session.AllChecklists
import ai.pleos.playground.sample.vehicle.session.formatClock
import ai.pleos.playground.sample.vehicle.ui.summary.OSM_CREDIT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SC-520 요약 변환 확인. 값은 모두 지어낸 것이며 실제 주소·좌표가 아니다. */
class SavedRecordSummaryTest {

    private val fakeAddress = "가나도 다라시 마바로 12"

    private fun full(): Map<String, Any?> = linkedMapOf(
        "version" to 2,
        "startedAt" to 1_700_000_000_000L,
        "location" to linkedMapOf(
            "status" to "CONFIRMED", "addressStatus" to "OK", "address" to fakeAddress, "addressCredit" to OSM_CREDIT
        ),
        "weather" to linkedMapOf("status" to "OK", "condition" to "맑음", "temperatureC" to 11),
        "vehicle" to linkedMapOf(
            "readAt" to 1L,
            "doors" to linkedMapOf("ROW_1_LEFT" to 0, "REAR" to null),
            "windows" to linkedMapOf("ROW_1_LEFT" to null),
            "battery" to 100,
            "odometer" to 0.0,
            "turnSignal" to null
        ),
        "checks" to linkedMapOf("a" to true, "b" to false, "c" to true)
    )

    @Test
    fun fullRecordIsSummarized() {
        val s = SavedRecordSummary.from(full())!!
        assertEquals(SavedState.OK, s.location)
        assertEquals(SavedState.OK, s.address)
        assertEquals(fakeAddress, s.addressText)
        assertEquals(OSM_CREDIT, s.addressCredit)
        assertEquals(SavedState.OK, s.weather)
        assertEquals("맑음 11°C", s.weatherText)
        // 문(값 있음), 창문(값 없음), 배터리, 주행 거리 → 3개 (방향지시등 값 없음)
        assertEquals(3, s.vehicleRead)
        assertEquals(2, s.checked)
    }

    @Test
    fun summaryLineUsesEndingFormat() {
        val s = SavedRecordSummary.from(full())!!
        assertEquals(
            "기록 시각 ${formatClock(1_700_000_000_000L)} · 차량 상태 3/5 · 안내 이행 2/${AllChecklists.sumOf { it.items.size }}",
            s.summaryLine()
        )
    }

    @Test
    fun unusedLocationAndWeatherAreNotUsed() {
        val record = full().toMutableMap().apply {
            put("location", mapOf("status" to "NOT_USED", "addressStatus" to "NONE", "address" to null, "addressCredit" to null))
            put("weather", mapOf("status" to "NOT_USED", "condition" to null, "temperatureC" to null))
        }
        val s = SavedRecordSummary.from(record)!!
        assertEquals(SavedState.NOT_USED, s.location)
        assertEquals(SavedState.NOT_USED, s.address)
        assertNull(s.addressText)
        assertNull(s.addressCredit)
        assertEquals(SavedState.NOT_USED, s.weather)
        assertNull(s.weatherText)
    }

    @Test
    fun confirmedLocationWithFailedAddressAndWeather() {
        val record = full().toMutableMap().apply {
            put("location", mapOf("status" to "CONFIRMED", "addressStatus" to "UNAVAILABLE", "address" to null, "addressCredit" to null))
            put("weather", mapOf("status" to "UNAVAILABLE", "condition" to null, "temperatureC" to null))
        }
        val s = SavedRecordSummary.from(record)!!
        assertEquals(SavedState.OK, s.location)
        assertEquals(SavedState.UNAVAILABLE, s.address)
        assertNull(s.addressText)
        assertEquals(SavedState.UNAVAILABLE, s.weather)
    }

    @Test
    fun version1RecordWithoutWeatherIsReadable() {
        val record = full().toMutableMap().apply { remove("weather"); put("version", 1) }
        val s = SavedRecordSummary.from(record)!!
        assertEquals(SavedState.NOT_USED, s.weather)
        assertNull(s.weatherText)
    }

    @Test
    fun missingCreditFallsBackToOsm() {
        val record = full().toMutableMap().apply {
            put("location", mapOf("status" to "CONFIRMED", "addressStatus" to "OK", "address" to fakeAddress))
        }
        assertEquals(OSM_CREDIT, SavedRecordSummary.from(record)!!.addressCredit)
    }

    @Test
    fun temperatureAsDoubleIsShownWithoutDecimalWhenWhole() {
        val record = full().toMutableMap().apply {
            put("weather", mapOf("status" to "OK", "condition" to "비", "temperatureC" to 7.0))
        }
        assertEquals("비 7°C", SavedRecordSummary.from(record)!!.weatherText)
    }

    @Test
    fun noVehicleCountsZero() {
        val record = full().toMutableMap().apply { put("vehicle", null) }
        assertEquals(0, SavedRecordSummary.from(record)!!.vehicleRead)
    }

    @Test
    fun unreadableRecordsReturnNull() {
        assertNull(SavedRecordSummary.from(emptyMap()))
        assertNull(SavedRecordSummary.from(mapOf("startedAt" to "not a number")))
        assertNotNull(SavedRecordSummary.from(mapOf("startedAt" to 5)))
    }

    @Test
    fun roundTripFromAccidentRecordShowsOnlyKnownItems() {
        val checks = AllChecklists.flatMap { it.items }.take(3).associate { it.id to true }
        val record = AccidentRecord.from(AccidentSession(startedAtMillis = 1_700_000_000_000L, checks = checks))
        val s = SavedRecordSummary.from(record)!!
        assertEquals(3, s.checked)
        assertEquals(SavedState.NOT_USED, s.location)
        assertEquals(0, s.vehicleRead)
        // 요약에는 좌표, 부상 응답, 음성 메모 같은 칸이 없다(저장 형식에도 없다).
        assertFalse(s.toString().contains("latitude", ignoreCase = true))
        assertTrue(record.keys.none { it in setOf("injury", "voiceMemo", "latitude", "longitude") })
    }
}
