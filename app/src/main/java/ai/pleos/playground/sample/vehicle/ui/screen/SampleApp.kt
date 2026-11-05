package ai.pleos.playground.sample.vehicle.ui.screen

import ai.pleos.playground.sample.vehicle.BuildConfig
import ai.pleos.playground.sample.vehicle.explorer.VehicleDomain
import androidx.activity.compose.BackHandler
import ai.pleos.playground.sample.vehicle.session.InjuryAnswer
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewButton
import ai.pleos.playground.sample.vehicle.ui.data.AppPage
import ai.pleos.playground.sample.vehicle.ui.data.StartupPhase
import ai.pleos.playground.sample.vehicle.ui.home.summarizeLink
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import androidx.compose.foundation.layout.width
import ai.pleos.playground.sample.vehicle.ui.nav.PitCrewNavHost
import ai.pleos.playground.sample.vehicle.ui.screen.dev.ComponentGalleryScreen
import ai.pleos.playground.sample.vehicle.ui.screen.dev.DevMenuScreen
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
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
    onShowDevPage: (AppPage) -> Unit,
    onStartSession: () -> Unit,
    onSetChecked: (String, Boolean) -> Unit,
    onSetInjury: (InjuryAnswer) -> Unit,
    onEndSession: () -> Unit,
    onReadVehicle: () -> Unit,
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

    // 개발자 도구(읽기 전용 탐색기, 컴포넌트 미리보기, 화면 이동 틀)는 디버그 빌드에서만 열린다.
    // 릴리스 빌드에는 진입 경로가 없다. 진입점은 Phase 14에서 제거한다.
    val devToolsEnabled = BuildConfig.DEBUG
    val page = if (devToolsEnabled) uiState.page else AppPage.HOME
    BackHandler(enabled = page != AppPage.HOME) {
        onShowDevPage(if (page == AppPage.DEV_MENU) AppPage.HOME else AppPage.DEV_MENU)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (page) {
            AppPage.DEV_TOOLS -> ApiCatalogScreen(
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
            AppPage.DEV_MENU -> PitCrewFrame {
                DevMenuScreen(
                    onOpenApiExplorer = { onOpenDomain(VehicleDomain.DOOR) },
                    onOpenComponents = { onShowDevPage(AppPage.DEV_COMPONENTS) },
                    onBackHome = onShowHome
                )
            }
            AppPage.DEV_COMPONENTS -> PitCrewFrame {
                ComponentGalleryScreen(onBack = { onShowDevPage(AppPage.DEV_MENU) })
            }
            AppPage.HOME -> PitCrewFrame {
                PitCrewNavHost(
                    startupDone = uiState.startup == StartupPhase.DONE,
                    summary = summarizeLink(uiState.isInitialized, uiState.featureSupport),
                    session = uiState.accidentSession,
                    onStartSession = onStartSession,
                    onSetChecked = onSetChecked,
                    onSetInjury = onSetInjury,
                    onEndSession = onEndSession,
                    isReadingVehicle = uiState.isReadingVehicle,
                    onReadVehicle = onReadVehicle,
                    // 개발자 도구 진입점: 디버그 빌드에서만 상단 바 오른쪽에 둔다(Phase 14에서 제거).
                    topBarTrailing = if (devToolsEnabled) {
                        {
                            PitCrewButton(
                                text = "개발자 도구",
                                onClick = { onShowDevPage(AppPage.DEV_MENU) },
                                modifier = Modifier.width(PitCrewDimens.prevButtonWidth)
                            )
                        }
                    } else null
                )
            }
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

/** Pit Crew 화면을 새 테마로 감싸고, 샘플 앱과 같은 방식으로 왼쪽 시스템 레일 영역을 비운다(에뮬레이터에서 확인 필요). */
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
