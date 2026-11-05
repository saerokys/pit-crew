package ai.pleos.playground.sample.vehicle.ui.theme.pitcrew

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** docs/design/tokens.md 2절의 색. hex는 이 파일에만 쓴다. 표에 없는 색은 추가하지 않는다. */
@Immutable
class PitCrewColors(
    val background: Color,
    val surface: Color,
    val surfaceSelected: Color,
    val divider: Color,
    val bar: Color,
    val accent: Color,
    val onAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val statusOk: Color,
    val statusWarn: Color,
    val statusUrgent: Color,
    val statusUrgentText: Color,
    val statusUnknownBg: Color,
    val statusUnknownText: Color
)

val PitCrewDarkColors = PitCrewColors(
    background = Color(0xFF0A1424),
    surface = Color(0xFF12233D),
    surfaceSelected = Color(0xFF1A3254),
    divider = Color(0xFF2A4468),
    bar = Color(0xFF002C5F),
    accent = Color(0xFF00AAD2),
    onAccent = Color(0xFF00122B),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFAACAE6),
    textDisabled = Color(0xFF7F8DA3),
    statusOk = Color(0xFF3DD68C),
    statusWarn = Color(0xFFFFC83D),
    statusUrgent = Color(0xFFC42A0E),
    statusUrgentText = Color(0xFFFF6B4A),
    statusUnknownBg = Color(0xFF2A3547),
    statusUnknownText = Color(0xFFC5CEDA)
)
