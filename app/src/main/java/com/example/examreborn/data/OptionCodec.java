package com.example.examreborn.data;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 选项列表 &lt;-&gt; 单字段的编解码。
 * 用 ASCII 单元分隔符拼接，避免和选项内容里的逗号、竖线打架。
 */
public final class OptionCodec {

    private static final String SEP = "\u001F";

    private OptionCodec() {
    }

    public static String encode(List<String> options) {
        if (options == null || options.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < options.size(); i++) {
            if (i > 0) {
                builder.append(SEP);
            }
            String option = options.get(i);
            builder.append(option == null ? "" : option.replace(SEP, " "));
        }
        return builder.toString();
    }

    public static List<String> decode(String raw) {
        List<String> options = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return options;
        }
        for (String part : raw.split(Pattern.quote(SEP), -1)) {
            options.add(part);
        }
        return options;
    }

    /** 0 -> "A" */
    public static String letter(int index) {
        return index >= 0 && index < 26 ? String.valueOf((char) ('A' + index)) : "?";
    }
}
