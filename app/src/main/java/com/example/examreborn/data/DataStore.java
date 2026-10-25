package com.example.examreborn.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.examreborn.model.BankWithCount;
import com.example.examreborn.model.Question;
import com.example.examreborn.model.QuestionBank;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 全 app 唯一的数据入口。
 * <p>
 * Room 禁止在主线程访问数据库，所以这里统一做线程切换：
 * 所有读写丢进单线程队列（顺序有保证，先建库灌数据、再查），结果用 Handler 回主线程。
 */
public final class DataStore {

    public interface Callback<T> {
        void onResult(T value);
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static volatile AppDatabase database;

    private DataStore() {
    }

    /** 由 App.onCreate 调用一次 */
    public static void init(Context context) {
        if (database == null) {
            synchronized (DataStore.class) {
                if (database == null) {
                    database = AppDatabase.get(context);
                    // 排在队列最前面：先灌示例数据，后面的查询自然排在它之后
                    IO.execute(() -> SeedData.installIfEmpty(dao()));
                }
            }
        }
    }

    public static void loadBanks(Callback<List<BankWithCount>> callback) {
        IO.execute(() -> {
            List<BankWithCount> result = dao().banksWithCount();
            post(() -> callback.onResult(result));
        });
    }

    public static void loadBank(long bankId, Callback<QuestionBank> callback) {
        IO.execute(() -> {
            QuestionBank bank = dao().bankById(bankId);
            post(() -> callback.onResult(bank));
        });
    }

    public static void loadQuestions(long bankId, Callback<List<Question>> callback) {
        IO.execute(() -> {
            List<Question> questions = dao().questionsOfBank(bankId);
            post(() -> callback.onResult(questions == null ? new ArrayList<>() : questions));
        });
    }

    public static void loadQuestion(long questionId, Callback<Question> callback) {
        IO.execute(() -> {
            Question question = dao().questionById(questionId);
            post(() -> callback.onResult(question));
        });
    }

    public static void addBank(QuestionBank bank, Callback<Long> callback) {
        IO.execute(() -> {
            long id = dao().insertBank(bank);
            post(() -> callback.onResult(id));
        });
    }

    public static void addQuestion(Question question, Runnable done) {
        IO.execute(() -> {
            dao().insertQuestion(question);
            post(done);
        });
    }

    /**
     * 批量导入：一次事务插完，并把 orderIndex 接到该题库已有题目后面。
     *
     * @param done 回主线程的完成回调，带上实际插入条数
     */
    public static void addQuestions(long bankId, List<Question> questions, Callback<Integer> done) {
        IO.execute(() -> {
            int inserted = 0;
            if (questions != null && !questions.isEmpty()) {
                int base = dao().questionCount(bankId);
                List<Question> batch = new ArrayList<>(questions.size());
                for (Question question : questions) {
                    question.bankId = bankId;
                    question.orderIndex = base + question.orderIndex;
                    question.updatedAt = System.currentTimeMillis();
                    batch.add(question);
                }
                dao().insertQuestions(batch);
                inserted = batch.size();
            }
            int count = inserted;
            post(() -> done.onResult(count));
        });
    }

    public static void updateQuestion(Question question, Runnable done) {
        IO.execute(() -> {
            question.updatedAt = System.currentTimeMillis();
            dao().updateQuestion(question);
            post(done);
        });
    }

    public static void deleteBank(QuestionBank bank, Runnable done) {
        IO.execute(() -> {
            dao().deleteBank(bank);   // 题目靠外键 CASCADE 一起删
            post(done);
        });
    }

    public static void deleteQuestion(Question question, Runnable done) {
        IO.execute(() -> {
            dao().deleteQuestion(question);
            post(done);
        });
    }

    private static AppDao dao() {
        AppDatabase current = database;
        if (current == null) {
            throw new IllegalStateException("DataStore.init() 还没调用，检查 App 是否注册到 manifest");
        }
        return current.dao();
    }

    private static void post(Runnable action) {
        if (action != null) {
            MAIN.post(action);
        }
    }
}
