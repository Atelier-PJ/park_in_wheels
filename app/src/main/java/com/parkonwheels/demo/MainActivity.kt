package com.parkonwheels.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.parkonwheels.core.SafariEngine.Phase
import com.parkonwheels.core.VehicleGateway.Gate
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val model: SafariModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFA7F3A0),
                background = Color(0xFF101F1B), surface = Color(0xFF1D332C))) {
                SafariScreen(model)
            }
        }
    }
    override fun onStop() { model.background(); super.onStop() }
}

@Composable
private fun SafariScreen(model: SafariModel) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp)
            .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("PARK ON WHEELS", style = MaterialTheme.typography.headlineMedium)
            Text("DEMO · 모의 차량 데이터 · 실제 Pleos API 미연결", color = Color(0xFFFFD180))
            Text("테스트 전용 앱입니다. 실제 차량 운행 중 사용하지 마세요.")
            model.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (!model.canPlay) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("탐험이 잠시 멈췄어요", style = MaterialTheme.typography.headlineSmall)
                        Text(if (model.gate == Gate.RESTRICTED) "이용 제한 중입니다. 진행은 저장됩니다."
                            else "이용 가능 상태 확인이 필요합니다.")
                    }
                }
            } else {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("DINO SAFARI", style = MaterialTheme.typography.headlineSmall)
                        Text("탐험 기록을 모아 공룡 알을 부화시켜요.")
                        Text("모의 이동 ${String.format(Locale.US, "%.1f", model.state.travelledKm)} / 5.0 km")
                        LinearProgressIndicator(progress = { (model.state.travelledKm / 5.0).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        Text("공룡 카드 ${model.state.cards}장 · ${model.state.phase}")
                        if (!model.hasDistance) Text("거리 정보 없음 또는 누적 거리 오류. 오류 여정은 취소 후 새로 시작하세요.")
                        when (model.state.phase) {
                            Phase.IDLE, Phase.COMPLETE -> {
                                if (model.state.phase == Phase.COMPLETE) Text("부화 완료! 새로운 공룡 카드를 얻었어요.")
                                Button(onClick = model::start, enabled = model.hasDistance) { Text("새 탐험 시작") }
                            }
                            Phase.PAUSED -> Button(onClick = model::resume) { Text("보호자 확인 · 이어하기") }
                            Phase.ACTIVE -> {
                                Button(onClick = model::hatch, enabled = model.readyToHatch) { Text("공룡 알 부화하기") }
                                OutlinedButton(onClick = model::pause) { Text("저장하고 쉬기") }
                            }
                        }
                        if (model.state.phase != Phase.IDLE) TextButton(onClick = model::cancel) { Text("현재 탐험 취소 · 카드는 유지") }
                    }
                }
            }
            HorizontalDivider()
            Column(Modifier.fillMaxWidth().background(Color(0xFF28352C)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("개발자 모의 입력", style = MaterialTheme.typography.titleMedium)
                Text("아래 버튼은 차량을 제어하지 않습니다. 실제 배포판에 포함하면 안 됩니다.")
                Text("제한 상태: ${model.gate}")
                OutlinedButton(onClick = { model.simulateGate(Gate.AVAILABLE) }) { Text("주차·이용 가능 가정") }
                OutlinedButton(onClick = { model.simulateGate(Gate.RESTRICTED) }) { Text("주행·이용 제한 가정") }
                OutlinedButton(onClick = { model.simulateGate(Gate.UNKNOWN) }) { Text("연결 끊김 가정") }
                OutlinedButton(onClick = model::addDistance) { Text("모의 누적 거리 +1 km") }
                OutlinedButton(onClick = model::missingDistance) { Text("거리 미지원 가정") }
            }
        }
    }
}
