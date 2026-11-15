package com.parkonwheels.demo

import androidx.compose.ui.graphics.Color

data class ParkMission(val title: String, val prompt: String, val choices: List<String>, val response: String)
data class AdventureTheme(
    val id: String, val name: String, val english: String, val subtitle: String,
    val role: String, val companion: String, val reward: String, val ending: String,
    val accent: Color, val pale: Color, val missions: List<ParkMission>
)
val adventureThemes = listOf(
    AdventureTheme("dino", "다이노 사파리", "DINO SAFARI", "작은 알에 담긴, 커다란 모험", "공룡 탐험대원", "토리", "아기 공룡", "작은 친구가 알을 깨고 나왔어요! 토리도 반가워서 폴짝 뛰어요.",
        Color(0xFF27775A), Color(0xFFE4F1D9), listOf(
            ParkMission("냠냠, 소풍 시간", "토리가 좋아할 간식을 골라 줄래?", listOf("아삭아삭 풀잎", "달콤한 열매"), "냠냠! 토리가 꼬리를 살랑살랑 흔들어요."),
            ParkMission("알의 보금자리", "작은 알이 쉴 곳을 찾아보자!", listOf("포근한 풀밭", "커다란 나무 아래"), "푹신한 보금자리 완성! 알이 살짝 움직였어요."),
            ParkMission("깨어날 준비", "우리의 작은 친구에게 인사해 줘!", listOf("안녕, 작은 친구!", "함께 놀러 가자!"), "톡, 토독! 알 안에서 반가운 소리가 나요."))),
    AdventureTheme("magic", "매직 캐슬", "MAGIC CASTLE", "반짝이는 별이 성문을 열 때", "견습 마법사", "루미", "별빛 마법 배지", "반짝! 별빛 성문이 열렸어요. 루미와 다음 주문을 찾아봐요.",
        Color(0xFF7652AE), Color(0xFFF0E7FA), listOf(
            ParkMission("잃어버린 별 조각", "루미와 마법 주머니를 채워 보자!", listOf("노란 별 담기", "분홍 별 담기"), "반짝반짝! 첫 번째 별 조각을 찾았어요."),
            ParkMission("나만의 마법빛", "성문을 비출 빛은 어떤 색일까?", listOf("따뜻한 햇살빛", "은은한 달빛"), "별들이 모여 나만의 마법빛을 만들었어요."),
            ParkMission("성문을 여는 주문", "우리 목소리로 별빛을 깨워 볼까?", listOf("별빛을 밝혀라!", "반짝, 문을 열어라!"), "쉿, 성문 너머에서 별들이 우리를 기다려요."))),
    AdventureTheme("space", "스페이스 포트", "SPACE PORT", "우리가 처음 만나는 작은 행성", "우주 탐사대원", "코코", "무지개 행성", "새로운 행성 발견! 코코와 다음 우주 지도를 펼쳐 봐요.",
        Color(0xFF3866BD), Color(0xFFE3EDFF), listOf(
            ParkMission("별빛 항로", "우주선이 지나갈 길을 골라 줘!", listOf("반짝이는 별 길", "동그란 달 길"), "별빛 길을 찾았어요. 코코가 손을 흔들어요."),
            ParkMission("별빛 우주정거장", "우주정거장에 도착! 반짝이는 에너지를 채워 볼까?", listOf("햇살 코어", "무지개 코어"), "우주선에 에너지가 가득 찼어요!"),
            ParkMission("행성에 보내는 인사", "처음 만나는 행성에 무엇을 남길까?", listOf("우리의 손인사", "작은 별 깃발"), "우주 탐사 신호를 보냈어요. 행성이 반짝여요."))),
    AdventureTheme("ocean", "오션 서브마린", "OCEAN SUBMARINE", "파란 바다 아래, 반짝이는 비밀", "바다 탐험대원", "버블", "보물 진주", "조개가 활짝! 보물 진주를 찾았어요. 버블과 또 만나요.",
        Color(0xFF167C91), Color(0xFFDCF3F0), listOf(
            ParkMission("바다 친구 찾기", "버블과 누구를 먼저 만나 볼까?", listOf("느긋한 바다거북", "알록달록 물고기"), "안녕! 바다 친구가 우리 곁으로 헤엄쳐 와요."),
            ParkMission("산호 정원", "산호 사이에 숨어 있는 반짝임을 찾아봐!", listOf("분홍 산호 옆", "해초 숲 사이"), "보글보글! 보물 조개가 있는 길을 찾았어요."),
            ParkMission("조개에게 인사", "조개가 마음을 열도록 다정하게 인사해 줘!", listOf("안녕, 반가워!", "너의 바다가 멋져!"), "조개가 조금씩 열리고 있어요. 무엇이 들어 있을까요?")))
)

/** Each park has two authored chapters; later visits alternate them with a new episode number. */
fun AdventureTheme.chapterTitle(episode: Int): String = if (episode % 2 == 1) when(id) {
    "dino" -> "잃어버린 작은 알"; "magic" -> "별빛 성문의 비밀"; "space" -> "첫 번째 행성 신호"; else -> "진주를 품은 조개"
} else when(id) {
    "dino" -> "토리의 숲속 소풍"; "magic" -> "달빛 정원의 초대"; "space" -> "코코의 혜성 우체국"; else -> "버블의 산호 정원"
}
fun AdventureTheme.chapterMissions(episode: Int): List<ParkMission> = if (episode % 2 == 1) missions else when(id) {
    "dino" -> listOf(
        ParkMission("소풍 가방", "토리와 숲에 갈 준비를 해요. 무엇을 챙길까?", listOf("물과 열매", "돗자리와 모자"), "토리도 가방을 멨어요. 소풍 준비 끝!"),
        ParkMission("발자국 친구", "동글동글 발자국! 친구에게 어떻게 인사할까?", listOf("살짝 손 흔들기", "다정하게 안녕!"), "숲속 공룡 친구가 빼꼼 고개를 내밀어요."),
        ParkMission("함께하는 보금자리", "새 친구와 나눌 선물을 골라 줘!", listOf("포근한 풀잎 이불", "반짝이는 조약돌"), "새 친구도 행복해졌어요. 다음에 또 만나자!"))
    "magic" -> listOf(
        ParkMission("달빛 초대장", "루미에게 도착한 초대장을 열어 봐요!", listOf("은빛 봉투 열기", "별 모양 봉투 열기"), "달빛 정원에서 우리를 기다리고 있어요."),
        ParkMission("잠든 꽃의 주문", "조용히 잠든 꽃을 깨워 줄래?", listOf("꽃아, 반가워!", "달빛아, 살짝 비춰 줘!"), "보랏빛 꽃이 살며시 피어났어요."),
        ParkMission("정원의 작은 빛", "친구들과 나눌 빛을 골라요.", listOf("둥근 달빛", "작은 반딧불빛"), "정원 가득 부드러운 빛이 번져요. 마법사님, 고마워요!"))
    "space" -> listOf(
        ParkMission("우주 편지", "코코와 별 친구에게 편지를 보내요!", listOf("반가운 인사 쓰기", "우리 행성 그리기"), "별 친구에게 보낼 편지가 완성됐어요."),
        ParkMission("혜성 우체부", "우리 편지를 어느 우주선에 실어 보낼까?", listOf("별빛 우주선", "달빛 우주선"), "우주 우체부가 안전하게 편지를 싣고 출발해요."),
        ParkMission("도착한 답장", "먼 행성에서 답장이 왔어요!", listOf("함께 열어 보기", "코코에게 먼저 보여 주기"), "우리도 반가워! 새로운 행성 친구가 생겼어요."))
    else -> listOf(
        ParkMission("버블의 초대", "오늘은 산호 정원을 구경하는 날!", listOf("바다거북과 가기", "물고기들과 가기"), "친구들과 함께 정원으로 출발해요."),
        ParkMission("바다를 아껴요", "정원에 떨어진 종이 조각을 발견했어요.", listOf("조심히 주워 담기", "친구와 함께 정리하기"), "산호 정원이 다시 깨끗해졌어요. 고마워요!"),
        ParkMission("정원의 선물", "버블이 고마운 마음을 담아 선물을 준비했대요!", listOf("함께 조개 열기", "바다 친구들 초대하기"), "친구들과 함께 발견하니 더 반짝여요!"))
}
