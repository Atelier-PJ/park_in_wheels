package com.parkonwheels.demo

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.parkonwheels.core.VoiceChoiceParser
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parkonwheels.core.SafariEngine.Phase
import com.parkonwheels.core.VehicleGateway.Gate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Paper = Color(0xFFFFFCF5)
private val Navy = Color(0xFF24394A)
private val Muted = Color(0xFF67716F)
private val Line = Color(0xFFE9E7DF)
private val Green = Color(0xFF27775A)
private val Shape = RoundedCornerShape(28.dp)

class MainActivity : ComponentActivity() {
    private val model: SafariModel by viewModels()
    private lateinit var voice: VoiceDirector
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        voice = VoiceDirector(this) { model.canPlay }
        setContent {
            ParkTheme {
                ParkApp(model, voice)
            }
        }
    }
    override fun onStart() { super.onStart(); model.foreground() }
    override fun onStop() { voice.stop(); model.background(); super.onStop() }
    override fun onDestroy() { voice.close(); super.onDestroy() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ParkApp(model: SafariModel, voice: VoiceDirector) {
    var page by rememberSaveable { mutableStateOf(if (model.inJourney) "journey" else "home") }
    var parentGate by remember { mutableStateOf(false) }
    var answer by remember { mutableStateOf("") }
    var cancelDialog by remember { mutableStateOf(false) }
    LaunchedEffect(model.state.phase) { if (model.state.phase == Phase.ACTIVE || model.state.phase == Phase.COMPLETE) page = "journey" }
    LaunchedEffect(page, model.canPlay) { if(page != "journey" || !model.canPlay) voice.stop() }
    Surface(Modifier.fillMaxSize(), color = Paper) {
        Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 26.dp, vertical = 18.dp)) {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.widthIn(min=230.dp)) {
                    Text("WONDER WHEELS", fontSize = 10.sp, letterSpacing = 3.sp, color = Green, fontWeight = FontWeight.Bold)
                    Text("PARK ON WHEELS", fontSize = 23.sp, fontWeight = FontWeight.Black, color = Navy, letterSpacing = (-.7).sp)
                }
                Row(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFF0F1E9)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    NavPill("탐험", page == "home" || page == "ticket" || page == "journey", model.canPlay) { page = if(model.inJourney) "journey" else "home" }
                    NavPill("패스포트", page == "passport", model.canPlay) { page = "passport" }
                    NavPill("보호자", page == "parents", model.canPlay || model.isSimulation) { answer = ""; parentGate = true }
                }
            }
            Spacer(Modifier.height(15.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Badge(if(model.isSimulation) "DEMO · 모의 차량" else "PLEOS · 차량 연결", Color(0xFFE9EEE7), Green)
                Text(if(model.canPlay) if(model.isSimulation) "모의 주차 · 체험 가능" else "기어 P 확인 · 주차 중 체험" else "안전 상태 확인 중", fontSize = 12.sp, color = Muted)
            }
            Spacer(Modifier.height(18.dp))
            Box(Modifier.weight(1f)) {
                if (!model.canPlay && page != "parents") SafetyScreen(model) { cancelDialog = true }
                else when(page) {
                    "home" -> WorldSelection(model, onChoose = { model.chooseTheme(it); page = "ticket" }, onContinue = { page = "journey" })
                    "ticket" -> TicketScreen(model, onBack = { page = "home" }, onStart = { model.start() })
                    "journey" -> JourneyScreen(model, voice, onHome = { page = "home" }, onPassport = { page = "passport" }, onCancel = { cancelDialog = true })
                    "passport" -> PassportScreen(model, onExplore = { page = if(model.inJourney) "journey" else "home" })
                    "parents" -> ParentScreen(model)
                }
            }
        }
    }
    if(parentGate) AlertDialog(onDismissRequest={parentGate=false}, title={Text("보호자 전용 공간")},
        text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){Text("설정은 보호자와 함께해요.\n8 + 6의 답을 입력해 주세요.");OutlinedTextField(value=answer,onValueChange={answer=it.take(2)},singleLine=true,label={Text("정답")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))}},
        confirmButton={TextButton(onClick={if(answer=="14"){parentGate=false;page="parents"}},enabled=answer=="14"){Text("보호자 확인")}}, dismissButton={TextButton(onClick={parentGate=false}){Text("돌아가기")}})
    if(cancelDialog) AlertDialog(onDismissRequest={cancelDialog=false},title={Text("이번 탐험을 마칠까요?")},text={Text("진행 중인 미션은 초기화돼요. 패스포트에 모은 스탬프는 남아 있어요.")},confirmButton={TextButton(onClick={model.cancel();cancelDialog=false;page="home"}){Text("탐험 취소")}},dismissButton={TextButton(onClick={cancelDialog=false}){Text("계속할래요")}})
}

@Composable private fun NavPill(label:String, selected:Boolean, enabled:Boolean=true, click:()->Unit) {
    Surface(shape=RoundedCornerShape(50),color=if(selected)Navy else Color.Transparent) {
        Box(Modifier.clickable(enabled=enabled,role=Role.Tab,onClick=click).heightIn(min=48.dp).padding(horizontal=17.dp),contentAlignment=Alignment.Center){Text(label,fontSize=14.sp,fontWeight=FontWeight.Bold,color=if(selected)Color.White else Navy)}
    }
}
@Composable private fun Badge(text:String, bg:Color, fg:Color) {
    Text(text,Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal=12.dp,vertical=7.dp),color=fg,fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=.4.sp)
}
@Composable private fun SectionLabel(text:String) { Text(text,color=Green,fontSize=11.sp,letterSpacing=2.sp,fontWeight=FontWeight.Bold) }
@Composable private fun Action(text:String, color:Color=Green, enabled:Boolean=true, modifier:Modifier=Modifier, onClick:()->Unit) {
    Button(onClick=onClick,enabled=enabled,modifier=modifier.heightIn(min=58.dp),shape=RoundedCornerShape(20.dp),colors=ButtonDefaults.buttonColors(containerColor=color),contentPadding=PaddingValues(horizontal=24.dp,vertical=15.dp)) { Text(text,fontSize=17.sp,fontWeight=FontWeight.Bold) }
}

@Composable private fun WorldSelection(model:SafariModel,onChoose:(String)->Unit,onContinue:()->Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val columns = if (maxWidth < 620.dp) 1 else 2
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionLabel("YOUR LITTLE ADVENTURE")
        Text("오늘은 어디로 떠나볼까?",fontSize=32.sp,lineHeight=42.sp,fontWeight=FontWeight.ExtraBold,letterSpacing=(-1).sp)
        Text("우리 차 안에서 시작되는 네 가지 작은 세계",fontSize=15.sp,color=Muted,modifier=Modifier.padding(top=5.dp,bottom=20.dp))
        if(model.inJourney) {
            Surface(color=model.theme.pale,shape=Shape,modifier=Modifier.fillMaxWidth().padding(bottom=18.dp)) {
                Row(Modifier.padding(20.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("아직 끝나지 않은 모험이 있어요",fontWeight=FontWeight.Bold);Text("${model.theme.name} · ${model.role}",color=Muted,fontSize=13.sp)};Action("이어서 탐험",onClick=onContinue)}
            }
        }
        for (row in adventureThemes.chunked(columns)) {
            Row(Modifier.fillMaxWidth().padding(bottom=16.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                row.forEachIndexed { index, theme ->
                    Surface(color=theme.pale,shape=Shape,modifier=Modifier.weight(1f).heightIn(min=214.dp).clickable(enabled=!model.inJourney,role=Role.Button,onClick={onChoose(theme.id)}).semantics { contentDescription="${theme.name}, ${theme.subtitle}, 테마 선택" }) {
                        Box(Modifier.fillMaxWidth().height(214.dp)) {
                            ThemeArtwork(theme.id,Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(.60f))
                            Column(Modifier.align(Alignment.CenterStart).padding(22.dp).fillMaxWidth(.57f)) {
                                Text("0${adventureThemes.indexOf(theme)+1}  /  EXPLORE",fontSize=9.sp,letterSpacing=1.sp,fontWeight=FontWeight.Bold,color=theme.accent)
                                Spacer(Modifier.height(13.dp));Text(theme.name,fontSize=23.sp,lineHeight=30.sp,fontWeight=FontWeight.ExtraBold,color=Navy)
                                Text(theme.subtitle,fontSize=12.sp,lineHeight=18.sp,color=Muted,modifier=Modifier.padding(top=8.dp))
                                Text("탐험하기  →",fontSize=13.sp,fontWeight=FontWeight.Bold,color=theme.accent,modifier=Modifier.padding(top=16.dp))
                            }
                        }
                    }
                }
            }
        }
        Text("모든 이야기는 앱에 담겨 있어요. 인터넷 없이, 주차 중에 함께해요.",color=Muted,fontSize=12.sp,modifier=Modifier.padding(top=3.dp))
    }
}
}

@Composable internal fun TicketScreen(model:SafariModel,onBack:()->Unit,onStart:()->Unit) {
    val t=model.theme
    AdaptivePanels(left={
        IllustratedPanel(model,"BOARDING PASS",t.name,"에피소드 ${model.episode} · ${t.chapterTitle(model.episode)}")
    },right={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            TextButton(onClick=onBack){Text("← 다른 세계 보기",color=Muted)}
            Text("탐험 준비, 됐나요?",fontSize=28.sp,fontWeight=FontWeight.ExtraBold)
            Text("${t.companion}와 함께할 오늘의 역할을 골라요.",color=Muted,fontSize=14.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(t.role,"꼬마 길잡이").forEach{role->FilterChip(selected=model.role==role,onClick={model.chooseRole(role)},label={Text(role)},modifier=Modifier.heightIn(min=52.dp))}}
            SectionLabel("HOW WE EXPLORE")
            ModeChoice("주차 중 짧은 탐험","세 가지 미션을 마치면 스탬프를 받아요.",model.mode==ExperienceMode.PARKED,t.accent){model.chooseMode(ExperienceMode.PARKED)}
            ModeChoice("이동 기록과 함께 탐험","실제 5km 기록 + 주차 미션. 내비 도착과 별개예요.",model.mode==ExperienceMode.DISTANCE,t.accent){model.chooseMode(ExperienceMode.DISTANCE)}
            Surface(color=Color.White,shape=RoundedCornerShape(18.dp),border=androidx.compose.foundation.BorderStroke(1.dp,Line)) {
                Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("나의 여행 탑승권",fontSize=11.sp,color=Muted)
                    Text("${t.english}  /  EP.${model.episode.toString().padStart(2,'0')}",fontWeight=FontWeight.Bold,fontSize=15.sp,color=t.accent)
                    Text("${model.role}  ·  ${t.companion}와 함께",fontSize=14.sp)
                }
            }
            if(!model.hasDistance) Text("차량의 거리 정보를 확인 중이에요. 확인 후 출발할 수 있어요.",color=Muted,fontSize=13.sp)
            Action("탑승권 받고 시작!  →",t.accent,model.hasDistance,Modifier.fillMaxWidth(),onStart)
            Text("주행 중에는 놀이·음성을 쉬어요. 음성과 테마 조명은 주차 중 지원 범위에서 사용할 수 있어요.",fontSize=12.sp,color=Muted,lineHeight=18.sp)
        }
    })
}
@Composable private fun ModeChoice(title:String,description:String,selected:Boolean,accent:Color,click:()->Unit){
    Surface(shape=RoundedCornerShape(18.dp),color=if(selected)accent.copy(alpha=.08f)else Color.White,border=androidx.compose.foundation.BorderStroke(if(selected)2.dp else 1.dp,if(selected)accent else Line),modifier=Modifier.fillMaxWidth().clickable(role=Role.RadioButton,onClick=click)){
        Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
            Box(Modifier.size(22.dp).border(2.dp,if(selected)accent else Line,CircleShape),contentAlignment=Alignment.Center){if(selected)Box(Modifier.size(10.dp).background(accent,CircleShape))}
            Column{Text(title,fontWeight=FontWeight.Bold,fontSize=15.sp);Text(description,fontSize=12.sp,color=Muted,lineHeight=18.sp,modifier=Modifier.padding(top=4.dp))}
        }
    }
}
@Composable private fun AdaptivePanels(left:@Composable ()->Unit,right:@Composable ()->Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if(maxWidth>=720.dp) Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(24.dp)){
            Box(Modifier.weight(.47f).fillMaxHeight()){left()};Box(Modifier.weight(.53f).fillMaxHeight()){right()}
        }else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(20.dp)){
            Box(Modifier.fillMaxWidth().height(310.dp)){left()};Box(Modifier.fillMaxWidth().heightIn(min=550.dp, max=800.dp)){right()}
        }
    }
}
@Composable private fun IllustratedPanel(model:SafariModel,kicker:String,title:String,subtitle:String,celebration:Boolean=false) {
    val t=model.theme
    Surface(color=t.pale,shape=Shape,modifier=Modifier.fillMaxSize()) {
        Column(Modifier.padding(24.dp)) {
            Text(kicker,fontSize=10.sp,letterSpacing=2.sp,fontWeight=FontWeight.Bold,color=t.accent)
            Text(title,fontSize=30.sp,fontWeight=FontWeight.ExtraBold,lineHeight=38.sp,modifier=Modifier.padding(top=12.dp))
            Text(subtitle,fontSize=14.sp,color=Muted,lineHeight=21.sp,modifier=Modifier.padding(top=6.dp))
            ThemeArtwork(t.id,Modifier.weight(1f).fillMaxWidth().semantics{contentDescription="${t.name}의 친구 ${t.companion} 일러스트"},celebration && !model.reducedMotion)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Badge("OFFLINE READY",Color.White.copy(alpha=.75f),t.accent);Text("WONDER / 0${adventureThemes.indexOf(t)+1}",fontSize=10.sp,color=t.accent,modifier=Modifier.align(Alignment.CenterVertically))}
        }
    }
}

@Composable private fun JourneyScreen(model:SafariModel,voice:VoiceDirector,onHome:()->Unit,onPassport:()->Unit,onCancel:()->Unit) {
    val t=model.theme;val complete=model.state.phase==Phase.COMPLETE
    AdaptivePanels(left={IllustratedPanel(model,if(complete)"A LITTLE WONDER, FOUND"else"OUR ADVENTURE",if(complete)"${t.reward} 발견!"else t.name,if(complete)t.ending else "EP.${model.episode} · ${t.chapterTitle(model.episode)}",complete)},right={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(17.dp)) {
            if(complete) {
                SectionLabel("PASSPORT STAMP COLLECTED")
                Text("우리의 모험을\n기억할 선물이야!",fontSize=31.sp,lineHeight=42.sp,fontWeight=FontWeight.ExtraBold)
                Text(t.ending,color=Muted,fontSize=16.sp,lineHeight=25.sp)
                Surface(color=t.pale,shape=Shape,modifier=Modifier.fillMaxWidth()) {Column(Modifier.padding(24.dp)){Text("${t.reward} 스탬프",fontSize=22.sp,fontWeight=FontWeight.Bold,color=t.accent);Text("여행 패스포트에 저장했어요",fontSize=14.sp,color=Muted,modifier=Modifier.padding(top=8.dp))}}
                Action("내 패스포트 보기  →",t.accent,modifier=Modifier.fillMaxWidth(),onClick=onPassport)
                OutlinedButton(onClick=onHome,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)){Text("다음 에피소드 고르기")}
            } else {
                SectionLabel(if(model.mode==ExperienceMode.PARKED)"PARKED LITTLE ADVENTURE"else"JOURNEY WITH REAL DISTANCE")
                Text(if(model.state.phase==Phase.PAUSED)"잠깐 쉬어도 괜찮아"else if(model.missionStep==3)"마지막 마법을 완성해요"else"함께 해볼까?",fontSize=29.sp,fontWeight=FontWeight.ExtraBold)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("미션 ${model.missionStep} / 3",color=t.accent,fontSize=13.sp,fontWeight=FontWeight.Bold);Text(if(model.mode==ExperienceMode.PARKED)"주차 체험"else String.format(Locale.KOREA,"실제 이동 %.1f / 5.0 km",model.state.travelledKm),fontSize=12.sp,color=Muted)}
                LinearProgressIndicator(progress={model.progress},modifier=Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),color=t.accent,trackColor=t.pale)
                VoicePanel(model, voice)
                when {
                    model.state.phase==Phase.PAUSED -> {
                        Text("이야기는 여기 그대로 있어요.\n보호자가 확인한 뒤 다시 만나요.",fontSize=17.sp,lineHeight=28.sp,color=Muted)
                        Action("보호자 확인 · 이어하기",t.accent,modifier=Modifier.fillMaxWidth(),onClick=model::resume)
                    }
                    model.feedback!=null -> {
                        Surface(color=t.pale,shape=Shape,modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Badge("참 잘했어요!",Color.White,t.accent);Text(model.feedback!!,fontSize=23.sp,lineHeight=34.sp,fontWeight=FontWeight.Bold)}}
                        Action(if(model.missionStep==3)"마지막 장면으로  →"else"다음 이야기  →",t.accent,modifier=Modifier.fillMaxWidth(),onClick=model::nextMission)
                    }
                    model.missionStep<3 -> {
                        val mission=model.missions[model.missionStep]
                        Text("0${model.missionStep+1}  ${mission.title}",fontSize=21.sp,fontWeight=FontWeight.Bold)
                        Text(mission.prompt,fontSize=17.sp,lineHeight=27.sp,color=Muted)
                        mission.choices.forEachIndexed{index,choice->
                            OutlinedButton(onClick={model.answer(index)},modifier=Modifier.fillMaxWidth().heightIn(min=68.dp),shape=RoundedCornerShape(20.dp),border=androidx.compose.foundation.BorderStroke(1.5.dp,t.accent.copy(alpha=.35f)),colors=ButtonDefaults.outlinedButtonColors(contentColor=Navy,containerColor=Color.White)){
                                Text(choice,Modifier.weight(1f),fontSize=18.sp,fontWeight=FontWeight.Bold);Text("→",fontSize=21.sp,color=t.accent)
                            }
                        }
                    }
                    else -> {
                        Text(if(model.canFinish)"세 가지 마음을 모았어요.\n이제 ${t.reward}을 만나 볼까요?"else"미션은 모두 마쳤어요!\n실제 이동 기록이 5km가 되면, 다시 주차한 뒤 마지막 장면을 열 수 있어요.",fontSize=18.sp,lineHeight=29.sp,color=Muted)
                        Action(when(t.id){"dino"->"톡톡, 공룡 알 깨우기!";"magic"->"별빛 성문 열기!";"space"->"새로운 행성 발견!";else->"반짝, 보물 조개 열기!"},t.accent,model.canFinish,Modifier.fillMaxWidth(),model::hatch)
                    }
                }
                if(model.state.phase==Phase.ACTIVE) TextButton(onClick=model::pause,modifier=Modifier.heightIn(min=52.dp)){Text("잠깐 쉬기 · 자동 저장",color=Muted,fontSize=14.sp)}
                TextButton(onClick=onCancel,modifier=Modifier.heightIn(min=48.dp)){Text("이번 탐험 취소",color=Muted,fontSize=12.sp)}
            }
        }
    })
}

@Composable private fun PassportScreen(model:SafariModel,onExplore:()->Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val columns = if (maxWidth < 620.dp) 1 else 2
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        SectionLabel("OUR FAMILY PASSPORT")
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("작은 발견, 오래 남는 추억",fontSize=30.sp,fontWeight=FontWeight.ExtraBold);Text("우리가 만난 세계를 한 장씩 모아 봐요.",fontSize=14.sp,color=Muted,modifier=Modifier.padding(top=7.dp))};Badge("${model.stamps.size} STAMPS",Navy,Color.White)}
        for(row in adventureThemes.chunked(columns)) Row(horizontalArrangement=Arrangement.spacedBy(16.dp)){
            row.forEach{t->val count=model.stamps.count{it.themeId==t.id}
                Surface(color=if(count>0)t.pale else Color(0xFFF0F0E9),shape=Shape,modifier=Modifier.weight(1f)){
                    Row(Modifier.padding(18.dp).heightIn(min=140.dp),verticalAlignment=Alignment.CenterVertically){ThemeArtwork(t.id,Modifier.size(130.dp),count>0);Column(Modifier.weight(1f)){Text(t.name,fontSize=18.sp,fontWeight=FontWeight.Bold);Text(if(count>0)"${t.reward} · ${count}개"else"첫 스탬프를 기다려요",color=if(count>0)t.accent else Muted,fontSize=13.sp,modifier=Modifier.padding(top=8.dp));Text(if(count>0)"다음은 에피소드 ${count+1}"else"탐험을 마치면 여기에!",fontSize=11.sp,color=Muted,modifier=Modifier.padding(top=10.dp))}}
                }
            }
        }
        if(model.stamps.isNotEmpty()) {
            Text("우리의 여행 기록",fontSize=18.sp,fontWeight=FontWeight.Bold)
            model.stamps.asReversed().forEach{stamp->val t=adventureThemes.firstOrNull{it.id==stamp.themeId}?:adventureThemes.first()
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(17.dp),horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text("${t.reward} · EP.${stamp.episode}",fontWeight=FontWeight.Bold);Text(stamp.role,color=Muted,fontSize=12.sp)};Text(if(stamp.earnedAt==0L)"이전 기록"else SimpleDateFormat("yyyy.MM.dd",Locale.KOREA).format(Date(stamp.earnedAt)),fontSize=12.sp,color=Muted)}
            }
        }
        Action(if(model.inJourney)"진행 중인 탐험으로  →"else"새로운 모험 만나기  →",modifier=Modifier.fillMaxWidth(),onClick=onExplore)
    }
}
}

@Composable private fun SafetyScreen(model:SafariModel, onReset:()->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
        Badge("PAUSE FOR SAFETY",Color(0xFFE6EEE7),Green)
        Text("모험은 잠깐, 쉬어가요",fontSize=30.sp,fontWeight=FontWeight.ExtraBold,modifier=Modifier.padding(top=24.dp),textAlign=TextAlign.Center)
        Text(if(model.error!=null)model.error!! else if(model.gate==Gate.RESTRICTED)"차가 안전하게 주차되면 다시 만나요.\n지금까지의 이야기는 저장되어 있어요."else"차량의 주차 상태와 연결을 확인하고 있어요.\n확인되기 전에는 놀이를 시작하지 않아요.",fontSize=17.sp,lineHeight=28.sp,color=Muted,textAlign=TextAlign.Center,modifier=Modifier.padding(top=17.dp))
        if(model.canResetJourney && !model.hasDistance) {
            Spacer(Modifier.height(20.dp))
            OutlinedButton(onClick=onReset,modifier=Modifier.heightIn(min=56.dp)){Text("보호자: 거리 오류 여정 초기화")}
        }
        if(model.isSimulation) {
            Spacer(Modifier.height(26.dp));Action("데모: 주차 상태로 전환",onClick={model.simulateGate(Gate.AVAILABLE)})
            Text("실제 차량 상태가 아닌 모의 입력입니다",fontSize=12.sp,color=Muted,modifier=Modifier.padding(top=10.dp))
        }
    }
}

@Composable private fun ParentScreen(model:SafariModel) {
    var showDiagnostics by remember{mutableStateOf(false)}
    var erase by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(17.dp)) {
        SectionLabel("FOR THE GROWN-UPS")
        Text("안심하고 함께하는 탐험",fontSize=29.sp,fontWeight=FontWeight.ExtraBold)
        ParentNote("콘텐츠 준비 완료","네 가지 테마의 그림·미션·엔딩이 앱 안에 있어요. 별도 다운로드나 로그인 없이 사용할 수 있습니다.")
        ParentNote("주차 중에만 만나요","Pleos UX 제한, 기어 P, 최신 거리값을 함께 확인합니다. 신호 대기나 속도 0을 주차로 간주하지 않습니다. 상태가 불명확하면 차단하고 자동으로 재개하지 않습니다.")
        ParentNote("현재 지원 범위","주차 중 한국어 오프라인 안내와 기기 내 음성 선택을 지원합니다. 음성 엔진이 없는 기기는 터치로 진행합니다. 내비 도착·실제 충전은 아직 미연동이며 이동 기록 탐험은 별도의 5km 기준입니다.")
        ParentNote("음성과 주행 제한","현재 공개 Pleos 규칙에 따라 주행 중에는 게임과 음성 상호작용을 중단합니다. 화면이 닫히거나 다른 차량 안내가 오면 듣기·말하기를 멈추고 자동으로 다시 시작하지 않습니다.")
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("테마 앰비언트 색상",fontWeight=FontWeight.Bold);Text("주차 중 지원 차량에만 은은한 고정 색 적용",fontSize=12.sp,color=Muted)};Switch(checked=model.ambientEnabled,onCheckedChange=model::changeAmbient,enabled=model.canPlay)}
        ParentNote("조명 연결 상태",model.ambientStatus + "\n밝기·점멸·외부 조명은 제어하지 않습니다. 주행 중 자동 색 변경은 중지됩니다. 앱이 갑자기 종료되면 색이 남을 수 있으며 차량에서 직접 바꿀 수 있어요.")
        if(model.ambientEnabled) OutlinedButton(onClick=model::retryAmbient,enabled=model.canPlay,modifier=Modifier.heightIn(min=52.dp)){Text("테마 색 다시 적용")}
        ParentNote("기록과 개인정보","테마·역할·미션·스탬프를 기기에만 저장합니다. 음성 선택 시에만 마이크를 잠깐 사용합니다. 원본 음성을 저장하거나 서버로 보내지 않으며, 위치 정보도 수집하지 않습니다.")
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("차분한 화면",fontWeight=FontWeight.Bold);Text("엔딩의 반짝이 장식을 줄여요",fontSize=12.sp,color=Muted)};Switch(checked=model.reducedMotion,onCheckedChange=model::changeReducedMotion,enabled=model.canPlay)}
        OutlinedButton(onClick={showDiagnostics=!showDiagnostics},modifier=Modifier.heightIn(min=52.dp)){Text(if(showDiagnostics)"연결 정보 접기"else"차량 연결·개발 정보 보기")}
        if(showDiagnostics) {
            ParentNote(model.modeLabel,"${model.diagnostic}\nUX: ${model.gate} · 거리 준비: ${model.hasDistance}\n테스트 전용 빌드 · 실제 차량 배포 전 추가 검증 필요")
            if(model.isSimulation) {
                Text("개발자 모의 입력 · 차량을 제어하지 않아요",fontWeight=FontWeight.Bold)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={model.simulateGate(Gate.AVAILABLE)}){Text("주차")};OutlinedButton(onClick={model.simulateGate(Gate.RESTRICTED)}){Text("주행 제한")};OutlinedButton(onClick={model.simulateGate(Gate.UNKNOWN)}){Text("연결 끊김")}}
                OutlinedButton(onClick=model::addDistance){Text("모의 누적 거리 +1km")}
                OutlinedButton(onClick=model::missingDistance){Text("거리 미지원")}
            }
        }
        TextButton(onClick={erase=true},enabled=model.canPlay,modifier=Modifier.heightIn(min=52.dp)){Text("이 기기의 탐험·패스포트 기록 삭제",color=Color(0xFFAA4F48))}
    }
    if(erase)AlertDialog(onDismissRequest={erase=false},title={Text("탐험 기록을 삭제할까요?")},text={Text("진행 중인 탐험과 모든 스탬프가 이 기기에서 삭제됩니다. 되돌릴 수 없어요.")},confirmButton={TextButton(onClick={model.erasePassport();erase=false}){Text("모든 기록 삭제")}},dismissButton={TextButton(onClick={erase=false}){Text("취소")}})
}
@Composable private fun ParentNote(title:String,body:String){Surface(shape=RoundedCornerShape(20.dp),color=Color.White,border=androidx.compose.foundation.BorderStroke(1.dp,Line),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(title,fontSize=17.sp,fontWeight=FontWeight.Bold);Text(body,fontSize=14.sp,lineHeight=23.sp,color=Muted)}}}

@Composable private fun VoicePanel(model: SafariModel, voice: VoiceDirector) {
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if(granted && model.canPlay) voice.start(true)
        else voice.stop(if(granted) "주차 상태를 다시 확인한 뒤 시작해 주세요" else "마이크가 허용되지 않았어요. 손으로 골라도 괜찮아요.")
    }
    LaunchedEffect(voice.enabled, model.themeId, model.state.phase, model.missionStep, model.feedback, model.canFinish) {
        if (!voice.enabled || !model.canPlay) return@LaunchedEffect
        val paused = model.state.phase == Phase.PAUSED
        val options = if(!paused && model.feedback == null && model.missionStep < 3) model.missions[model.missionStep].choices else emptyList()
        val prompt = when {
            paused -> "이야기를 계속하려면 보호자가 확인한 뒤 이어하기라고 말해 주세요."
            model.feedback != null -> "${model.feedback} 다음이라고 말하면 이야기를 계속해요."
            model.missionStep < 3 -> "${model.theme.companion}와 함께해요. ${model.missions[model.missionStep].prompt}"
            model.canFinish -> "미션을 모두 마쳤어요. 완성, 또는 깨어나라라고 말해 주세요."
            else -> "미션은 모두 마쳤어요. 실제 이동 기록이 오 킬로미터가 된 뒤 안전하게 주차하면 마지막 장면을 열 수 있어요."
        }
        voice.present(prompt, options) { command ->
            if(!model.canPlay) { voice.stop(); return@present }
            when {
                command >= 0 && command in options.indices -> model.answer(command)
                command == VoiceChoiceParser.NEXT && paused -> model.resume()
                command == VoiceChoiceParser.NEXT && model.feedback != null -> model.nextMission()
                command == VoiceChoiceParser.FINISH && model.canFinish -> { voice.stop(); model.hatch() }
                else -> voice.stop("지금은 그 선택을 할 수 없어요. 화면의 선택지로 다시 골라 주세요.")
            }
        }
    }
    DisposableEffect(Unit) { onDispose { voice.stop() } }
    Surface(color=Color(0xFFF0F1EA),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(if(voice.ready)voice.status else "이 기기에서는 손으로 선택해 주세요. 음성 지원 상태는 보호자와 확인할 수 있어요.",fontSize=12.sp,color=Muted,lineHeight=18.sp)
            if(voice.ready) Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                if(voice.enabled) OutlinedButton(onClick={voice.stop()},modifier=Modifier.heightIn(min=48.dp)){Text("음성 멈추기")}
                else {
                    OutlinedButton(onClick={voice.start(false)},enabled=voice.ready,modifier=Modifier.heightIn(min=48.dp)){Text("선택지 듣기")}
                    OutlinedButton(onClick={
                        if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED) voice.start(true)
                        else permission.launch(Manifest.permission.RECORD_AUDIO)
                    },enabled=voice.ready && voice.onDeviceAvailable,modifier=Modifier.heightIn(min=48.dp)){Text("말로 선택하기")}
                }
            }
        }
    }
}

@Composable internal fun ParkTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Green, onPrimary = Color.White,
                background = Paper, surface = Paper, onSurface = Navy, secondary = Navy,
                secondaryContainer = Color(0xFFE5EEE2), onSecondaryContainer = Navy,
                surfaceContainer = Paper, surfaceContainerHigh = Paper, surfaceContainerHighest = Color(0xFFF1F2EC),
                surfaceTint = Color.Transparent, surfaceVariant = Color(0xFFEBEEE7), onSurfaceVariant = Muted), typography = Typography()) {
        content()
    }
}
