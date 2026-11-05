package ai.pleos.playground.sample.vehicle.ui.data

import ai.pleos.playground.sample.vehicle.explorer.ApiMethodDescriptor
import ai.pleos.playground.sample.vehicle.explorer.ApiMethodStatus
import ai.pleos.playground.sample.vehicle.explorer.ApiFeatureDescriptor
import ai.pleos.playground.sample.vehicle.explorer.ApiNumericRange
import ai.pleos.playground.sample.vehicle.explorer.ApiOperation
import ai.pleos.playground.sample.vehicle.explorer.VehicleDomain
import ai.pleos.playground.sample.vehicle.session.AccidentSession
import ai.pleos.playground.sample.vehicle.settings.Consent
import ai.pleos.playground.sample.vehicle.vehicle.CapabilityAreas
import ai.pleos.playground.sample.vehicle.vehicle.SupportState
import ai.pleos.playground.sample.vehicle.vehicle.VehicleFeature
import ai.pleos.playground.sample.vehicle.vehicle.VehicleSnapshot

data class MainUiState(
    val page: AppPage = AppPage.HOME,
    val runtimeVersions: RuntimeVersions = RuntimeVersions(),
    val selectedDomain: VehicleDomain? = null,
    val apiExplorerPane: ApiExplorerPane = ApiExplorerPane.FEATURE_LIST,
    val apiMethods: List<ApiMethodDescriptor> = emptyList(),
    val apiFeatures: List<ApiFeatureDescriptor> = emptyList(),
    val domainFeatureCounts: Map<VehicleDomain, Int> = emptyMap(),
    val featureSearchQuery: String = "",
    val selectedFeatureId: String? = null,
    val focusedMethodId: String? = null,
    val apiFocusSequence: Long = 0,
    val apiMethodStatuses: Map<String, ApiMethodStatus> = emptyMap(),
    val apiMethodResponses: Map<String, String> = emptyMap(),
    val featureCurrentValues: Map<String, String> = emptyMap(),
    val featureCurrentDetails: Map<String, String> = emptyMap(),
    val featureValueRanges: Map<String, Map<String, ApiNumericRange>> = emptyMap(),
    val explorerEvents: List<String> = emptyList(),
    val isInitialized: Boolean = false,
    val startup: StartupPhase = StartupPhase.LOADING,
    /** 사고 모드 세션. 사고 모드 밖(홈)에서는 null. */
    val accidentSession: AccidentSession? = null,
    val featureSupport: Map<VehicleFeature, SupportState> = emptyMap(),
    /** capability 응답에서 확인된 영역(문, 창문). SC-250이 이 영역만 읽는다. */
    val capabilityAreas: CapabilityAreas = CapabilityAreas(),
    val isReadingVehicle: Boolean = false,
    /** SC-205 동의 상태(앱을 껐다 켜도 유지, 값은 스위치 3개뿐). */
    val consent: Consent = Consent(),
    /** 이 차량에 위치 서비스가 있는지(isSupported). 없으면 SC-205 위치 스위치가 회색이다. */
    val locationSupported: Boolean = false,
    /** SC-300 출발 전 점검에서 읽은 값(메모리에만 보관, 사고 모드 세션과 무관). 화면에 들어올 때마다 지우고 새로 읽는다. */
    val preCheckSnapshot: VehicleSnapshot? = null,
    val isPreChecking: Boolean = false,
    /** SC-400이 보여 주는 지원 확인 시각(SC-000 또는 [다시 확인]). */
    val linkCheckedAtMillis: Long? = null,
    val isCheckingLink: Boolean = false,
    val isSubscribed: Boolean = false,
    val subscribedFeatureId: String? = null,
    val capabilityAccess: String? = null,
    val currentPosition: Int? = null,
    val errorMessage: String? = null,
    val featureErrorMessage: String? = null
)

internal fun MainUiState.withCapabilityRanges(
    featureId: String,
    ranges: Map<String, ApiNumericRange>
): MainUiState = copy(
    featureValueRanges = if (ranges.isEmpty()) {
        featureValueRanges - featureId
    } else {
        featureValueRanges + (featureId to ranges)
    }
)

internal fun MainUiState.withFeatureResult(
    featureId: String,
    operation: ApiOperation,
    value: String,
    detail: String?
): MainUiState {
    if (operation !in setOf(ApiOperation.GET, ApiOperation.REGISTER, ApiOperation.OTHER)) return this
    return copy(
        featureCurrentValues = featureCurrentValues + (featureId to value),
        featureCurrentDetails = if (detail == null) {
            featureCurrentDetails - featureId
        } else {
            featureCurrentDetails + (featureId to detail)
        }
    )
}

data class RuntimeVersions(
    val sdkVersion: String = "unknown",
    val vehicleServiceVersion: String = "unknown"
)

enum class AppPage {
    HOME,
    DEV_MENU,
    DEV_TOOLS,
    DEV_COMPONENTS
}

/** SC-000 진행 상태. */
enum class StartupPhase {
    LOADING,
    DONE
}

enum class ApiExplorerPane {
    FEATURE_LIST,
    FEATURE_DASHBOARD
}
