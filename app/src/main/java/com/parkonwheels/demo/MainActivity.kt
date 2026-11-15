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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
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
private val SoftWhite = Color(0xFFFFFEFA)
private val Shape = RoundedCornerShape(28.dp)
private val TicketPaper = Color(0xFFFFF6DF)
private val TicketInk = Color(0xFF59452E)
private val TicketEdge = Color(0xFF957347)
private val TicketShape = RoundedCornerShape(13.dp)
private val StoryFont = androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(R.font.jua_regular))

class MainActivity : ComponentActivity() {
    private val model: SafariModel by viewModels {
        val dependencies=application as? AdventureDependencies
        if(dependencies==null) defaultViewModelProviderFactory else object:androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T:androidx.lifecycle.ViewModel> create(modelClass:Class<T>):T = dependencies.createModel(application) as T
        }
    }
    internal lateinit var voice: VoiceDirector
    private var detachPlaybackSafety:()->Unit={}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dependencies=application as? AdventureDependencies
        voice = if(dependencies==null) VoiceDirector(this) { model.canPlay }
            else VoiceDirector(this,dependencies.createSpeechInput(this)) { model.canPlay }
        detachPlaybackSafety=model.onPlaybackBlocked { voice.stop("주차 상태를 확인한 뒤 다시 시작해 주세요.") }
        setContent {
            ParkTheme {
                ParkApp(model, voice)
            }
        }
    }
    override fun onStart() { super.onStart(); model.foreground() }
    override fun onStop() { voice.stop(); model.background(); super.onStop() }
    override fun onDestroy() { detachPlaybackSafety(); voice.close(); super.onDestroy() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ParkApp(model: SafariModel, voice: VoiceDirector) {
    var page by rememberSaveable { mutableStateOf(if (model.inJourney) "ticket" else "entrance") }
    var storyRequested by remember { mutableStateOf(false) }
    var parentGate by remember { mutableStateOf(false) }
    var answer by remember { mutableStateOf("") }
    var visitEarned by remember { mutableStateOf(false) }
    var cancelDialog by remember { mutableStateOf(false) }
    LaunchedEffect(model.state.phase) {
        if (model.state.phase == Phase.ACTIVE || model.state.phase == Phase.COMPLETE) page = "journey"
        else if(model.state.phase == Phase.PAUSED && page == "journey") page = "ticket"
    }
    LaunchedEffect(page, model.canPlay, model.soundsEnabled) {
        if(page != "journey" || !model.canPlay) voice.stop()
        if(page == "ticket" && model.canPlay && model.soundsEnabled) voice.departure(model.themeId)
        voice.configureThemeSound(if(page == "journey")model.themeId else null, model.soundsEnabled)
    }
    Surface(Modifier.fillMaxSize(), color = Paper) {
        Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = if(page=="ticket" || page=="journey" || page=="destination") 0.dp else if(page=="home" || page=="entrance" || page=="journey" || page=="ticket" || page=="passport") 12.dp else 26.dp, vertical = if(page=="ticket" || page=="journey" || page=="destination") 0.dp else if(page=="home" || page=="entrance" || page=="journey" || page=="ticket" || page=="passport") 10.dp else 18.dp)) {
            if(page != "home" && page != "entrance" && page != "journey" && page != "ticket" && page != "destination" && page != "passport") {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.widthIn(min=230.dp)) {
                    Text("WONDER WHEELS", fontSize = 10.sp, letterSpacing = 3.sp, color = Green, fontWeight = FontWeight.Bold)
                    Text("PARK ON WHEELS", fontSize = 23.sp, fontWeight = FontWeight.Black, color = Navy, letterSpacing = (-.7).sp)
                }
                Row(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFF0F1E9)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    NavPill("탐험", page == "home" || page == "ticket" || page == "journey", model.canPlay) { page = if(model.inJourney) "ticket" else "home" }
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
            }
            if(model.arrivalAvailable && page in listOf("entrance","home","passport")) {
                Surface(color=TicketPaper,shape=TicketShape,modifier=Modifier.fillMaxWidth().padding(bottom=8.dp)) {
                    Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                        Text("여행지에 도착했어요!",modifier=Modifier.weight(1f),fontSize=20.sp,color=Navy)
                        TextButton(onClick={val before=model.visits.size;model.claimVisit();visitEarned=model.visits.size>before}) {Text("방문 스탬프 받기",fontSize=18.sp,color=Green)}
                    }
                }
            }
            Box(Modifier.weight(1f)) {
                if (!model.canPlay && page != "parents") SafetyScreen(model) { cancelDialog = true }
                else when(page) {
                    "entrance" -> ParkEntrance(model.inJourney,model.reducedMotion) { page = if(model.inJourney) "ticket" else "home" }
                    "home" -> WorldSelection(model, onChoose = { model.chooseTheme(it); page = "ticket" }, onContinue = { page = "ticket" })
                    "ticket" -> TicketScreen(model, onBack = { page = "home" }, onStart = { if(model.inJourney){storyRequested=true;model.resume();page="journey"} else page="destination" })
                    "destination" -> DestinationSelectionScreen(model,onBack={page="home"},onStart={storyRequested=true;model.start()})
                    "journey" -> StoryJourneyScreen(model, voice, autoStart=storyRequested,onStartConsumed={storyRequested=false},onPassport = { page = "passport" }, onCancel = { cancelDialog = true })
                    "passport" -> PassportScreen(model, voice, onExplore = { page = if(model.inJourney) "ticket" else "home" })
                    "parents" -> ParentScreen(model)
                }
            }
            if(page == "home" || page == "entrance") Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                if(page == "home" || page == "journey") TextButton(onClick={page="entrance"}){Text("← 공원 입구",fontSize=12.sp,color=Muted)}
                TextButton(onClick={page="passport"},enabled=model.canPlay){Text("여행 패스포트",fontSize=12.sp,color=Navy)}
                TextButton(onClick={answer="";parentGate=true},enabled=model.canPlay || model.isSimulation){Text("보호자",fontSize=12.sp,color=Muted)}
            }
        }
    }
    if(visitEarned) AlertDialog(onDismissRequest={visitEarned=false},title={Text("콩! 방문 스탬프")},text={Text("이번 여행의 도착 스탬프를 패스포트에 담았어요.")},confirmButton={TextButton(onClick={visitEarned=false;page="passport"}){Text("패스포트 보기")}})
    if(parentGate) AlertDialog(onDismissRequest={parentGate=false}, title={Text("보호자 전용 공간")},
        text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){Text("설정은 보호자와 함께해요.\n8 + 6의 답을 입력해 주세요.");OutlinedTextField(value=answer,onValueChange={answer=it.take(2)},singleLine=true,label={Text("정답")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))}},
        confirmButton={TextButton(onClick={if(answer=="14"){parentGate=false;page="parents"}},enabled=answer=="14"){Text("보호자 확인")}}, dismissButton={TextButton(onClick={parentGate=false}){Text("돌아가기")}})
    if(cancelDialog) AlertDialog(onDismissRequest={cancelDialog=false},title={Text("이번 탐험을 마칠까요?")},text={Text("진행 중인 미션은 초기화돼요. 패스포트에 모은 스탬프는 남아 있어요.")},confirmButton={TextButton(onClick={model.cancel();cancelDialog=false;page="home"}){Text("탐험 취소")}},dismissButton={TextButton(onClick={cancelDialog=false}){Text("계속할래요")}})
}

@Composable private fun NavPill(label:String, selected:Boolean, enabled:Boolean=true, click:()->Unit) {
    Surface(shape=RoundedCornerShape(50),color=if(selected)TicketPaper else Color.Transparent) {
        Box(Modifier.clickable(enabled=enabled,role=Role.Tab,onClick=click).heightIn(min=48.dp).padding(horizontal=17.dp),contentAlignment=Alignment.Center){Text(label,fontSize=14.sp,fontWeight=FontWeight.Bold,color=if(selected)TicketInk else Muted)}
    }
}
@Composable private fun Badge(text:String, bg:Color, fg:Color) {
    Text(text,Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal=12.dp,vertical=7.dp),color=fg,fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=.4.sp)
}
@Composable private fun SectionLabel(text:String) { Text(text,color=Green,fontSize=11.sp,letterSpacing=2.sp,fontWeight=FontWeight.Bold) }
@Composable internal fun Action(text:String, color:Color=Green, enabled:Boolean=true, modifier:Modifier=Modifier, onClick:()->Unit) {
    Button(onClick=onClick,enabled=enabled,modifier=modifier.heightIn(min=58.dp),shape=TicketShape,
        border=androidx.compose.foundation.BorderStroke(1.5.dp,if(enabled)TicketEdge else TicketEdge.copy(alpha=.3f)),
        elevation=ButtonDefaults.buttonElevation(defaultElevation=3.dp,pressedElevation=1.dp,disabledElevation=0.dp),
        colors=ButtonDefaults.buttonColors(containerColor=TicketPaper,contentColor=TicketInk,disabledContainerColor=TicketPaper.copy(alpha=.5f),disabledContentColor=TicketInk.copy(alpha=.4f)),
        contentPadding=PaddingValues(horizontal=24.dp,vertical=13.dp)) {
        Text("✦",fontSize=15.sp,color=if(enabled)Color(0xFFB18745) else TicketEdge.copy(alpha=.3f))
        Text(text.replace("→", "").trim(),Modifier.padding(horizontal=16.dp),fontSize=21.sp,fontFamily=StoryFont,fontWeight=FontWeight.Normal)
        Text("✦",fontSize=15.sp,color=if(enabled)Color(0xFFB18745) else TicketEdge.copy(alpha=.3f))
    }
}

@Composable private fun ParkEntrance(inJourney:Boolean,reducedMotion:Boolean,onEnter:()->Unit) {
    val storyFont=StoryFont
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp))) {
        if(!reducedMotion) DinoSafariGif(Modifier.fillMaxSize().semantics { contentDescription="구름이 흐르고 별이 반짝이는 놀이공원 입구" },assetName="park_entrance_animated.gif")
        else androidx.compose.foundation.Image(painter=androidx.compose.ui.res.painterResource(R.drawable.park_entrance),contentDescription="공룡 숲, 마법성, 우주 항구, 해저 세계가 펼쳐진 놀이공원 입구",modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        Column(Modifier.align(Alignment.TopCenter).padding(top=22.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Text("W O N D E R  W H E E L S",fontSize=11.sp,fontWeight=FontWeight.Bold,color=Green)
            Text("PARK ON WHEELS",fontSize=40.sp,fontFamily=storyFont,letterSpacing=(.3).sp,color=Navy)
            Text("우리 차에서 시작되는 작은 놀이공원",fontFamily=storyFont,fontSize=18.sp,color=Navy,modifier=Modifier.padding(top=5.dp))
        }
        Action(if(inJourney)"모험 이어가기" else "입장하기",modifier=Modifier.align(Alignment.BottomCenter).padding(bottom=20.dp).widthIn(min=212.dp),onClick=onEnter)
    }
}

@Composable private fun WorldSelection(model:SafariModel,onChoose:(String)->Unit,onContinue:()->Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        if(model.inJourney) {
            Surface(color=model.theme.pale,shape=Shape,modifier=Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal=18.dp, vertical=12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("이어서 갈 모험이 있어요",fontWeight=FontWeight.Bold,fontSize=16.sp);Text("${model.theme.name} · ${model.role}",color=Muted,fontSize=12.sp)};Action("계속하기  →",model.theme.accent,modifier=Modifier.heightIn(min=48.dp),onClick=onContinue)}
            }
        }
        ThemeCardGrid(Modifier.weight(1f), enabled=!model.inJourney, onChoose=onChoose)

    }
}

/** Home and collection covers share geometry so the four worlds stay in the same places. */
@Composable private fun ThemeCardGrid(modifier:Modifier=Modifier, collection:Boolean=false, enabled:Boolean=true, recommendedId:String?=null, onChoose:(String)->Unit) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val columns=if(maxWidth<620.dp) 1 else 2
        Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            adventureThemes.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth().weight(1f),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    row.forEach { theme ->
                        Surface(color=theme.pale,shape=Shape,modifier=Modifier.weight(1f).fillMaxHeight()
                            .clickable(enabled=enabled,role=Role.Button){onChoose(theme.id)}
                            .semantics{contentDescription=if(collection) "${collectionTitle(theme.id)} 펼치기" else "${theme.name}, ${theme.subtitle}, 테마 선택"+(if(recommendedId==theme.id) ", 오늘의 여행 추천" else "")}) {
                            Box(Modifier.fillMaxSize()) {
                                ThemeArtwork(theme.id,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                                Box(Modifier.padding(12.dp)) { Badge(if(collection)theme.name else if(recommendedId==theme.id)"오늘의 여행 추천" else "${theme.companion}와 함께",SoftWhite.copy(alpha=.94f),theme.accent) }
                                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent,Color(0xFF172C32).copy(alpha=.7f))))) {
                                    Row(Modifier.padding(horizontal=15.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
                                        Text(if(collection)collectionTitle(theme.id) else theme.name,Modifier.weight(1f),fontSize=25.sp,fontWeight=FontWeight.ExtraBold,color=Color.White,style=LocalTextStyle.current.copy(shadow=androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha=.4f),androidx.compose.ui.geometry.Offset(0f,2f),5f)))
                                        Text("↗",Modifier.padding(horizontal=10.dp,vertical=6.dp),fontSize=26.sp,color=Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable internal fun TicketScreen(model:SafariModel,onBack:()->Unit,onStart:()->Unit) {
    VehicleBoardingScreen(model, onBack, onStart)
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
@Composable private fun IllustratedPanel(model:SafariModel,kicker:String,title:String,subtitle:String,celebration:Boolean=false,animateDinoPreview:Boolean=false) {
    val t=model.theme
    Surface(color=t.pale,shape=Shape,modifier=Modifier.fillMaxSize()) {
        Column(Modifier.padding(24.dp)) {
            Text(kicker,fontSize=10.sp,letterSpacing=2.sp,fontWeight=FontWeight.Bold,color=t.accent)
            Text(title,fontSize=30.sp,fontWeight=FontWeight.ExtraBold,lineHeight=38.sp,modifier=Modifier.padding(top=12.dp))
            Text(subtitle,fontSize=14.sp,color=Muted,lineHeight=21.sp,modifier=Modifier.padding(top=6.dp))
            val cleanupScene=t.id=="ocean" && model.episode % 2 == 0 && !animateDinoPreview && !celebration &&
                ((model.missionStep==1 && model.feedback==null) || (model.missionStep==2 && model.feedback!=null))
            if(cleanupScene) {
                Surface(color=SoftWhite,shape=RoundedCornerShape(24.dp),modifier=Modifier.weight(1f).fillMaxWidth().padding(vertical=8.dp)) {
                    OceanCleanupScene(model.feedback!=null,model.reducedMotion,Modifier.fillMaxSize().padding(8.dp))
                }
            } else if (!model.reducedMotion) {
                Surface(color=SoftWhite,shape=RoundedCornerShape(24.dp),border=androidx.compose.foundation.BorderStroke(1.dp,Color.White),modifier=Modifier.weight(1f).fillMaxWidth().padding(vertical=8.dp)) {
                    Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment=Alignment.Center) {
                        DinoSafariGif(Modifier.fillMaxWidth().aspectRatio(16f/9f).clip(RoundedCornerShape(18.dp)).semantics { contentDescription="${t.name} 이야기 장면" },t.id,if(animateDinoPreview) null else if(celebration) 3 else model.missionStep.coerceIn(0,2))
                    }
                }
            } else {
                Box(Modifier.weight(1f).fillMaxWidth().padding(vertical=8.dp).clip(RoundedCornerShape(24.dp)).background(SoftWhite), contentAlignment=Alignment.Center) {
                    ThemeArtwork(t.id,Modifier.fillMaxSize().semantics{contentDescription="${t.name}의 친구 ${t.companion} 일러스트"},celebration && !model.reducedMotion,ContentScale.Crop)
                }
            }
            if (kicker == "OUR ADVENTURE" || kicker == "A LITTLE WONDER, FOUND") {
                AdventureTrail(theme=t, step=if(celebration) 3 else model.missionStep, completed=celebration)
            } else {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Badge("OFFLINE STORY",Color.White.copy(alpha=.8f),t.accent);Text("WONDER / 0${adventureThemes.indexOf(t)+1}",fontSize=10.sp,color=t.accent,modifier=Modifier.align(Alignment.CenterVertically))}
            }
        }
    }
}

@Composable private fun AdventureTrail(theme:AdventureTheme,step:Int,completed:Boolean) {
    val labels=listOf("만남","발견","선물")
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(SoftWhite.copy(alpha=.9f)).padding(horizontal=10.dp,vertical=9.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
        labels.forEachIndexed { index,label ->
            val reached=completed || index < step
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(23.dp).clip(CircleShape).background(if(reached)theme.accent else theme.pale),contentAlignment=Alignment.Center) {
                    Text(if(reached)"✓" else "${index+1}",fontSize=11.sp,fontWeight=FontWeight.Bold,color=if(reached)Color.White else theme.accent)
                }
                Text(label,fontSize=12.sp,fontWeight=if(index==step && !completed)FontWeight.Bold else FontWeight.Medium,color=if(reached || index==step)theme.accent else Muted)
            }
        }
    }
}

@Composable private fun JourneyScreen(model:SafariModel,voice:VoiceDirector,onHome:()->Unit,onPassport:()->Unit,onCancel:()->Unit) {
    val t=model.theme;val complete=model.state.phase==Phase.COMPLETE
    AdaptivePanels(left={IllustratedPanel(model,if(complete)"A LITTLE WONDER, FOUND"else"OUR ADVENTURE",if(complete)"${t.reward} 발견!"else t.name,if(complete)t.ending else "EP.${model.episode} · ${t.chapterTitle(model.episode)}",complete)},right={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(11.dp)) {
            if(complete) {
                SectionLabel("PASSPORT STAMP COLLECTED")
                Text("우리의 모험을\n기억할 선물이야!",fontSize=31.sp,lineHeight=42.sp,fontWeight=FontWeight.ExtraBold)
                Text(t.ending,color=Muted,fontSize=16.sp,lineHeight=25.sp)
                Surface(color=t.pale,shape=Shape,modifier=Modifier.fillMaxWidth()) {Column(Modifier.padding(24.dp)){Text("${t.reward} 스탬프",fontSize=22.sp,fontWeight=FontWeight.Bold,color=t.accent);Text("여행 패스포트에 저장했어요",fontSize=14.sp,color=Muted,modifier=Modifier.padding(top=8.dp))}}
                Action("내 패스포트 보기  →",t.accent,modifier=Modifier.fillMaxWidth(),onClick=onPassport)
                OutlinedButton(onClick=onHome,shape=TicketShape,border=androidx.compose.foundation.BorderStroke(1.dp,TicketEdge),colors=ButtonDefaults.outlinedButtonColors(contentColor=TicketInk,containerColor=SoftWhite),modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)){Text("다음 에피소드 고르기")}
            } else {
                SectionLabel(if(model.mode==ExperienceMode.PARKED)"PARKED LITTLE ADVENTURE"else"JOURNEY WITH REAL DISTANCE")
                Text(if(model.state.phase==Phase.PAUSED)"잠깐 쉬어도 괜찮아"else if(model.missionStep==3)"마지막 장면을 완성해요"else"함께 해볼까?",fontSize=26.sp,fontWeight=FontWeight.ExtraBold)
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
                        Text("0${model.missionStep+1}  ${mission.title}",fontSize=19.sp,fontWeight=FontWeight.Bold)
                        Text(mission.prompt,fontSize=16.sp,lineHeight=23.sp,color=Muted)
                        mission.choices.forEachIndexed{index,choice->
                            OutlinedButton(onClick={model.answer(index)},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp),shape=TicketShape,border=androidx.compose.foundation.BorderStroke(1.5.dp,TicketEdge.copy(alpha=.6f)),colors=ButtonDefaults.outlinedButtonColors(contentColor=TicketInk,containerColor=TicketPaper),contentPadding=PaddingValues(horizontal=17.dp,vertical=10.dp)){
                                Text(choice,Modifier.weight(1f),fontSize=16.sp,fontWeight=FontWeight.Bold);Text("✦",fontSize=17.sp,color=TicketEdge)
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

private fun collectionTitle(id:String)=when(id){"dino"->"공룡 도감";"magic"->"마법 배지";"space"->"나의 행성";else->"물고기 도감"}

@Composable private fun PassportScreen(model:SafariModel,voice:VoiceDirector,onExplore:()->Unit) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val theme=adventureThemes.firstOrNull{it.id==selected}
    var detail by remember { mutableStateOf<Int?>(null) }
    DisposableEffect(Unit) { onDispose { voice.stop() } }
    androidx.activity.compose.BackHandler(enabled=detail!=null || selected!=null) {
        voice.stop();if(detail!=null)detail=null else selected=null
    }
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(if(theme==null)0.dp else 10.dp)) {
        if(theme!=null) Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(collectionTitle(theme.id),fontSize=27.sp,color=Navy)
            }
            TextButton(onClick={if(theme==null)onExplore() else selected=null}) { Text(if(theme==null)"← 테마로" else "← 수집함",color=Navy) }
        }
        if(theme==null) {
            ThemeCardGrid(Modifier.weight(1f),collection=true,onChoose={selected=it})
            Row(Modifier.fillMaxWidth().heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                if(model.visits.isNotEmpty()) Text("방문 스탬프 ${model.visits.size}개",fontSize=12.sp,color=Green)
                TextButton(onClick=onExplore){Text("← 테마로",fontSize=12.sp,color=Navy)}
            }
        } else {
            val entries=collectionEntries.getValue(theme.id)
            val names=entries.map{it.name}
            Row(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFF8EB)).border(1.dp,Color(0xFFE5DBCA),RoundedCornerShape(12.dp))) {
                // Binding belongs to the notebook rather than to each collectible.
                androidx.compose.foundation.Canvas(Modifier.width(20.dp).fillMaxHeight().background(theme.pale.copy(alpha=.65f))) {
                    repeat(6) { i ->
                        val y=size.height*(i+.5f)/6
                        drawCircle(Color(0xFFC4BAAA),3.dp.toPx(),androidx.compose.ui.geometry.Offset(size.width*.5f,y))
                        drawCircle(Paper,1.5.dp.toPx(),androidx.compose.ui.geometry.Offset(size.width*.5f,y))
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight().padding(horizontal=12.dp,vertical=10.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val columns=if(maxWidth>=600.dp)3 else 2
                        val rows=names.chunked(columns)
                        val scroll=maxHeight<150.dp*rows.size
                        Column(if(scroll) Modifier.fillMaxSize().verticalScroll(rememberScrollState()) else Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            rows.forEachIndexed { r,row ->
                                Row(if(scroll)Modifier.fillMaxWidth().height(180.dp) else Modifier.fillMaxWidth().weight(1f),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                                    row.forEachIndexed { c,name ->
                                        val entryIndex=r*columns+c
                                        Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).clickable(role=Role.Button){
                                            voice.stop();detail=entryIndex
                                        }.semantics{contentDescription="$name, 큰 그림과 설명 듣기"}.padding(4.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                            CollectionCreature(theme.id,entryIndex,Modifier.weight(1f).fillMaxWidth())
                                            Box(Modifier.fillMaxWidth().height(44.dp),contentAlignment=Alignment.Center) {
                                                Text(name,fontSize=17.sp,lineHeight=21.sp,color=Navy,maxLines=2,textAlign=TextAlign.Center)
                                            }
                                        }
                                    }
                                }
                                if(r<rows.lastIndex) HorizontalDivider(color=Color(0xFFE9DFCE),thickness=1.dp)
                            }
                        }
                    }
                }
            }
        }
    }
    if(theme!=null && detail!=null) {
        val index=detail!!
        val entry=collectionEntries.getValue(theme.id)[index]
        CollectionDetail(theme.id,index,entry,voice,onClose={voice.stop();detail=null})
    }

}

@Composable private fun CollectionDetail(theme:String,index:Int,entry:CollectionEntry,voice:VoiceDirector,onClose:()->Unit) {
    // A fresh tap is required to initiate this foreground narration; returning from background never restarts it.
    var started by remember { mutableStateOf(false) }
    var speaking by remember { mutableStateOf(false) }
    LaunchedEffect(voice.ready) {
        if(voice.ready && !started) {started=true;speaking=true;voice.tell(entry.narration){speaking=false}}
    }
    DisposableEffect(Unit) { onDispose { voice.stop() } }
    androidx.compose.ui.window.Dialog(onDismissRequest=onClose,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxWidth(.94f).fillMaxHeight(.94f),shape=Shape,color=Paper) {
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(entry.name,Modifier.weight(1f),fontSize=27.sp,lineHeight=33.sp,maxLines=2,color=Navy)
                    TextButton(onClick=onClose){Text("닫기 ×",fontSize=17.sp,color=Navy)}
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    if(maxWidth>=560.dp && maxWidth>maxHeight) Row(Modifier.fillMaxSize(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                        CollectionCreature(theme,index,Modifier.weight(1f).fillMaxHeight())
                        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.Center) { Text(entry.description,fontSize=25.sp,lineHeight=37.sp,color=Navy) }
                    } else Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally) {
                        CollectionCreature(theme,index,Modifier.weight(1f).fillMaxWidth())
                        Text(entry.description,Modifier.heightIn(max=150.dp).verticalScroll(rememberScrollState()),fontSize=22.sp,lineHeight=32.sp,color=Navy)
                    }
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(if(!voice.ready)"소리 준비 중" else if(speaking && voice.enabled)"설명을 들려주고 있어요" else "그림 속 친구를 만나 봐요",fontSize=13.sp,color=Muted)
                    Action("다시 듣기",enabled=voice.ready,onClick={voice.stop();speaking=true;voice.tell(entry.narration){speaking=false}})
                }
            }
        }
    }
}

private object CollectionSheets {
    private val cache=android.util.LruCache<String,android.graphics.Bitmap>(2)
    @Synchronized fun get(context:android.content.Context,theme:String):android.graphics.Bitmap =
        cache.get(theme) ?: context.assets.open("collections/${if(theme=="magic")"magic-v2" else theme}.png").use {
            checkNotNull(android.graphics.BitmapFactory.decodeStream(it)).also { bitmap -> cache.put(theme,bitmap) }
        }
}

@Composable private fun CollectionCreature(theme:String,index:Int,modifier:Modifier=Modifier) {
    val context=LocalContext.current
    val tile by produceState<android.graphics.Bitmap?>(null,theme,index) {
        value=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val sheet=CollectionSheets.get(context,theme)
            // Keep the full dinosaur silhouettes where the sheet crosses regular grid lines.
            val bounds=if(theme=="dino") listOf(
                intArrayOf(0,0,512,480),intArrayOf(512,0,1000,480),intArrayOf(1000,0,1536,480),
                intArrayOf(0,480,488,1024),intArrayOf(488,480,1000,1024),intArrayOf(1000,480,1536,1024)
            )[index] else null
            if(bounds!=null) {
                val left=bounds[0]*sheet.width/1536;val top=bounds[1]*sheet.height/1024
                val right=bounds[2]*sheet.width/1536;val bottom=bounds[3]*sheet.height/1024
                android.graphics.Bitmap.createBitmap(sheet,left,top,right-left,bottom-top)
            } else {
                val left=(index%3)*sheet.width/3;val top=(index/3)*sheet.height/2
                val right=((index%3)+1)*sheet.width/3;val bottom=((index/3)+1)*sheet.height/2
                android.graphics.Bitmap.createBitmap(sheet,left,top,right-left,bottom-top)
            }
        }
    }
    Box(modifier.padding(6.dp)) {
        tile?.let { androidx.compose.foundation.Image(bitmap=it.asImageBitmap(),contentDescription=null,modifier=Modifier.fillMaxSize().testTag("collection-art-$theme-$index"),contentScale=ContentScale.Fit) }
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
    val context=LocalContext.current
    val energyPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { model.reconnectTravel() }
    var tripName by remember{mutableStateOf("")}
    DisposableEffect(context) {
        val owner=context as? androidx.lifecycle.LifecycleOwner
        val observer=androidx.lifecycle.LifecycleEventObserver { _,event ->
            if(event==androidx.lifecycle.Lifecycle.Event.ON_STOP)tripName=""
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer) }
    }
    var showDiagnostics by remember{mutableStateOf(false)}
    var erase by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(17.dp)) {
        SectionLabel("FOR THE GROWN-UPS")
        Text("안심하고 함께하는 탐험",fontSize=29.sp,fontWeight=FontWeight.ExtraBold)
        ParentNote("콘텐츠 준비 완료","네 가지 테마의 그림·미션·엔딩이 앱 안에 있어요. 별도 다운로드나 로그인 없이 사용할 수 있습니다.")
        ParentNote("주차 중에만 만나요","Pleos UX 제한, 기어 P, 최신 거리값을 함께 확인합니다. 신호 대기나 속도 0을 주차로 간주하지 않습니다. 상태가 불명확하면 차단하고 자동으로 재개하지 않습니다.")
        ParentNote("현재 지원 범위","주차 중 한국어 오프라인 안내와 기기 내 음성 선택을 지원합니다. 음성 엔진이 없는 기기는 터치로 진행합니다. 보호자가 선택하면 실제 충전 상태에 따른 우주정거장 이야기와 목적지 도착 후 방문 스탬프를 연결합니다. 지원하지 않는 차량에서는 일반 이야기로 진행합니다.")
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("충전소 특별 모험");Text("충전 상태를 읽어 우주정거장 이야기를 열어요",fontSize=12.sp,color=Muted)};Switch(checked=model.chargingEnabled,onCheckedChange={value->model.changeCharging(value);if(value && !model.isSimulation && context.checkSelfPermission("android.car.permission.CAR_ENERGY")!=PackageManager.PERMISSION_GRANTED)energyPermission.launch("android.car.permission.CAR_ENERGY")},enabled=model.canPlay)}
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("여행지 방문 스탬프");Text("내비 도착 신호를 받은 뒤 주차하면 받을 수 있어요",fontSize=12.sp,color=Muted)};Switch(checked=model.visitsEnabled,onCheckedChange=model::changeVisits,enabled=model.canPlay)}
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)){Text("여행지에 맞는 테마 추천");Text("내비 목적지로 추천하고, 원하는 테마는 자유롭게 골라요",fontSize=12.sp,color=Muted)}
            Switch(checked=model.recommendationsEnabled,onCheckedChange=model::changeRecommendations,enabled=model.canPlay)
        }
        if(model.recommendationsEnabled) {
            OutlinedTextField(value=tripName,onValueChange={tripName=it.take(160)},label={Text("여행지 직접 입력")},placeholder={Text("예: 해운대해수욕장, 국립과천과학관")},singleLine=true,modifier=Modifier.fillMaxWidth(),enabled=model.canPlay)
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick={model.setTripDestination(tripName);tripName=""},enabled=model.canPlay && tripName.isNotBlank()){Text("여행지 적용")}
                TextButton(onClick={tripName="";model.setTripDestination("")},enabled=model.canPlay){Text("내비 목적지 사용")}
            }
            ParentNote("추천 기준", "${model.recommendationSource} · ${model.recommendedTheme?.let{id->adventureThemes.first{it.id==id}.name} ?: "분류 가능한 여행지를 기다려요"}\n목적지 이름을 기기 안에서만 분류합니다. 주소나 좌표로 주변 환경을 추측하지 않으며, 모호하면 추천하지 않습니다. 이름은 저장·외부 전송하지 않고 앱을 벗어나면 지웁니다.")
        }
        ParentNote("여행 정보 사용", "여행 연동은 모두 선택 사항입니다. 충전은 상태만 읽으며 충전을 조작하지 않습니다. 내비의 목적지 이름과 주소는 도착 알림을 중복 처리하지 않기 위해 기기 안에서 잠시 처리하고 저장하지 않습니다. 방문 스탬프에는 테마와 날짜, 중복 방지용 식별자만 남습니다. 연결 상태: ${model.travelStatus}")
        ParentNote("음성과 주행 제한","현재 공개 Pleos 규칙에 따라 주행 중에는 게임과 음성 상호작용을 중단합니다. 화면이 닫히거나 다른 차량 안내가 오면 듣기·말하기를 멈추고 자동으로 다시 시작하지 않습니다.")
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("테마 앰비언트 색상",fontWeight=FontWeight.Bold);Text("주차 중 지원 차량에만 은은한 고정 색 적용",fontSize=12.sp,color=Muted)};Switch(checked=model.ambientEnabled,onCheckedChange=model::changeAmbient,enabled=model.canPlay,modifier=Modifier.semantics{contentDescription="테마 앰비언트 색상 설정"})}
        ParentNote("조명 연결 상태",model.ambientStatus + "\n밝기·점멸·외부 조명은 제어하지 않습니다. 주행 중 자동 색 변경은 중지됩니다. 앱이 갑자기 종료되면 색이 남을 수 있으며 차량에서 직접 바꿀 수 있어요.")
        if(model.ambientEnabled) OutlinedButton(onClick=model::retryAmbient,enabled=model.canPlay,modifier=Modifier.heightIn(min=52.dp)){Text("테마 색 다시 적용")}
        ParentNote("기록과 개인정보","테마·역할·미션·스탬프를 기기에만 저장합니다. 음성 선택 시에만 마이크를 잠깐 사용합니다. 원본 음성을 저장하거나 서버로 보내지 않습니다. 방문 스탬프 또는 테마 추천을 켠 경우에 내비 여행 정보를 기기 안에서 처리하며 좌표나 목적지 이름·주소는 저장하지 않습니다.")
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("모험 소리");Text("출발 효과음과 이야기 배경음",fontSize=12.sp,color=Muted)};Switch(checked=model.soundsEnabled,onCheckedChange=model::changeSounds,enabled=model.canPlay)}
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
                surfaceTint = Color.Transparent, surfaceVariant = Color(0xFFEBEEE7), onSurfaceVariant = Muted), typography = StoryTypography()) {
        content()
    }
}

private fun StoryTypography(): Typography {
    val base=Typography()
    return base.copy(
        displayLarge=base.displayLarge.copy(fontFamily=StoryFont,fontWeight=FontWeight.Normal),
        displayMedium=base.displayMedium.copy(fontFamily=StoryFont,fontWeight=FontWeight.Normal),
        displaySmall=base.displaySmall.copy(fontFamily=StoryFont,fontWeight=FontWeight.Normal),
        headlineLarge=base.headlineLarge.copy(fontFamily=StoryFont,fontWeight=FontWeight.Normal),
        headlineMedium=base.headlineMedium.copy(fontFamily=StoryFont,fontWeight=FontWeight.Normal),
        headlineSmall=base.headlineSmall.copy(fontFamily=StoryFont,fontWeight=FontWeight.Normal),
        titleLarge=base.titleLarge.copy(fontFamily=StoryFont,fontWeight=FontWeight.Normal),
        titleMedium=base.titleMedium.copy(fontFamily=StoryFont,fontWeight=FontWeight.Normal),
        titleSmall=base.titleSmall.copy(fontFamily=StoryFont,fontWeight=FontWeight.Normal),
        bodyLarge=base.bodyLarge.copy(fontFamily=StoryFont,fontWeight=FontWeight.Normal),
        bodyMedium=base.bodyMedium.copy(fontFamily=StoryFont,fontWeight=FontWeight.Normal),
        bodySmall=base.bodySmall.copy(fontFamily=StoryFont,fontWeight=FontWeight.Normal),
        labelLarge=base.labelLarge.copy(fontFamily=StoryFont,fontWeight=FontWeight.Normal),
        labelMedium=base.labelMedium.copy(fontFamily=StoryFont,fontWeight=FontWeight.Normal),
        labelSmall=base.labelSmall.copy(fontFamily=StoryFont,fontWeight=FontWeight.Normal)
    )
}
