package ai.pleos.playground.sample.vehicle.ui.data

import ai.pleos.playground.sample.vehicle.explorer.ApiMethodDescriptor
import ai.pleos.playground.sample.vehicle.explorer.ApiMethodStatus
import ai.pleos.playground.sample.vehicle.explorer.ApiFeatureDescriptor
import ai.pleos.playground.sample.vehicle.explorer.ApiNumericRange
import ai.pleos.playground.sample.vehicle.explorer.ApiOperation
import ai.pleos.playground.sample.vehicle.explorer.VehicleDomain
import ai.pleos.playground.sample.vehicle.insurer.InsurerItem
import ai.pleos.playground.sample.vehicle.navi.DemoPlaces
import ai.pleos.playground.sample.vehicle.navi.DestinationCategory
import ai.pleos.playground.sample.vehicle.navi.NaviState
import ai.pleos.playground.sample.vehicle.navi.RouteResult
import ai.pleos.playground.sample.vehicle.insurer.InsurerSendState
import ai.pleos.playground.sample.vehicle.record.SaveState
import ai.pleos.playground.sample.vehicle.record.SavedRecordUi
import ai.pleos.playground.sample.vehicle.session.AccidentSession
import ai.pleos.playground.sample.vehicle.settings.Consent
import ai.pleos.playground.sample.vehicle.stt.VoiceState
import ai.pleos.playground.sample.vehicle.tts.SpeechState
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
    /** [소리로 듣기] 버튼 상태(음성 스위치를 켜고 버튼을 눌렀을 때만 바뀐다). */
    val speechState: SpeechState = SpeechState.IDLE,
    /** SC-230 음성 메모 인식 상태(음성 스위치를 켜고 [말하기]를 눌렀을 때만 바뀐다). */
    val voiceState: VoiceState = VoiceState.IDLE,
    /** 방금 인식한 결과(아직 [저장]하지 않음). 화면을 떠나거나 [다시 말하기]를 누르면 버린다. 메모리에만 둔다. */
    val voiceDraft: String? = null,
    /** [voiceDraft]가 글자 수 제한(100자)으로 잘렸는지(잘렸을 때만 확인 화면에 안내 한 줄). */
    val voiceDraftCut: Boolean = false,
    /** 방금 못 알아들어 대기로 돌아왔는지("잘 듣지 못했어요. 다시 말해 주세요."). 다시 말하기를 누르거나 화면을 떠나면 지운다. */
    val voiceNotHeard: Boolean = false,
    /** SC-290에서 사용자가 끈 항목(기본은 고를 수 있는 항목 모두 켜짐). 세션과 함께 사라진다. */
    val insurerUnchecked: Set<InsurerItem> = emptySet(),
    val insurerSendState: InsurerSendState = InsurerSendState.IDLE,
    /** 마지막으로 보낸 항목(같은 항목으로 다시 보내지 않게 한다). */
    val insurerLastSent: Set<InsurerItem> = emptySet(),
    /** SC-280 NaviHelper 연결 상태와 길안내 요청 결과(SC-280에 있는 동안에만 연결). */
    val naviState: NaviState = NaviState.IDLE,
    val routeResult: RouteResult = RouteResult.NONE,
    /** SC-280에서 고른 목적지 유형(기본은 시안처럼 병원)과 후보 id(기본은 첫 후보). 세션과 함께 초기화된다. */
    val destCategory: DestinationCategory = DestinationCategory.HOSPITAL,
    val destPlaceId: String? = DemoPlaces.forCategory(DestinationCategory.HOSPITAL).first().id,
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
    /** SC-270 [기록 저장] 진행 상태. */
    val saveState: SaveState = SaveState.IDLE,
    /** 저장된 기록이 있는지(SC-270의 "바뀌어요" 안내와 개발자 도구 표시용). 기록 내용은 들고 있지 않다. */
    val hasSavedRecord: Boolean = false,
    /** SC-520이 보여 주는 저장 기록 요약(화면에 들어올 때 읽는다). 좌표·음성 메모 등은 기록에 없다. */
    val savedRecord: SavedRecordUi = SavedRecordUi.Loading,
    /** SC-520 [삭제]가 실패했는지("삭제하지 못했어요"). */
    val deleteFailed: Boolean = false,
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
