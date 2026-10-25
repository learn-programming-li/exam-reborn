package com.example.examreborn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.examreborn.data.AnswerCodec;
import com.example.examreborn.data.OptionCodec;
import com.example.examreborn.model.Question;
import com.example.examreborn.parse.PaperParser;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * 解析器单测：纯 Java，直接在本机跑，不用装到手机。
 * 命令行：gradlew test
 */
public class PaperParserTest {

    /** 用户给的那份 JSON 试卷格式 */
    private static final String JSON_PAPER = "{"
            + "\"name\": \"微积分期末测试\","
            + "\"duration\": 30,"
            + "\"single\": ["
            + "  {\"q\": \"导数的定义式是？\", \"opts\": [\"lim(Δx→0) Δy/Δx\", \"f(x)/x\", \"x²\", \"1\"],"
            + "   \"ans\": 0, \"exp\": \"差商的极限\"}"
            + "],"
            + "\"multi\": ["
            + "  {\"q\": \"下列哪些是连续函数？\", \"opts\": [\"x²\", \"|x|\", \"1/x\", \"sin x\"],"
            + "   \"ans\": [0, 1, 3], \"exp\": \"1/x 在 0 处不连续\"}"
            + "],"
            + "\"judge\": ["
            + "  {\"q\": \"可导必连续。\", \"ans\": true, \"exp\": \"反过来不成立\"}"
            + "],"
            + "\"fill\": ["
            + "  {\"q\": \"分部积分公式是 ____。\", \"ans\": \"∫u dv = uv - ∫v du\", \"exp\": \"反对幂指三\"}"
            + "],"
            + "\"essay\": ["
            + "  {\"q\": \"简述导数的几何意义。\", \"score\": 2, \"ref\": \"切线斜率\"}"
            + "]"
            + "}";

    private static final String MD_PAPER =
            "# 英语语法小测\n"
                    + "时长：20 分钟\n"
                    + "\n"
                    + "## 单选题\n"
                    + "1. 下面哪个动词后面只能接动名词？\n"
                    + "A. want\n"
                    + "B. avoid\n"
                    + "C. decide\n"
                    + "D. hope\n"
                    + "答案：B\n"
                    + "解析：avoid doing sth\n"
                    + "章节：非谓语动词\n"
                    + "难度：较易\n"
                    + "标签：英语,语法\n"
                    + "\n"
                    + "## 多选题\n"
                    + "2. 下列哪些是情态动词？\n"
                    + "A. can\n"
                    + "B. must\n"
                    + "C. run\n"
                    + "D. should\n"
                    + "答案：ABD\n"
                    + "\n"
                    + "## 判断题\n"
                    + "3. 英语中第三人称单数动词要加 s。\n"
                    + "答案：对\n"
                    + "\n"
                    + "## 填空题\n"
                    + "4. He ____ (go) to school every day.\n"
                    + "答案：goes\n"
                    + "\n"
                    + "## 简答题\n"
                    + "5. 简述一般现在时的用法。\n"
                    + "分值：3\n"
                    + "答案：表示经常发生的动作或存在的状态。\n";

    @Test
    public void parsesJsonPaper() {
        PaperParser.Result result = PaperParser.parse(JSON_PAPER, "数学.json");

        assertEquals("微积分期末测试", result.paper.name);
        assertEquals(30, result.paper.duration);
        assertEquals(5, result.questions.size());
        assertTrue(result.warnings.toString(), result.warnings.isEmpty());

        Question single = byType(result.questions, Question.TYPE_SINGLE);
        assertEquals("0", single.answer);
        assertEquals(4, OptionCodec.decode(single.options).size());
        assertEquals("差商的极限", single.explanation);

        Question multi = byType(result.questions, Question.TYPE_MULTI);
        assertEquals("0,1,3", multi.answer);
        assertEquals(Arrays.asList(0, 1, 3), AnswerCodec.multiIndexes(multi.answer));

        Question judge = byType(result.questions, Question.TYPE_JUDGE);
        assertEquals("true", judge.answer);

        Question fill = byType(result.questions, Question.TYPE_FILL);
        assertEquals("∫u dv = uv - ∫v du", fill.answer);

        Question essay = byType(result.questions, Question.TYPE_ESSAY);
        assertEquals(2, essay.score);
        assertEquals("切线斜率", essay.answer);

        assertEquals(0, result.questions.get(0).orderIndex);
        assertEquals(4, result.questions.get(4).orderIndex);
    }

    @Test
    public void parsesMarkdownPaper() {
        PaperParser.Result result = PaperParser.parse(MD_PAPER, "英语.md");

        assertEquals("英语语法小测", result.paper.name);
        assertEquals(20, result.paper.duration);
        assertEquals(5, result.questions.size());
        assertTrue(result.warnings.toString(), result.warnings.isEmpty());

        Question single = result.questions.get(0);
        assertEquals(Question.TYPE_SINGLE, single.type);
        assertEquals("下面哪个动词后面只能接动名词？", single.title);
        assertEquals(Arrays.asList("want", "avoid", "decide", "hope"), OptionCodec.decode(single.options));
        assertEquals("1", single.answer);              // B -> 下标 1
        assertEquals("avoid doing sth", single.explanation);
        assertEquals("非谓语动词", single.chapter);
        assertEquals(2, single.difficulty);            // 较易
        assertEquals("英语,语法", single.tags);

        Question multi = result.questions.get(1);
        assertEquals(Question.TYPE_MULTI, multi.type);
        assertEquals("0,1,3", multi.answer);           // ABD

        Question judge = result.questions.get(2);
        assertEquals(Question.TYPE_JUDGE, judge.type);
        assertEquals("true", judge.answer);

        Question fill = result.questions.get(3);
        assertEquals(Question.TYPE_FILL, fill.type);
        assertEquals("goes", fill.answer);

        Question essay = result.questions.get(4);
        assertEquals(Question.TYPE_ESSAY, essay.type);
        assertEquals(3, essay.score);
        assertEquals("表示经常发生的动作或存在的状态。", essay.answer);
    }

    @Test
    public void infersTypeWithoutSectionHeadings() {
        String text = "1. 下面哪个是对的？\n"
                + "A. 甲\nB. 乙\n答案：A\n"
                + "2. 判断题：地球是圆的。\n"
                + "答案：对\n";

        PaperParser.Result result = PaperParser.parse(text, "无标题.txt");

        assertEquals(2, result.questions.size());
        assertEquals(Question.TYPE_SINGLE, result.questions.get(0).type);
        assertEquals("0", result.questions.get(0).answer);
        assertEquals(Question.TYPE_JUDGE, result.questions.get(1).type);
        assertEquals("true", result.questions.get(1).answer);
        assertEquals("无标题", result.paper.name);
    }

    @Test
    public void ignoresBadJsonAnswers() {
        String json = "{\"name\":\"坏数据\",\"single\":["
                + "{\"q\":\"越界\",\"opts\":[\"A\",\"B\"],\"ans\":5},"
                + "{\"q\":\"正常\",\"opts\":[\"A\",\"B\"],\"ans\":1}"
                + "]}";
        PaperParser.Result result = PaperParser.parse(json, "bad.json");

        assertEquals(1, result.questions.size());
        assertEquals("正常", result.questions.get(0).title);
        assertEquals("1", result.questions.get(0).answer);
        assertEquals(1, result.warnings.size());
    }

    @Test
    public void handlesMarkdownDecorationsAndBom() {
        String text = "\uFEFF# 带装饰的文档\n"
                + "**1. 题干加粗**\n"
                + "- A. 选项一\n"
                + "- B. 选项二\n"
                + "**答案：** B\n";

        PaperParser.Result result = PaperParser.parse(text, "deco.md");

        assertEquals("带装饰的文档", result.paper.name);
        assertEquals(1, result.questions.size());
        assertEquals("题干加粗", result.questions.get(0).title);
        assertEquals("1", result.questions.get(0).answer);
        assertEquals(2, OptionCodec.decode(result.questions.get(0).options).size());
    }

    @Test
    public void emptyTextGivesEmptyResult() {
        PaperParser.Result result = PaperParser.parse("", "空文件.txt");
        assertEquals(0, result.questions.size());
    }

    private static Question byType(List<Question> questions, int type) {
        for (Question question : questions) {
            if (question.type == type) {
                return question;
            }
        }
        throw new AssertionError("没有找到题型 " + type);
    }
}
