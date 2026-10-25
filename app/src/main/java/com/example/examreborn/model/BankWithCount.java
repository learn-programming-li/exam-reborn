package com.example.examreborn.model;

import androidx.room.Embedded;

/** 题库 + 题目数量（列表页只需要这两个） */
public class BankWithCount {

    @Embedded
    public QuestionBank bank;

    public int questionCount;
}
