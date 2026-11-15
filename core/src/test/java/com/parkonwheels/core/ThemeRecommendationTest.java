package com.parkonwheels.core;
public final class ThemeRecommendationTest {
    private static int count;
    private static void check(String name,String expected) {
        String actual=ThemeRecommendation.forDestination(name);
        if(!java.util.Objects.equals(expected,actual))throw new AssertionError(name+": "+actual);
        count++;
    }
    public static void main(String[] args) {
        check("해운대해수욕장","ocean");check("강릉 경포해변","ocean");check("COEX 아쿠아리움","ocean");
        check("설악산","dino");check("북한산국립공원","dino");check("국립 유명산 자연휴양림","dino");check("서울숲","dino");
        check("경복궁","magic");check("수원 화성","magic");check("한국민속촌","magic");check("낙안읍성","magic");
        check("국립과천과학관","space");check("별마로천문대","space");check("나로우주센터 우주과학관","space");
        check("  해 운 대 해 수 욕 장 ","ocean");
        check(null,null);check("",null);check("서울역",null);check("부산역",null);check("산본역",null);
        check("바다횟집",null);check("우주카페",null);check("별빛펜션",null);check("성산일출봉",null);
        check("해변 과학관",null);check("국립역사박물관",null);check("해변맛집",null);
        System.out.println("PASS: "+count+" destination recommendation assertions");
    }
}
