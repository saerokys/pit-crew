package ai.pleos.playground.sample.vehicle.vehicle

import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 시작 때 UNKNOWN인 기능만 다시 확인하고 결과를 합치는 순수 함수 확인(지어낸 영역과 상태). */
class CapabilityRetryTest {

    private fun result(states: Map<VehicleFeature, SupportState>, door: List<DoorValue> = emptyList(), window: List<WindowValue> = emptyList()) =
        CapabilityResult(states, CapabilityAreas(door = door, window = window))

    private val startup = result(
        mapOf(
            VehicleFeature.DOOR to SupportState.UNKNOWN,
            VehicleFeature.WINDOW to SupportState.READABLE,
            VehicleFeature.BATTERY to SupportState.READABLE,
            VehicleFeature.ODOMETER to SupportState.UNSUPPORTED,
            VehicleFeature.TURN_SIGNAL to SupportState.READABLE
        ),
        window = listOf(WindowValue.ROW_1_LEFT, WindowValue.ROW_1_RIGHT)
    )

    @Test
    fun onlyUnknownIsSelected() {
        assertEquals(setOf(VehicleFeature.DOOR), unknownFeatures(startup.states))
    }

    @Test
    fun missingStateCountsAsUnknown() {
        assertEquals(VehicleFeature.entries.toSet(), unknownFeatures(emptyMap()))
        val partial = mapOf(VehicleFeature.DOOR to SupportState.READABLE)
        assertEquals(VehicleFeature.entries.toSet() - VehicleFeature.DOOR, unknownFeatures(partial))
    }

    @Test
    fun nothingToRetryWhenNoUnknown() {
        val all = VehicleFeature.entries.associateWith { SupportState.READABLE }
        assertTrue(unknownFeatures(all).isEmpty())
        // 지원 안 함도 다시 확인하지 않는다.
        assertTrue(unknownFeatures(mapOf(VehicleFeature.DOOR to SupportState.UNSUPPORTED)).contains(VehicleFeature.DOOR).not())
    }

    @Test
    fun resolvedDoorIsMergedWithAreasAndOthersKept() {
        val retried = result(
            mapOf(VehicleFeature.DOOR to SupportState.READABLE),
            door = listOf(DoorValue.ROW_1_LEFT, DoorValue.HOOD)
        )
        val merged = mergeProbe(startup, retried)
        assertEquals(SupportState.READABLE, merged.states[VehicleFeature.DOOR])
        assertEquals(listOf(DoorValue.ROW_1_LEFT, DoorValue.HOOD), merged.areas.door)
        // 다시 확인하지 않은 기능과 창문 영역은 그대로
        assertEquals(startup.states.filterKeys { it != VehicleFeature.DOOR }, merged.states.filterKeys { it != VehicleFeature.DOOR })
        assertEquals(startup.areas.window, merged.areas.window)
    }

    @Test
    fun stillUnknownKeepsStateAndPreviousAreas() {
        val retried = result(mapOf(VehicleFeature.DOOR to SupportState.UNKNOWN))
        val merged = mergeProbe(startup, retried)
        assertEquals(SupportState.UNKNOWN, merged.states[VehicleFeature.DOOR])
        assertEquals(startup.areas, merged.areas)
    }

    @Test
    fun readableIsNeverDowngradedBecauseItIsNotRetried() {
        // 다시 확인 대상(UNKNOWN)만 retried에 들어오므로, READABLE이던 창문은 retried에 없고 그대로 남는다.
        val retried = result(mapOf(VehicleFeature.DOOR to SupportState.UNKNOWN))
        val merged = mergeProbe(startup, retried)
        assertEquals(SupportState.READABLE, merged.states[VehicleFeature.WINDOW])
        assertEquals(startup.areas.window, merged.areas.window)
    }
}
