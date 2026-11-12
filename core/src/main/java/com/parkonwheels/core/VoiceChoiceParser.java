package com.parkonwheels.core;

import java.util.List;
import java.util.Locale;

/** Closed-vocabulary commands. Ambiguous or unrelated speech never selects a reward/action. */
public final class VoiceChoiceParser {
    public static final int UNKNOWN = -1, STOP = -2, REPEAT = -3, NEXT = -4, FINISH = -5;
    private static String clean(String text) { return text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", ""); }
    public static int parse(String utterance, List<String> choices) {
        if (utterance == null) return UNKNOWN;
        String value = clean(utterance);
        if (List.of("그만", "멈춰", "쉬기", "취소", "그만할래", "종료").contains(value)) return STOP;
        if (List.of("다시", "다시말해줘", "다시들려줘", "반복").contains(value)) return REPEAT;
        if (List.of("다음", "다음이야기", "계속", "이어서", "이어하기").contains(value)) return NEXT;
        if (List.of("완성", "열어줘", "깨어나라", "부화", "완성해줘", "별빛을밝혀라").contains(value)) return FINISH;
        if (!choices.isEmpty() && List.of("첫번째", "첫번째선택", "1", "1번", "일번", "하나", "첫째").contains(value)) return 0;
        if (choices.size() > 1 && List.of("두번째", "두번째선택", "2", "2번", "이번", "둘", "둘째").contains(value)) return 1;
        int match = UNKNOWN;
        for (int i = 0; i < choices.size(); i++) {
            String choice = clean(choices.get(i));
            boolean matches = value.length() >= 2 && (choice.equals(value) || choice.contains(value) || value.contains(choice));
            if (matches) { if (match != UNKNOWN) return UNKNOWN; match = i; }
        }
        return match;
    }
}
