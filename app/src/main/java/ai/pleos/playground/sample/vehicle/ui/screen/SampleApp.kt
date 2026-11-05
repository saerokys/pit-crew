package ai.pleos.playground.sample.vehicle.ui.screen

import ai.pleos.playground.sample.vehicle.BuildConfig
import ai.pleos.playground.sample.vehicle.explorer.VehicleDomain
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import ai.pleos.playground.sample.vehicle.ui.data.AppPage
import ai.pleos.playground.sample.vehicle.ui.data.MainUiState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SampleApp(
    uiState: MainUiState,
    onOpenDomain: (VehicleDomain) -> Unit,
    onSelectFeature: (String) -> Unit,
    onSearchFeatures: (String) -> Unit,
    onRunCapability: () -> Unit,
    onRunRead: (Map<String, String>) -> Unit,
    onStartUpdates: (Map<String, String>) -> Unit,
    onStopUpdates: (Map<String, String>) -> Unit,
    onBackToFeatureList: () -> Unit,
    onDismissError: () -> Unit,
    onOpenDevTools: () -> Unit,
    onShowHome: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        snackbarHostState.currentSnackbarData?.dismiss()
        val message = uiState.errorMessage ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = "Dismiss",
            duration = SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed || result == SnackbarResult.Dismissed) {
            onDismissError()
        }
    }

    // 개발자 도구(읽기 전용 탐색기)는 디버그 빌드에서만 열린다. 릴리스 빌드에는 진입 경로가 없다.
    val devToolsEnabled = BuildConfig.DEBUG
    BackHandler(enabled = uiState.page == AppPage.DEV_TOOLS) { onShowHome() }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.page == AppPage.DEV_TOOLS && devToolsEnabled -> ApiCatalogScreen(
                uiState = uiState,
                onSelectDomain = onOpenDomain,
                onSelectFeature = onSelectFeature,
                onSearchFeatures = onSearchFeatures,
                onRunCapability = onRunCapability,
                onRunRead = onRunRead,
                onStartUpdates = onStartUpdates,
                onStopUpdates = onStopUpdates,
                onBackToFeatureList = onBackToFeatureList,
                onDismissFeatureError = onDismissError
            )
            else -> HomePlaceholder(
                showDevTools = devToolsEnabled,
                onOpenDevTools = onOpenDevTools
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(24.dp)
        )
    }
}

// Phase 2에서 SC-100 홈으로 교체할 임시 자리표시 화면.
@Composable
private fun HomePlaceholder(showDevTools: Boolean, onOpenDevTools: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Pit Crew (Phase 2에서 홈 구현)")
        if (showDevTools) {
            Button(onClick = onOpenDevTools, modifier = Modifier.padding(top = 24.dp)) {
                Text("개발자 도구 (디버그 전용)")
            }
        }
    }
}
