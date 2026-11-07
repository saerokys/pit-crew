package ai.pleos.playground.sample.vehicle.ui.settings

import ai.pleos.playground.sample.vehicle.location.FUSED_LOCATION_PERMISSION
import ai.pleos.playground.sample.vehicle.location.hasLocationPermission
import ai.pleos.playground.sample.vehicle.settings.Consent
import ai.pleos.playground.sample.vehicle.ui.components.InfoRow
import ai.pleos.playground.sample.vehicle.ui.components.InfoRowsCard
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewButton
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewIcons
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewSwitchRow
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat

/** SC-500 문구. 모든 글자는 28 이상이다. SC-205와 같은 문구는 그대로 쓴다. 라벨·값 문구는 구현 판단이다(PROGRESS 4-2). */
object SettingsTexts {
    const val TITLE = "설정·정보"
    const val APP_NAME = "Pit Crew"
    const val APP_INTRO_LABEL = "앱 소개"
    const val APP_INTRO = "사고 직후 정차부터 마무리까지 안내"
    const val APP_VERSION_LABEL = "앱 버전"
    const val SDK_LABEL = "Vehicle SDK"
    const val SERVICE_LABEL = "Vehicle Service"
    const val UNKNOWN = "확인 불가"
    const val OPEN_RECORDS = "저장 기록 삭제"
    const val PERMISSION_DENIED = "위치 권한을 허용하지 않았어요"
    const val PERMISSION_GUIDE = "시스템 설정에서 허용해 주세요"
    const val NETWORK_NOTICE = "켜면 사고 위치를 주소·날씨 조회 서버로 보내요"
    const val FOOTER = "꺼 두어도 사고 안내는 끝까지 사용할 수 있어요"
}

/** 버전 글자. 읽지 못했거나 아직 읽기 전이면 "확인 불가"(Vehicle Service 버전이 대상). */
fun versionOrUnknown(raw: String?): String {
    val text = raw?.trim().orEmpty()
    return when (text.lowercase()) {
        "", "unknown", "not installed" -> SettingsTexts.UNKNOWN
        else -> text
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/**
 * SC-500 설정·정보. 왼쪽에 SC-205와 같은 동의 스위치 3개(바로 저장), 오른쪽에 앱 정보(앱 소개, 앱 버전, Vehicle SDK·Service 버전),
 * 아래에 [저장 기록 삭제](SC-520). 위치 스위치는 SC-205와 같은 권한 요청 흐름을 쓰고, 권한을 두 번 거부해 시스템이 창을 띄우지 않으면
 * 거부 직후 글자 안내만 보여 준다(설정을 여는 버튼은 없다). 시스템 설정에서 권한을 뺐으면 위치는 꺼진 것으로 보인다.
 */
@Composable
fun SettingsScreen(
    consent: Consent,
    locationSupported: Boolean,
    appVersion: String,
    sdkVersion: String,
    serviceVersion: String,
    onConsentChange: (location: Boolean, voice: Boolean, network: Boolean) -> Unit,
    onOpenRecords: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PitCrewTheme.colors
    val context = LocalContext.current
    val current by rememberUpdatedState(consent)
    var denied by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }

    val permitted = hasLocationPermission(context)
    val locationOn = current.location && permitted && locationSupported

    // 저장된 동의는 켜져 있는데 시스템 설정에서 권한을 뺀 경우: 위치를 끈 것으로 맞춘다.
    LaunchedEffect(consent.location, permitted) {
        if (consent.location && !permitted) onConsentChange(false, consent.voice, consent.network)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            denied = false
            showGuide = false
            onConsentChange(true, current.voice, current.network)
        } else {
            denied = true
            val activity = context.findActivity()
            // 두 번 거부하면 시스템이 창을 띄우지 않고 바로 거부로 돌려준다. 이때는 이유 안내 창을 더 보여 줄 수 없다(false).
            // 호출이 예외를 던지면 안내를 띄우지 않는다(앱은 계속 동작한다).
            val rationale: Boolean? = try {
                activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, FUSED_LOCATION_PERMISSION) }
            } catch (e: Exception) {
                null
            }
            showGuide = rationale == false
            Log.i("PitCrew", "SC-500 location permission denied, rationale=$rationale")
            onConsentChange(false, current.voice, current.network)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = PitCrewDimens.safeMargin,
                end = PitCrewDimens.safeMargin,
                top = PitCrewDimens.stepScreenTopGap,
                bottom = PitCrewDimens.stepScreenBottomGap
            )
    ) {
        Text(SettingsTexts.TITLE, style = PitCrewTheme.typography.screenTitle, color = colors.textPrimary)
        Spacer(Modifier.height(PitCrewDimens.touchGap))
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PitCrewDimens.stepColumnGap)
        ) {
            // 왼쪽: 동의 스위치
            Column(modifier = Modifier.weight(1.2f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                Column(verticalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)) {
                    PitCrewSwitchRow(
                        icon = PitCrewIcons.Pin,
                        title = "위치",
                        description = if (locationSupported) "사고 장소를 기록해요" else "이 차량에서 사용할 수 없어요",
                        checked = locationOn,
                        available = locationSupported,
                        onCheckedChange = { on ->
                            denied = false
                            showGuide = false
                            if (!on) {
                                onConsentChange(false, current.voice, current.network)
                            } else if (hasLocationPermission(context)) {
                                onConsentChange(true, current.voice, current.network)
                            } else {
                                try {
                                    permissionLauncher.launch(FUSED_LOCATION_PERMISSION)
                                } catch (e: Exception) {
                                    denied = true
                                    onConsentChange(false, current.voice, current.network)
                                }
                            }
                        }
                    )
                    PitCrewSwitchRow(
                        icon = PitCrewIcons.Mic,
                        title = "음성",
                        description = "소리로 듣고 말로 메모를 남겨요",
                        checked = current.voice,
                        onCheckedChange = { onConsentChange(locationOn, it, current.network) }
                    )
                    PitCrewSwitchRow(
                        icon = PitCrewIcons.Wifi,
                        title = "네트워크",
                        description = "주소와 날씨를 조회해요",
                        checked = current.network,
                        onCheckedChange = { onConsentChange(locationOn, current.voice, it) }
                    )
                }
                // 네트워크를 켰을 때만 보이는 안내(자리는 항상 잡아 둔다).
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(NOTICE_HEIGHT), contentAlignment = Alignment.CenterStart) {
                    if (current.network) {
                        Text(SettingsTexts.NETWORK_NOTICE, style = PitCrewTheme.typography.caption, color = colors.textSecondary)
                    }
                }
                if (denied) {
                    WarnLine(SettingsTexts.PERMISSION_DENIED)
                    if (showGuide) WarnLine(SettingsTexts.PERMISSION_GUIDE)
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(PitCrewIcons.ShieldCheck, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(PitCrewDimens.iconSize))
                    Spacer(Modifier.width(16.dp))
                    Text(SettingsTexts.FOOTER, style = PitCrewTheme.typography.caption, color = colors.textSecondary)
                }
            }
            // 오른쪽: 앱 정보
            Column(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                InfoRowsCard(
                    title = SettingsTexts.APP_NAME,
                    rows = listOf(
                        InfoRow(PitCrewIcons.Info, SettingsTexts.APP_INTRO_LABEL, SettingsTexts.APP_INTRO),
                        InfoRow(PitCrewIcons.Info, SettingsTexts.APP_VERSION_LABEL, appVersion),
                        InfoRow(PitCrewIcons.Car, SettingsTexts.SDK_LABEL, versionOrUnknown(sdkVersion)),
                        InfoRow(PitCrewIcons.Car, SettingsTexts.SERVICE_LABEL, versionOrUnknown(serviceVersion))
                    ),
                    iconTint = colors.accent,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(Modifier.height(PitCrewDimens.touchGap))
        PitCrewButton(
            text = SettingsTexts.OPEN_RECORDS,
            onClick = onOpenRecords,
            modifier = Modifier.width(PitCrewDimens.recheckButtonWidth).height(PitCrewDimens.touchMin)
        )
    }
}

private val NOTICE_HEIGHT = 48.dp // 캡션 한 줄(줄 높이 40)이 들어가는 고정 자리

@Composable
private fun WarnLine(text: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(PitCrewIcons.Warning, contentDescription = null, tint = PitCrewTheme.colors.statusWarn, modifier = Modifier.size(PitCrewDimens.iconSize))
        Spacer(Modifier.width(16.dp))
        Text(text, style = PitCrewTheme.typography.caption, color = PitCrewTheme.colors.textPrimary)
    }
}
