package ai.pleos.playground.sample.vehicle.weather

import ai.pleos.playground.sample.vehicle.location.LocationStatus
import ai.pleos.playground.sample.vehicle.session.AccidentSession
import ai.pleos.playground.sample.vehicle.ui.summary.WeatherUi
import ai.pleos.playground.sample.vehicle.ui.summary.weatherText
import ai.pleos.playground.sample.vehicle.ui.summary.weatherUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 날씨 좌표 반올림, 코드 변환, 표시 상태 확인. 값은 모두 지어낸 숫자이며 실제 좌표·날씨가 아니다. */
class WeatherFormatTest {

    @Test
    fun roundsCoordinateToTwoDecimals() {
        assertEquals("12.35", WeatherFormat.roundCoordinate(12.3456))
        assertEquals("-7.89", WeatherFormat.roundCoordinate(-7.891))
        assertEquals("100.00", WeatherFormat.roundCoordinate(100.0))
    }

    @Test
    fun describesCodeInTableAndRoundsTemperature() {
        val ok = WeatherFormat.describe(18.4, 0)
        assertEquals(WeatherStatus.OK, ok.status)
        assertEquals("맑음", ok.condition)
        assertEquals(18, ok.temperatureC)
        assertEquals(19, WeatherFormat.describe(18.5, 3).temperatureC)
        assertEquals(-3, WeatherFormat.describe(-2.6, 61).temperatureC)
    }

    @Test
    fun codeNotInTableIsNoData() {
        // 표에 없는 코드(예: 62, 4, 100)는 확인 불가
        listOf(4, 62, 64, 72, 78, 100, -1).forEach {
            assertEquals("코드 $it", WeatherStatus.NO_DATA, WeatherFormat.describe(10.0, it).status)
        }
    }

    @Test
    fun missingOrBrokenValuesAreNoData() {
        assertEquals(WeatherStatus.NO_DATA, WeatherFormat.describe(null, 0).status)
        assertEquals(WeatherStatus.NO_DATA, WeatherFormat.describe(10.0, null).status)
        assertEquals(WeatherStatus.NO_DATA, WeatherFormat.describe(Double.NaN, 0).status)
        assertEquals(WeatherStatus.NO_DATA, WeatherFormat.describe(Double.POSITIVE_INFINITY, 0).status)
    }

    @Test
    fun everyTableCodeHasAShortKoreanText() {
        val codes = listOf(0, 1, 2, 3, 45, 48, 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77, 80, 81, 82, 85, 86, 95, 96, 97, 99)
        codes.forEach {
            val text = WeatherFormat.condition(it)
            assertTrue("코드 $it", !text.isNullOrBlank() && text.length <= 8)
        }
    }

    @Test
    fun displayTextOnlyWhenOk() {
        assertEquals("맑음 18°C", WeatherFormat.displayText(WeatherResult(WeatherStatus.OK, "맑음", 18)))
        assertEquals("비 -3°C", WeatherFormat.displayText(WeatherResult(WeatherStatus.OK, "비", -3)))
        assertNull(WeatherFormat.displayText(WeatherResult(WeatherStatus.TIMEOUT)))
        assertNull(WeatherFormat.displayText(WeatherResult(WeatherStatus.OK, null, 5)))
        assertNull(WeatherFormat.displayText(null))
    }

    private fun session(
        locationRequested: Boolean,
        weatherRequested: Boolean,
        locationStatus: LocationStatus? = null,
        weather: WeatherResult? = null
    ) = AccidentSession(
        startedAtMillis = 1L,
        locationRequested = locationRequested,
        locationStatus = locationStatus,
        weatherRequested = weatherRequested,
        weather = weather
    )

    @Test
    fun cardHiddenWhenLocationOrNetworkOff() {
        // 위치 꺼짐
        assertNull(weatherUi(session(locationRequested = false, weatherRequested = false)))
        // 위치만 켜짐(네트워크 꺼짐)
        assertNull(weatherUi(session(locationRequested = true, weatherRequested = false, locationStatus = LocationStatus.OK)))
        assertNull(weatherUi(null))
    }

    @Test
    fun pendingThenOkOrFailedWhenBothOn() {
        assertEquals(WeatherUi.PENDING, weatherUi(session(true, true)))
        assertEquals(WeatherUi.PENDING, weatherUi(session(true, true, LocationStatus.OK)))
        val ok = session(true, true, LocationStatus.OK, WeatherResult(WeatherStatus.OK, "맑음", 18))
        assertEquals(WeatherUi.OK, weatherUi(ok))
        assertEquals("맑음 18°C", weatherText(ok))
        WeatherStatus.entries.filter { it != WeatherStatus.OK }.forEach { status ->
            val failed = session(true, true, LocationStatus.OK, WeatherResult(status))
            assertEquals(WeatherUi.FAILED, weatherUi(failed))
            assertNull(weatherText(failed))
        }
    }

    @Test
    fun locationFailureMeansFailedNotPending() {
        assertEquals(WeatherUi.FAILED, weatherUi(session(true, true, LocationStatus.TIMEOUT)))
    }
}
