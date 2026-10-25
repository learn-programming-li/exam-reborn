package com.example.examreborn.data;

import com.example.examreborn.model.Question;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 答案编解码。answer 字段统一是字符串：
 * 单选 "0"、多选 "0,1,2"（升序）、判断 "true"/"false"、填空/简答直接是文本。
 */
public final class AnswerCodec {

    private AnswerCodec() {
    }

    public static String single(int index) {
        return String.valueOf(index);
    }

    public static int singleIndex(String answer) {
        try {
            return Integer.parseInt(answer == null ? "" : answer.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String multi(List<Integer> indexes) {
        if (indexes == null || indexes.isEmpty()) {
            return "";
        }
        List<Integer> sorted = new ArrayList<>(indexes);
        Collections.sort(sorted);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(sorted.get(i));
        }
        return builder.toString();
    }

    public static List<Integer> multiIndexes(String answer) {
        List<Integer> indexes = new ArrayList<>();
        if (answer == null) {
            return indexes;
        }
        for (String part : answer.split(",")) {
            String value = part.trim();
            if (value.isEmpty()) {
                continue;
            }
            try {
                indexes.add(Integer.parseInt(value));
            } catch (NumberFormatException ignored) {
                // 忽略脏数据
            }
        }
        Collections.sort(indexes);
        return indexes;
    }

    public static String judge(boolean value) {
        return value ? "true" : "false";
    }

    public static boolean judgeValue(String answer) {
        return "true".equalsIgnoreCase(answer == null ? "" : answer.trim());
    }

    /** "ABD" / "A,B,D" / "A、B、D" -> [0,1,3] */
    public static List<Integer> parseLetters(String letters) {
        List<Integer> indexes = new ArrayList<>();
        if (letters == null) {
            return indexes;
        }
        String cleaned = letters.replaceAll("[^A-Za-z]", "").toUpperCase(Locale.ROOT);
        for (int i = 0; i < cleaned.length(); i++) {
            int index = cleaned.charAt(i) - 'A';
            if (index >= 0 && !indexes.contains(index)) {
                indexes.add(index);
            }
        }
        Collections.sort(indexes);
        return indexes;
    }

    /** [0,1,3] -> "ABD" */
    public static String toLetters(List<Integer> indexes) {
        if (indexes == null || indexes.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int index : indexes) {
            if (index >= 0) {
                builder.append((char) ('A' + index));
            }
        }
        return builder.toString();
    }

    /** 按题型把存储值转成给人看的字母 / 文本 */
    public static String display(String answer, int type) {
        if (type == Question.TYPE_SINGLE) {
            int index = singleIndex(answer);
            return index < 0 ? "" : toLetters(Collections.singletonList(index));
        }
        if (type == Question.TYPE_MULTI) {
            return toLetters(multiIndexes(answer));
        }
        if (type == Question.TYPE_JUDGE) {
            return judgeValue(answer) ? "true" : "false";
        }
        return answer == null ? "" : answer;
    }

    /** 判断题的规范化：对/正确/√/T → "true"，错/×/F → "false"，认不出返回空串 */
    public static String normalizeJudge(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim().toLowerCase(Locale.ROOT).replace("。", "").replace("，", "");
        if (value.isEmpty()) {
            return "";
        }
        if (value.equals("对") || value.equals("正确") || value.equals("是") || value.equals("√")
                || value.equals("t") || value.equals("true") || value.equals("y") || value.equals("yes")) {
            return "true";
        }
        if (value.equals("错") || value.equals("错误") || value.equals("否") || value.equals("×")
                || value.equals("x") || value.equals("f") || value.equals("false")
                || value.equals("n") || value.equals("no")) {
            return "false";
        }
        return "";
    }
}
