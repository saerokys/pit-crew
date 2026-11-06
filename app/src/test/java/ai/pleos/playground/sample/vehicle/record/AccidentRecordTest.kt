package ai.pleos.playground.sample.vehicle.record

import ai.pleos.playground.sample.vehicle.address.AddressResult
import ai.pleos.playground.sample.vehicle.address.AddressStatus
import ai.pleos.playground.sample.vehicle.location.LocationStatus
import ai.pleos.playground.sample.vehicle.session.AccidentSession
import ai.pleos.playground.sample.vehicle.session.AllChecklists
import ai.pleos.playground.sample.vehicle.session.InjuryAnswer
import ai.pleos.playground.sample.vehicle.vehicle.Reading
import ai.pleos.playground.sample.vehicle.vehicle.SupportState
import ai.pleos.playground.sample.vehicle.vehicle.VehicleSnapshot
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 기록 패키지 변환 확인. 값은 모두 지어낸 것이며 실제 주소·좌표가 아니다. */
class AccidentRecordTest {

    private val fakeAddress = "가나도 다라시 마바로 12"

    private fun session(
        locationRequested: Boolean = false,
        locationStatus: LocationStatus? = null,
        addressRequested: Boolean = false,
        address: AddressResult? = null,
        injury: InjuryAnswer = InjuryAnswer.NOT_ANSWERED,
        checks: Map<String, Boolean> = emptyMap(),
        vehicle: VehicleSnapshot? = null
    ) = AccidentSession(
        startedAtMillis = 1_700_000_000_000L,
        checks = checks,
        injury = injury,
        vehicle = vehicle,
        locationRequested = locationRequested,
        locationStatus = locationStatus,
        addressRequested = addressRequested,
        address = address
    )

    private fun snapshot() = VehicleSnapshot(
        readAtMillis = 1_700_000_100_000L,
        doorsState = SupportState.READABLE,
        doors = mapOf(DoorValue.ROW_1_LEFT to 0, DoorValue.HOOD to 100, DoorValue.REAR to null),
        windowsState = SupportState.READABLE,
        windows = mapOf(WindowValue.ROW_1_LEFT to 6),
        battery = Reading(SupportState.READABLE, 80),
        odometer = Reading(SupportState.READABLE, 12.3f),
        turnSignal = Reading(SupportState.READABLE, TurnSignalValue.OFF)
    )

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.part(key: String) = this[key] as Map<String, Any?>

    /** 모든 단계의 키 이름을 모은다. */
    private fun allKeys(value: Any?): Set<String> = when (value) {
        is Map<*, *> -> value.keys.map { it.toString() }.toSet() + value.values.flatMap { allKeys(it) }
        is List<*> -> value.flatMap { allKeys(it) }.toSet()
        else -> emptySet()
    }

    @Test
    fun topLevelHasOnlyTheFourItemsAndFormatVersion() {
        val record = AccidentRecord.from(session())
        assertEquals(
            setOf("version", "startedAt", "location", "vehicle", "checks"),
            record.keys
        )
        assertEquals(AccidentRecord.FORMAT_VERSION, record["version"])
        assertEquals(1_700_000_000_000L, record["startedAt"])
    }

    @Test
    fun noForbiddenFieldsAnywhere() {
        // 주소 성공, 부상 "있음", 차량 값, 체크까지 모두 있는 가장 큰 기록으로 모든 키를 모아 허용 목록과 비교한다.
        val full = session(
            locationRequested = true,
            locationStatus = LocationStatus.OK,
            addressRequested = true,
            address = AddressResult(AddressStatus.OK, fakeAddress),
            injury = InjuryAnswer.YES,
            checks = AllChecklists.flatMap { it.items }.associate { it.id to true },
            vehicle = snapshot()
        )
        val keys = allKeys(AccidentRecord.from(full))
        val allowed = setOf(
            "version", "startedAt", "location", "vehicle", "checks",
            "status", "addressStatus", "address", "addressCredit",
            "readAt", "doors", "windows", "battery", "odometer", "turnSignal",
            "ROW_1_LEFT", "HOOD", "REAR"
        ) + AllChecklists.flatMap { it.items }.map { it.id }
        assertEquals("허용 목록에 없는 키: ${keys - allowed}", emptySet<String>(), keys - allowed)
        // 부상 응답, 좌표, 사진, 차대번호, 상대방 정보 칸이 없다.
        listOf("injury", "latitude", "longitude", "lat", "lng", "coordinates", "photo", "image", "vin", "contact", "plateNumber").forEach {
            assertFalse("금지 키: $it", keys.contains(it))
        }
        // 부상 응답 글자가 값으로도 새지 않는다.
        assertFalse(AccidentRecord.from(full).toString().contains("몇 명"))
    }

    @Test
    fun checksHaveAllTwelveItemsAndUncheckedAreFalse() {
        val ids = AllChecklists.flatMap { it.items }.map { it.id }
        assertEquals(12, ids.size)
        val checks = AccidentRecord.from(session(checks = mapOf(ids[0] to true, ids[5] to true))).part("checks")
        assertEquals(ids.toSet(), checks.keys)
        assertEquals(2, checks.values.count { it == true })
        assertEquals(10, checks.values.count { it == false })
    }

    @Test
    fun locationOffRecordsNotUsedAndNoAddress() {
        val loc = AccidentRecord.from(session(locationRequested = false)).part("location")
        assertEquals("NOT_USED", loc["status"])
        assertEquals("NONE", loc["addressStatus"])
        assertNull(loc["address"])
        assertNull(loc["addressCredit"])
    }

    @Test
    fun locationFailedRecordsUnavailable() {
        val failed = AccidentRecord.from(session(locationRequested = true, locationStatus = LocationStatus.TIMEOUT)).part("location")
        assertEquals("UNAVAILABLE", failed["status"])
        assertEquals("NONE", failed["addressStatus"])
        // 읽는 중(상태 없음)에 저장해도 확인됨으로 쓰지 않는다.
        val pending = AccidentRecord.from(session(locationRequested = true, locationStatus = null)).part("location")
        assertEquals("UNAVAILABLE", pending["status"])
    }

    @Test
    fun locationOkNetworkOffHasNoAddress() {
        val loc = AccidentRecord.from(
            session(locationRequested = true, locationStatus = LocationStatus.OK, addressRequested = false)
        ).part("location")
        assertEquals("CONFIRMED", loc["status"])
        assertEquals("NONE", loc["addressStatus"])
        assertNull(loc["address"])
    }

    @Test
    fun addressFailureRecordsUnavailableWithoutText() {
        AddressStatus.entries.filter { it != AddressStatus.OK }.forEach { status ->
            val loc = AccidentRecord.from(
                session(
                    locationRequested = true, locationStatus = LocationStatus.OK,
                    addressRequested = true, address = AddressResult(status)
                )
            ).part("location")
            assertEquals("CONFIRMED", loc["status"])
            assertEquals("UNAVAILABLE", loc["addressStatus"])
            assertNull(loc["address"])
            assertNull(loc["addressCredit"])
        }
        // 주소를 찾는 중에 저장한 경우도 같다.
        val pending = AccidentRecord.from(
            session(locationRequested = true, locationStatus = LocationStatus.OK, addressRequested = true, address = null)
        ).part("location")
        assertEquals("UNAVAILABLE", pending["addressStatus"])
    }

    @Test
    fun addressSuccessRecordsTextAndCredit() {
        val loc = AccidentRecord.from(
            session(
                locationRequested = true, locationStatus = LocationStatus.OK,
                addressRequested = true, address = AddressResult(AddressStatus.OK, fakeAddress)
            )
        ).part("location")
        assertEquals("OK", loc["addressStatus"])
        assertEquals(fakeAddress, loc["address"])
        assertEquals("© OpenStreetMap contributors", loc["addressCredit"])
    }

    @Test
    fun vehicleIsNullWhenNotRead() {
        assertNull(AccidentRecord.from(session(vehicle = null))[AccidentRecord.KEY_VEHICLE])
    }

    @Test
    fun vehicleValuesAreCopiedAndUnreadAreNull() {
        val vehicle = AccidentRecord.from(session(vehicle = snapshot())).part("vehicle")
        assertEquals(1_700_000_100_000L, vehicle["readAt"])
        assertEquals(mapOf("ROW_1_LEFT" to 0, "HOOD" to 100, "REAR" to null), vehicle["doors"])
        assertEquals(mapOf("ROW_1_LEFT" to 6), vehicle["windows"])
        assertEquals(80, vehicle["battery"])
        assertEquals(12.3, vehicle["odometer"])
        assertEquals("OFF", vehicle["turnSignal"])
    }

    @Test
    fun summaryCounts() {
        assertEquals(12, EndingSummary.checkTotal)
        assertEquals(0, EndingSummary.checkedCount(null))
        val ids = AllChecklists.flatMap { it.items }.map { it.id }
        assertEquals(3, EndingSummary.checkedCount(session(checks = ids.take(3).associateWith { true })))
        assertEquals(0, EndingSummary.vehicleReadCount(null))
        assertEquals(5, EndingSummary.vehicleReadCount(snapshot()))
        val onlyBattery = snapshot().copy(
            doors = mapOf(DoorValue.REAR to null),
            windows = emptyMap(),
            odometer = Reading(SupportState.UNKNOWN),
            turnSignal = Reading(SupportState.UNKNOWN)
        )
        assertEquals(1, EndingSummary.vehicleReadCount(onlyBattery))
        assertTrue(EndingSummary.VEHICLE_TOTAL == 5)
    }
}
