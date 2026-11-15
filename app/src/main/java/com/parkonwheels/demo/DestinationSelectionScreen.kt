package com.parkonwheels.demo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class TripIdea(val name:String,val region:String,val invitation:String)
/** Curated nationwide ideas, not nearby search or a navigation destination setter. */
private fun tripIdeas(theme:String)=when(theme){
    "magic" -> listOf(TripIdea("롯데월드","서울 송파","반짝이는 성을 만나러 가자!"),TripIdea("에버랜드","경기 용인","동화 속 친구들이 기다려!"),TripIdea("경주월드","경북 경주","마법 정원으로 모험을 떠나자!"))
    "space" -> listOf(TripIdea("국립과천과학관","경기 과천","궁금한 게 가득한 과학 여행"),TripIdea("영월 별마로천문대","강원 영월","밤하늘의 별을 만나러"),TripIdea("고흥 나로우주센터 우주과학관","전남 고흥","로켓은 어떻게 우주로 갈까?"))
    "ocean" -> listOf(TripIdea("해운대해수욕장","부산 해운대","파도 소리를 들으러 가자!"),TripIdea("국립해양생물자원관","충남 서천","어떤 바다 친구들이 있을까?"),TripIdea("경포해수욕장","강원 강릉","파란 바다를 만나러"))
    else -> listOf(TripIdea("서울숲","서울 성동","나무 사이로 탐험을 떠나자!"),TripIdea("국립수목원","경기 포천","커다란 나무와 작은 풀을 찾아서"),TripIdea("설악산","강원 속초","초록 산에는 누가 살고 있을까?"))
}

@Composable
internal fun DestinationSelectionScreen(model:SafariModel,onBack:()->Unit,onStart:()->Unit){
    BackHandler(onBack=onBack)
    val theme=model.theme
    val ideas=remember(theme.id){tripIdeas(theme.id)}
    var selectedName by rememberSaveable(theme.id){mutableStateOf<String?>(null)}
    val ink=Color(0xFF24394A)
    BoxWithConstraints(Modifier.fillMaxSize().background(theme.pale)) {
        ThemeArtwork(theme.id,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFFFFFCF5),Color(0xFFFFFCF5).copy(alpha=.96f),Color(0xFFFFFCF5).copy(alpha=.15f),Color.Transparent),endX=constraints.maxWidth.toFloat())))
        TextButton(onClick=onBack,modifier=Modifier.align(Alignment.TopEnd).padding(8.dp).heightIn(min=48.dp)){Text("다른 테마 고르기",color=ink,fontSize=15.sp)}
        Column(Modifier.fillMaxHeight().width(if(maxWidth<600.dp)maxWidth*.76f else minOf(maxWidth*.56f,560.dp)).verticalScroll(rememberScrollState()).padding(start=22.dp,end=16.dp,top=16.dp,bottom=12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("오늘은 어디로 가볼까?",color=ink,fontSize=36.sp,lineHeight=42.sp)
            Text("${theme.companion}와 함께 가고 싶은 곳을 골라 봐!",color=ink,fontSize=18.sp,lineHeight=23.sp)
            Text("함께 가 볼 여행지",color=theme.accent,fontSize=12.sp)
            ideas.forEach { idea ->
                val chosen=selectedName==idea.name
                Surface(shape=RoundedCornerShape(12.dp),color=if(chosen)theme.pale else Color(0xFFFFFEFA).copy(alpha=.94f),border=androidx.compose.foundation.BorderStroke(if(chosen)2.dp else 1.dp,if(chosen)theme.accent else Color(0xFFDADDD3)),modifier=Modifier.fillMaxWidth().semantics{selected=chosen}.clickable(enabled=model.canPlay,role=Role.RadioButton){selectedName=idea.name}) {
                    Row(Modifier.padding(horizontal=13.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
                        Column(Modifier.weight(1f)){
                            Text(idea.name,color=ink,fontSize=20.sp,lineHeight=24.sp)
                            Text("${idea.region} · ${idea.invitation}",color=Color(0xFF536560),fontSize=14.sp,lineHeight=18.sp)
                        }
                        Text(if(chosen)"✓" else "○",color=theme.accent,fontSize=21.sp)
                    }
                }
            }
            Action("이야기 시작",enabled=model.canPlay && selectedName!=null,modifier=Modifier.fillMaxWidth(),onClick=onStart)
            TextButton(onClick=onStart,enabled=model.canPlay,modifier=Modifier.align(Alignment.CenterHorizontally)){Text("여행지는 나중에 고를래요",color=ink,fontSize=13.sp)}
        }
    }
}
