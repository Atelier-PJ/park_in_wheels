package com.parkonwheels.core;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Conservative on-device destination-name rules. Never infers surroundings from an address. */
public final class ThemeRecommendation {
    private ThemeRecommendation() { }
    private static final String[][] RULES={
        {"ocean","해수욕장","해변","아쿠아리움","수족관","해양생물관"},
        {"dino","자연휴양림","수목원","삼림욕장","산림욕장","치유의숲","서울숲","설악산","지리산","한라산","북한산","내장산","오대산","덕유산","속리산","소백산","주왕산","태백산"},
        {"magic","경복궁","창덕궁","창경궁","덕수궁","경희궁","수원화성","화성행궁","공산성","낙안읍성","해미읍성","민속촌","고궁","역사문화공원"},
        {"space","천문대","과학관","우주센터","천체관측소","플라네타륨"}
    };
    public static String forDestination(String name) {
        if(name==null || name.length()>160)return null;
        String n=Normalizer.normalize(name,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replaceAll("[\\s\\p{Punct}·]", "");
        if(n.isEmpty() || n.endsWith("역"))return null;
        for(String excluded:new String[]{"맛집","횟집","식당","카페","펜션","호텔"})if(n.contains(excluded))return null;
        Set<String> matches=new LinkedHashSet<>();
        for(String[] rule:RULES)for(int i=1;i<rule.length;i++)if(n.contains(rule[i])){matches.add(rule[0]);break;}
        return matches.size()==1?matches.iterator().next():null;
    }
}
