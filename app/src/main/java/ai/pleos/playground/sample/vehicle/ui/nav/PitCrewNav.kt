package ai.pleos.playground.sample.vehicle.ui.nav

import ai.pleos.playground.sample.vehicle.session.AccidentSession
import ai.pleos.playground.sample.vehicle.session.InjuryAnswer
import ai.pleos.playground.sample.vehicle.ui.accident.EmergencyScreen
import ai.pleos.playground.sample.vehicle.ui.accident.InjuryScreen
import ai.pleos.playground.sample.vehicle.ui.accident.OtherPartyScreen
import ai.pleos.playground.sample.vehicle.ui.accident.RecordScreen
import ai.pleos.playground.sample.vehicle.ui.accident.VoiceMemoActions
import ai.pleos.playground.sample.vehicle.ui.accident.VoiceMemoUi
import ai.pleos.playground.sample.vehicle.stt.VoiceState
import ai.pleos.playground.sample.vehicle.ui.accident.SafetyCheckScreen
import ai.pleos.playground.sample.vehicle.ui.accident.StopCheckScreen
import ai.pleos.playground.sample.vehicle.ui.components.DialogButton
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewBottomBar
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewDialog
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewIcons
import ai.pleos.playground.sample.vehicle.ui.components.SpeechButton
import ai.pleos.playground.sample.vehicle.tts.SpeechState
import ai.pleos.playground.sample.vehicle.tts.SpeechTexts
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewButton
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewTopBar
import ai.pleos.playground.sample.vehicle.settings.Consent
import ai.pleos.playground.sample.vehicle.insurer.InsurerItem
import ai.pleos.playground.sample.vehicle.navi.DestinationCategory
import ai.pleos.playground.sample.vehicle.navi.NaviState
import ai.pleos.playground.sample.vehicle.navi.RouteResult
import ai.pleos.playground.sample.vehicle.ui.destination.DestinationScreen
import ai.pleos.playground.sample.vehicle.ui.destination.DestinationTexts
import ai.pleos.playground.sample.vehicle.insurer.InsurerSendState
import ai.pleos.playground.sample.vehicle.ui.insurer.InsurerScreen
import ai.pleos.playground.sample.vehicle.ui.insurer.InsurerTexts
import ai.pleos.playground.sample.vehicle.record.SavedRecordUi
import ai.pleos.playground.sample.vehicle.ui.records.SavedRecordScreen
import ai.pleos.playground.sample.vehicle.ui.records.SavedRecordTexts
import ai.pleos.playground.sample.vehicle.ui.consent.ConsentChoice
import ai.pleos.playground.sample.vehicle.ui.consent.ConsentScreen
import ai.pleos.playground.sample.vehicle.ui.ending.EndingScreen
import ai.pleos.playground.sample.vehicle.record.SaveState
import ai.pleos.playground.sample.vehicle.ui.home.HomeScreen
import ai.pleos.playground.sample.vehicle.ui.precheck.PreDepartureScreen
import ai.pleos.playground.sample.vehicle.ui.status.LinkStatusScreen
import ai.pleos.playground.sample.vehicle.vehicle.SupportState
import ai.pleos.playground.sample.vehicle.vehicle.VehicleFeature
import ai.pleos.playground.sample.vehicle.vehicle.VehicleSnapshot
import ai.pleos.playground.sample.vehicle.ui.summary.SummaryScreen
import ai.pleos.playground.sample.vehicle.ui.vehicle.VehicleStateScreen
import ai.pleos.playground.sample.vehicle.ui.home.LinkSummary
import ai.pleos.playground.sample.vehicle.ui.home.StartupScreen
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 화면 이동 틀. 실제 화면은 Phase 3 이후에 route마다 채운다.
 * step은 SPEC 1.3의 진행률(SC-200 1/7 ... SC-260 7/7, SC-221은 3/7)이다.
 */
enum class PitCrewRoute(val screenId: String, val title: String, val step: Int?) {
    STARTUP("SC-000", "앱 시작", null),
    HOME("SC-100", "홈", null),
    CONSENT("SC-205", "권한·동의", null),
    STOP_CHECK("SC-200", "정차 확인", 1),
    SAFETY("SC-210", "안전 확보", 2),
    INJURY("SC-220", "부상 확인", 3),
    EMERGENCY("SC-221", "긴급 안내", 3),
    RECORD("SC-230", "현장 기록 안내", 4),
    OTHER_PARTY("SC-240", "상대방 정보 확인", 5),
    VEHICLE_STATE("SC-250", "차량 상태 기록", 6),
    SUMMARY("SC-260", "접수 요약", 7),
    INSURER("SC-290", "가상 보험사 전달", 7),
    DESTINATION("SC-280", "다음 장소 안내", 7),
    ENDING("SC-270", "종료와 기록 처리", 7),
    PRE_DEPARTURE("SC-300", "출발 전 점검", null),
    VEHICLE_LINK("SC-400", "차량 연동 상태", null),
    SETTINGS("SC-500", "설정·정보", null),
    SAVED_RECORD("SC-520", "저장 기록", null)
}

/** 틀 확인용 직선 순서. SC-221 분기(부상 있어요/모르겠어요)는 Phase 3에서 만든다. */
private val AccidentFlow = listOf(
    PitCrewRoute.STOP_CHECK,
    PitCrewRoute.SAFETY,
    PitCrewRoute.INJURY,
    PitCrewRoute.RECORD,
    PitCrewRoute.OTHER_PARTY,
    PitCrewRoute.VEHICLE_STATE,
    PitCrewRoute.SUMMARY,
    PitCrewRoute.ENDING
)

/** 경로 스택. push로 들어가고 pop으로 직전 화면에 돌아간다. */
class PitCrewNavigator(start: PitCrewRoute) {
    private val stack = mutableStateListOf(start)
    val current: PitCrewRoute get() = stack.last()
    val canPop: Boolean get() = stack.size > 1
    fun push(route: PitCrewRoute) { stack.add(route) }
    /** 맨 위 화면을 [route]로 바꾼다(SC-205에서 SC-200으로 갈 때 뒤로 가기에 SC-205가 남지 않게). */
    fun replaceTop(route: PitCrewRoute) { stack[stack.lastIndex] = route }
    fun pop(): Boolean = if (canPop) { stack.removeAt(stack.lastIndex); true } else false
    /** 시작 화면(SC-000)을 홈으로 바꾼다. 홈에서 뒤로 가도 SC-000으로 돌아오지 않는다. */
    fun replaceWithHome() { stack.clear(); stack.add(PitCrewRoute.HOME) }
    fun next(): PitCrewRoute? {
        val index = AccidentFlow.indexOf(current)
        return if (index < 0) null else AccidentFlow.getOrNull(index + 1)
    }
}

/**
 * 앱의 Pit Crew 화면 흐름: SC-000 -> SC-100 홈 -> (사고 모드 틀 | 자리표시 화면).
 * [topBarTrailing]은 상단 바 오른쪽 칸이다(디버그 빌드의 개발자 도구 버튼).
 * 홈에서 뒤로 가면 앱이 종료되는 시스템 기본 동작을 그대로 쓴다.
 */
@Composable
fun PitCrewNavHost(
    startupDone: Boolean,
    summary: LinkSummary,
    session: AccidentSession?,
    onStartSession: () -> Unit,
    onSetChecked: (String, Boolean) -> Unit,
    onSetInjury: (InjuryAnswer) -> Unit,
    onEndSession: () -> Unit,
    saveState: SaveState,
    hasSavedRecord: Boolean,
    onSaveRecord: () -> Unit,
    onResetSaveState: () -> Unit,
    onRefreshSavedRecord: () -> Unit,
    savedRecord: SavedRecordUi,
    deleteFailed: Boolean,
    onLoadSavedRecord: () -> Unit,
    onDeleteSavedRecord: () -> Unit,
    isReadingVehicle: Boolean,
    onReadVehicle: () -> Unit,
    preCheckSnapshot: VehicleSnapshot?,
    isPreChecking: Boolean,
    onCheckPreDeparture: () -> Unit,
    consent: Consent,
    locationSupported: Boolean,
    onConfirmConsent: (location: Boolean, voice: Boolean, network: Boolean) -> Unit,
    speechState: SpeechState,
    onToggleSpeech: (String) -> Unit,
    onStopSpeech: () -> Unit,
    onReleaseSpeech: () -> Unit,
    voiceState: VoiceState,
    voiceDraft: String?,
    voiceDraftCut: Boolean,
    destCategory: DestinationCategory,
    destPlaceId: String?,
    naviState: NaviState,
    routeResult: RouteResult,
    onSelectDestCategory: (DestinationCategory) -> Unit,
    onSelectDestPlace: (String) -> Unit,
    onStartNavigation: () -> Unit,
    onConnectNavi: () -> Unit,
    onReleaseNavi: () -> Unit,
    insurerUnchecked: Set<InsurerItem>,
    insurerSendState: InsurerSendState,
    insurerLastSent: Set<InsurerItem>,
    onToggleInsurerItem: (InsurerItem) -> Unit,
    onSendInsurer: () -> Unit,
    voiceNotHeard: Boolean,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onSaveVoice: () -> Unit,
    onReleaseVoice: () -> Unit,
    featureSupport: Map<VehicleFeature, SupportState>,
    linkCheckedAtMillis: Long?,
    isCheckingLink: Boolean,
    onRecheckLink: () -> Unit,
    modifier: Modifier = Modifier,
    topBarTrailing: (@Composable () -> Unit)? = null
) {
    val navigator = remember { PitCrewNavigator(if (startupDone) PitCrewRoute.HOME else PitCrewRoute.STARTUP) }
    LaunchedEffect(startupDone) {
        if (startupDone && navigator.current == PitCrewRoute.STARTUP) navigator.replaceWithHome()
    }
    val route = navigator.current
    var showExitDialog by remember { mutableStateOf(false) }
    var showNotYetDialog by remember { mutableStateOf(false) }
    var consentChoice by remember { mutableStateOf(ConsentChoice(false, false, false)) }

    // 화면이 바뀌면 읽기를 멈추고, 홈에 도착하면(사고 모드 끝) TTS를 해제한다.
    LaunchedEffect(route) {
        onStopSpeech()
        // 화면을 옮기면 듣기를 멈추고 SDK를 해제하며 저장하지 않은 인식 결과를 버린다(저장한 메모는 세션에 남는다).
        onReleaseVoice()
        // SC-280에 들어올 때만 NaviHelper에 연결하고, 떠나면(홈 도착 포함) 해제한다.
        if (route == PitCrewRoute.DESTINATION) onConnectNavi() else onReleaseNavi()
        if (route == PitCrewRoute.HOME) onReleaseSpeech()
    }

    // SC-270에 들어오면 저장된 기록이 있는지 확인하고(덮어쓰기 안내용), 떠나면 저장 진행 상태를 처음으로 돌린다.
    LaunchedEffect(route) {
        if (route == PitCrewRoute.ENDING) onRefreshSavedRecord() else onResetSaveState()
    }

    // SC-520에 들어올 때마다 저장된 기록을 새로 읽는다.
    LaunchedEffect(route) {
        if (route == PitCrewRoute.SAVED_RECORD) onLoadSavedRecord()
    }

    // 홈에 도착하면(나가기, 아직이에요, 마치기, 이전) 사고 모드 세션 데이터를 버린다.
    LaunchedEffect(route, session != null) {
        if (route == PitCrewRoute.HOME && session != null) onEndSession()
    }

    // [이전]과 시스템 뒤로 가기는 같다. 세션이 있는 채로 SC-200에서 나가면 확인 대화상자를 먼저 보여 준다.
    fun goBack() {
        when {
            route == PitCrewRoute.STOP_CHECK && session != null -> showExitDialog = true
            route == PitCrewRoute.INJURY -> {
                // SC-220에서 [이전]으로 나가면 부상 응답은 "응답 안 함"으로 둔다(SPEC).
                onSetInjury(InjuryAnswer.NOT_ANSWERED)
                navigator.pop()
            }
            route == PitCrewRoute.RECORD -> {
                // SC-230에서 [이전]은 SC-221을 거쳐 왔더라도 SC-220으로 간다(SPEC 1.3).
                navigator.pop()
                if (navigator.current == PitCrewRoute.EMERGENCY) navigator.pop()
            }
            else -> navigator.pop()
        }
    }
    // SC-000에서는 뒤로 가기를 무시한다. 홈에서는 시스템 기본(앱 종료).
    BackHandler(enabled = route == PitCrewRoute.STARTUP || navigator.canPop) {
        if (route != PitCrewRoute.STARTUP) goBack()
    }

    Column(modifier = modifier.fillMaxSize().background(PitCrewTheme.colors.background)) {
        val showSubtitle = route == PitCrewRoute.STARTUP || route == PitCrewRoute.HOME
        val step = route.step
        val speechText = if (route in SpeechTexts.speechRoutes) SpeechTexts.forRoute(route, session) else null
        val trailing: (@Composable () -> Unit)? = when {
            showSubtitle -> topBarTrailing // 개발자 도구 버튼(디버그 빌드, 홈에서만)
            speechText != null -> {
                // [소리로 듣기]는 SC-210, 220, 221, 230, 240에서만 보인다(그 밖의 화면에서는 숨긴다).
                { SpeechButton(state = speechState, voiceOn = consent.voice, onClick = { onToggleSpeech(speechText) }) }
            }
            else -> null
        }
        PitCrewTopBar(
            step = step,
            subtitle = if (showSubtitle) "사고 직후 정차부터 마무리까지 안내" else null,
            trailing = trailing
        )
        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            when (route) {
                PitCrewRoute.STARTUP -> StartupScreen()
                PitCrewRoute.HOME -> HomeScreen(
                    summary = summary,
                    // 사고 모드 첫 진입에서만 SC-205를 보여 주고, 이미 정했으면 바로 SC-200으로 간다.
                    onAccident = {
                        if (consent.decided) navigator.push(PitCrewRoute.STOP_CHECK) else {
                            consentChoice = ConsentChoice(false, false, false)
                            navigator.push(PitCrewRoute.CONSENT)
                        }
                    },
                    onPreDeparture = { navigator.push(PitCrewRoute.PRE_DEPARTURE) },
                    onVehicleLink = { navigator.push(PitCrewRoute.VEHICLE_LINK) },
                    onSettings = { navigator.push(PitCrewRoute.SETTINGS) }
                )
                PitCrewRoute.CONSENT -> ConsentScreen(
                    locationSupported = locationSupported,
                    onChoiceChange = { consentChoice = it }
                )
                PitCrewRoute.STOP_CHECK -> StopCheckScreen(
                    session = session,
                    consentLine = consent.summaryLine(),
                    onStopped = {
                        onStartSession()
                        navigator.push(PitCrewRoute.SAFETY)
                    },
                    onNotYet = { showNotYetDialog = true }
                )
                PitCrewRoute.SAFETY -> SafetyCheckScreen(
                    session = session,
                    onCheckedChange = onSetChecked,
                    onExitRequest = { showExitDialog = true }
                )
                PitCrewRoute.INJURY -> InjuryScreen(
                    session = session,
                    onAnswer = { answer ->
                        onSetInjury(answer)
                        // 없어요는 SC-230으로, 있어요/잘 모르겠어요는 SC-221을 거쳐 SC-230으로 간다.
                        navigator.push(if (answer == InjuryAnswer.NONE) PitCrewRoute.RECORD else PitCrewRoute.EMERGENCY)
                    },
                    onExitRequest = { showExitDialog = true }
                )
                PitCrewRoute.EMERGENCY -> EmergencyScreen(session = session)
                PitCrewRoute.RECORD -> RecordScreen(
                    session = session,
                    onCheckedChange = onSetChecked,
                    onExitRequest = { showExitDialog = true },
                    voiceUi = VoiceMemoUi(
                        voiceOn = consent.voice,
                        state = voiceState,
                        draft = voiceDraft,
                        saved = session?.voiceMemo,
                        cut = voiceDraftCut,
                        notHeard = voiceNotHeard
                    ),
                    voiceActions = VoiceMemoActions(
                        onSpeak = onStartVoice,
                        onStop = onStopVoice,
                        onSave = onSaveVoice,
                        onRetry = onStartVoice
                    )
                )
                PitCrewRoute.VEHICLE_STATE -> VehicleStateScreen(
                    session = session,
                    isReading = isReadingVehicle,
                    onReread = onReadVehicle
                )
                PitCrewRoute.SUMMARY -> SummaryScreen(session = session)
                PitCrewRoute.DESTINATION -> DestinationScreen(
                    category = destCategory,
                    placeId = destPlaceId,
                    naviState = naviState,
                    routeResult = routeResult,
                    onSelectCategory = onSelectDestCategory,
                    onSelectPlace = onSelectDestPlace,
                    onStart = onStartNavigation
                )
                PitCrewRoute.INSURER -> InsurerScreen(
                    session = session,
                    unchecked = insurerUnchecked,
                    sendState = insurerSendState,
                    lastSent = insurerLastSent,
                    onToggle = onToggleInsurerItem,
                    onSend = onSendInsurer
                )
                PitCrewRoute.ENDING -> EndingScreen(
                    session = session,
                    saveState = saveState,
                    hasSavedRecord = hasSavedRecord,
                    onSave = onSaveRecord,
                    onDiscardAndExit = { navigator.replaceWithHome() },
                    onHome = { navigator.replaceWithHome() }
                )
                PitCrewRoute.PRE_DEPARTURE -> PreDepartureScreen(
                    snapshot = preCheckSnapshot,
                    isChecking = isPreChecking,
                    onCheck = onCheckPreDeparture
                )
                PitCrewRoute.VEHICLE_LINK -> LinkStatusScreen(
                    featureSupport = featureSupport,
                    checkedAtMillis = linkCheckedAtMillis
                )
                PitCrewRoute.OTHER_PARTY -> OtherPartyScreen(
                    session = session,
                    onCheckedChange = onSetChecked,
                    onExitRequest = { showExitDialog = true }
                )
                PitCrewRoute.SAVED_RECORD -> SavedRecordScreen(
                    state = savedRecord,
                    deleteFailed = deleteFailed,
                    onDelete = onDeleteSavedRecord
                )
                else -> PlaceholderScreen(
                    route,
                    onHome = { navigator.replaceWithHome() },
                    // SC-500 자리표시 화면에서 SC-520으로 들어간다(SC-500 실제 화면은 단계 2에서 만든다).
                    extraLabel = if (route == PitCrewRoute.SETTINGS) "저장 기록 삭제" else null,
                    onExtra = { navigator.push(PitCrewRoute.SAVED_RECORD) }
                )
            }
        }
        if (route == PitCrewRoute.PRE_DEPARTURE || route == PitCrewRoute.VEHICLE_LINK) {
            // 사고 모드 밖 점검 화면: 왼쪽 [홈으로], 오른쪽 채운 [다시 확인]
            val busy = if (route == PitCrewRoute.PRE_DEPARTURE) isPreChecking else isCheckingLink
            PitCrewBottomBar(
                onPrevious = { navigator.replaceWithHome() },
                previousLabel = "홈으로",
                previousIcon = null,
                previousWidth = PitCrewDimens.homeButtonWidth,
                onNext = if (route == PitCrewRoute.PRE_DEPARTURE) onCheckPreDeparture else onRecheckLink,
                nextLabel = if (busy) "확인 중…" else "다시 확인",
                nextIcon = PitCrewIcons.Refresh,
                nextWidth = PitCrewDimens.recheckButtonWidth,
                nextEnabled = !busy
            )
        } else if (route == PitCrewRoute.CONSENT) {
            // [이전]은 저장하지 않고 홈으로, [다음]은 3개 값을 저장하고 SC-200으로(SC-205는 스택에서 뺀다)
            PitCrewBottomBar(
                onPrevious = { navigator.pop() },
                onNext = {
                    onConfirmConsent(consentChoice.location, consentChoice.voice, consentChoice.network)
                    navigator.replaceTop(PitCrewRoute.STOP_CHECK)
                }
            )
        } else if (route == PitCrewRoute.INSURER) {
            // SC-290: [이전]과 [다음](SC-270). 보내기 전에도 [다음]으로 갈 수 있다. 채운 버튼은 보낸 뒤의 [다음] 하나뿐이다.
            PitCrewBottomBar(
                onPrevious = { goBack() },
                onNext = { navigator.push(PitCrewRoute.ENDING) },
                nextFilled = !session?.insurerReceipts.isNullOrEmpty(),
                // 화면 안 세로 공간을 확보하려고 "참고용 기록…" 문구를 아래쪽에서 하단 바 가운데로 옮김
                centerText = InsurerTexts.DISCLAIMER,
                centerIcon = PitCrewIcons.Info
            )
        } else if (route == PitCrewRoute.SAVED_RECORD) {
            // SC-520: [이전]만(SC-500으로). 삭제는 화면 안 기본 버튼이다. "참고용" 문구는 하단 바 가운데에 둔다.
            PitCrewBottomBar(
                onPrevious = { goBack() },
                onNext = null,
                centerText = SavedRecordTexts.DISCLAIMER,
                centerIcon = PitCrewIcons.Info
            )
        } else if (route == PitCrewRoute.DESTINATION) {
            // SC-280: [이전](SC-270)만. 저장하지 않은 채 홈으로 가 버리지 않게 [홈으로]를 두지 않는다. 채운 버튼은 [길안내 시작] 하나뿐이다.
            // 빈 가운데 칸에 "참고용 안내예요…" 문구를 둔다(화면 안 세로 공간을 확보하려고 아래쪽에서 옮김).
            PitCrewBottomBar(
                onPrevious = { goBack() },
                onNext = null,
                centerText = DestinationTexts.DISCLAIMER,
                centerIcon = PitCrewIcons.Info
            )
        } else if (route == PitCrewRoute.ENDING) {
            // SC-270: 저장하기 전에는 [이전]과 기본 버튼 [다음 장소 안내](SC-280). 저장은 화면 안 큰 버튼이고,
            // 저장한 뒤에는 [홈으로]가 화면 안에 있어 [이전]은 없고 [다음 장소 안내]만 남는다. 채운 버튼은 화면 안 하나뿐이다.
            PitCrewBottomBar(
                onPrevious = if (saveState == SaveState.SAVED) null else ({ goBack() }),
                onNext = { navigator.push(PitCrewRoute.DESTINATION) },
                nextLabel = "다음 장소 안내",
                nextIcon = PitCrewIcons.Send,
                nextFilled = false,
                nextWidth = PitCrewDimens.recheckButtonWidth
            )
        } else if (route.step != null) {
            PitCrewBottomBar(
                onPrevious = { goBack() },
                // SC-200은 두 선택 버튼으로만 이동한다. 그 밖의 [다음]은 체크와 관계없이 항상 눌린다.
                onNext = when (route) {
                    PitCrewRoute.STOP_CHECK, PitCrewRoute.INJURY -> null // 선택 버튼으로만 이동
                    PitCrewRoute.EMERGENCY -> { { navigator.push(PitCrewRoute.RECORD) } }
                    else -> {
                        {
                            val next = navigator.next()
                            if (next != null) navigator.push(next) else navigator.replaceWithHome()
                        }
                    }
                },
                nextLabel = when {
                    route == PitCrewRoute.EMERGENCY -> "확인했어요"
                    navigator.next() == null -> "마치기"
                    else -> "다음"
                },
                // SC-260: 왼쪽에 기본 버튼 [보험사로 전달](채운 버튼은 [다음] 하나). 너비는 둘이 한 줄에 들어가게 줄인다.
                extraLabel = if (route == PitCrewRoute.SUMMARY) "보험사로 전달" else null,
                onExtra = if (route == PitCrewRoute.SUMMARY) ({ navigator.push(PitCrewRoute.INSURER) }) else null,
                extraIcon = PitCrewIcons.Send,
                nextWidth = if (route == PitCrewRoute.SUMMARY) PitCrewDimens.recheckButtonWidth else PitCrewDimens.nextButtonWidth
            )
        }
    }
    SessionDialogs(
        showExit = showExitDialog,
        showNotYet = showNotYetDialog,
        onDismissExit = { showExitDialog = false },
        onDismissNotYet = { showNotYetDialog = false },
        onGoHome = { navigator.replaceWithHome() }
    )
}

@Composable
private fun SessionDialogs(
    showExit: Boolean,
    showNotYet: Boolean,
    onDismissExit: () -> Unit,
    onDismissNotYet: () -> Unit,
    onGoHome: () -> Unit
) {
    if (showExit) {
        PitCrewDialog(
            title = "사고 모드를 나갈까요?",
            body = "지금까지 체크한 내용은 사라져요.",
            note = "참고용 안내예요.",
            buttons = listOf(
                DialogButton("계속하기", onDismissExit),
                DialogButton("나가기") { onDismissExit(); onGoHome() }
            ),
            onDismiss = onDismissExit
        )
    }
    if (showNotYet) {
        PitCrewDialog(
            title = "먼저 안전한 곳에 차를 세워 주세요",
            body = "이 앱은 차를 세운 뒤에 사용해 주세요",
            buttons = listOf(DialogButton("홈으로") { onDismissNotYet(); onGoHome() }),
            onDismiss = onDismissNotYet
        )
    }
}

/** 실제 화면이 아직 없는 route의 자리표시 화면. 흐름 밖 화면은 [홈으로]로 돌아간다. */
@Composable
private fun PlaceholderScreen(
    route: PitCrewRoute,
    onHome: () -> Unit,
    extraLabel: String? = null,
    onExtra: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)
        ) {
            Text(
                "${route.screenId} ${route.title}",
                style = PitCrewTheme.typography.title,
                color = PitCrewTheme.colors.textPrimary
            )
            Text(
                "자리표시 화면 (실제 화면은 이후 Phase)",
                style = PitCrewTheme.typography.caption,
                color = PitCrewTheme.colors.textSecondary
            )
            if (route.step == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)) {
                    PitCrewButton(text = "홈으로", onClick = onHome, modifier = Modifier.width(PitCrewDimens.prevButtonWidth))
                    if (extraLabel != null) {
                        PitCrewButton(text = extraLabel, onClick = onExtra, modifier = Modifier.width(PitCrewDimens.recheckButtonWidth))
                    }
                }
            }
        }
    }
}
