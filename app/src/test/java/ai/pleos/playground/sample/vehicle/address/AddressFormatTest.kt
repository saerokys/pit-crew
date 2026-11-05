package ai.pleos.playground.sample.vehicle.address

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 주소 조립과 좌표 반올림 확인. 값은 모두 지어낸 이름과 숫자이며 실제 주소가 아니다. */
class AddressFormatTest {

    @Test
    fun composesProvinceCityBoroughRoadAndNumber() {
        val address = mapOf(
            "state" to "가나도", "city" to "다라시", "borough" to "마바구", "road" to "사아로", "house_number" to "12"
        )
        assertEquals("가나도 다라시 마바구 사아로 12", AddressFormat.compose(address))
    }

    @Test
    fun composesMetropolitanCityWithoutProvince() {
        val address = mapOf("city" to "가나시", "borough" to "다라구", "road" to "마바로", "house_number" to "7")
        assertEquals("가나시 다라구 마바로 7", AddressFormat.compose(address))
    }

    @Test
    fun usesAreaNameWhenRoadIsMissingAndSkipsHouseNumber() {
        val address = mapOf("city" to "가나시", "borough" to "다라구", "suburb" to "마바동", "house_number" to "3")
        assertEquals("가나시 다라구 마바동", AddressFormat.compose(address))
    }

    @Test
    fun returnsNullWhenThereIsNoRoadOrArea() {
        assertNull(AddressFormat.compose(mapOf("city" to "가나시", "country" to "테스트국")))
        assertNull(AddressFormat.compose(emptyMap()))
    }

    @Test
    fun ignoresNamePostcodeAndCountry() {
        val address = mapOf(
            "name" to "지어낸 상호", "road" to "사아로", "city" to "가나시",
            "postcode" to "00000", "country" to "테스트국", "country_code" to "xx"
        )
        assertEquals("가나시 사아로", AddressFormat.compose(address))
    }

    @Test
    fun skipsBlankValuesAndRepeatedNeighbors() {
        val address = mapOf("city" to "가나시", "borough" to "가나시", "county" to "  ", "road" to "사아로")
        assertEquals("가나시 사아로", AddressFormat.compose(address))
    }

    @Test
    fun roundsCoordinatesToFourDecimalsWithDot() {
        assertEquals("12.3457", AddressFormat.roundCoordinate(12.345678))
        assertEquals("-98.7654", AddressFormat.roundCoordinate(-98.765432))
        assertEquals("0.0000", AddressFormat.roundCoordinate(0.0))
    }
}
