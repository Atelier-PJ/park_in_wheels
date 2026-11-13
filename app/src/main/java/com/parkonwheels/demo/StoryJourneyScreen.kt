package com.parkonwheels.demo

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parkonwheels.core.SafariEngine.Phase
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Explicitly started, parked-only story playback. No touch-choice quiz. */
@Composable
internal fun StoryJourneyScreen(model:SafariModel,voice:VoiceDirector,onPassport:()->Unit,onCancel:()->Unit,autoStart:Boolean=false,onStartConsumed:()->Unit={}) {
    val t=model.theme
    val context=LocalContext.current
    var playing by remember { mutableStateOf(false) }
    var usedVoice by remember { mutableStateOf(false) }
    var microphone by remember { mutableStateOf(false) }
    val complete=model.state.phase==Phase.COMPLETE
    val phrase=when(t.id){"magic"->"별빛을 밝혀라";"space"->"새로운 행성 안녕";"ocean"->"반짝 보물아 나와라";else->"깨어나라 작은 친구"}
    var caption by remember(t.id) { mutableStateOf(if(model.missionStep>=3)"“$phrase!” 하고 말해 주세요" else "${t.companion}${withAnd(t.companion)} 이야기 속으로 들어가요.") }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->microphone=granted;playing=true}
    val stampScale by animateFloatAsState(if(complete)1f else .35f,animationSpec=spring(dampingRatio=.48f,stiffness=270f),label="passport stamp")
    fun begin() {
        if(!model.canPlay)return
        if(model.state.phase==Phase.PAUSED)model.resume()
        if(voice.ready && voice.onDeviceAvailable && context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)permission.launch(Manifest.permission.RECORD_AUDIO)
        else {microphone=voice.ready && voice.onDeviceAvailable && context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;playing=true}
    }
    LaunchedEffect(Unit) { if(autoStart){onStartConsumed();begin()} }
    suspend fun narrate(text:String,minimum:Long=6500) {
        caption=text
        val start=SystemClock.elapsedRealtime()
        if(voice.ready) {
            usedVoice=true
            suspendCancellableCoroutine<Unit>{continuation->
                voice.tell(text){if(continuation.isActive)continuation.resume(Unit)}
                continuation.invokeOnCancellation { voice.stop() }
            }
        }
        delay((minimum-(SystemClock.elapsedRealtime()-start)).coerceAtLeast(0))
    }
    LaunchedEffect(playing,model.canPlay) {
        if(!playing || !model.canPlay)return@LaunchedEffect
        if(model.state.phase!=Phase.ACTIVE)return@LaunchedEffect
        while(model.missionStep<3 && model.canPlay) {
            if(model.feedback!=null)model.nextMission()
            val mission=model.missions[model.missionStep]
            val line=when(t.id){
                "magic"->listOf("루미와 잃어버린 별 조각을 찾아요. 반짝이는 별을 마법 주머니에 모아 볼까요?","별들이 모여 따뜻한 마법빛을 만들어요. 저기, 성문이 살짝 반짝이네요!","성문 앞에 도착했어요. 우리 목소리로 별빛을 깨울 준비를 해요.")[model.missionStep]
                "space"->listOf("코코와 별빛 항로를 따라 출발해요. 창밖의 별들이 길을 알려 주네요!","상상 우주정거장에 도착했어요. 반짝이는 에너지를 채우고 다시 출발해요.","새로운 행성이 보여요. 코코와 함께 반가운 인사를 준비해요.")[model.missionStep]
                "ocean"->if(model.episode%2==0) listOf("버블과 산호 정원으로 가요. 바다 친구들이 우리를 기다리고 있어요.","어라, 모래 위에 종이가 떨어져 있네요. 집게로 조심히 주워 수거함에 담아 줄게요.","바다가 다시 깨끗해졌어요. 친구들이 준비한 보물 조개를 만나러 가요.")[model.missionStep] else listOf("버블과 바다 친구들을 만나러 가요. 바다거북이 길을 안내해 주네요.","산호 사이로 반짝이는 길이 이어져요. 천천히 따라가 볼까요?","반짝이는 조개를 찾았어요. 다정한 목소리로 보물을 깨울 준비를 해요.")[model.missionStep]
                else->listOf("토리와 작은 공룡 알을 찾았어요. 아삭한 풀잎과 달콤한 열매를 챙겨 줄게요.","알이 편히 쉴 수 있도록 포근한 풀잎 보금자리를 만들어요.","알 안에서 톡톡 소리가 나요. 작은 친구를 깨울 인사를 준비해요.")[model.missionStep]
            }
            narrate(line)
            if(!model.canPlay)return@LaunchedEffect
            model.answer(0)
            narrate(mission.response,if(t.id=="ocean" && model.episode%2==0 && model.missionStep==2)7200 else 2600)
            model.nextMission()
        }
        if(!model.canFinish){caption="이야기는 여기 저장돼 있어요. 이동 기록이 준비되면 주차 후 다시 만나요.";playing=false;voice.stop();return@LaunchedEffect}
        caption="“$phrase!” 하고 말해 주세요"
        if(microphone && voice.ready) {
            usedVoice=true
            voice.tell("마지막 장면을 열어 볼까요? $phrase! 하고 말해 주세요.",phrase){
                if(model.canFinish){voice.stop();model.completeRecognizedFinale();playing=false}
            }
        } else {
            narrate("마지막 장면을 열어 볼까요? $phrase! 하고 말해 주세요.",1500)
            caption="“$phrase!” 하고 말해 주세요"
            playing=false;voice.stop()
        }
    }
    LaunchedEffect(playing,model.canPlay,model.state.phase) {
        if(!model.canPlay || model.state.phase==Phase.PAUSED){playing=false;voice.stop()}
    }
    LaunchedEffect(playing,usedVoice) {
        while(playing && usedVoice){delay(300);if(!voice.enabled){playing=false;if(model.state.phase==Phase.ACTIVE)model.pause()}}
    }
    DisposableEffect(Unit){onDispose{voice.stop()}}
    val cleanup=t.id=="ocean" && model.episode%2==0 && !complete && ((model.missionStep==1 && model.feedback==null)||(model.missionStep==2 && model.feedback!=null))
    BoxWithConstraints(Modifier.fillMaxSize().background(t.pale)) {
        val compact=maxWidth<600.dp
        val captionSize=if(compact)26.sp else 32.sp
        val captionLine=if(compact)36.sp else 43.sp
        if(cleanup) OceanCleanupScene(model.feedback!=null,model.reducedMotion,Modifier.fillMaxSize())
        else DinoSafariGif(Modifier.fillMaxSize(),t.id,if(complete)3 else model.missionStep.coerceIn(0,2),crop=false,animate=!model.reducedMotion)
        Surface(color=if(complete)Color(0xFFFFFCF5).copy(alpha=.94f) else Color(0xFF172C32).copy(alpha=.65f),shape=RoundedCornerShape(22.dp),modifier=Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp)) {
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
                if(complete) Text("여행 패스포트",color=t.accent,fontSize=15.sp)
                if(complete) {
                    Surface(color=t.pale,shape=RoundedCornerShape(18.dp),modifier=Modifier.graphicsLayer{scaleX=if(model.reducedMotion)1f else stampScale;scaleY=if(model.reducedMotion)1f else stampScale}) {
                        Text("콩!  ${t.reward} 스탬프",Modifier.padding(20.dp),fontSize=27.sp,color=t.accent)
                    }
                    Text("${t.ending}  패스포트에 저장했어요.",fontSize=17.sp)
                    TextButton(onClick=onPassport){Text("내 여행 패스포트 펼치기")}
                } else {
                    Text(caption,fontSize=captionSize,lineHeight=captionLine,fontWeight=FontWeight.Bold,color=Color.White)
                    if(model.missionStep==3 && usedVoice) Text(voice.status,fontSize=13.sp,color=Color.White.copy(alpha=.85f))
                    if(model.missionStep>=3 && !playing) Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        if(voice.ready && voice.onDeviceAvailable) Action("주문 다시 말하기",onClick={begin()})
                        OutlinedButton(colors=ButtonDefaults.outlinedButtonColors(contentColor=Color.White),onClick={if(model.canFinish){voice.stop();model.hatch()}},enabled=model.canFinish){Text("말하기 어려우면 손으로 열기")}
                    }
                    if(!voice.ready && !playing) Text("이 기기에는 한국어 음성이 없어 자막으로 이야기해요.",fontSize=12.sp,color=Color.White.copy(alpha=.8f))
                    else if(!voice.onDeviceAvailable && model.missionStep==3 && !playing) Text(voice.recognitionStatus+" · 손으로 열 수도 있어요.",fontSize=12.sp,color=Color.White.copy(alpha=.8f))
                }
            }
        }
        if(!complete) Surface(onClick=onCancel,color=Color(0xFF172C32).copy(alpha=.6f),shape=RoundedCornerShape(50),modifier=Modifier.align(Alignment.TopEnd).padding(12.dp).heightIn(min=44.dp)) {
            Box(Modifier.padding(horizontal=16.dp,vertical=10.dp),contentAlignment=Alignment.Center){Text("탐험 끝내기",fontSize=14.sp,color=Color.White)}
        }
    }
}

/** Korean 와/과 particle: 과 after a final consonant (버블과), 와 otherwise (루미와). */
private fun withAnd(word:String):String { val c=word.lastOrNull()?:return "와"; return if(c in '가'..'힣' && (c-'가')%28!=0)"과" else "와" }
