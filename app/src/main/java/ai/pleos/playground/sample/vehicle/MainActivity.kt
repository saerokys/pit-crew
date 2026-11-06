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
                    onShowDevPage = viewModel::showDevPage,
                    onStartSession = viewModel::startAccidentSession,
                    onSetChecked = viewModel::setChecked,
                    onSetInjury = viewModel::setInjuryAnswer,
                    onEndSession = viewModel::endAccidentSession,
                    onSaveRecord = viewModel::saveRecord,
                    onResetSaveState = viewModel::resetSaveState,
                    onRefreshSavedRecord = viewModel::refreshSavedRecord,
                    onDeleteSavedRecord = viewModel::deleteSavedRecord,
                    onReadVehicle = viewModel::readVehicleState,
                    onCheckPreDeparture = viewModel::checkPreDeparture,
                    onRecheckLink = viewModel::recheckVehicleLink,
                    onConfirmConsent = viewModel::confirmConsent,
                    onResetConsent = viewModel::resetConsent,
                    onToggleSpeech = viewModel::toggleSpeech,
                    onStopSpeech = viewModel::stopSpeech,
                    onReleaseSpeech = { viewModel.releaseSpeech() },
                    onShowHome = viewModel::showHome,
                    onOpenDomain = viewModel::showApiExplorer,
                    onSelectFeature = viewModel::selectFeature,
                    onSearchFeatures = viewModel::searchFeatures,
                    onRunCapability = viewModel::runFeatureCapability,
                    onRunRead = viewModel::runFeatureRead,
                    onStartUpdates = viewModel::startFeatureUpdates,
                    onStopUpdates = viewModel::stopFeatureUpdates,
                    onBackToFeatureList = viewModel::backToFeatureList,
                    onDismissError = viewModel::dismissError
                )
            }
        }
    }
}
