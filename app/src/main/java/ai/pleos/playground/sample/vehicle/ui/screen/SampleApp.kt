package ai.pleos.playground.sample.vehicle.ui.screen

import ai.pleos.playground.sample.vehicle.insurer.InsurerItem
import ai.pleos.playground.sample.vehicle.navi.DestinationCategory
import ai.pleos.playground.sample.vehicle.session.InjuryAnswer
import ai.pleos.playground.sample.vehicle.ui.data.MainUiState
import ai.pleos.playground.sample.vehicle.ui.data.StartupPhase
import ai.pleos.playground.sample.vehicle.ui.home.summarizeLink
import ai.pleos.playground.sample.vehicle.ui.nav.PitCrewNavHost
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** 앱의 유일한 화면 틀. Pit Crew 화면 흐름([PitCrewNavHost])을 새 테마로 감싼다. */
@Composable
fun SampleApp(
    uiState: MainUiState,
    onStartSession: () -> Unit,
    onSetChecked: (String, Boolean) -> Unit,
    onSetInjury: (InjuryAnswer) -> Unit,
    onEndSession: () -> Unit,
    onSaveRecord: () -> Unit,
    onResetSaveState: () -> Unit,
    onRefreshSavedRecord: () -> Unit,
    onDeleteSavedRecord: () -> Unit,
    onLoadSavedRecord: () -> Unit,
    onReadVehicle: () -> Unit,
    onCheckPreDeparture: () -> Unit,
    onRecheckLink: () -> Unit,
    onConfirmConsent: (Boolean, Boolean, Boolean) -> Unit,
    onUpdateConsent: (Boolean, Boolean, Boolean) -> Unit,
    onToggleSpeech: (String) -> Unit,
    onStopSpeech: () -> Unit,
    onReleaseSpeech: () -> Unit,
    onSelectDestCategory: (DestinationCategory) -> Unit,
    onSelectDestPlace: (String) -> Unit,
    onStartNavigation: () -> Unit,
    onConnectNavi: () -> Unit,
    onReleaseNavi: () -> Unit,
    onToggleInsurerItem: (InsurerItem) -> Unit,
    onSendInsurer: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onSaveVoice: () -> Unit,
    onReleaseVoice: () -> Unit
) {
    PitCrewFrame {
        PitCrewNavHost(
            startupDone = uiState.startup == StartupPhase.DONE,
            summary = summarizeLink(uiState.isInitialized, uiState.featureSupport),
            session = uiState.accidentSession,
            onStartSession = onStartSession,
            onSetChecked = onSetChecked,
            onSetInjury = onSetInjury,
            onEndSession = onEndSession,
            saveState = uiState.saveState,
            hasSavedRecord = uiState.hasSavedRecord,
            onSaveRecord = onSaveRecord,
            onResetSaveState = onResetSaveState,
            onRefreshSavedRecord = onRefreshSavedRecord,
            savedRecord = uiState.savedRecord,
            deleteFailed = uiState.deleteFailed,
            onLoadSavedRecord = onLoadSavedRecord,
            onDeleteSavedRecord = onDeleteSavedRecord,
            isReadingVehicle = uiState.isReadingVehicle,
            onReadVehicle = onReadVehicle,
            preCheckSnapshot = uiState.preCheckSnapshot,
            isPreChecking = uiState.isPreChecking,
            onCheckPreDeparture = onCheckPreDeparture,
            consent = uiState.consent,
            locationSupported = uiState.locationSupported,
            onConfirmConsent = onConfirmConsent,
            onUpdateConsent = onUpdateConsent,
            runtimeVersions = uiState.runtimeVersions,
            speechState = uiState.speechState,
            onToggleSpeech = onToggleSpeech,
            onStopSpeech = onStopSpeech,
            onReleaseSpeech = onReleaseSpeech,
            voiceState = uiState.voiceState,
            voiceDraft = uiState.voiceDraft,
            voiceDraftCut = uiState.voiceDraftCut,
            destCategory = uiState.destCategory,
            destPlaceId = uiState.destPlaceId,
            naviState = uiState.naviState,
            routeResult = uiState.routeResult,
            onSelectDestCategory = onSelectDestCategory,
            onSelectDestPlace = onSelectDestPlace,
            onStartNavigation = onStartNavigation,
            onConnectNavi = onConnectNavi,
            onReleaseNavi = onReleaseNavi,
            insurerUnchecked = uiState.insurerUnchecked,
            insurerSendState = uiState.insurerSendState,
            insurerLastSent = uiState.insurerLastSent,
            onToggleInsurerItem = onToggleInsurerItem,
            onSendInsurer = onSendInsurer,
            voiceNotHeard = uiState.voiceNotHeard,
            onStartVoice = onStartVoice,
            onStopVoice = onStopVoice,
            onSaveVoice = onSaveVoice,
            onReleaseVoice = onReleaseVoice,
            featureSupport = uiState.featureSupport,
            linkCheckedAtMillis = uiState.linkCheckedAtMillis,
            isCheckingLink = uiState.isCheckingLink,
            onRecheckLink = onRecheckLink
        )
    }
}

/** Pit Crew 화면을 새 테마로 감싸고, 왼쪽 시스템 레일 영역을 비운다. */
@Composable
private fun PitCrewFrame(content: @Composable () -> Unit) {
    PitCrewTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PitCrewTheme.colors.background)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Start))
                .padding(start = AaosSystemRailInset)
        ) { content() }
    }
}
