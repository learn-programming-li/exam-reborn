package com.example.examreborn.data;

import com.example.examreborn.model.Question;
import com.example.examreborn.model.QuestionBank;

import java.util.ArrayList;
import java.util.Arrays;

/** 首次建库时灌进去的示例数据，五种题型各来一道。数据库一旦有题库就跳过。 */
public final class SeedData {

    private SeedData() {
    }

    public static void installIfEmpty(AppDao dao) {
        if (dao.bankCount() > 0) {
            return;
        }

        // 微积分：单选 + 判断 + 填空
        long calculus = dao.insertBank(QuestionBank.create("微积分公式", 30, "极限、导数、积分常用公式"));
        dao.insertQuestion(single(calculus, 0,
                "导数的定义式是下面哪一个？",
                Arrays.asList(
                        "f'(x) = lim(Δx→0) [f(x+Δx) - f(x)] / Δx",
                        "f'(x) = [f(x+1) - f(x)] / 1",
                        "f'(x) = f(x) / x",
                        "f'(x) = lim(x→0) f(x)"),
                0,
                "导数的本质是差商的极限",
                "第二章 导数与微分", 3, "高数,导数"));

        dao.insertQuestion(judge(calculus, 1,
                "可导必连续，连续不一定可导。",
                true,
                "经典反例：y = |x| 在 x = 0 处连续但不可导",
                "第二章 导数与微分", 2, "高数,连续"));

        dao.insertQuestion(fill(calculus, 2,
                "分部积分公式是 ____。",
                "∫u dv = uv - ∫v du",
                "口诀：反对幂指三，谁前谁为 u",
                "第三章 不定积分", 4, "高数,积分"));

        // 英语：多选
        long verb = dao.insertBank(QuestionBank.create("动词不定时", 0, "to do 的常见搭配"));
        dao.insertQuestion(multi(verb, 0,
                "下列哪些动词后面只能接动名词（doing）？",
                Arrays.asList("avoid", "enjoy", "want", "finish"),
                Arrays.asList(0, 1, 3),
                "want 后面接 to do：want to do sth",
                "非谓语动词", 3, "英语,语法"));

        // 六级：简答
        long cet6 = dao.insertBank(QuestionBank.create("英语六级", 0, "真题高频词"));
        dao.insertQuestion(essay(cet6, 0,
                "简述英语六级作文的常见三段式结构。",
                2,
                "第一段引出话题并表明观点；第二段用两到三个论据展开；第三段总结并重申观点。",
                "写作", 3, "英语,写作"));

        dao.insertBank(QuestionBank.create("英语四级", 0, "基础词汇"));
    }

    private static Question base(long bankId, int type, long orderIndex, String title,
                                 String explanation, String chapter, int difficulty, String tags) {
        Question question = Question.create(bankId, type);
        question.orderIndex = (int) orderIndex;
        question.title = title;
        question.explanation = explanation == null ? "" : explanation;
        question.chapter = chapter == null ? "" : chapter;
        question.difficulty = difficulty;
        question.tags = tags == null ? "" : tags;
        return question;
    }

    private static Question single(long bankId, long order, String title, java.util.List<String> options,
                                   int answer, String explanation, String chapter, int difficulty, String tags) {
        Question question = base(bankId, Question.TYPE_SINGLE, order, title, explanation, chapter, difficulty, tags);
        question.options = OptionCodec.encode(new ArrayList<>(options));
        question.answer = AnswerCodec.single(answer);
        return question;
    }

    private static Question multi(long bankId, long order, String title, java.util.List<String> options,
                                  java.util.List<Integer> answers, String explanation, String chapter,
                                  int difficulty, String tags) {
        Question question = base(bankId, Question.TYPE_MULTI, order, title, explanation, chapter, difficulty, tags);
        question.options = OptionCodec.encode(new ArrayList<>(options));
        question.answer = AnswerCodec.multi(answers);
        return question;
    }

    private static Question judge(long bankId, long order, String title, boolean answer,
                                  String explanation, String chapter, int difficulty, String tags) {
        Question question = base(bankId, Question.TYPE_JUDGE, order, title, explanation, chapter, difficulty, tags);
        question.answer = AnswerCodec.judge(answer);
        return question;
    }

    private static Question fill(long bankId, long order, String title, String answer,
                                 String explanation, String chapter, int difficulty, String tags) {
        Question question = base(bankId, Question.TYPE_FILL, order, title, explanation, chapter, difficulty, tags);
        question.answer = answer;
        return question;
    }

    private static Question essay(long bankId, long order, String title, int score, String reference,
                                  String chapter, int difficulty, String tags) {
        Question question = base(bankId, Question.TYPE_ESSAY, order, title, reference, chapter, difficulty, tags);
        question.score = score;
        return question;
    }
}
