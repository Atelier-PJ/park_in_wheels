package com.parkonwheels.demo

data class CollectionEntry(val name:String,val description:String) {
    val narration get() = "$name. $description"
}

val collectionEntries = mapOf(
    "dino" to listOf(
        CollectionEntry("스테고사우루스","등에 커다란 판이 줄지어 있어요. 식물을 먹으며 살던 공룡이에요."),
        CollectionEntry("트리케라톱스","얼굴에 뿔이 세 개 있어요. 머리 뒤에는 방패처럼 넓은 뼈가 있어요."),
        CollectionEntry("브라키오사우루스","목이 길쭉하고 몸집이 아주 커요. 높은 나무에 달린 잎도 먹을 수 있었어요."),
        CollectionEntry("티라노사우루스","튼튼한 뒷다리 두 개로 걸었어요. 앞발은 작지만 턱과 이빨은 아주 튼튼했어요."),
        CollectionEntry("파라사우롤로푸스","머리 뒤로 길게 뻗은 볏이 있어요. 이 볏으로 소리를 내어 친구와 이야기했을 거예요."),
        CollectionEntry("안킬로사우루스","몸을 단단한 갑옷 같은 뼈가 덮고 있어요. 꼬리 끝에는 커다란 곤봉이 달려 있어요.")
    ),
    "magic" to listOf(
        CollectionEntry("별빛 마법사","반짝이는 별을 모은 마법사의 배지예요. 다정한 목소리로 별빛을 깨워 보세요."),
        CollectionEntry("달빛 수호자","깜깜한 밤을 밝혀 주는 달의 배지예요. 길을 잃은 친구를 따뜻하게 비춰 줘요."),
        CollectionEntry("무지개 요정","여러 빛깔이 사이좋게 모인 배지예요. 서로 다른 친구들도 함께하면 멋진 무지개가 돼요."),
        CollectionEntry("꽃잎 마법사","작은 꽃을 소중히 돌보는 배지예요. 다정한 한마디가 꽃처럼 활짝 피어나요."),
        CollectionEntry("햇살 친구","따뜻한 햇살을 닮은 배지예요. 활짝 웃으며 친구에게 인사해 볼까요?"),
        CollectionEntry("성문의 열쇠","비밀의 성문을 여는 마법 열쇠예요. 용기와 친절을 모으면 새로운 모험이 시작돼요.")
    ),
    "space" to listOf(
        CollectionEntry("푸른 지구","우리가 사는 지구에는 파란 바다와 땅이 있어요. 소중한 생명들이 함께 사는 집이에요."),
        CollectionEntry("고리 토성","토성 둘레에는 커다란 고리가 있어요. 고리는 수많은 얼음과 돌 조각으로 이루어져 있어요."),
        CollectionEntry("붉은 화성","화성은 붉은빛을 띠는 행성이에요. 표면의 녹슨 철 성분 때문에 붉게 보여요."),
        CollectionEntry("커다란 목성","목성은 태양계에서 가장 큰 행성이에요. 줄무늬 구름과 커다란 폭풍이 있어요."),
        CollectionEntry("반짝 금성","금성은 두꺼운 구름으로 덮여 있어요. 하늘에서 아주 밝게 보이지만 표면은 무척 뜨거워요."),
        CollectionEntry("푸른 해왕성","해왕성은 태양에서 아주 멀리 있어요. 푸른빛을 띠며 아주 차갑고 바람도 세게 불어요.")
    ),
    "ocean" to listOf(
        CollectionEntry("흰동가리","주황색 몸에 하얀 줄무늬가 있어요. 말미잘 사이에 숨어 지내는 작은 물고기예요."),
        CollectionEntry("노랑나비고기","납작한 몸과 노란빛이 나비를 닮았어요. 산호 사이를 요리조리 헤엄쳐 다녀요."),
        CollectionEntry("블루탱","파란 몸에 노란 꼬리가 눈에 띄어요. 바위나 산호에 붙은 해조류를 먹어요."),
        CollectionEntry("복어","위험을 느끼면 몸을 부풀리는 물고기예요. 눈으로만 반갑게 인사하고 만지지는 말아요."),
        CollectionEntry("해마","말처럼 생긴 얼굴과 돌돌 말린 꼬리가 있어요. 꼬리로 해초를 붙잡고 쉴 수 있어요."),
        CollectionEntry("분홍 물고기","산호 곁을 헤엄치는 분홍빛 친구예요. 작은 지느러미와 꼬리를 움직이며 물속을 여행해요.")
    )
)
