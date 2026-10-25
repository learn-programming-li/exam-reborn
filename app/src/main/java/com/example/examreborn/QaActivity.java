package com.example.examreborn;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.examreborn.data.AnswerCodec;
import com.example.examreborn.data.DataStore;
import com.example.examreborn.data.OptionCodec;
import com.example.examreborn.dialog.AppDialogs;
import com.example.examreborn.model.Question;
import com.example.examreborn.util.GlassUtils;
import com.example.examreborn.util.InsetUtils;
import com.example.examreborn.util.QuestionFormat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/** 问答助手：按题型渲染作答控件（单选/多选/判断/填空/简答） */
public class QaActivity extends AppCompatActivity {

    public static final String EXTRA_BANK_ID = "extra_bank_id";
    public static final String EXTRA_QUESTION_ID = "extra_question_id";

    private MaterialToolbar toolbar;
    private TextView tvQuestion;
    private TextView tvMeta;
    private TextView tvCorrect;
    private FrameLayout answerContainer;
    private MaterialButton btnSubmit;
    private MaterialButton btnPrev;
    private MaterialButton btnNext;

    private long bankId = -1;
    private long wantedQuestionId = -1;
    private List<Question> questions = new ArrayList<>();
    private int index = 0;

    /** 当前这题的作答状态 */
    private int selectedSingle = -1;
    private final List<CheckBox> multiBoxes = new ArrayList<>();
    private RadioGroup judgeGroup;
    private EditText inputAnswer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_qa);
        InsetUtils.lightStatusBar(this, true);
        GlassUtils.blurMesh(this);

        bankId = getIntent().getLongExtra(EXTRA_BANK_ID, -1);
        wantedQuestionId = getIntent().getLongExtra(EXTRA_QUESTION_ID, -1);

        toolbar = findViewById(R.id.toolbar);
        InsetUtils.padTop(findViewById(R.id.toolbarBar));
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_edit_question) {
                editCurrentQuestion();
                return true;
            }
            return false;
        });

        tvQuestion = findViewById(R.id.tvQuestion);
        tvMeta = findViewById(R.id.tvMeta);
        tvCorrect = findViewById(R.id.tvCorrect);
        answerContainer = findViewById(R.id.answerContainer);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);

        btnSubmit.setOnClickListener(v -> submit());
        btnPrev.setOnClickListener(v -> move(-1));
        btnNext.setOnClickListener(v -> move(1));
        findViewById(R.id.btnNote).setOnClickListener(v -> editNote());

        InsetUtils.padBottom(findViewById(R.id.bottomBar));
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        if (bankId <= 0) {
            render();
            return;
        }
        DataStore.loadBank(bankId, loaded -> {
            if (loaded != null) {
                toolbar.setSubtitle(loaded.name);
            }
        });
        DataStore.loadQuestions(bankId, list -> {
            questions = list;
            if (wantedQuestionId > 0) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).id == wantedQuestionId) {
                        index = i;
                        break;
                    }
                }
            }
            render();
        });
    }

    private Question current() {
        if (questions.isEmpty()) {
            return null;
        }
        if (index < 0) {
            index = 0;
        }
        if (index >= questions.size()) {
            index = questions.size() - 1;
        }
        return questions.get(index);
    }

    private void render() {
        Question question = current();
        if (question == null) {
            tvQuestion.setText(R.string.qa_empty_question);
            tvMeta.setVisibility(View.GONE);
            tvCorrect.setText("");
        } else {
            wantedQuestionId = question.id;
            tvQuestion.setText(question.title);
            tvMeta.setText(QuestionFormat.metaText(this, question));
            tvMeta.setVisibility(View.VISIBLE);
            tvCorrect.setText(R.string.qa_correct_placeholder);
        }
        renderAnswerArea(question);

        boolean hasQuestion = question != null;
        btnSubmit.setEnabled(hasQuestion);
        btnPrev.setEnabled(hasQuestion && index > 0);
        btnNext.setEnabled(hasQuestion && index < questions.size() - 1);
    }

    /* ---------- 按题型渲染作答区 ---------- */

    private void renderAnswerArea(Question question) {
        answerContainer.removeAllViews();
        selectedSingle = -1;
        multiBoxes.clear();
        judgeGroup = null;
        inputAnswer = null;

        if (question == null) {
            return;
        }
        List<String> options = OptionCodec.decode(question.options);
        switch (question.type) {
            case Question.TYPE_SINGLE:
                buildRadioOptions(options);
                break;
            case Question.TYPE_MULTI:
                buildMultiOptions(options);
                break;
            case Question.TYPE_JUDGE:
                buildJudgeOptions();
                break;
            default:
                buildInput();
                break;
        }
    }

    private void buildRadioOptions(List<String> options) {
        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);
        for (int i = 0; i < options.size(); i++) {
            RadioButton button = new RadioButton(this);
            button.setId(View.generateViewId());
            button.setText(OptionCodec.letter(i) + ". " + options.get(i));
            styleChoice(button);
            group.addView(button);
        }
        group.setOnCheckedChangeListener((g, checkedId) ->
                selectedSingle = g.indexOfChild(g.findViewById(checkedId)));
        answerContainer.addView(group);
    }

    private void buildMultiOptions(List<String> options) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < options.size(); i++) {
            CheckBox box = new CheckBox(this);
            box.setText(OptionCodec.letter(i) + ". " + options.get(i));
            styleChoice(box);
            multiBoxes.add(box);
            container.addView(box);
        }
        answerContainer.addView(container);
    }

    private void buildJudgeOptions() {
        judgeGroup = new RadioGroup(this);
        judgeGroup.setOrientation(RadioGroup.VERTICAL);
        String[] labels = {getString(R.string.qa_true), getString(R.string.qa_false)};
        for (String label : labels) {
            RadioButton button = new RadioButton(this);
            button.setId(View.generateViewId());
            button.setText(label);
            styleChoice(button);
            judgeGroup.addView(button);
        }
        answerContainer.addView(judgeGroup);
    }

    private void buildInput() {
        View view = getLayoutInflater().inflate(R.layout.view_answer_input, answerContainer, false);
        if (view instanceof EditText) {
            inputAnswer = (EditText) view;
        }
        answerContainer.addView(view);
    }

    private void styleChoice(TextView view) {
        view.setTextColor(getColor(R.color.text_primary));
        view.setTextSize(16f);
        view.setPadding(dp(4), dp(10), dp(4), dp(10));
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    /* ---------- 作答 / 判分 ---------- */

    private void move(int delta) {
        if (current() == null) {
            Toast.makeText(this, R.string.toast_no_question, Toast.LENGTH_SHORT).show();
            return;
        }
        int target = index + delta;
        if (target < 0 || target >= questions.size()) {
            return;
        }
        index = target;
        render();
    }

    private void submit() {
        Question question = current();
        if (question == null) {
            Toast.makeText(this, R.string.toast_no_question, Toast.LENGTH_SHORT).show();
            return;
        }

        switch (question.type) {
            case Question.TYPE_SINGLE: {
                if (selectedSingle < 0) {
                    Toast.makeText(this, R.string.qa_choice_needed, Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean correct = AnswerCodec.single(selectedSingle)
                        .equals(AnswerCodec.single(AnswerCodec.singleIndex(question.answer)));
                showResult(question, correct);
                break;
            }
            case Question.TYPE_MULTI: {
                List<Integer> picked = new ArrayList<>();
                for (int i = 0; i < multiBoxes.size(); i++) {
                    if (multiBoxes.get(i).isChecked()) {
                        picked.add(i);
                    }
                }
                if (picked.isEmpty()) {
                    Toast.makeText(this, R.string.qa_choice_needed, Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean correct = AnswerCodec.multi(picked)
                        .equals(AnswerCodec.multi(AnswerCodec.multiIndexes(question.answer)));
                showResult(question, correct);
                break;
            }
            case Question.TYPE_JUDGE: {
                if (judgeGroup == null || judgeGroup.getCheckedRadioButtonId() == View.NO_ID) {
                    Toast.makeText(this, R.string.qa_choice_needed, Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean picked = judgeGroup.indexOfChild(
                        judgeGroup.findViewById(judgeGroup.getCheckedRadioButtonId())) == 0;
                showResult(question, AnswerCodec.judge(picked)
                        .equals(AnswerCodec.judge(AnswerCodec.judgeValue(question.answer))));
                break;
            }
            case Question.TYPE_FILL: {
                String input = inputAnswer == null ? "" : inputAnswer.getText().toString().trim();
                if (input.isEmpty()) {
                    Toast.makeText(this, R.string.toast_need_answer, Toast.LENGTH_SHORT).show();
                    return;
                }
                showResult(question, same(question.answer, input));
                break;
            }
            default: {
                // 简答题：不自动判分，只给参考答案
                StringBuilder text = new StringBuilder(getString(R.string.qa_essay_no_auto));
                text.append("\n").append(QuestionFormat.answerText(this, question));
                if (QuestionFormat.notEmpty(question.explanation)) {
                    text.append("\n\n").append(getString(R.string.qa_note_prefix))
                            .append(question.explanation.trim());
                }
                tvCorrect.setText(text.toString());
                break;
            }
        }
    }

    private void showResult(Question question, boolean correct) {
        StringBuilder text = new StringBuilder();
        text.append(correct ? getString(R.string.qa_result_right) : getString(R.string.qa_result_wrong));
        text.append("\n").append(getString(R.string.qa_correct_label))
                .append(QuestionFormat.answerText(this, question));
        if (QuestionFormat.notEmpty(question.explanation)) {
            text.append("\n\n").append(getString(R.string.qa_note_prefix))
                    .append(question.explanation.trim());
        }
        tvCorrect.setText(text.toString());
    }

    /** 忽略空格和大小写的比对；输入太短就不算"包含即正确" */
    private boolean same(String right, String input) {
        String expected = right == null ? "" : right.replaceAll("\\s+", "");
        String given = input.replaceAll("\\s+", "");
        if (expected.equalsIgnoreCase(given)) {
            return true;
        }
        return given.length() >= 4
                && expected.toLowerCase().contains(given.toLowerCase());
    }

    private void editNote() {
        Question question = current();
        if (question == null) {
            Toast.makeText(this, R.string.toast_no_question, Toast.LENGTH_SHORT).show();
            return;
        }
        AppDialogs.editNote(this, question, () -> { });
    }

    private void editCurrentQuestion() {
        Question question = current();
        if (question == null) {
            Toast.makeText(this, R.string.toast_no_question, Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(EditQuestionActivity.newIntentForEdit(this, question.id));
    }
}
