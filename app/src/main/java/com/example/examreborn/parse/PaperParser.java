package com.example.examreborn.parse;

import com.example.examreborn.data.AnswerCodec;
import com.example.examreborn.data.OptionCodec;
import com.example.examreborn.model.Question;
import com.example.examreborn.model.QuestionBank;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 试卷解析器：纯 Java，不碰任何 Android API，可以直接跑 JUnit。
 * <p>
 * 支持两种输入：
 * 1) JSON 试卷（single / multi / judge / fill / essay 五段）
 * 2) txt / md 文本（## 单选题 分节 + A. B. C. 选项 + 答案：X）
 * <p>
 * 解析出来的 Question 里 bankId 还是 0，导入时由调用方指定。
 */
public final class PaperParser {

    public static final class Result {
        public final QuestionBank paper = new QuestionBank();
        public final List<Question> questions = new ArrayList<>();
        public final List<String> warnings = new ArrayList<>();

        public int countOf(int type) {
            int count = 0;
            for (Question question : questions) {
                if (question.type == type) {
                    count++;
                }
            }
            return count;
        }
    }

    /* ---------- 正则 ---------- */

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s*(.+)$");
    private static final Pattern NUMBERED = Pattern.compile(
            "^(\\d+\\s*[.、)）]|（\\d+）|\\(\\d+\\)|第\\s*\\d+\\s*题).*$");
    private static final String NUMBERED_PREFIX =
            "^(\\d+\\s*[.、)）]|（\\d+）|\\(\\d+\\)|第\\s*\\d+\\s*题)\\s*";
    private static final Pattern OPTION = Pattern.compile("^([A-Ha-h])\\s*[.、)）]\\s*(.+)$");

    private static final Pattern F_ANSWER = Pattern.compile(
            "^(?:答案|参考答案|答|answer)([:：]|\\s+)\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern F_EXPLANATION = Pattern.compile(
            "^(?:解析|答案解析|说明|笔记|note|explanation)([:：]|\\s+)\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern F_CHAPTER = Pattern.compile(
            "^(?:章节|chapter)([:：]|\\s+)\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern F_DIFFICULTY = Pattern.compile(
            "^(?:难度|difficulty)([:：]|\\s+)\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern F_TAGS = Pattern.compile(
            "^(?:标签|tags?)([:：]|\\s+)\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern F_SCORE = Pattern.compile(
            "^(?:分值|分数|score)([:：]|\\s+)\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern F_DURATION = Pattern.compile(
            "^(?:时长|考试时长|限时|duration)([:：]|\\s+)\\s*(\\d+)\\s*(?:分钟|min|minutes)?$",
            Pattern.CASE_INSENSITIVE);

    /* ---------- 文本解析的内部字段 ---------- */

    private static final int FIELD_TITLE = 0;
    private static final int FIELD_ANSWER = 1;
    private static final int FIELD_EXPLANATION = 2;
    private static final int FIELD_CHAPTER = 3;
    private static final int FIELD_TAGS = 4;
    private static final int FIELD_SCORE = 5;
    private static final int FIELD_DIFFICULTY = 6;

    private PaperParser() {
    }

    public static Result parse(String text, String fileName) {
        Result result = new Result();
        try {
            String content = text == null ? "" : text;
            if (content.startsWith("\uFEFF")) {
                content = content.substring(1);
            }
            String trimmed = content.trim();
            if (trimmed.startsWith("{")) {
                return parseJson(trimmed, baseName(fileName));
            }
            return parseText(content, baseName(fileName));
        } catch (Exception e) {
            result.warnings.add("解析出错：" + e.getMessage());
            return result;
        }
    }

    public static boolean looksLikeJson(String text) {
        String trimmed = text == null ? "" : text.trim();
        return trimmed.startsWith("{");
    }

    /* ---------- JSON ---------- */

    private static Result parseJson(String json, String fallbackName) {
        Result result = new Result();
        try {
            JSONObject root = new JSONObject(json);

            String name = root.optString("name", "").trim();
            result.paper.name = name.isEmpty() ? fallbackName : name;
            result.paper.duration = root.optInt("duration", 0);
            if (name.isEmpty()) {
                result.warnings.add("JSON 里没有 name，已用文件名当试卷名");
            }

            int order = 0;
            order = readSingle(root.optJSONArray("single"), result, order);
            order = readMulti(root.optJSONArray("multi"), result, order);
            order = readJudge(root.optJSONArray("judge"), result, order);
            order = readFill(root.optJSONArray("fill"), result, order);
            readEssay(root.optJSONArray("essay"), result, order);
        } catch (JSONException e) {
            result.warnings.add("JSON 解析失败：" + e.getMessage());
        }
        return result;
    }

    private static int readSingle(JSONArray array, Result result, int order) {
        if (array == null) {
            return order;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String label = "单选题第 " + (i + 1) + " 题";
            String title = item.optString("q", "").trim();
            List<String> options = stringList(item.optJSONArray("opts"));
            int answer = item.optInt("ans", -1);

            if (title.isEmpty()) {
                result.warnings.add(label + "题干为空，已忽略");
                continue;
            }
            if (options.size() < 2) {
                result.warnings.add(label + "选项少于 2 个");
            }
            if (answer < 0 || answer >= options.size()) {
                result.warnings.add(label + "答案下标越界，已忽略");
                continue;
            }
            Question question = base(Question.TYPE_SINGLE, title, item.optString("exp", ""));
            question.options = OptionCodec.encode(options);
            question.answer = AnswerCodec.single(answer);
            question.orderIndex = order++;
            result.questions.add(question);
        }
        return order;
    }

    private static int readMulti(JSONArray array, Result result, int order) {
        if (array == null) {
            return order;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String label = "多选题第 " + (i + 1) + " 题";
            String title = item.optString("q", "").trim();
            List<String> options = stringList(item.optJSONArray("opts"));
            List<Integer> answers = intList(item.optJSONArray("ans"));

            if (title.isEmpty()) {
                result.warnings.add(label + "题干为空，已忽略");
                continue;
            }
            if (options.size() < 2) {
                result.warnings.add(label + "选项少于 2 个");
            }
            if (answers.isEmpty()) {
                result.warnings.add(label + "答案为空，已忽略");
                continue;
            }
            boolean outOfRange = false;
            for (int value : answers) {
                if (value < 0 || value >= options.size()) {
                    outOfRange = true;
                    break;
                }
            }
            if (outOfRange) {
                result.warnings.add(label + "答案下标越界，已忽略");
                continue;
            }
            Question question = base(Question.TYPE_MULTI, title, item.optString("exp", ""));
            question.options = OptionCodec.encode(options);
            question.answer = AnswerCodec.multi(answers);
            question.orderIndex = order++;
            result.questions.add(question);
        }
        return order;
    }

    private static int readJudge(JSONArray array, Result result, int order) {
        if (array == null) {
            return order;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String label = "判断题第 " + (i + 1) + " 题";
            String title = item.optString("q", "").trim();
            if (title.isEmpty()) {
                result.warnings.add(label + "题干为空，已忽略");
                continue;
            }
            if (!item.has("ans")) {
                result.warnings.add(label + "没有 ans 字段，已忽略");
                continue;
            }
            Question question = base(Question.TYPE_JUDGE, title, item.optString("exp", ""));
            question.answer = AnswerCodec.judge(item.optBoolean("ans", false));
            question.orderIndex = order++;
            result.questions.add(question);
        }
        return order;
    }

    private static int readFill(JSONArray array, Result result, int order) {
        if (array == null) {
            return order;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String label = "填空题第 " + (i + 1) + " 题";
            String title = item.optString("q", "").trim();
            String answer = item.optString("ans", "").trim();
            if (title.isEmpty() && answer.isEmpty()) {
                result.warnings.add(label + "题干和答案都为空，已忽略");
                continue;
            }
            if (answer.isEmpty()) {
                result.warnings.add(label + "答案为空");
            }
            Question question = base(Question.TYPE_FILL, title, item.optString("exp", ""));
            question.answer = answer;
            question.orderIndex = order++;
            result.questions.add(question);
        }
        return order;
    }

    private static void readEssay(JSONArray array, Result result, int order) {
        if (array == null) {
            return;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String label = "简答题第 " + (i + 1) + " 题";
            String title = item.optString("q", "").trim();
            if (title.isEmpty()) {
                result.warnings.add(label + "题干为空，已忽略");
                continue;
            }
            Question question = base(Question.TYPE_ESSAY, title, item.optString("exp", ""));
            question.answer = item.optString("ref", "");
            question.score = item.optInt("score", 2);
            question.orderIndex = order++;
            result.questions.add(question);
        }
    }

    /* ---------- txt / md ---------- */

    private static Result parseText(String text, String fallbackName) {
        Result result = new Result();
        result.paper.name = fallbackName;
        result.paper.duration = 0;

        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);

        Builder builder = null;
        int sectionType = 0;
        boolean named = false;
        int[] order = {0};

        for (String rawLine : lines) {
            String line = clean(rawLine);
            if (line.isEmpty()) {
                continue;
            }

            Matcher heading = HEADING.matcher(line);
            if (heading.matches()) {
                String headingText = heading.group(2).trim();
                int level = heading.group(1).length();
                int type = typeOf(headingText);
                if (type != 0) {
                    builder = flush(builder, result, order);
                    sectionType = type;
                    continue;
                }
                if (level == 1 && !named) {
                    result.paper.name = headingText;
                    named = true;
                    continue;
                }
                builder = flush(builder, result, order);
                builder = new Builder(sectionType);
                builder.appendTo(FIELD_TITLE, headingText);
                continue;
            }

            if (isTypeLine(line)) {
                builder = flush(builder, result, order);
                sectionType = typeOf(line);
                continue;
            }

            Matcher duration = F_DURATION.matcher(line);
            if (duration.matches()) {
                result.paper.duration = parseInt(duration.group(2), 0);
                continue;
            }

            Field field = matchField(line);
            if (field != null) {
                if (builder == null) {
                    builder = new Builder(sectionType);
                }
                if (field.field == FIELD_SCORE) {
                    builder.score = parseInt(field.value, builder.score);
                } else if (field.field == FIELD_DIFFICULTY) {
                    builder.difficulty = parseDifficulty(field.value);
                } else {
                    builder.appendTo(field.field, field.value);
                    builder.field = field.field;
                }
                continue;
            }

            Matcher option = OPTION.matcher(line);
            if (option.matches()) {
                if (builder == null) {
                    builder = new Builder(sectionType);
                }
                builder.options.add(option.group(2).trim());
                continue;
            }

            if (NUMBERED.matcher(line).matches()) {
                builder = flush(builder, result, order);
                builder = new Builder(sectionType);
                String title = line.replaceFirst(NUMBERED_PREFIX, "").trim();
                builder.appendTo(FIELD_TITLE, title.isEmpty() ? line : title);
                continue;
            }

            if (builder == null) {
                builder = new Builder(sectionType);
            }
            builder.appendTo(builder.field, line);
        }
        flush(builder, result, order);

        validateText(result);
        return result;
    }

    private static void validateText(Result result) {
        for (Question question : result.questions) {
            String name = shorten(question.title);
            List<String> options = OptionCodec.decode(question.options);
            if (question.type == Question.TYPE_SINGLE || question.type == Question.TYPE_MULTI) {
                if (options.size() < 2) {
                    result.warnings.add("题目「" + name + "」选项少于 2 个");
                }
                if (question.answer.isEmpty()) {
                    result.warnings.add("题目「" + name + "」没识别到答案");
                    continue;
                }
                if (question.type == Question.TYPE_SINGLE) {
                    int index = AnswerCodec.singleIndex(question.answer);
                    if (index < 0 || index >= options.size()) {
                        result.warnings.add("题目「" + name + "」答案超出选项范围");
                    }
                } else {
                    for (int index : AnswerCodec.multiIndexes(question.answer)) {
                        if (index < 0 || index >= options.size()) {
                            result.warnings.add("题目「" + name + "」答案超出选项范围");
                            break;
                        }
                    }
                }
            }
        }
    }

    private static Builder flush(Builder builder, Result result, int[] order) {
        if (builder == null) {
            return null;
        }
        String title = builder.title.toString().trim();
        if (title.isEmpty()) {
            return null;
        }
        int type = inferType(builder);
        Question question = Question.create(0, type);
        question.title = title;
        question.options = OptionCodec.encode(builder.options);
        question.answer = buildAnswer(builder, type);
        question.explanation = builder.explanation.toString().trim();
        question.chapter = builder.chapter.toString().trim();
        question.tags = builder.tags.toString().trim();
        question.difficulty = builder.difficulty;
        question.score = builder.score;
        question.orderIndex = order[0]++;
        result.questions.add(question);
        return null;
    }

    private static int inferType(Builder builder) {
        if (builder.type != 0) {
            return builder.type;
        }
        if (!builder.options.isEmpty()) {
            return AnswerCodec.parseLetters(builder.answer.toString()).size() > 1
                    ? Question.TYPE_MULTI : Question.TYPE_SINGLE;
        }
        if (!AnswerCodec.normalizeJudge(builder.answer.toString()).isEmpty()) {
            return Question.TYPE_JUDGE;
        }
        return Question.TYPE_FILL;
    }

    private static String buildAnswer(Builder builder, int type) {
        String raw = builder.answer.toString().trim();
        switch (type) {
            case Question.TYPE_SINGLE: {
                List<Integer> indexes = AnswerCodec.parseLetters(raw);
                return indexes.isEmpty() ? "" : AnswerCodec.single(indexes.get(0));
            }
            case Question.TYPE_MULTI:
                return AnswerCodec.multi(AnswerCodec.parseLetters(raw));
            case Question.TYPE_JUDGE:
                return AnswerCodec.normalizeJudge(raw);
            default:
                return raw;
        }
    }

    private static int typeOf(String text) {
        if (text == null) {
            return 0;
        }
        String value = text.replaceAll("[\\s:：]", "");
        if (value.contains("多选") || value.contains("不定项")) {
            return Question.TYPE_MULTI;
        }
        if (value.contains("单选") || value.contains("单项选择")) {
            return Question.TYPE_SINGLE;
        }
        if (value.contains("判断")) {
            return Question.TYPE_JUDGE;
        }
        if (value.contains("填空")) {
            return Question.TYPE_FILL;
        }
        if (value.contains("简答") || value.contains("问答") || value.contains("论述")) {
            return Question.TYPE_ESSAY;
        }
        return 0;
    }

    private static boolean isTypeLine(String line) {
        String value = line.replaceAll("[\\s:：、.。]", "");
        return value.length() <= 6 && typeOf(value) != 0;
    }

    private static Field matchField(String line) {
        Matcher matcher;
        matcher = F_ANSWER.matcher(line);
        if (matcher.matches()) {
            return new Field(FIELD_ANSWER, matcher.group(2).trim());
        }
        matcher = F_EXPLANATION.matcher(line);
        if (matcher.matches()) {
            return new Field(FIELD_EXPLANATION, matcher.group(2).trim());
        }
        matcher = F_CHAPTER.matcher(line);
        if (matcher.matches()) {
            return new Field(FIELD_CHAPTER, matcher.group(2).trim());
        }
        matcher = F_DIFFICULTY.matcher(line);
        if (matcher.matches()) {
            return new Field(FIELD_DIFFICULTY, matcher.group(2).trim());
        }
        matcher = F_TAGS.matcher(line);
        if (matcher.matches()) {
            return new Field(FIELD_TAGS, matcher.group(2).trim());
        }
        matcher = F_SCORE.matcher(line);
        if (matcher.matches()) {
            return new Field(FIELD_SCORE, matcher.group(2).trim());
        }
        return null;
    }

    private static String clean(String rawLine) {
        String line = rawLine == null ? "" : rawLine.trim();
        while (true) {
            String next = line.replaceFirst("^[>\\-*+•]\\s+", "");
            if (next.equals(line)) {
                break;
            }
            line = next;
        }
        return line.replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replace("【", "")
                .replace("】", "")
                .trim();
    }

    private static int parseDifficulty(String value) {
        String text = value == null ? "" : value.trim();
        int direct = parseInt(text, 0);
        if (direct >= 1 && direct <= 5) {
            return direct;
        }
        if (text.contains("较难")) {
            return 4;
        }
        if (text.contains("简单") || text.contains("容易")) {
            return 1;
        }
        if (text.contains("较易")) {
            return 2;
        }
        if (text.contains("中等") || text.contains("一般")) {
            return 3;
        }
        if (text.contains("困难") || text.contains("难")) {
            return 5;
        }
        return 3;
    }

    private static Question base(int type, String title, String explanation) {
        Question question = Question.create(0, type);
        question.title = title;
        question.explanation = explanation == null ? "" : explanation.trim();
        return question;
    }

    private static List<String> stringList(JSONArray array) {
        List<String> list = new ArrayList<>();
        if (array == null) {
            return list;
        }
        for (int i = 0; i < array.length(); i++) {
            String value = array.optString(i, "");
            list.add(value == null ? "" : value);
        }
        return list;
    }

    private static List<Integer> intList(JSONArray array) {
        List<Integer> list = new ArrayList<>();
        if (array == null) {
            return list;
        }
        for (int i = 0; i < array.length(); i++) {
            list.add(array.optInt(i, -1));
        }
        return list;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String baseName(String fileName) {
        String name = fileName == null ? "" : fileName.trim();
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        return name.isEmpty() ? "导入的试卷" : name;
    }

    private static String shorten(String title) {
        if (title == null) {
            return "";
        }
        return title.length() <= 12 ? title : title.substring(0, 12) + "…";
    }

    /* ---------- 内部类 ---------- */

    private static final class Field {
        final int field;
        final String value;

        Field(int field, String value) {
            this.field = field;
            this.value = value;
        }
    }

    private static final class Builder {
        final int type;
        final StringBuilder title = new StringBuilder();
        final StringBuilder answer = new StringBuilder();
        final StringBuilder explanation = new StringBuilder();
        final StringBuilder chapter = new StringBuilder();
        final StringBuilder tags = new StringBuilder();
        final List<String> options = new ArrayList<>();
        int field = FIELD_TITLE;
        int difficulty = 3;
        int score = 1;

        Builder(int type) {
            this.type = type;
        }

        void appendTo(int target, String text) {
            if (text == null || text.isEmpty()) {
                return;
            }
            switch (target) {
                case FIELD_ANSWER:
                    append(answer, text, "\n");
                    break;
                case FIELD_EXPLANATION:
                    append(explanation, text, "\n");
                    break;
                case FIELD_CHAPTER:
                    append(chapter, text, " ");
                    break;
                case FIELD_TAGS:
                    append(tags, text, " ");
                    break;
                default:
                    append(title, text, " ");
                    break;
            }
        }

        private static void append(StringBuilder builder, String text, String separator) {
            if (builder.length() > 0) {
                builder.append(separator);
            }
            builder.append(text);
        }
    }
}
