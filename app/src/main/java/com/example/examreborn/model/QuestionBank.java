package com.example.examreborn.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** 题库 / 试卷（Room 实体，表名 banks） */
@Entity(tableName = "banks")
public class QuestionBank {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;

    /** 考试时长，单位分钟；0 表示不限时 */
    public int duration;

    public String note;
    public long createdAt;

    public QuestionBank() {
    }

    public static QuestionBank create(String name, String note) {
        return create(name, 0, note);
    }

    public static QuestionBank create(String name, int duration, String note) {
        QuestionBank bank = new QuestionBank();
        bank.name = name;
        bank.duration = duration;
        bank.note = note;
        bank.createdAt = System.currentTimeMillis();
        return bank;
    }
}
