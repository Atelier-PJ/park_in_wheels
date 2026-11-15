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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.parkonwheels.core.SafariEngine.Phase
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Explicitly started, parked-only story playback. No touch-choice quiz. */
@Composable
internal fun StoryJourneyScreen(model:SafariModel,voice:VoiceDirector,onPassport:()->Unit,onCancel:()->Unit,autoStart:Boolean=false,onStartConsumed:()->Unit={}) {
    val t=model.theme
    val context=LocalContext.current
    var energyCollected by remember { mutableStateOf(emptySet<Int>()) }
    val energyCount=energyCollected.size
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
            val line=if(t.id=="space" && model.chargingEpisode) chargingStationLines[model.missionStep] else when(t.id){
                "magic"->if(model.episode%2==0) listOf("루미에게 은빛 초대장이 왔어요! 달빛 정원에서 누가 우리를 기다릴까요?","정원에 도착했어요. 잠든 꽃 위로 달빛이 살며시 내려앉아요.","꽃잎 사이로 작은 빛이 모여요. 우리 목소리도 보태 볼까요?")[model.missionStep] else listOf("어라, 풀숲에 별 조각이 떨어졌어요! 루미와 마법 주머니에 담아 볼까요?","주머니 속 별들이 반짝반짝! 저 멀리 성문에도 작은 빛이 번져요.","꼭 닫힌 성문이에요. 루미가 속삭여요. 우리 목소리로 별빛을 깨워 볼까?")[model.missionStep]
                "space"->if(model.episode%2==0) listOf("코코가 편지를 쓰고 있어요. 먼 행성의 친구에게 우리 인사를 보내요!","별빛 우주선이 편지를 싣고 떠나요. 작은 별 사이로 쌩, 날아가요!","띵동, 답장이 왔어요! 창밖에 편지를 보낸 친구의 행성이 보여요.")[model.missionStep] else listOf("코코, 출발! 창밖의 별들이 반짝이며 우주선 앞에 길을 만들어요.","우주정거장에 사뿐 도착! 코코가 반짝이는 에너지를 우주선에 채워요.","저기, 처음 보는 행성이에요! 우리를 봤나 봐요. 작은 불빛이 깜빡여요.")[model.missionStep]
                "ocean"->if(model.episode%2==0) listOf("보글보글, 산호 정원에 도착했어요! 물고기들이 버블 주위를 빙글 돌아요.","어라, 모래 위에 종이가 있네요. 버블이 집게를 쏙 내밀어요. 살살 집어서 통에 쏙!","깨끗해진 정원에서 물고기들이 춤을 춰요. 저기, 조개 하나가 반짝여요!")[model.missionStep] else listOf("버블, 저기 봐! 바다거북이 지느러미를 흔들어요. 우리를 따라오라는 걸까요?","산호 사이로 반짝이는 길이 보여요. 버블과 살금살금 따라가 봐요.","커다란 조개를 찾았어요! 안에서 빛이 새어 나와요. 무엇이 숨어 있을까요?")[model.missionStep]
                else->if(model.episode%2==0) listOf("토리와 소풍 가는 날이에요! 가방에 열매를 담고 숲으로 출발해요.","어, 동글동글 발자국이에요! 나무 뒤에서 누가 빼꼼 고개를 내밀어요.","새 친구에게 풀잎 이불을 선물했어요. 작은 알에서도 톡톡, 인사가 들려요.")[model.missionStep] else listOf("토리야, 저기 알이 있어! 작은 친구를 기다리며 토리와 간식을 나눠 먹어요.","작은 알이 데굴, 굴러가려 해요! 폭신한 풀잎을 깔아 포근하게 감싸 줘요.","톡, 토독! 알 안에서 소리가 들려요. 토리도 귀를 쫑긋 세워요.")[model.missionStep]
            }
            narrate(line)
            if(t.id=="space" && model.chargingEpisode && model.missionStep==1) {
                while(energyCollected.size<3 && model.canPlay) delay(100)
            }
            if(!model.canPlay)return@LaunchedEffect
            model.answer(0)
            narrate(mission.response,if(t.id=="ocean" && model.episode%2==0 && model.missionStep==2)7200 else 2600)
            model.nextMission()
        }
        if(!model.canFinish){caption="이동 거리를 더 모으면 다음 이야기가 열려요. 주차한 뒤 다시 만나요.";playing=false;voice.stop();return@LaunchedEffect}
        caption="“$phrase!” 하고 말해 주세요"
        if(microphone && voice.ready) {
            usedVoice=true
            voice.tell("함께 외쳐 볼까요? $phrase!",phrase){
                if(model.canFinish){voice.stop();model.completeRecognizedFinale();playing=false}
            }
        } else {
            narrate("함께 외쳐 볼까요? $phrase!",1500)
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
        if(model.chargingEpisode && model.missionStep==1 && model.feedback==null) {
            Row(Modifier.align(Alignment.Center).padding(bottom=90.dp),horizontalArrangement=Arrangement.spacedBy(38.dp)) {
                repeat(3) { index ->
                    Button(onClick={energyCollected=energyCollected+index},enabled=index !in energyCollected,shape=androidx.compose.foundation.shape.CircleShape,contentPadding=PaddingValues(0.dp),modifier=Modifier.size(74.dp).semantics{contentDescription="에너지 조각 ${index+1}"},colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFFFE29A),contentColor=Color(0xFF59452E),disabledContainerColor=Color(0xFFB8D8DF),disabledContentColor=Color(0xFF24394A))) {
                        Text(if(index in energyCollected) "✓" else "✦",fontSize=38.sp)
                    }
                }
            }
        }
        Surface(color=if(complete)Color(0xFFFFFCF5).copy(alpha=.94f) else Color(0xFF172C32).copy(alpha=.65f),shape=RoundedCornerShape(22.dp),modifier=Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp)) {
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
                if(complete) Text("여행 패스포트",color=t.accent,fontSize=15.sp)
                if(complete) {
                    Surface(color=t.pale,shape=RoundedCornerShape(18.dp),modifier=Modifier.graphicsLayer{scaleX=if(model.reducedMotion)1f else stampScale;scaleY=if(model.reducedMotion)1f else stampScale}) {
                        Text("콩!  ${model.reward} 스탬프",Modifier.padding(20.dp),fontSize=27.sp,color=t.accent)
                    }
                    Text("${if(model.chargingEpisode) "우주정거장 정비를 마쳤어요!" else t.ending}  오늘의 모험을 패스포트에 담았어요.",fontSize=17.sp)
                    TextButton(onClick=onPassport){Text("내 여행 패스포트 펼치기")}
                } else {
                    Text(caption,fontSize=captionSize,lineHeight=captionLine,fontWeight=FontWeight.Bold,color=Color.White)
                    if(model.chargingEpisode && model.missionStep==1 && model.feedback==null) Text("코코에게 보낸 에너지 $energyCount / 3",fontSize=17.sp,color=Color(0xFFFFE29A))
                    if(model.missionStep==3 && usedVoice) Text(voice.status,fontSize=13.sp,color=Color.White.copy(alpha=.85f))
                    if(model.missionStep>=3 && !playing) Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        if(voice.ready && voice.onDeviceAvailable) Action("주문 다시 말하기",onClick={begin()})
                        OutlinedButton(modifier=Modifier.semantics{contentDescription="마지막 장면 열기"},colors=ButtonDefaults.outlinedButtonColors(contentColor=Color.White),onClick={if(model.canFinish){voice.stop();model.hatch()}},enabled=model.canFinish){Text("말하기 어려우면 손으로 열기")}
                    }
                    if(!voice.ready && !playing) Text("소리가 들리지 않으면 글을 보며 함께해요.",fontSize=12.sp,color=Color.White.copy(alpha=.8f))
                    else if(!voice.onDeviceAvailable && model.missionStep==3 && !playing) Text("목소리 대신 아래 버튼으로도 열 수 있어요.",fontSize=12.sp,color=Color.White.copy(alpha=.8f))
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
