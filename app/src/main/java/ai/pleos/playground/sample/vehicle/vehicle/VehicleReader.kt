package ai.pleos.playground.sample.vehicle.vehicle

import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/** 기능 하나의 읽기 결과. [state]가 READABLE이고 [value]가 있을 때만 값이 있다. */
data class Reading<T>(val state: SupportState, val value: T? = null)

/**
 * SC-250과 SC-260이 쓰는 차량 상태 스냅샷(메모리에만 보관).
 * 문, 창문은 영역별 값(null은 그 영역을 읽지 못함)이고 [doorsState], [windowsState]는 기능 전체 상태이다.
 */
data class VehicleSnapshot(
    val readAtMillis: Long,
    val doorsState: SupportState,
    val doors: Map<DoorValue, Int?>,
    val windowsState: SupportState,
    val windows: Map<WindowValue, Int?>,
    val battery: Reading<Int>,
    val odometer: Reading<Float>,
    val turnSignal: Reading<TurnSignalValue>
) {
    /** 하나라도 값을 읽었는지. 제목 문구("기록했어요"/"확인하지 못했어요")를 정할 때 쓴다. */
    fun hasAnyValue(): Boolean =
        doors.values.any { it != null } || windows.values.any { it != null } ||
            battery.value != null || odometer.value != null || turnSignal.value != null
}

/**
 * 5개 기능을 한 번씩 읽는다(get 호출만, 리스너 등록 없음, set/제어 호출 없음).
 * 값을 읽기 전에 capability 확인 결과가 "읽을 수 있어요"인 기능만 읽는다. 영역은 capability 응답에서 확인된 것만 읽는다.
 * 호출마다 예외를 잡고 3초 제한을 둔다. 실패한 호출은 null이 되어 "확인 불가"로 표시된다.
 */
class VehicleReader(
    private val session: VehicleSession,
    private val timeoutMs: Long = 3_000L
) {

    suspend fun readAll(
        support: Map<VehicleFeature, SupportState>,
        areas: CapabilityAreas
    ): VehicleSnapshot = coroutineScope {
        val doorsState = support[VehicleFeature.DOOR] ?: SupportState.UNKNOWN
        val windowsState = support[VehicleFeature.WINDOW] ?: SupportState.UNKNOWN
        val doors = if (doorsState == SupportState.READABLE) {
            areas.door.map { area ->
                area to async {
                    call<Int> { ok, fail -> session.vehicle.getDoor().getCurrentDoorPosition(DoorArea(area), ok, fail) }
                }
            }
        } else emptyList()
        val windows = if (windowsState == SupportState.READABLE) {
            areas.window.map { area ->
                area to async {
                    call<Int> { ok, fail -> session.vehicle.getWindow().getCurrentWindowPosition(WindowArea(area), ok, fail) }
                }
            }
        } else emptyList()
        val battery = async {
            readOne<Int>(support[VehicleFeature.BATTERY]) { ok, fail ->
                session.vehicle.getEvBattery().getEvBatteryStateOfCharge(ok, fail)
            }
        }
        val odometer = async {
            readOne<Float>(support[VehicleFeature.ODOMETER]) { ok, fail ->
                session.vehicle.getOdometer().getCurrentDistanceDriven(ok, fail)
            }
        }
        val turnSignal = async {
            readOne<TurnSignalValue>(support[VehicleFeature.TURN_SIGNAL]) { ok, fail ->
                session.vehicle.getTurnSignal().getCurrentTurnSignalState(ok, fail)
            }
        }
        VehicleSnapshot(
            readAtMillis = System.currentTimeMillis(),
            doorsState = doorsState,
            doors = doors.associate { (area, deferred) -> area to deferred.await() },
            windowsState = windowsState,
            windows = windows.associate { (area, deferred) -> area to deferred.await() },
            battery = battery.await(),
            odometer = odometer.await(),
            turnSignal = turnSignal.await()
        )
    }

    /** capability가 "읽을 수 있어요"가 아니면 읽지 않고 그 상태를 그대로 돌려준다. */
    private suspend fun <T> readOne(
        state: SupportState?,
        block: (ok: (T?) -> Unit, fail: (Exception) -> Unit) -> Unit
    ): Reading<T> {
        val support = state ?: SupportState.UNKNOWN
        if (support != SupportState.READABLE) return Reading(support)
        val value = call(block)
        return if (value != null) Reading(SupportState.READABLE, value) else Reading(SupportState.UNKNOWN)
    }

    /** get 호출 한 번. 예외, 실패 콜백, 시간 초과는 모두 null이다. */
    private suspend fun <T> call(
        block: (ok: (T?) -> Unit, fail: (Exception) -> Unit) -> Unit
    ): T? = try {
        session.requireInitialized()
        withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<T?> { continuation ->
                val finished = AtomicBoolean(false)
                fun finish(value: T?) {
                    if (finished.compareAndSet(false, true) && continuation.isActive) continuation.resume(value)
                }
                try {
                    block({ finish(it) }, { finish(null) })
                } catch (e: Exception) {
                    finish(null)
                }
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
