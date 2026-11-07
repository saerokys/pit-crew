package ai.pleos.playground.sample.vehicle

import ai.pleos.playground.sample.vehicle.ui.screen.SampleApp
import ai.pleos.playground.sample.vehicle.ui.theme.VehicleSampleAppTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onStop() {
        // 앱이 화면에서 사라지면 읽기를 멈춘다.
        viewModel.stopSpeech()
        // 앱이 화면에서 사라지면 듣기도 멈추고 SDK를 해제한다(마이크를 열어 두지 않는다).
        viewModel.releaseVoice()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

            VehicleSampleAppTheme {
                SampleApp(
                    uiState = uiState,
                    onStartSession = viewModel::startAccidentSession,
                    onSetChecked = viewModel::setChecked,
                    onSetInjury = viewModel::setInjuryAnswer,
                    onEndSession = viewModel::endAccidentSession,
                    onSaveRecord = viewModel::saveRecord,
                    onResetSaveState = viewModel::resetSaveState,
                    onRefreshSavedRecord = viewModel::refreshSavedRecord,
                    onDeleteSavedRecord = viewModel::deleteSavedRecord,
                    onLoadSavedRecord = viewModel::loadSavedRecord,
                    onReadVehicle = viewModel::readVehicleState,
                    onCheckPreDeparture = viewModel::checkPreDeparture,
                    onRecheckLink = viewModel::recheckVehicleLink,
                    onConfirmConsent = viewModel::confirmConsent,
                    onUpdateConsent = viewModel::updateConsent,
                    onToggleSpeech = viewModel::toggleSpeech,
                    onStopSpeech = viewModel::stopSpeech,
                    onReleaseSpeech = { viewModel.releaseSpeech() },
                    onSelectDestCategory = viewModel::selectDestinationCategory,
                    onSelectDestPlace = viewModel::selectDestinationPlace,
                    onStartNavigation = viewModel::startNavigation,
                    onConnectNavi = viewModel::connectNavi,
                    onReleaseNavi = viewModel::releaseNavi,
                    onToggleInsurerItem = viewModel::toggleInsurerItem,
                    onSendInsurer = viewModel::sendToInsurer,
                    onStartVoice = viewModel::startVoiceMemo,
                    onStopVoice = viewModel::stopVoiceMemo,
                    onSaveVoice = viewModel::confirmVoiceMemo,
                    onReleaseVoice = { viewModel.releaseVoice() }
                )
            }
        }
    }
}
