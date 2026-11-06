package ai.pleos.playground.sample.vehicle.vehicle

import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/** SC-000이 지원 여부를 확인하는 5개 기능. */
enum class VehicleFeature(val title: String) {
    DOOR("문"),
    WINDOW("창문"),
    BATTERY("배터리"),
    ODOMETER("주행 거리"),
    TURN_SIGNAL("방향지시등")
}

/** SPEC 1.5의 세 상태. */
enum class SupportState(val label: String) {
    READABLE("읽을 수 있어요"),
    UNSUPPORTED("이 차량에서는 지원하지 않아요"),
    UNKNOWN("확인하지 못했어요")
}

/** capability 응답에서 확인된 영역. SC-250은 이 영역만 읽는다(규칙 5). */
data class CapabilityAreas(
    val door: List<DoorValue> = emptyList(),
    val window: List<WindowValue> = emptyList()
)

data class CapabilityResult(
    val states: Map<VehicleFeature, SupportState>,
    val areas: CapabilityAreas
)

/**
 * 다시 확인할 기능을 고른다. 확인하지 못했거나(UNKNOWN) 아직 결과가 없는 기능만이다.
 * 읽을 수 있음(READABLE)과 지원 안 함(UNSUPPORTED)은 다시 확인하지 않는다.
 */
fun unknownFeatures(states: Map<VehicleFeature, SupportState>): Set<VehicleFeature> =
    VehicleFeature.entries.filter { (states[it] ?: SupportState.UNKNOWN) == SupportState.UNKNOWN }.toSet()

/**
 * 다시 확인한 [retried]를 [previous]에 합친다. [retried]에 들어 있는 기능의 상태만 바꾸고, 문·창문 영역은
 * 그 기능이 READABLE이 되어 영역이 온 경우에만 바꾼다. 다시 확인하지 않은 기능은 그대로 둔다.
 */
fun mergeProbe(previous: CapabilityResult, retried: CapabilityResult): CapabilityResult {
    val doorResolved = retried.states[VehicleFeature.DOOR] == SupportState.READABLE
    val windowResolved = retried.states[VehicleFeature.WINDOW] == SupportState.READABLE
    return CapabilityResult(
        states = previous.states + retried.states,
        areas = CapabilityAreas(
            door = if (doorResolved) retried.areas.door else previous.areas.door,
            window = if (windowResolved) retried.areas.window else previous.areas.window
        )
    )
}

/**
 * 5개 기능의 capability(지원 여부)만 확인한다. 값을 읽거나 set을 부르지 않는다.
 * 기능마다 예외를 잡고, 3초 안에 응답이 없으면 "확인하지 못했어요"로 둔다. 한 기능의 실패가 다른 기능에 번지지 않는다.
 */
class CapabilityProbe(
    private val session: VehicleSession,
    private val timeoutMs: Long = 3_000L
) {

    private data class FeatureProbe(
        val state: SupportState,
        val doorAreas: List<DoorValue> = emptyList(),
        val windowAreas: List<WindowValue> = emptyList()
    )

    /** [only]에 든 기능만 확인한다(기본은 5개 모두). 결과 상태 맵에도 그 기능만 들어 있다. */
    suspend fun probe(only: Set<VehicleFeature> = VehicleFeature.entries.toSet()): CapabilityResult = coroutineScope {
        val results = VehicleFeature.entries
            .filter { it in only }
            .map { feature -> feature to async { check(feature) } }
            .associate { (feature, deferred) -> feature to deferred.await() }
        CapabilityResult(
            states = results.mapValues { it.value.state },
            areas = CapabilityAreas(
                door = results[VehicleFeature.DOOR]?.doorAreas.orEmpty(),
                window = results[VehicleFeature.WINDOW]?.windowAreas.orEmpty()
            )
        )
    }

    private suspend fun check(feature: VehicleFeature): FeatureProbe = try {
        session.requireInitialized()
        withTimeoutOrNull(timeoutMs) { request(feature) } ?: FeatureProbe(SupportState.UNKNOWN)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        FeatureProbe(SupportState.UNKNOWN)
    }

    /** 콜백이 한 번만 결과를 돌려주도록 막는다(성공/실패 중복 호출, 타임아웃 뒤 늦은 응답). */
    private suspend fun request(feature: VehicleFeature): FeatureProbe =
        suspendCancellableCoroutine { continuation ->
            val finished = AtomicBoolean(false)
            fun finish(probe: FeatureProbe) {
                if (finished.compareAndSet(false, true) && continuation.isActive) continuation.resume(probe)
            }
            val onFailed: (Exception) -> Unit = { finish(FeatureProbe(SupportState.UNKNOWN)) }
            try {
                val vehicle = session.vehicle
                when (feature) {
                    VehicleFeature.DOOR -> vehicle.getDoor().checkDoorPositionCapabilities(
                        { config, areas ->
                            val values = areas.keys.flatMap { it.values.toList() }.distinct()
                            finish(FeatureProbe(judge(config, hasAreas = values.isNotEmpty()), doorAreas = values))
                        },
                        onFailed
                    )
                    VehicleFeature.WINDOW -> vehicle.getWindow().checkWindowPositionCapabilities(
                        { config, areas ->
                            val values = areas.keys.flatMap { it.values.toList() }.distinct()
                            finish(FeatureProbe(judge(config, hasAreas = values.isNotEmpty()), windowAreas = values))
                        },
                        onFailed
                    )
                    VehicleFeature.BATTERY -> vehicle.getEvBattery().checkEvBatteryStateOfChargeCapabilities(
                        { config -> finish(FeatureProbe(judge(config))) },
                        onFailed
                    )
                    VehicleFeature.ODOMETER -> vehicle.getOdometer().checkCurrentDistanceDrivenCapabilities(
                        { config -> finish(FeatureProbe(judge(config))) },
                        onFailed
                    )
                    VehicleFeature.TURN_SIGNAL -> vehicle.getTurnSignal().checkTurnSignalStateCapabilities(
                        { config -> finish(FeatureProbe(judge(config))) },
                        onFailed
                    )
                }
            } catch (e: Exception) {
                finish(FeatureProbe(SupportState.UNKNOWN))
            }
        }

    /** READ, READ_WRITE만 읽을 수 있다. NONE, WRITE만 오는 경우는 미지원이다. */
    private fun judge(config: CapabilityConfig, hasAreas: Boolean = true): SupportState {
        val readable = config.accessType == AccessType.READ || config.accessType == AccessType.READ_WRITE
        return if (readable && hasAreas) SupportState.READABLE else SupportState.UNSUPPORTED
    }
}
