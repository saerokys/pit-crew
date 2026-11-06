package ai.pleos.playground.sample.vehicle.ui.screen.dev

import ai.pleos.playground.sample.vehicle.ui.components.BadgeStatus
import ai.pleos.playground.sample.vehicle.ui.components.CheckItem
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewBottomBar
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewButton
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewCard
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewCardText
import ai.pleos.playground.sample.vehicle.ui.components.PitCrewTopBar
import ai.pleos.playground.sample.vehicle.ui.components.StatusBadge
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewDimens
import ai.pleos.playground.sample.vehicle.ui.theme.pitcrew.PitCrewTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 디버그 전용 개발자 도구 메뉴(Phase 14에서 진입점과 함께 제거). */
@Composable
fun DevMenuScreen(
    onOpenApiExplorer: () -> Unit,
    onOpenComponents: () -> Unit,
    onResetConsent: () -> Unit,
    hasSavedRecord: Boolean,
    onRefreshSavedRecord: () -> Unit,
    onDeleteSavedRecord: () -> Unit,
    onBackHome: () -> Unit
) {
    var consentCleared by remember { mutableStateOf(false) }
    // 메뉴를 열 때 저장된 기록이 있는지 확인한다.
    LaunchedEffect(Unit) { onRefreshSavedRecord() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PitCrewTheme.colors.background)
            .padding(PitCrewDimens.safeMargin),
        verticalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)
    ) {
        Text("개발자 도구 (디버그 전용)", style = PitCrewTheme.typography.title, color = PitCrewTheme.colors.textPrimary)
        PitCrewButton("API 탐색기 (읽기 전용)", onClick = onOpenApiExplorer, modifier = Modifier.width(720.dp))
        PitCrewButton("컴포넌트 미리보기", onClick = onOpenComponents, modifier = Modifier.width(720.dp))
        // 디버그 전용(Phase 14에서 제거): 저장된 동의 설정만 지운다. 위치 권한은 건드리지 않는다.
        PitCrewButton(
            "동의 초기화 (SC-205 다시 보기)",
            onClick = { onResetConsent(); consentCleared = true },
            modifier = Modifier.width(720.dp)
        )
        if (consentCleared) {
            Text("동의 설정을 지웠어요", style = PitCrewTheme.typography.caption, color = PitCrewTheme.colors.textSecondary)
        }
        // 디버그 전용(Phase 14에서 개발자 도구와 함께 제거): SC-270에서 저장한 사고 기록 파일을 지운다.
        PitCrewButton(
            "저장 기록 삭제",
            onClick = onDeleteSavedRecord,
            enabled = hasSavedRecord,
            modifier = Modifier.width(720.dp)
        )
        Text(
            if (hasSavedRecord) "저장된 기록: 있음" else "저장된 기록: 없음",
            style = PitCrewTheme.typography.caption,
            color = PitCrewTheme.colors.textSecondary
        )
        PitCrewButton("홈으로", onClick = onBackHome, modifier = Modifier.width(720.dp))
    }
}

/** 공통 컴포넌트를 한 화면에 모두 배치해 눈으로 확인하는 화면. 채운 버튼은 하단 바의 [다음] 하나뿐이다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComponentGalleryScreen(onBack: () -> Unit) {
    var checkedA by remember { mutableStateOf(true) }
    var checkedB by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(true) }
    val colors = PitCrewTheme.colors

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        PitCrewTopBar(step = 3)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(PitCrewDimens.safeMargin),
            verticalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)
        ) {
            Text("상태 배지", style = PitCrewTheme.typography.title, color = colors.textPrimary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap),
                verticalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)
            ) {
                BadgeStatus.entries.forEach { StatusBadge(it) }
            }

            Text("글자 크기", style = PitCrewTheme.typography.title, color = colors.textPrimary)
            Text("본문 32 Regular", style = PitCrewTheme.typography.body, color = colors.textPrimary)
            Text("보조 28 Regular", style = PitCrewTheme.typography.caption, color = colors.textSecondary)

            Text("버튼 (기본, 선택됨, 비활성. 눌림·포커스는 직접 눌러 확인)", style = PitCrewTheme.typography.title, color = colors.textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)) {
                PitCrewButton("기본", onClick = {}, modifier = Modifier.width(280.dp))
                PitCrewButton("선택됨", onClick = { selected = !selected }, selected = selected, modifier = Modifier.width(280.dp))
                PitCrewButton("비활성", onClick = {}, enabled = false, modifier = Modifier.width(280.dp))
            }

            Text("체크 항목", style = PitCrewTheme.typography.title, color = colors.textPrimary)
            CheckItem("비상등을 켰어요", checked = checkedA, onCheckedChange = { checkedA = it })
            CheckItem("안전한 곳에 내렸어요", checked = checkedB, onCheckedChange = { checkedB = it })

            Text("카드 (실선, 점선)", style = PitCrewTheme.typography.title, color = colors.textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(PitCrewDimens.touchGap)) {
                PitCrewCard(modifier = Modifier.weight(1f)) {
                    PitCrewCardText("보험사에 전화하기", "차를 세운 뒤에 연결해요.")
                }
                PitCrewCard(modifier = Modifier.weight(1f), dashed = true) {
                    PitCrewCardText("사용자가 허용한 경우만", "위치, 날씨 카드는 점선이에요.")
                }
            }
        }
        PitCrewBottomBar(onPrevious = onBack, onNext = onBack)
    }
}
