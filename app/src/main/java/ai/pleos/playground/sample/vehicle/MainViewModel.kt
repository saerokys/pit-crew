package ai.pleos.playground.sample.vehicle

import ai.pleos.playground.sample.vehicle.ui.data.MainUiState
import ai.pleos.playground.sample.vehicle.address.AddressResolver
import ai.pleos.playground.sample.vehicle.address.AddressResult
import ai.pleos.playground.sample.vehicle.address.AddressStatus
import ai.pleos.playground.sample.vehicle.location.LocationReader
import ai.pleos.playground.sample.vehicle.location.LocationStatus
import ai.pleos.playground.sample.vehicle.location.isLocationSupported
import ai.pleos.playground.sample.vehicle.insurer.InsurerItem
import ai.pleos.playground.sample.vehicle.navi.DemoPlaces
import ai.pleos.playground.sample.vehicle.navi.DestinationCategory
import ai.pleos.playground.sample.vehicle.navi.NaviCallback
import ai.pleos.playground.sample.vehicle.navi.NaviRouter
import ai.pleos.playground.sample.vehicle.navi.NaviState
import ai.pleos.playground.sample.vehicle.navi.RouteResult
import ai.pleos.playground.sample.vehicle.insurer.InsurerPackage
import ai.pleos.playground.sample.vehicle.insurer.InsurerSendState
import ai.pleos.playground.sample.vehicle.record.AccidentRecord
import ai.pleos.playground.sample.vehicle.record.RecordLoad
import ai.pleos.playground.sample.vehicle.record.RecordStore
import ai.pleos.playground.sample.vehicle.record.SaveState
import ai.pleos.playground.sample.vehicle.record.SavedRecordSummary
import ai.pleos.playground.sample.vehicle.record.SavedRecordUi
import ai.pleos.playground.sample.vehicle.session.AccidentSession
import ai.pleos.playground.sample.vehicle.settings.Consent
import ai.pleos.playground.sample.vehicle.settings.ConsentStore
import ai.pleos.playground.sample.vehicle.stt.VoiceMemoCallback
import ai.pleos.playground.sample.vehicle.stt.VoiceMemoRecognizer
import ai.pleos.playground.sample.vehicle.stt.VoiceState
import ai.pleos.playground.sample.vehicle.tts.SpeechPlayer
import ai.pleos.playground.sample.vehicle.weather.WeatherResolver
import ai.pleos.playground.sample.vehicle.weather.WeatherResult
import ai.pleos.playground.sample.vehicle.weather.WeatherStatus
import ai.pleos.playground.sample.vehicle.tts.SpeechState
import ai.pleos.playground.sample.vehicle.session.InjuryAnswer
import ai.pleos.playground.sample.vehicle.ui.data.StartupPhase
import ai.pleos.playground.sample.vehicle.vehicle.CapabilityProbe
import ai.pleos.playground.sample.vehicle.vehicle.CapabilityResult
import ai.pleos.playground.sample.vehicle.vehicle.mergeProbe
import ai.pleos.playground.sample.vehicle.vehicle.unknownFeatures
import ai.pleos.playground.sample.vehicle.vehicle.SupportState
import ai.pleos.playground.sample.vehicle.vehicle.VehicleClient
import ai.pleos.playground.sample.vehicle.vehicle.VehicleFeature
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private val vehicleClient = VehicleClient(application)
    private var readJob: Job? = null
    private var preCheckJob: Job? = null
    private var locationJob: Job? = null
    private var saveJob: Job? = null
    private var insurerJob: Job? = null
    // 기능 지원 확인(capability) 호출을 한 번에 하나만 하게 한다(시작 확인, 재확인, SC-400 [다시 확인]이 겹치지 않게).
    private val supportMutex = Mutex()
    // 시작 때 UNKNOWN인 기능을 다시 확인하는 일은 앱을 켜고 한 번만 한다.
    private var startupRetryStarted = false
    private val consentStore = ConsentStore(application)
    private val recordStore = RecordStore(application)
    // TextToSpeech는 음성 스위치를 켜고 [소리로 듣기]를 처음 누를 때 만들어진다(여기서는 SDK를 부르지 않는다).
    private val speechPlayer by lazy {
        SpeechPlayer(getApplication()) { state -> _uiState.update { it.copy(speechState = state) } }
    }
    // SpeechToText는 음성 스위치를 켜고 [말하기]를 처음 누를 때 만들어진다(여기서는 SDK 객체를 만들지 않는다).
    private val voiceRecognizer by lazy {
        VoiceMemoRecognizer(getApplication(), object : VoiceMemoCallback {
            override fun onState(state: VoiceState) {
                _uiState.update { it.copy(voiceState = state) }
            }

            override fun onFinalText(text: String, cut: Boolean) {
                // 인식 결과는 [저장]을 누를 때까지 화면 확인용으로만 들고 있다(내용은 로그에 남기지 않는다).
                _uiState.update { it.copy(voiceDraft = text, voiceDraftCut = cut, voiceNotHeard = false) }
            }

            override fun onNoResult() {
                _uiState.update { it.copy(voiceDraft = null, voiceDraftCut = false, voiceNotHeard = true) }
            }
        })
    }
    // NaviHelper는 SC-280에 들어올 때만 연결한다(여기서는 SDK 객체를 만들지 않는다).
    private val naviRouter by lazy {
        NaviRouter(getApplication(), object : NaviCallback {
            override fun onNaviState(state: NaviState) {
                _uiState.update { it.copy(naviState = state) }
            }

            override fun onRouteResult(result: RouteResult) {
                _uiState.update { it.copy(routeResult = result) }
            }
        })
    }

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

    /**
     * SC-500 스위치: 바꾼 값을 바로 저장한다. 처음 정하는 경우에도 "정했음"으로 보아 SC-205를 건너뛰고,
     * 건드리지 않은 스위치는 지금 값(처음이면 꺼짐) 그대로다. 음성을 끄면 쓰던 TTS·STT SDK를 해제한다.
     */
    fun updateConsent(location: Boolean, voice: Boolean, network: Boolean) {
        val before = _uiState.value.consent
        val consent = Consent(location = location, voice = voice, network = network, decided = true)
        consentStore.save(consent)
        _uiState.update { it.copy(consent = consent) }
        if (before.voice && !voice) {
            releaseSpeech(forgetUnavailable = true)
            releaseVoice(forgetUnavailable = true)
        }
        Log.i(TAG, "SC-500 consent saved: location=$location voice=$voice network=$network")
    }

    /**
     * [소리로 듣기]: 읽는 중이면 멈추고, 아니면 [text]를 한 번에 읽는다. 음성 스위치가 꺼져 있으면 SDK를 부르지 않는다.
     * [text]는 화면에 있는 문구를 이은 문장이며 로그에 남기지 않는다.
     */
    fun toggleSpeech(text: String) {
        if (!_uiState.value.consent.voice) return
        try {
            when (_uiState.value.speechState) {
                SpeechState.PREPARING, SpeechState.SPEAKING -> speechPlayer.stop()
                else -> {
                    // 듣는 중이면 먼저 멈춘다(읽는 소리를 마이크가 듣지 않게).
                    if (_uiState.value.voiceState == VoiceState.PREPARING || _uiState.value.voiceState == VoiceState.LISTENING) {
                        voiceRecognizer.release()
                    }
                    speechPlayer.speak(text)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "speech toggle failed: ${e.javaClass.simpleName}")
        }
    }

    /** 화면을 옮기거나 앱이 화면에서 사라질 때: 읽기를 멈추고 "소리 안내 불가"(재생 오류)를 푼다. */
    fun stopSpeech() {
        if (_uiState.value.speechState == SpeechState.IDLE) return
        try {
            speechPlayer.stop()
            speechPlayer.clearScreenError()
        } catch (e: Exception) {
            Log.w(TAG, "speech stop failed: ${e.javaClass.simpleName}")
        }
    }

    /** 사고 모드를 끝내고 홈에 도착했을 때, 동의 초기화 때, 뷰모델 정리 때 TTS를 해제한다. */
    fun releaseSpeech(forgetUnavailable: Boolean = false) {
        try {
            speechPlayer.release(forgetUnavailable)
        } catch (e: Exception) {
            Log.w(TAG, "speech release failed: ${e.javaClass.simpleName}")
        }
    }

    /**
     * SC-230 [말하기]/[다시 말하기]: 음성 스위치를 켠 경우에만 듣기를 시작한다(꺼져 있으면 SDK 객체를 만들지 않는다).
     * 소리 읽기(TTS)가 있으면 먼저 멈추고, 저장하지 않은 이전 인식 결과는 버린다.
     */
    fun startVoiceMemo() {
        val state = _uiState.value
        if (!state.consent.voice || state.accidentSession == null) return
        try {
            stopSpeech()
            _uiState.update { it.copy(voiceDraft = null, voiceDraftCut = false, voiceNotHeard = false) }
            voiceRecognizer.start()
        } catch (e: Exception) {
            Log.w(TAG, "voice start failed: ${e.javaClass.simpleName}")
        }
    }

    /** SC-230 듣는 중 [멈추기]: 그때까지 들은 결과가 있으면 결과 확인으로, 없으면 대기로 돌아간다. */
    fun stopVoiceMemo() {
        try {
            voiceRecognizer.stopListening()
        } catch (e: Exception) {
            Log.w(TAG, "voice stop failed: ${e.javaClass.simpleName}")
        }
    }

    /**
     * SC-230 결과 확인 [저장]: 인식 결과를 세션에 확정한다(1건, 최대 100자, 이미 있던 메모는 바뀐다).
     * 메모리에만 두고 디스크에 쓰지 않으며 사고 기록(SC-270)에도 넣지 않는다. 로그에는 글자 수만 남긴다.
     */
    fun confirmVoiceMemo() {
        val draft = _uiState.value.voiceDraft ?: return
        _uiState.update { state ->
            val session = state.accidentSession
            if (session == null) state.copy(voiceDraft = null)
            else state.copy(accidentSession = session.withVoiceMemo(draft), voiceDraft = null, voiceDraftCut = false, voiceNotHeard = false)
        }
        Log.i(TAG, "voice memo saved len=${draft.length}")
    }

    /**
     * 화면 이동, 앱이 가려질 때, 홈 도착, 동의 초기화, 뷰모델 정리에서: 듣기를 멈추고 SDK를 해제(stop -> removeListener -> release)하고
     * 저장하지 않은 인식 결과를 버린다. [forgetUnavailable]이 참이면 "사용할 수 없음"(초기화 실패)도 잊는다.
     */
    fun releaseVoice(forgetUnavailable: Boolean = false) {
        try {
            voiceRecognizer.release(forgetUnavailable)
        } catch (e: Exception) {
            Log.w(TAG, "voice release failed: ${e.javaClass.simpleName}")
        }
        _uiState.update {
            if (it.voiceDraft == null && !it.voiceDraftCut && !it.voiceNotHeard) it
            else it.copy(voiceDraft = null, voiceDraftCut = false, voiceNotHeard = false)
        }
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
                    linkCheckedAtMillis = System.currentTimeMillis()
                )
            }
            return
        }
        _uiState.update { it.copy(isInitialized = true) }
        viewModelScope.launch {
            val result = supportMutex.withLock { CapabilityProbe(vehicleClient).probe() }
            logProbe("SC-000", result)
            _uiState.update {
                it.copy(
                    featureSupport = result.states,
                    capabilityAreas = result.areas,
                    linkCheckedAtMillis = System.currentTimeMillis(),
                    startup = StartupPhase.DONE
                )
            }
            // 시작 확인에서 시간 안에 응답하지 못한 기능(UNKNOWN)만 백그라운드에서 한 번 더 확인한다(화면은 이미 홈으로 간다).
            if (!startupRetryStarted) {
                startupRetryStarted = true
                retryUnknownSupport("SC-000 retry")
            }
        }
    }

    /**
     * 아직 UNKNOWN인 기능만 지원 여부를 다시 확인해 `featureSupport`와 `capabilityAreas`에 합친다(읽기 전용, 기능별 3초 제한).
     * [supportMutex] 안에서 상태를 다시 보므로, 앞선 확인이 이미 해결했거나 UNKNOWN이 없으면 호출하지 않는다(중복 호출 방지).
     * 초기화에 실패했으면 하지 않는다. 실패하거나 예외가 나도 아무것도 바꾸지 않고(그대로 "확인 불가") 부른 쪽은 계속 진행한다.
     */
    private suspend fun retryUnknownSupport(tag: String) {
        try {
            supportMutex.withLock {
                val state = _uiState.value
                if (!state.isInitialized) return
                val targets = unknownFeatures(state.featureSupport)
                if (targets.isEmpty()) return
                val retried = CapabilityProbe(vehicleClient).probe(targets)
                logProbe(tag, retried)
                _uiState.update { current ->
                    val merged = mergeProbe(CapabilityResult(current.featureSupport, current.capabilityAreas), retried)
                    current.copy(featureSupport = merged.states, capabilityAreas = merged.areas)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "$tag support retry failed: ${e.javaClass.simpleName}")
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
                    val result = supportMutex.withLock { CapabilityProbe(vehicleClient).probe() }
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
                withTimeoutOrNull(5_000L) { _uiState.first { it.startup == StartupPhase.DONE } }
                // 읽기 전에 아직 UNKNOWN인 기능만 다시 확인한다(실패해도 읽기는 진행).
                retryUnknownSupport("SC-300")
                val ready = _uiState.value
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
                    addressRequested = it.consent.location && it.consent.network,
                    weatherRequested = it.consent.location && it.consent.network
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
                withTimeoutOrNull(5_000L) { _uiState.first { it.startup == StartupPhase.DONE } }
                // 읽기 전에 아직 UNKNOWN인 기능만 다시 확인한다(실패해도 읽기는 진행). SC-200 [네, 세웠어요]와 SC-250 [다시 읽기]가 함께 쓴다.
                retryUnknownSupport("SC-200")
                val ready = _uiState.value
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
     * 사고 모드 시작 때 위치를 1회 읽고, 네트워크도 허용했으면 주소와 날씨를 각각 1회 조회한다(동시에, 재시도 없음).
     * 좌표는 이 함수 안에서 주소·날씨 조회에만 쓰고 세션, 로그, 화면에 남기지 않는다. 세션에는 상태와 주소 문자열, 날씨 한 줄(조건, 온도)만 둔다.
     */
    private fun readLocationOnce(startedAt: Long) {
        locationJob = viewModelScope.launch {
            try {
                val result = LocationReader(getApplication()).readOnce()
                // 로그에는 결과 종류만 남긴다(좌표, 오류 메시지 제외).
                Log.i(TAG, "SC-200 location: ${result.status.name}")
                updateSession(startedAt) { it.withLocationStatus(result.status) }
                val point = result.location
                val current = _uiState.value.accidentSession
                val wantAddress = current?.addressRequested == true
                val wantWeather = current?.weatherRequested == true
                if (result.status == LocationStatus.OK && point != null && (wantAddress || wantWeather)) {
                    val latitude = point.latitude
                    val longitude = point.longitude
                    coroutineScope {
                        if (wantAddress) launch { resolveAddress(startedAt, latitude, longitude) }
                        if (wantWeather) launch { resolveWeather(startedAt, latitude, longitude) }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "SC-200 location failed: ${e.javaClass.simpleName}")
                updateSession(startedAt) { session ->
                    if (session.locationStatus == null) session.withLocationStatus(LocationStatus.ERROR)
                    else session.withAddress(AddressResult(AddressStatus.ERROR)).withWeather(WeatherResult(WeatherStatus.ERROR))
                }
            }
        }
    }

    /** 주소 1회 조회. 예외는 여기서 잡고 실패하면 ERROR로 둔다(좌표는 이 호출에만 쓴다). */
    private suspend fun resolveAddress(startedAt: Long, latitude: Double, longitude: Double) {
        val address = try {
            AddressResolver().resolve(latitude, longitude)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AddressResult(AddressStatus.ERROR)
        }
        Log.i(TAG, "SC-200 address: ${address.status.name}")
        updateSession(startedAt) { it.withAddress(address) }
    }

    /** 날씨 1회 조회(Open-Meteo, 좌표 소수점 2자리). 예외는 여기서 잡고 실패하면 ERROR로 둔다. 로그에는 결과 종류만 남긴다. */
    private suspend fun resolveWeather(startedAt: Long, latitude: Double, longitude: Double) {
        val weather = try {
            WeatherResolver().resolve(latitude, longitude)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            WeatherResult(WeatherStatus.ERROR)
        }
        Log.i(TAG, "SC-200 weather: ${weather.status.name}")
        updateSession(startedAt) { it.withWeather(weather) }
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
        saveJob?.cancel()
        insurerJob?.cancel()
        _uiState.update {
            if (it.accidentSession == null && !it.isReadingVehicle && it.saveState == SaveState.IDLE &&
                it.insurerSendState == InsurerSendState.IDLE && it.insurerUnchecked.isEmpty() && it.insurerLastSent.isEmpty() &&
                it.destCategory == DestinationCategory.HOSPITAL && it.destPlaceId == DemoPlaces.forCategory(DestinationCategory.HOSPITAL).first().id
            ) it
            else it.copy(
                accidentSession = null,
                isReadingVehicle = false,
                saveState = SaveState.IDLE,
                insurerUnchecked = emptySet(),
                insurerSendState = InsurerSendState.IDLE,
                insurerLastSent = emptySet(),
                destCategory = DestinationCategory.HOSPITAL,
                destPlaceId = DemoPlaces.forCategory(DestinationCategory.HOSPITAL).first().id
            )
        }
    }

    /** SC-280에 들어올 때: NaviHelper에 연결한다(길안내 요청은 [길안내 시작]을 눌러야만 한다). 실패해도 앱은 계속된다. */
    fun connectNavi() {
        try {
            naviRouter.connect()
        } catch (e: Exception) {
            Log.w(TAG, "navi connect failed: ${e.javaClass.simpleName}")
        }
    }

    /** SC-280을 떠날 때, 홈 도착, 동의 초기화, 뷰모델 정리에서: removeListener -> release. 처음 상태로 돌린다. */
    fun releaseNavi() {
        try {
            naviRouter.release()
        } catch (e: Exception) {
            Log.w(TAG, "navi release failed: ${e.javaClass.simpleName}")
        }
    }

    /** SC-280 목적지 유형 카드를 고르면 그 유형의 첫 후보가 고른 상태가 된다. 이전 요청 결과는 지운다. */
    fun selectDestinationCategory(category: DestinationCategory) {
        _uiState.update {
            if (it.destCategory == category) it
            else it.copy(destCategory = category, destPlaceId = DemoPlaces.forCategory(category).firstOrNull()?.id)
        }
    }

    fun selectDestinationPlace(placeId: String) {
        _uiState.update {
            if (DemoPlaces.find(it.destCategory, placeId) == null) it else it.copy(destPlaceId = placeId)
        }
    }

    /** SC-280 [길안내 시작]/[다시 시도]: 고른 후보 하나로 길안내를 요청한다. 연결되지 않았으면 아무것도 하지 않는다. */
    fun startNavigation() {
        val state = _uiState.value
        val place = DemoPlaces.find(state.destCategory, state.destPlaceId) ?: return
        try {
            naviRouter.requestRoute(place)
        } catch (e: Exception) {
            Log.w(TAG, "navi request failed: ${e.javaClass.simpleName}")
            _uiState.update { it.copy(routeResult = RouteResult.ERROR) }
        }
    }

    /** SC-290 항목 체크 켜고 끄기. 고를 수 없는 항목(값이 없음)은 바꾸지 않는다. */
    fun toggleInsurerItem(item: InsurerItem) {
        _uiState.update { state ->
            val session = state.accidentSession ?: return@update state
            if (item !in InsurerPackage.available(session)) state
            else state.copy(
                insurerUnchecked = if (item in state.insurerUnchecked) state.insurerUnchecked - item else state.insurerUnchecked + item
            )
        }
    }

    /**
     * SC-290 [보내기]: 사용자가 누를 때만, 앱 안 모의 처리로 접수 한 건을 만든다(외부 호출 없음, 모의 지연 0.8초).
     * 고른 항목이 없거나, 같은 항목을 이미 보냈거나, 보내는 중이면 아무것도 하지 않는다.
     * 로그에는 결과 종류와 항목 개수만 남긴다(값 제외).
     */
    fun sendToInsurer() {
        val state = _uiState.value
        val session = state.accidentSession ?: return
        if (state.insurerSendState == InsurerSendState.SENDING) return
        val selected = InsurerPackage.selected(session, state.insurerUnchecked)
        if (selected.isEmpty()) return
        if (state.insurerSendState == InsurerSendState.SENT && selected == state.insurerLastSent) return
        _uiState.update { it.copy(insurerSendState = InsurerSendState.SENDING) }
        val startedAt = session.startedAtMillis
        insurerJob = viewModelScope.launch {
            try {
                delay(800)
                val receipt = InsurerPackage.receipt(session, selected, System.currentTimeMillis(), kotlin.random.Random.nextInt(10_000))
                updateSession(startedAt) { it.withInsurerReceipt(receipt) }
                _uiState.update { it.copy(insurerSendState = InsurerSendState.SENT, insurerLastSent = selected) }
                Log.i(TAG, "SC-290 send: OK items=${selected.size}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "SC-290 send: FAILED ${e.javaClass.simpleName}")
                _uiState.update { it.copy(insurerSendState = InsurerSendState.FAILED) }
            }
        }
    }

    /**
     * SC-270 [기록 저장]: 사용자가 누를 때만 현재 세션에서 기록 패키지(사고 시각, 위치·주소, 차량 상태, 안내 이행)를 만들어
     * 앱 내부 저장소에 한 건 저장한다. 실패해도 앱은 계속되고 [SaveState.FAILED]로 알린다. 기록 내용은 로그에 남기지 않는다.
     */
    fun saveRecord() {
        val state = _uiState.value
        val session = state.accidentSession ?: return
        if (state.saveState == SaveState.SAVING || state.saveState == SaveState.SAVED) return
        _uiState.update { it.copy(saveState = SaveState.SAVING) }
        saveJob = viewModelScope.launch {
            val saved = try {
                withContext(Dispatchers.IO) { recordStore.save(AccidentRecord.from(session)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            Log.i(TAG, "SC-270 save: ${if (saved) "OK" else "FAILED"}")
            _uiState.update {
                it.copy(
                    saveState = if (saved) SaveState.SAVED else SaveState.FAILED,
                    hasSavedRecord = it.hasSavedRecord || saved
                )
            }
        }
    }

    /** SC-270을 떠날 때(이전, 홈): 저장 진행 상태를 처음으로 돌린다. 이미 저장된 파일은 그대로 둔다. */
    fun resetSaveState() {
        if (_uiState.value.saveState == SaveState.IDLE) return
        saveJob?.cancel()
        _uiState.update { it.copy(saveState = SaveState.IDLE) }
    }

    /** 저장된 기록이 있는지 다시 확인한다(SC-270에 들어올 때). */
    fun refreshSavedRecord() {
        viewModelScope.launch {
            val exists = withContext(Dispatchers.IO) { recordStore.exists() }
            _uiState.update { it.copy(hasSavedRecord = exists) }
        }
    }

    /** SC-520에 들어올 때 저장된 기록을 읽어 요약을 만든다(읽지 못하면 Unreadable). 기록 내용은 로그에 남기지 않는다. */
    fun loadSavedRecord() {
        viewModelScope.launch {
            _uiState.update { it.copy(savedRecord = SavedRecordUi.Loading, deleteFailed = false) }
            val loaded = withContext(Dispatchers.IO) { recordStore.load() }
            val ui = when (loaded) {
                RecordLoad.None -> SavedRecordUi.None
                RecordLoad.Unreadable -> SavedRecordUi.Unreadable
                is RecordLoad.Found -> SavedRecordSummary.from(loaded.record)?.let { SavedRecordUi.Found(it) }
                    ?: SavedRecordUi.Unreadable
            }
            Log.i(TAG, "SC-520 load: ${ui::class.simpleName}")
            _uiState.update { it.copy(savedRecord = ui, hasSavedRecord = ui != SavedRecordUi.None) }
        }
    }

    /** SC-520 [삭제](확인 대화상자 뒤): 저장된 사고 기록 파일을 지운다. */
    fun deleteSavedRecord() {
        viewModelScope.launch {
            val deleted = withContext(Dispatchers.IO) { recordStore.delete() }
            Log.i(TAG, "saved record delete: ${if (deleted) "OK" else "FAILED"}")
            _uiState.update {
                it.copy(
                    hasSavedRecord = if (deleted) false else recordStore.exists(),
                    savedRecord = if (deleted) SavedRecordUi.None else it.savedRecord,
                    deleteFailed = !deleted
                )
            }
        }
    }

    override fun onCleared() {
        Log.i(TAG, "onCleared: releasing Vehicle SDK")
        runCatching { speechPlayer.release() }
        runCatching { voiceRecognizer.release() }
        runCatching { naviRouter.release() }
        runCatching { vehicleClient.release() }
        Log.i(TAG, "onCleared: Vehicle SDK release requested")
        super.onCleared()
    }

    companion object {
        private const val MAX_EVENT_COUNT = 12
        private const val TAG = "PitCrewVehicle"
    }
}
