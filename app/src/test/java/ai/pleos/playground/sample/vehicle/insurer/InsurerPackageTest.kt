package ai.pleos.playground.sample.vehicle.insurer

import ai.pleos.playground.sample.vehicle.address.AddressResult
import ai.pleos.playground.sample.vehicle.address.AddressStatus
import ai.pleos.playground.sample.vehicle.location.LocationStatus
import ai.pleos.playground.sample.vehicle.session.AccidentSession
import ai.pleos.playground.sample.vehicle.session.AllChecklists
import ai.pleos.playground.sample.vehicle.session.InjuryAnswer
import ai.pleos.playground.sample.vehicle.vehicle.Reading
import ai.pleos.playground.sample.vehicle.vehicle.SupportState
import ai.pleos.playground.sample.vehicle.vehicle.VehicleSnapshot
import ai.pleos.playground.sample.vehicle.weather.WeatherResult
import ai.pleos.playground.sample.vehicle.weather.WeatherStatus
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SC-290 전송 항목, 미리보기, 전송 내용, 접수 번호 확인. 값은 모두 지어낸 것이며 실제 주소·좌표·메모가 아니다. */
class InsurerPackageTest {

    private val fakeAddress = "가나도 다라시 마바로 12"
    private val fakeMemo = "지어낸 메모 사아자"

    private fun snapshot() = VehicleSnapshot(
        readAtMillis = 1_700_000_100_000L,
        doorsState = SupportState.READABLE,
        doors = mapOf(DoorValue.ROW_1_LEFT to 0, DoorValue.HOOD to 100),
        windowsState = SupportState.READABLE,
        windows = mapOf(WindowValue.ROW_1_LEFT to 0),
        battery = Reading(SupportState.READABLE, 80),
        odometer = Reading(SupportState.READABLE, 12.3f),
        turnSignal = Reading(SupportState.READABLE, TurnSignalValue.OFF)
    )

    private fun minimal() = AccidentSession(startedAtMillis = 1_700_000_000_000L)

    private fun full() = AccidentSession(
        startedAtMillis = 1_700_000_000_000L,
        checks = AllChecklists.flatMap { it.items }.take(5).associate { it.id to true },
        injury = InjuryAnswer.YES,
        vehicle = snapshot(),
        locationRequested = true,
        locationStatus = LocationStatus.OK,
        addressRequested = true,
        address = AddressResult(AddressStatus.OK, fakeAddress),
        weatherRequested = true,
        weather = WeatherResult(WeatherStatus.OK, "맑음", 18),
        voiceMemo = fakeMemo
    )

    @Test
    fun exactlySixItemsFromSpec() {
        assertEquals(
            listOf("사고 시각", "위치와 주소", "날씨", "차량 상태", "안내 이행 기록", "음성 메모"),
            InsurerItem.entries.map { it.label }
        )
    }

    @Test
    fun onlyTimeAndChecksAvailableWithoutValues() {
        assertEquals(setOf(InsurerItem.TIME, InsurerItem.CHECKS), InsurerPackage.available(minimal()))
        assertTrue(InsurerPackage.available(null).isEmpty())
    }

    @Test
    fun allSixAvailableWhenValuesExist() {
        assertEquals(InsurerItem.entries.toSet(), InsurerPackage.available(full()))
    }

    @Test
    fun placeNeedsAddressWeatherNeedsSuccessMemoNeedsSavedText() {
        val noAddress = full().copy(address = AddressResult(AddressStatus.NETWORK))
        assertFalse(InsurerItem.PLACE in InsurerPackage.available(noAddress))
        val noWeather = full().copy(weather = WeatherResult(WeatherStatus.TIMEOUT))
        assertFalse(InsurerItem.WEATHER in InsurerPackage.available(noWeather))
        val noMemo = full().copy(voiceMemo = null)
        assertFalse(InsurerItem.VOICE in InsurerPackage.available(noMemo))
        val blankMemo = full().copy(voiceMemo = "  ")
        assertFalse(InsurerItem.VOICE in InsurerPackage.available(blankMemo))
        val noVehicle = full().copy(vehicle = null)
        assertFalse(InsurerItem.VEHICLE in InsurerPackage.available(noVehicle))
    }

    @Test
    fun selectedIsAvailableMinusUnchecked() {
        val s = full()
        val sel = InsurerPackage.selected(s, setOf(InsurerItem.WEATHER, InsurerItem.VOICE))
        assertEquals(setOf(InsurerItem.TIME, InsurerItem.PLACE, InsurerItem.VEHICLE, InsurerItem.CHECKS), sel)
        // 고를 수 없는 항목을 꺼 둔 것은 영향이 없고, 고를 수 없는 항목은 선택에 들지 않는다.
        assertEquals(setOf(InsurerItem.TIME, InsurerItem.CHECKS), InsurerPackage.selected(minimal(), setOf(InsurerItem.PLACE)))
        assertTrue(InsurerPackage.selected(full(), InsurerItem.entries.toSet()).isEmpty())
    }

    @Test
    fun payloadHasOnlySelectedItems() {
        val payload = InsurerPackage.payload(full(), setOf(InsurerItem.TIME, InsurerItem.VOICE))
        assertEquals(setOf("time", "voiceMemo"), payload.keys)
        assertEquals(fakeMemo, payload["voiceMemo"])
        assertTrue(InsurerPackage.payload(full(), emptySet()).isEmpty())
    }

    @Test
    fun payloadNeverIncludesUnavailableItemsEvenIfSelected() {
        val payload = InsurerPackage.payload(minimal(), InsurerItem.entries.toSet())
        assertEquals(setOf("time", "checks"), payload.keys)
    }

    private fun allKeys(value: Any?): Set<String> = when (value) {
        is Map<*, *> -> value.keys.map { it.toString() }.toSet() + value.values.flatMap { allKeys(it) }
        is List<*> -> value.flatMap { allKeys(it) }.toSet()
        else -> emptySet()
    }

    @Test
    fun noForbiddenFieldsAnywhere() {
        val payload = InsurerPackage.payload(full(), InsurerItem.entries.toSet())
        val keys = allKeys(payload)
        val topAllowed = setOf("time", "place", "weather", "vehicle", "checks", "voiceMemo")
        assertEquals(topAllowed, payload.keys)
        // 부상 응답, 좌표, 상대방 정보, 사진, 차대번호 칸이 없다.
        listOf("injury", "latitude", "longitude", "lat", "lng", "coordinates", "photo", "image", "vin", "contact", "plateNumber", "otherParty")
            .forEach { assertFalse("금지 키: $it", keys.contains(it)) }
        // 부상 응답 글자가 값으로도 새지 않는다.
        assertFalse(payload.toString().contains("몇 명"))
        assertFalse(payload.toString().contains("있음"))
    }

    @Test
    fun receiptNumberFormatAndNoPersonalValues() {
        val n = InsurerPackage.receiptNumber(1_700_000_123_456L, 4242)
        assertTrue(n, Regex("MOCK-\\d{4}-\\d{4}").matches(n))
        assertTrue(n.startsWith("MOCK-"))
        // 음수와 큰 값도 4자리로 접힌다.
        assertTrue(Regex("MOCK-\\d{4}-\\d{4}").matches(InsurerPackage.receiptNumber(0L, -7)))
        assertTrue(Regex("MOCK-\\d{4}-\\d{4}").matches(InsurerPackage.receiptNumber(Long.MAX_VALUE, Int.MIN_VALUE)))
    }

    @Test
    fun receiptLogHasNamesAndCountOnlyNoValues() {
        val s = full()
        val receipt = InsurerPackage.receipt(s, setOf(InsurerItem.PLACE, InsurerItem.WEATHER, InsurerItem.VOICE, InsurerItem.TIME), 1_700_000_200_000L, 1)
        assertEquals(listOf("사고 시각", "위치와 주소", "날씨", "음성 메모"), receipt.itemNames)
        val text = receipt.toString()
        assertFalse(text.contains(fakeAddress))
        assertFalse(text.contains(fakeMemo))
        assertFalse(text.contains("맑음"))
        assertEquals(1_700_000_200_000L, receipt.sentAtMillis)
    }

    @Test
    fun receiptSkipsUnavailableSelectedItems() {
        val receipt = InsurerPackage.receipt(minimal(), InsurerItem.entries.toSet(), 1L, 1)
        assertEquals(listOf("사고 시각", "안내 이행 기록"), receipt.itemNames)
    }

    @Test
    fun previewShowsFullMemoAndChecksSummary() {
        val s = full()
        assertEquals(fakeMemo, InsurerPackage.preview(InsurerItem.VOICE, s))
        val checks = InsurerPackage.preview(InsurerItem.CHECKS, s)
        assertTrue(checks, checks!!.startsWith("체크한 5/12"))
        assertTrue(checks.contains("외 2개"))
        assertEquals("체크한 0/12", InsurerPackage.preview(InsurerItem.CHECKS, minimal()))
        assertNull(InsurerPackage.preview(InsurerItem.VOICE, minimal()))
        assertEquals(fakeAddress, InsurerPackage.preview(InsurerItem.PLACE, s))
        assertEquals("맑음 18°C", InsurerPackage.preview(InsurerItem.WEATHER, s))
    }

    @Test
    fun insurerReceiptsAreNotInTheSavedRecord() {
        val s = full().withInsurerReceipt(InsurerReceipt("MOCK-0001-0002", 1L, listOf("사고 시각")))
        assertEquals(1, s.insurerReceipts.size)
        val record = ai.pleos.playground.sample.vehicle.record.AccidentRecord.from(s)
        assertFalse(record.toString().contains("MOCK-"))
        assertEquals(setOf("version", "startedAt", "location", "weather", "vehicle", "checks"), record.keys)
    }
}
