package com.example.examreborn.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 题目（Room 实体，表名 questions）。
 * <p>
 * 选项用 {@code OptionCodec} 编码进 options 字段，答案用 {@code AnswerCodec} 编码进 answer 字段，
 * 统一存成字符串，UI 层按 type 解释。
 */
@Entity(
        tableName = "questions",
        foreignKeys = @ForeignKey(
                entity = QuestionBank.class,
                parentColumns = "id",
                childColumns = "bankId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("bankId")})
public class Question {

    public static final int TYPE_SINGLE = 1;   // 单选
    public static final int TYPE_MULTI = 2;    // 多选
    public static final int TYPE_JUDGE = 3;    // 判断
    public static final int TYPE_FILL = 4;     // 填空
    public static final int TYPE_ESSAY = 5;    // 简答

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** 所属题库 */
    public long bankId;

    /** 题型，见上面的 TYPE_* */
    public int type;

    /** 题干 */
    public String title;

    /** 选项，OptionCodec 编码 */
    public String options;

    /** 正确答案，AnswerCodec 编码 */
    public String answer;

    /** 解析 */
    public String explanation;

    /** 分值 */
    public int score;

    public String chapter;

    /** 难度：1 最简单 ~ 5 最难 */
    public int difficulty;

    /** 标签，逗号分隔 */
    public String tags;

    /** 题目在试卷里的顺序 */
    public int orderIndex;

    public long createdAt;
    public long updatedAt;

    public Question() {
    }

    public static Question create(long bankId, int type) {
        Question question = new Question();
        question.bankId = bankId;
        question.type = type;
        question.title = "";
        question.options = "";
        question.answer = "";
        question.explanation = "";
        question.chapter = "";
        question.tags = "";
        question.score = 1;
        question.difficulty = 3;
        question.createdAt = System.currentTimeMillis();
        question.updatedAt = question.createdAt;
        return question;
    }
}
