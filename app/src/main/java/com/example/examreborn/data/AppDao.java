package com.example.examreborn.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.examreborn.model.BankWithCount;
import com.example.examreborn.model.Question;
import com.example.examreborn.model.QuestionBank;

import java.util.List;

@Dao
public interface AppDao {

    /** 题库列表 + 每个题库的题目数 */
    @Query("SELECT b.*, COUNT(q.id) AS questionCount FROM banks b "
            + "LEFT JOIN questions q ON q.bankId = b.id "
            + "GROUP BY b.id ORDER BY b.id DESC")
    List<BankWithCount> banksWithCount();

    @Query("SELECT COUNT(*) FROM banks")
    int bankCount();

    @Query("SELECT * FROM banks WHERE id = :bankId")
    QuestionBank bankById(long bankId);

    /** 按试卷顺序读题 */
    @Query("SELECT * FROM questions WHERE bankId = :bankId ORDER BY orderIndex ASC, id ASC")
    List<Question> questionsOfBank(long bankId);

    @Query("SELECT COUNT(*) FROM questions WHERE bankId = :bankId")
    int questionCount(long bankId);

    @Query("SELECT * FROM questions WHERE id = :questionId")
    Question questionById(long questionId);

    @Insert
    long insertBank(QuestionBank bank);

    @Insert
    long insertQuestion(Question question);

    /** 批量插入，一次事务 */
    @Insert
    void insertQuestions(List<Question> questions);

    @Update
    void updateBank(QuestionBank bank);

    @Update
    void updateQuestion(Question question);

    @Delete
    void deleteBank(QuestionBank bank);

    @Delete
    void deleteQuestion(Question question);
}
