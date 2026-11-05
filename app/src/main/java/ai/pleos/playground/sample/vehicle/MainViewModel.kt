package ai.pleos.playground.sample.vehicle

import ai.pleos.playground.sample.vehicle.explorer.VehicleDomain
import ai.pleos.playground.sample.vehicle.explorer.VehicleApiExplorer
import ai.pleos.playground.sample.vehicle.explorer.ApiFeatureDescriptor
import ai.pleos.playground.sample.vehicle.explorer.ApiMethodDescriptor
import ai.pleos.playground.sample.vehicle.explorer.ApiMethodStatus
import ai.pleos.playground.sample.vehicle.explorer.ApiExecutionMode
import ai.pleos.playground.sample.vehicle.explorer.ApiOperation
import ai.pleos.playground.sample.vehicle.explorer.ApiNumericRange
import ai.pleos.playground.sample.vehicle.explorer.apiFeatures
import ai.pleos.playground.sample.vehicle.explorer.formatApiResponse
import ai.pleos.playground.sample.vehicle.explorer.formatFeatureDisplay
import ai.pleos.playground.sample.vehicle.explorer.requestValues
import ai.pleos.playground.sample.vehicle.ui.data.AppPage
import ai.pleos.playground.sample.vehicle.ui.data.ApiExplorerPane
import ai.pleos.playground.sample.vehicle.ui.data.FeatureRequestFlow
import ai.pleos.playground.sample.vehicle.ui.data.MainUiState
import ai.pleos.playground.sample.vehicle.address.AddressResolver
import ai.pleos.playground.sample.vehicle.address.AddressResult
import ai.pleos.playground.sample.vehicle.address.AddressStatus
import ai.pleos.playground.sample.vehicle.location.LocationReader
import ai.pleos.playground.sample.vehicle.location.LocationStatus
import ai.pleos.playground.sample.vehicle.location.isLocationSupported
import ai.pleos.playground.sample.vehicle.session.AccidentSession
import ai.pleos.playground.sample.vehicle.settings.Consent
import ai.pleos.playground.sample.vehicle.settings.ConsentStore
import ai.pleos.playground.sample.vehicle.session.InjuryAnswer
import ai.pleos.playground.sample.vehicle.ui.data.StartupPhase
import ai.pleos.playground.sample.vehicle.ui.data.withCapabilityRanges
import ai.pleos.playground.sample.vehicle.ui.data.withFeatureResult
import ai.pleos.playground.sample.vehicle.vehicle.CapabilityProbe
import ai.pleos.playground.sample.vehicle.vehicle.SupportState
import ai.pleos.playground.sample.vehicle.vehicle.VehicleClient
import ai.pleos.playground.sample.vehicle.vehicle.VehicleFeature
import ai.pleos.playground.sample.vehicle.vehicle.VehicleClient.DoorOperation
import ai.pleos.playground.sample.vehicle.vehicle.RuntimeVersionProvider
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import ai.pleos.playground.sample.vehicle.vehicle.VehicleReader
import ai.pleos.playground.sample.vehicle.vehicle.VehicleSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) :
    AndroidViewModel(application),
    VehicleClient.Callback,
    VehicleApiExplorer.Callback {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private val vehicleClient = VehicleClient(application, this)
    // 리플렉션 탐색기는 개발자 도구를 열 때 처음 만든다(시작 시 실패가 앱을 멈추지 않게).
    private var explorerInstance: VehicleApiExplorer? = null
    private val apiExplorer: VehicleApiExplorer
        get() = explorerInstance ?: VehicleApiExplorer(
            session = vehicleClient,
            callback = this
        ).also { explorerInstance = it }
    private var activeTypedMethodId: String? = null
    private var readJob: Job? = null
    private var preCheckJob: Job? = null
    private var locationJob: Job? = null
    private val consentStore = ConsentStore(application)
    private val pendingApiValues = mutableMapOf<String, Map<String, String>>()
    private val featureRequestFlow = FeatureRequestFlow()

    init {
        runCatching {
            _uiState.update { it.copy(runtimeVersions = RuntimeVersionProvider(application).read()) }
        }
        runCatching {
            _uiState.update { it.copy(consent = consentStore.load(), locationSupported = isLocationSupported(application)) }
        }
        startVehicleLink()
    }

    /** SC-205 [다음]: 스위치 3개의 값을 저장하고 "정했음"으로 표시한다. 위치 허용 요청은 화면에서 이미 끝났다. */
    fun confirmConsent(location: Boolean, voice: Boolean, network: Boolean) {
        val consent = Consent(location = location, voice = voice, network = network, decided = true)
        consentStore.save(consent)
        _uiState.update { it.copy(consent = consent) }
    }

    /** 디버그 빌드의 [동의 초기화]: 저장된 동의 설정만 지운다(위치 권한은 건드리지 않는다). */
    fun resetConsent() {
        consentStore.clear()
        _uiState.update { it.copy(consent = Consent()) }
    }

    /**
     * SC-000: Vehicle SDK 초기화 -> 5개 기능의 지원 여부 확인 -> 결과 보관.
     * 초기화가 실패해도 홈으로 가고 차량 연동만 끈다. 값 읽기는 하지 않는다.
     */
    private fun startVehicleLink() {
        val initialized = tryInitialize("SC-000")
        if (!initialized) {
            _uiState.update {
                it.copy(
                    isInitialized = false,
                    startup = StartupPhase.DONE,
                    featureSupport = VehicleFeature.entries.associateWith { SupportState.UNKNOWN },
                    linkCheckedAtMillis = System.currentTimeMillis(),
                    errorMessage = null // 홈에서 한국어 안내를 보여 준다(기술 오류 문구는 숨김)
                )
            }
            return
        }
        _uiState.update { it.copy(isInitialized = true) }
        viewModelScope.launch {
            val result = CapabilityProbe(vehicleClient).probe()
            logProbe("SC-000", result)
            _uiState.update {
                it.copy(
                    featureSupport = result.states,
                    capabilityAreas = result.areas,
                    linkCheckedAtMillis = System.currentTimeMillis(),
                    startup = StartupPhase.DONE
                )
            }
        }
    }

    private fun tryInitialize(tag: String): Boolean {
        val initialized = try {
            vehicleClient.initialize()
        } catch (e: Exception) {
            false
        }
        Log.i(TAG, "$tag vehicle initialize: $initialized")
        return initialized
    }

    private fun logProbe(tag: String, result: ai.pleos.playground.sample.vehicle.vehicle.CapabilityResult) {
        result.states.forEach { (feature, state) -> Log.i(TAG, "$tag capability ${feature.name}: ${state.name}") }
        Log.i(TAG, "$tag capability areas: door=${result.areas.door.map { it.name }} window=${result.areas.window.map { it.name }}")
    }

    /**
     * SC-400 [다시 확인]: 지원 여부를 다시 확인한다(홈의 "N개를 읽을 수 있어요"도 같은 상태라 함께 바뀐다).
     * 이미 초기화에 성공했으면 다시 초기화하지 않고, 이전에 실패했을 때만 한 번 더 시도한다. 값은 읽지 않는다.
     */
    fun recheckVehicleLink() {
        if (_uiState.value.isCheckingLink) return
        _uiState.update { it.copy(isCheckingLink = true) }
        viewModelScope.launch {
            try {
                val initialized = _uiState.value.isInitialized || tryInitialize("SC-400")
                if (initialized) {
                    val result = CapabilityProbe(vehicleClient).probe()
                    logProbe("SC-400", result)
                    _uiState.update {
                        it.copy(
                            isInitialized = true,
                            featureSupport = result.states,
                            capabilityAreas = result.areas,
                            linkCheckedAtMillis = System.currentTimeMillis()
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isInitialized = false,
                            featureSupport = VehicleFeature.entries.associateWith { SupportState.UNKNOWN },
                            linkCheckedAtMillis = System.currentTimeMillis()
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "SC-400 recheck failed: ${e.javaClass.simpleName}")
            } finally {
                _uiState.update { it.copy(isCheckingLink = false) }
            }
        }
    }

    /**
     * SC-300 출발 전 점검: 들어올 때와 [다시 확인]에서 5개 중 필요한 값을 읽는다(get만, 사고 모드 세션을 만들거나 바꾸지 않음).
     * 시작할 때 이전 값을 지워 오래된 값이 남지 않게 한다. 같은 읽기 코드(VehicleReader)를 쓴다.
     */
    fun checkPreDeparture() {
        preCheckJob?.cancel()
        _uiState.update { it.copy(preCheckSnapshot = null, isPreChecking = true) }
        preCheckJob = viewModelScope.launch {
            try {
                val ready = withTimeoutOrNull(5_000L) { _uiState.first { it.startup == StartupPhase.DONE } }
                    ?: _uiState.value
                val snapshot = VehicleReader(vehicleClient).readAll(ready.featureSupport, ready.capabilityAreas)
                Log.i(TAG, "SC-300 read: " + describe(snapshot))
                _uiState.update { it.copy(preCheckSnapshot = snapshot, isPreChecking = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "SC-300 read failed: ${e.javaClass.simpleName}")
                _uiState.update { it.copy(isPreChecking = false) }
            }
        }
    }

    /** SC-200 [네, 세웠어요]: 세션을 시작하고 시작 시각을 기록한다. 이미 있으면 시각을 덮어쓰지 않는다. */
    fun startAccidentSession() {
        _uiState.update {
            if (it.accidentSession != null) it
            else it.copy(
                accidentSession = AccidentSession(
                    startedAtMillis = System.currentTimeMillis(),
                    locationRequested = it.consent.location,
                    addressRequested = it.consent.location && it.consent.network
                )
            )
        }
        // 위치는 SC-205에서 허용한 경우에만, 사고 모드 시작 때 한 번만 읽는다(꺼져 있으면 FusedLocation을 부르지 않는다).
        val session = _uiState.value.accidentSession
        if (session != null && session.locationRequested && session.locationStatus == null && locationJob?.isActive != true) {
            readLocationOnce(session.startedAtMillis)
        }
        // SC-200 [네, 세웠어요]에서 차량 상태를 한 번 읽는다. 기다리지 않고 SC-210으로 가며, 실패해도 진행한다.
        val state = _uiState.value
        if (state.accidentSession?.vehicle == null && !state.isReadingVehicle) readVehicleState()
    }

    /**
     * 5개 기능을 한 번 읽어 세션에 스냅샷으로 보관한다(get 호출만, 리스너 없음).
     * SC-200 [네, 세웠어요]와 SC-250 [다시 읽기]가 부른다. 읽는 동안 세션이 없어지면 결과를 버린다.
     */
    fun readVehicleState() {
        val startedAt = _uiState.value.accidentSession?.startedAtMillis ?: return
        readJob?.cancel()
        _uiState.update { it.copy(isReadingVehicle = true) }
        readJob = viewModelScope.launch {
            try {
                // 시작 직후라면 지원 확인(SC-000)이 끝나기를 잠시 기다린다.
                val ready = withTimeoutOrNull(5_000L) { _uiState.first { it.startup == StartupPhase.DONE } }
                    ?: _uiState.value
                val snapshot = VehicleReader(vehicleClient).readAll(ready.featureSupport, ready.capabilityAreas)
                Log.i(TAG, "SC-250 read: " + describe(snapshot))
                _uiState.update { state ->
                    val session = state.accidentSession
                    if (session == null || session.startedAtMillis != startedAt) {
                        state.copy(isReadingVehicle = false)
                    } else {
                        state.copy(accidentSession = session.withVehicle(snapshot), isReadingVehicle = false)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "SC-250 read failed: ${e.javaClass.simpleName}")
                _uiState.update { it.copy(isReadingVehicle = false) }
            }
        }
    }

    /**
     * 사고 모드 시작 때 위치를 1회 읽고, 네트워크도 허용했으면 주소를 1회 조회한다(재시도 없음).
     * 좌표는 이 함수 안에서 주소 조회에만 쓰고 세션, 로그, 화면에 남기지 않는다. 세션에는 상태와 주소 문자열만 둔다.
     */
    private fun readLocationOnce(startedAt: Long) {
        locationJob = viewModelScope.launch {
            try {
                val result = LocationReader(getApplication()).readOnce()
                // 로그에는 결과 종류만 남긴다(좌표, 오류 메시지 제외).
                Log.i(TAG, "SC-200 location: ${result.status.name}")
                updateSession(startedAt) { it.withLocationStatus(result.status) }
                val point = result.location
                val wantAddress = _uiState.value.accidentSession?.addressRequested == true
                if (result.status == LocationStatus.OK && point != null && wantAddress) {
                    val address = AddressResolver().resolve(point.latitude, point.longitude)
                    Log.i(TAG, "SC-200 address: ${address.status.name}")
                    updateSession(startedAt) { it.withAddress(address) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "SC-200 location failed: ${e.javaClass.simpleName}")
                updateSession(startedAt) { session ->
                    if (session.locationStatus == null) session.withLocationStatus(LocationStatus.ERROR)
                    else session.withAddress(AddressResult(AddressStatus.ERROR))
                }
            }
        }
    }

    /** 시작 시각이 같은 세션일 때만 바꾼다(세션이 끝났거나 새로 시작됐으면 결과를 버린다). */
    private fun updateSession(startedAt: Long, change: (AccidentSession) -> AccidentSession) {
        _uiState.update { state ->
            val session = state.accidentSession
            if (session == null || session.startedAtMillis != startedAt) state
            else state.copy(accidentSession = change(session))
        }
    }

    private fun describe(s: VehicleSnapshot): String =
        "doors=${s.doorsState.name}${s.doors.map { "${it.key.name}=${it.value}" }} " +
            "windows=${s.windowsState.name}${s.windows.map { "${it.key.name}=${it.value}" }} " +
            "battery=${s.battery.state.name}:${s.battery.value} odometer=${s.odometer.state.name}:${s.odometer.value} " +
            "turnSignal=${s.turnSignal.state.name}:${s.turnSignal.value}"

    fun setChecked(id: String, checked: Boolean) {
        _uiState.update { state ->
            val session = state.accidentSession ?: return@update state
            state.copy(accidentSession = session.withChecked(id, checked))
        }
    }

    fun setInjuryAnswer(answer: InjuryAnswer) {
        _uiState.update { state ->
            val session = state.accidentSession ?: return@update state
            state.copy(accidentSession = session.withInjury(answer))
        }
    }

    /** 사고 모드를 나가거나 끝냈을 때(홈 도착) 세션 데이터를 버린다. */
    fun endAccidentSession() {
        readJob?.cancel()
        locationJob?.cancel()
        _uiState.update {
            if (it.accidentSession == null && !it.isReadingVehicle) it
            else it.copy(accidentSession = null, isReadingVehicle = false)
        }
    }

    private fun ensureDomainFeatureCounts() {
        if (_uiState.value.domainFeatureCounts.isNotEmpty()) return
        try {
            val counts = VehicleDomain.entries.associateWith { domain ->
                apiFeatures(domain, apiExplorer.methods(domain)).size
            }
            _uiState.update { it.copy(domainFeatureCounts = counts) }
        } catch (e: Exception) {
            Log.w(TAG, "domain feature counts unavailable: ${e.message}")
        }
    }

    /** 디버그 전용 개발자 도구 페이지로 이동한다(릴리스에서는 SampleApp이 막는다). */
    fun showDevPage(page: AppPage) {
        _uiState.update { it.copy(page = page, errorMessage = null, featureErrorMessage = null) }
    }

    fun showHome() {
        _uiState.update {
            it.copy(
                page = AppPage.HOME,
                featureSearchQuery = "",
                errorMessage = null,
                featureErrorMessage = null
            )
        }
    }

    fun showApiExplorer(domain: VehicleDomain) {
        try {
            openApiExplorer(domain)
        } catch (e: Exception) {
            publishError(e)
        }
    }

    private fun openApiExplorer(domain: VehicleDomain) {
        ensureDomainFeatureCounts()
        if (_uiState.value.isSubscribed) {
            vehicleClient.stopDoorPositionUpdates()
        }
        apiExplorer.release()
        pendingApiValues.clear()
        activeTypedMethodId = null
        featureRequestFlow.clear()
        val methods = apiExplorer.methods(domain)
        val features = apiFeatures(domain, methods)
        _uiState.update {
            it.copy(
                page = AppPage.DEV_TOOLS,
                selectedDomain = domain,
                apiExplorerPane = ApiExplorerPane.FEATURE_LIST,
                apiMethods = methods,
                apiFeatures = features,
                featureSearchQuery = "",
                selectedFeatureId = null,
                focusedMethodId = null,
                apiFocusSequence = 0,
                explorerEvents = emptyList(),
                apiMethodStatuses = emptyMap(),
                apiMethodResponses = emptyMap(),
                featureCurrentValues = emptyMap(),
                featureCurrentDetails = emptyMap(),
                featureValueRanges = emptyMap(),
                isSubscribed = false,
                subscribedFeatureId = null,
                capabilityAccess = null,
                currentPosition = null,
                errorMessage = null,
                featureErrorMessage = null
            )
        }
    }

    fun selectFeature(featureId: String) {
        val feature = _uiState.value.apiFeatures.firstOrNull { it.id == featureId } ?: return
        featureRequestFlow.beginEntry(featureId)
        _uiState.update {
            it.copy(
                page = AppPage.DEV_TOOLS,
                apiExplorerPane = ApiExplorerPane.FEATURE_DASHBOARD,
                selectedFeatureId = featureId,
                focusedMethodId = null,
                capabilityAccess = null,
                errorMessage = null,
                featureErrorMessage = null
            )
        }
        if (!runFeatureCapability(feature)) {
            featureRequestFlow.shouldRunAutomaticRead(
                featureId = featureId,
                operation = ApiOperation.CAPABILITY,
                succeeded = false
            )
        }
    }

    fun searchFeatures(query: String) {
        _uiState.update { it.copy(featureSearchQuery = query) }
    }

    fun backToFeatureList() {
        featureRequestFlow.clear()
        _uiState.update {
            it.copy(
                page = AppPage.DEV_TOOLS,
                apiExplorerPane = ApiExplorerPane.FEATURE_LIST,
                selectedFeatureId = null,
                errorMessage = null,
                featureErrorMessage = null
            )
        }
    }

    fun runFeatureCapability() {
        _uiState.value.apiFeatures
            .firstOrNull { it.id == _uiState.value.selectedFeatureId }
            ?.let(::runFeatureCapability)
    }

    fun runFeatureRead(selectedValues: Map<String, String>) {
        val feature = selectedFeature() ?: return
        val method = feature.read
        if (method == null) {
            publishFeatureError("No read API is available for ${feature.title}.")
            return
        }
        invokeApi(method.id, requestValues(method, selectedValues))
    }

    fun startFeatureUpdates(values: Map<String, String>) {
        val feature = selectedFeature() ?: return
        val method = feature.register
        if (method == null) {
            publishFeatureError("No register API is available for ${feature.title}.")
            return
        }
        invokeApi(method.id, requestValues(method, values))
    }

    fun stopFeatureUpdates(values: Map<String, String>) {
        val feature = selectedFeature() ?: return
        val method = feature.unregister
        if (method == null) {
            publishFeatureError("No unregister API is available for ${feature.title}.")
            return
        }
        invokeApi(method.id, requestValues(method, values))
    }

    private fun selectedFeature(): ApiFeatureDescriptor? =
        _uiState.value.apiFeatures.firstOrNull {
            it.id == _uiState.value.selectedFeatureId
        }

    private fun runFeatureCapability(feature: ApiFeatureDescriptor): Boolean {
        _uiState.update { it.withCapabilityRanges(feature.id, emptyMap()) }
        val method = feature.capability
        if (method == null) {
            _uiState.update {
                it.copy(featureCurrentValues = it.featureCurrentValues +
                    (feature.id to "No capability API declared"))
            }
            return false
        }
        return invokeApi(method.id, defaultValues(method))
    }

    private fun defaultValues(method: ApiMethodDescriptor): Map<String, String> =
        requestValues(method, emptyMap())

    private fun invokeApi(methodId: String, values: Map<String, String>): Boolean {
        val domain = _uiState.value.selectedDomain ?: return false
        val method = _uiState.value.apiMethods.firstOrNull { it.id == methodId } ?: return false
        return try {
            if (method.executionMode == ApiExecutionMode.TYPED_REFERENCE) {
                pendingApiValues.remove(methodId)
                invokeTypedDoorApi(method.name, methodId, values)
            } else {
                pendingApiValues[methodId] = values
                focusApiMethod(methodId)
                markApiMethod(methodId, ApiMethodStatus.RUNNING)
                apiExplorer.invoke(domain, methodId, values)
                true
            }
        } catch (exception: Exception) {
            pendingApiValues.remove(methodId)
            if (method.executionMode == ApiExecutionMode.TYPED_REFERENCE) {
                onError(exception)
            } else {
                onExplorerEvent(
                    message = "${method.name} failed: ${exception.message ?: exception.javaClass.simpleName}",
                    isError = true,
                    methodId = methodId,
                    isResult = true,
                    numericRanges = null,
                    isValueUpdate = false
                )
            }
            false
        }
    }

    private fun focusApiMethod(methodId: String) {
        _uiState.update {
            it.copy(
                focusedMethodId = methodId,
                apiFocusSequence = it.apiFocusSequence + 1
            )
        }
    }

    fun dismissError() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                featureErrorMessage = null
            )
        }
    }

    override fun onCapabilities(
        accessType: String,
        ranges: Map<String, ApiNumericRange>
    ) {
        val methodId = activeTypedMethodId
        val feature = methodId?.let(::featureForMethod)
        val supported = accessType != "NONE"
        markActiveTypedMethod(if (supported) ApiMethodStatus.SUPPORTED else ApiMethodStatus.UNSUPPORTED)
        updateApiMethodResponse(methodId, "Capability: $accessType, ranges $ranges")
        _uiState.update { current ->
            val updated = feature?.let {
                current.withCapabilityRanges(it.id, if (supported) ranges else emptyMap())
            } ?: current
            updated.copy(
                capabilityAccess = accessType,
                errorMessage = null,
                featureErrorMessage = null
            )
        }
        feature?.let { completeFeatureOperation(it, ApiOperation.CAPABILITY, succeeded = supported) }
    }

    override fun onDoorPositionRead(position: Int?) {
        val methodId = activeTypedMethodId
        markActiveTypedMethod(ApiMethodStatus.RESULT)
        updateApiMethodResponse(methodId, "Door position read: ${position ?: "unavailable"}")
        updateFeatureValue(
            methodName = "getCurrentDoorPosition",
            value = position?.toString() ?: "null"
        )
        _uiState.update {
            it.copy(
                currentPosition = position,
                errorMessage = null,
                featureErrorMessage = null
            )
        }
    }

    override fun onDoorPositionChanged(position: Int?) {
        val methodId = _uiState.value.apiMethods
            .firstOrNull { it.name == "registerDoorPosition" }
            ?.id
        methodId?.let { markApiMethod(it, ApiMethodStatus.RESULT) }
        updateApiMethodResponse(methodId, "Door update: ${position ?: "unavailable"}")
        updateFeatureValue(
            methodName = "registerDoorPosition",
            value = position?.toString() ?: "null"
        )
        _uiState.update {
            it.copy(
                currentPosition = position,
                errorMessage = null,
                featureErrorMessage = null
            )
        }
    }

    override fun onExplorerEvent(
        message: String,
        isError: Boolean,
        methodId: String?,
        isResult: Boolean,
        numericRanges: Map<String, ApiNumericRange>?,
        isValueUpdate: Boolean
    ) {
        val method = methodId?.let { id ->
            _uiState.value.apiMethods.firstOrNull { it.id == id }
        }
        val feature = methodId?.let(::featureForMethod)
        val methodOperation = methodId?.let { id ->
            _uiState.value.apiMethods.firstOrNull { it.id == id }?.operation
        }
        val requestValues = if (isResult && methodId != null) {
            pendingApiValues.remove(methodId).orEmpty()
        } else {
            methodId?.let { pendingApiValues[it] }.orEmpty()
        }
        val displayValue = if (feature != null && method != null) {
            formatFeatureDisplay(
                featureTitle = feature.title,
                message = message,
                requestValues = requestValues
            )
        } else {
            null
        }
        val displayMessage = if (feature != null && method != null) {
            formatApiResponse(
                featureTitle = feature.title,
                methodName = method.name,
                message = message,
                requestValues = requestValues
            )
        } else {
            message
        }
        methodId?.let { id ->
            val status = when {
                !isResult -> ApiMethodStatus.RUNNING
                isError && method?.operation == ApiOperation.CAPABILITY -> ApiMethodStatus.UNSUPPORTED
                isError -> ApiMethodStatus.FAILED
                method?.operation == ApiOperation.CAPABILITY -> ApiMethodStatus.SUPPORTED
                else -> ApiMethodStatus.RESULT
            }
            markApiMethod(id, status)
            if (isResult) {
                updateApiMethodResponse(id, displayMessage)
            }
            if (isResult && feature != null && method?.operation == ApiOperation.CAPABILITY) {
                _uiState.update {
                    it.withCapabilityRanges(feature.id, if (isError) emptyMap() else numericRanges.orEmpty())
                }
            }
            if (isResult && !isError && feature != null && method != null &&
                method.operation != ApiOperation.CAPABILITY
            ) {
                _uiState.update { current ->
                    val updated = if (isValueUpdate) {
                        current.withFeatureResult(feature.id, method.operation, displayValue!!.value, displayValue.detail)
                    } else {
                        current
                    }
                    updated.copy(
                        isSubscribed = when (method.operation) {
                            ApiOperation.REGISTER -> true
                            ApiOperation.UNREGISTER -> false
                            else -> current.isSubscribed
                        },
                        subscribedFeatureId = when (method.operation) {
                            ApiOperation.REGISTER -> feature.id
                            ApiOperation.UNREGISTER -> null
                            else -> current.subscribedFeatureId
                        }
                    )
                }
            }
        }
        _uiState.update {
            it.copy(
                explorerEvents = it.explorerEvents.prepend(event(displayMessage)),
                errorMessage = null,
                featureErrorMessage = when {
                    !isError || methodOperation == ApiOperation.CAPABILITY -> null
                    else -> message
                }
            )
        }
        if (isResult && feature != null && method != null) {
            completeFeatureOperation(feature, method.operation, succeeded = !isError)
        }
    }

    override fun onError(exception: Exception) {
        val methodId = activeTypedMethodId
        val method = methodId?.let { id -> _uiState.value.apiMethods.firstOrNull { it.id == id } }
        val feature = methodId?.let(::featureForMethod)
        markActiveTypedMethod(ApiMethodStatus.FAILED)
        updateApiMethodResponse(methodId, "Error: ${exception.message ?: exception.javaClass.simpleName}")
        publishError(exception)
        if (feature != null && method != null) {
            if (method.operation == ApiOperation.CAPABILITY) {
                _uiState.update { it.withCapabilityRanges(feature.id, emptyMap()) }
            }
            completeFeatureOperation(feature, method.operation, succeeded = false)
        }
    }

    override fun onDoorOperationError(operation: DoorOperation, exception: Exception) {
        val methodId = activeTypedMethodId
        val method = methodId?.let { id -> _uiState.value.apiMethods.firstOrNull { it.id == id } }
        val feature = methodId?.let(::featureForMethod)
        markActiveTypedMethod(
            if (operation == DoorOperation.CAPABILITY) {
                ApiMethodStatus.UNSUPPORTED
            } else {
                ApiMethodStatus.FAILED
            }
        )
        updateApiMethodResponse(
            methodId,
            "Error: ${exception.message ?: exception.javaClass.simpleName}"
        )
        publishError(exception)
        if (feature != null && method != null) {
            if (operation == DoorOperation.CAPABILITY) {
                _uiState.update { it.withCapabilityRanges(feature.id, emptyMap()) }
            }
            completeFeatureOperation(feature, method.operation, succeeded = false)
        }
    }

    private fun publishError(exception: Exception) {
        val message = exception.message ?: exception.javaClass.simpleName
        _uiState.update {
            it.copy(
                errorMessage = message,
                featureErrorMessage = null
            )
        }
    }

    private fun publishFeatureError(message: String) {
        _uiState.update {
            it.copy(
                errorMessage = null,
                featureErrorMessage = message,
                explorerEvents = it.explorerEvents.prepend(event(message))
            )
        }
    }

    override fun onCleared() {
        Log.i(TAG, "onCleared: releasing Vehicle SDK")
        runCatching { explorerInstance?.release() }
        runCatching { vehicleClient.release() }
        Log.i(TAG, "onCleared: Vehicle SDK release requested")
        super.onCleared()
    }

    private fun event(message: String): String =
        "${SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())}  $message"

    private fun invokeTypedDoorApi(
        methodName: String,
        methodId: String,
        values: Map<String, String>
    ): Boolean {
        if (!beginTypedMethod(methodId)) return false
        focusApiMethod(methodId)
        markApiMethod(methodId, ApiMethodStatus.RUNNING)
        when (methodName) {
            "checkDoorPositionCapabilities" -> vehicleClient.inspectDoorPositionCapabilities()
            "getCurrentDoorPosition" -> vehicleClient.readDoorPosition(values.getValue("area"))
            "registerDoorPosition" -> {
                if (vehicleClient.startDoorPositionUpdates(values.getValue("area"))) {
                    markActiveTypedMethod(ApiMethodStatus.RESULT)
                    updateApiMethodResponse(methodId, "Door updates registered")
                    _uiState.update {
                        it.copy(
                            isSubscribed = true,
                            subscribedFeatureId = featureIdForMethod(methodName)
                        )
                    }
                } else {
                    markActiveTypedMethod(ApiMethodStatus.FAILED)
                    updateApiMethodResponse(methodId, "Door updates could not be registered")
                    return false
                }
            }
            "unregisterDoorPosition" -> {
                if (vehicleClient.stopDoorPositionUpdates(values.getValue("area"))) {
                    markActiveTypedMethod(ApiMethodStatus.RESULT)
                    updateApiMethodResponse(methodId, "Door updates unregistered")
                    _uiState.update {
                        it.copy(isSubscribed = false, subscribedFeatureId = null)
                    }
                } else {
                    markActiveTypedMethod(ApiMethodStatus.FAILED)
                    updateApiMethodResponse(methodId, "Door updates could not be unregistered")
                    return false
                }
            }
            else -> error("No typed sample is registered for $methodName")
        }
        return true
    }

    private fun completeFeatureOperation(
        feature: ApiFeatureDescriptor,
        operation: ApiOperation,
        succeeded: Boolean
    ) {
        val read = feature.read
        if (featureRequestFlow.shouldRunAutomaticRead(feature.id, operation, succeeded) &&
            read != null
        ) {
            invokeApi(read.id, defaultValues(read))
        }
    }

    private fun beginTypedMethod(methodId: String?): Boolean {
        if (methodId == null) {
            publishError(IllegalStateException("Door API metadata is unavailable."))
            return false
        }
        if (activeTypedMethodId != null) {
            publishError(IllegalStateException("Wait for the current Door API call to finish."))
            return false
        }
        activeTypedMethodId = methodId
        return true
    }

    private fun markActiveTypedMethod(status: ApiMethodStatus) {
        activeTypedMethodId?.let { methodId ->
            markApiMethod(methodId, status)
            activeTypedMethodId = null
        }
    }

    private fun markApiMethod(methodId: String, status: ApiMethodStatus) {
        _uiState.update {
            it.copy(apiMethodStatuses = it.apiMethodStatuses + (methodId to status))
        }
    }

    private fun featureForMethod(methodId: String): ApiFeatureDescriptor? =
        _uiState.value.apiFeatures.firstOrNull { feature ->
            feature.methods.any { it.id == methodId }
        }

    private fun featureIdForMethod(methodName: String): String? =
        _uiState.value.apiMethods
            .firstOrNull { it.name == methodName }
            ?.let { featureForMethod(it.id)?.id }

    private fun updateFeatureValue(methodName: String, value: String) {
        val method = _uiState.value.apiMethods.firstOrNull { it.name == methodName } ?: return
        val feature = featureForMethod(method.id) ?: return
        _uiState.update { it.withFeatureResult(feature.id, method.operation, value, null) }
    }

    private fun updateApiMethodResponse(methodId: String?, message: String) {
        methodId ?: return
        _uiState.update {
            it.copy(apiMethodResponses = it.apiMethodResponses + (methodId to event(message)))
        }
    }

    private fun List<String>.prepend(value: String): List<String> =
        (listOf(value) + this).take(MAX_EVENT_COUNT)

    companion object {
        private const val MAX_EVENT_COUNT = 12
        private const val TAG = "PitCrewVehicle"
    }
}
