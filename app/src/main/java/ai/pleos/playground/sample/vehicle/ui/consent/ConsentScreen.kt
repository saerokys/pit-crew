package ai.pleos.playground.sample.vehicle.ui.consent

import ai.pleos.playground.sample.vehicle.location.FUSED_LOCATION_PERMISSION
import ai.pleos.playground.sample.vehicle.location.hasLocationPermission
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewIcons
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewSwitchRow
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val NETWORK_NOTICE_HEIGHT = 48.dp // 캡션 한 줄(줄 높이 40)이 들어가는 고정 자리

/** SC-205 스위치 3개의 값. [다음]에서만 저장한다. */
data class ConsentChoice(val location: Boolean, val voice: Boolean, val network: Boolean)

/**
 * SC-205 권한·동의. 스위치 3개(기본 꺼짐), 입력창 없음.
 * 위치 스위치를 켤 때 앱 권한(FUSED_LOCATION)을 요청하고, 거부되면 다시 끄고 안내 한 줄을 보인다.
 * 이 차량에 위치 서비스가 없으면([locationSupported]가 false) 위치 행은 회색 비활성이다.
 * 음성, 네트워크는 값만 저장한다(쓰는 기능은 Phase 8, 9, 11).
 */
@Composable
fun ConsentScreen(
    locationSupported: Boolean,
    onChoiceChange: (ConsentChoice) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PitCrewTheme.colors
    val context = LocalContext.current
    var location by remember { mutableStateOf(false) }
    var voice by remember { mutableStateOf(false) }
    var network by remember { mutableStateOf(false) }
    var denied by remember { mutableStateOf(false) }

    fun publish() = onChoiceChange(ConsentChoice(location, voice, network))
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        location = granted
        denied = !granted
        publish()
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
        Text("사용할 기능을 골라 주세요", style = PitCrewTheme.typography.screenTitle, color = colors.textPrimary)
        Text("필요한 것만 켜세요. 언제든 끌 수 있어요.", style = PitCrewTheme.typography.body, color = colors.textSecondary)
        Spacer(Modifier.height(PitCrewDimens.touchGap))
        Column(verticalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)) {
            PitCrewSwitchRow(
                icon = PitCrewIcons.Pin,
                title = "위치",
                description = if (locationSupported) "사고 장소를 기록해요" else "이 차량에서 사용할 수 없어요",
                checked = location,
                available = locationSupported,
                onCheckedChange = { on ->
                    denied = false
                    if (!on) {
                        location = false
                        publish()
                    } else if (hasLocationPermission(context)) {
                        location = true
                        publish()
                    } else {
                        try {
                            permissionLauncher.launch(FUSED_LOCATION_PERMISSION)
                        } catch (e: Exception) {
                            location = false
                            denied = true
                            publish()
                        }
                    }
                }
            )
            PitCrewSwitchRow(
                icon = PitCrewIcons.Mic,
                title = "음성",
                description = "소리로 듣고 말로 메모를 남겨요",
                checked = voice,
                onCheckedChange = { voice = it; publish() }
            )
            PitCrewSwitchRow(
                icon = PitCrewIcons.Wifi,
                title = "네트워크",
                description = "주소와 날씨를 조회해요",
                checked = network,
                onCheckedChange = { network = it; publish() }
            )
        }
        // 네트워크를 켰을 때만 보이는 안내 줄. 자리는 항상 잡아 두어 위쪽 행과 하단이 밀리지 않게 한다.
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(NETWORK_NOTICE_HEIGHT), contentAlignment = Alignment.CenterStart) {
            if (network) {
                Text("켜면 사고 위치를 주소·날씨 조회 서버로 보내요", style = PitCrewTheme.typography.caption, color = colors.textSecondary)
            }
        }
        Spacer(Modifier.height(16.dp))
        if (denied) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(PitCrewIcons.Warning, contentDescription = null, tint = colors.statusWarn, modifier = Modifier.size(PitCrewDimens.iconSize))
                Spacer(Modifier.width(16.dp))
                Text("위치 권한을 허용하지 않았어요", style = PitCrewTheme.typography.caption, color = colors.textPrimary)
            }
            Spacer(Modifier.height(12.dp))
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(PitCrewIcons.ShieldCheck, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(PitCrewDimens.iconSize))
            Spacer(Modifier.width(16.dp))
            Text("꺼 두어도 사고 안내는 끝까지 사용할 수 있어요", style = PitCrewTheme.typography.caption, color = colors.textSecondary)
        }
    }
}
