package ai.pleos.playground.sample.vehicle.ui.theme.pitcrew

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalPitCrewColors = staticCompositionLocalOf { PitCrewDarkColors }
private val LocalPitCrewTypography = staticCompositionLocalOf { PitCrewDefaultTypography }

/** Pit Crew 화면 전용 테마. 기존 샘플 테마(VehicleSampleAppTheme)와 분리한다(Phase 14에서 정리). */
object PitCrewTheme {
    val colors: PitCrewColors
        @Composable @ReadOnlyComposable get() = LocalPitCrewColors.current
    val typography: PitCrewTypography
        @Composable @ReadOnlyComposable get() = LocalPitCrewTypography.current
}

@Composable
fun PitCrewTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalPitCrewColors provides PitCrewDarkColors,
        LocalPitCrewTypography provides PitCrewDefaultTypography,
        content = content
    )
}
