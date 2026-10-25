package com.example.examreborn.util;

import android.content.Context;

import com.example.examreborn.R;
import com.example.examreborn.data.AnswerCodec;
import com.example.examreborn.data.OptionCodec;
import com.example.examreborn.model.Question;

import java.util.ArrayList;
import java.util.List;

/** 题目的展示格式化：题型名、答案文本、元信息行 */
public final class QuestionFormat {

    private QuestionFormat() {
    }

    public static String typeName(Context context, int type) {
        switch (type) {
            case Question.TYPE_SINGLE:
                return context.getString(R.string.type_single);
            case Question.TYPE_MULTI:
                return context.getString(R.string.type_multi);
            case Question.TYPE_JUDGE:
                return context.getString(R.string.type_judge);
            case Question.TYPE_FILL:
                return context.getString(R.string.type_fill);
            case Question.TYPE_ESSAY:
                return context.getString(R.string.type_essay);
            default:
                return "";
        }
    }

    /** 给人看的答案：单选 "A. xxx"、多选 "AB. xxx / yyy"、判断 "正确" */
    public static String answerText(Context context, Question question) {
        List<String> options = OptionCodec.decode(question.options);
        switch (question.type) {
            case Question.TYPE_SINGLE: {
                int index = AnswerCodec.singleIndex(question.answer);
                if (index < 0 || index >= options.size()) {
                    return "";
                }
                return OptionCodec.letter(index) + ". " + options.get(index);
            }
            case Question.TYPE_MULTI: {
                List<Integer> indexes = AnswerCodec.multiIndexes(question.answer);
                StringBuilder builder = new StringBuilder(AnswerCodec.toLetters(indexes));
                if (!indexes.isEmpty()) {
                    builder.append(". ");
                }
                for (int i = 0; i < indexes.size(); i++) {
                    int index = indexes.get(i);
                    if (i > 0) {
                        builder.append(" / ");
                    }
                    if (index >= 0 && index < options.size()) {
                        builder.append(options.get(index));
                    }
                }
                return builder.toString();
            }
            case Question.TYPE_JUDGE:
                return AnswerCodec.judgeValue(question.answer)
                        ? context.getString(R.string.qa_true)
                        : context.getString(R.string.qa_false);
            default:
                return question.answer == null ? "" : question.answer;
        }
    }

    /** 章节 · 难度 · 分值 · 标签 */
    public static String metaText(Context context, Question question) {
        List<String> parts = new ArrayList<>();
        if (notEmpty(question.chapter)) {
            parts.add(question.chapter.trim());
        }
        parts.add(context.getString(R.string.qa_meta_difficulty, question.difficulty));
        if (question.score > 0) {
            parts.add(context.getString(R.string.qa_meta_score, question.score));
        }
        if (notEmpty(question.tags)) {
            parts.add(question.tags.trim());
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                builder.append(" · ");
            }
            builder.append(parts.get(i));
        }
        return builder.toString();
    }

    public static boolean notEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
