package com.example.examreborn;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.examreborn.data.AnswerCodec;
import com.example.examreborn.data.DataStore;
import com.example.examreborn.data.OptionCodec;
import com.example.examreborn.model.Question;
import com.example.examreborn.util.GlassUtils;
import com.example.examreborn.util.InsetUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * 全屏题目编辑页，添加 / 编辑共用。
 * 带 EXTRA_QUESTION_ID 是编辑，只带 EXTRA_BANK_ID 是新增。
 */
public class EditQuestionActivity extends AppCompatActivity {

    public static final String EXTRA_BANK_ID = "extra_bank_id";
    public static final String EXTRA_QUESTION_ID = "extra_question_id";

    /** 顺序必须和 Question.TYPE_SINGLE..TYPE_ESSAY 对齐 */
    private static final int[] TYPE_CHIPS = {
            R.id.chipTypeSingle,
            R.id.chipTypeMulti,
            R.id.chipTypeJudge,
            R.id.chipTypeFill,
            R.id.chipTypeEssay
    };

    private static final int[] DIFFICULTY_CHIPS = {
            R.id.chipDifficulty1,
            R.id.chipDifficulty2,
            R.id.chipDifficulty3,
            R.id.chipDifficulty4,
            R.id.chipDifficulty5
    };

    public static Intent newIntentForAdd(Context context, long bankId) {
        Intent intent = new Intent(context, EditQuestionActivity.class);
        intent.putExtra(EXTRA_BANK_ID, bankId);
        return intent;
    }

    public static Intent newIntentForEdit(Context context, long questionId) {
        Intent intent = new Intent(context, EditQuestionActivity.class);
        intent.putExtra(EXTRA_QUESTION_ID, questionId);
        return intent;
    }

    private TextInputLayout tilTitle;
    private TextInputLayout tilAnswer;
    private TextInputEditText etTitle;
    private TextInputEditText etAnswer;
    private TextInputEditText etExplanation;
    private TextInputEditText etChapter;
    private TextInputEditText etTags;
    private TextInputEditText etScore;
    private ChipGroup chipGroupType;
    private ChipGroup chipGroupJudge;
    private ChipGroup chipGroupDifficulty;
    private View optionSection;
    private LinearLayout optionContainer;

    private long bankId = -1;
    private long questionId = -1;
    private Question editing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_question);
        InsetUtils.lightStatusBar(this, true);
        GlassUtils.blurMesh(this);

        bankId = getIntent().getLongExtra(EXTRA_BANK_ID, -1);
        questionId = getIntent().getLongExtra(EXTRA_QUESTION_ID, -1);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        InsetUtils.padTop(findViewById(R.id.toolbarBar));
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_save) {
                save();
                return true;
            }
            return false;
        });

        tilTitle = findViewById(R.id.tilTitle);
        tilAnswer = findViewById(R.id.tilAnswer);
        etTitle = findViewById(R.id.etTitle);
        etAnswer = findViewById(R.id.etAnswer);
        etExplanation = findViewById(R.id.etExplanation);
        etChapter = findViewById(R.id.etChapter);
        etTags = findViewById(R.id.etTags);
        etScore = findViewById(R.id.etScore);
        chipGroupType = findViewById(R.id.chipGroupType);
        chipGroupJudge = findViewById(R.id.chipGroupJudge);
        chipGroupDifficulty = findViewById(R.id.chipGroupDifficulty);
        optionSection = findViewById(R.id.optionSection);
        optionContainer = findViewById(R.id.optionContainer);

        chipGroupType.setOnCheckedChangeListener((group, checkedId) -> applyTypeUi());
        findViewById(R.id.btnAddOption).setOnClickListener(v -> addOptionRow(""));

        if (questionId > 0) {
            toolbar.setTitle(R.string.editor_edit_title);
            DataStore.loadQuestion(questionId, this::bind);
        } else {
            toolbar.setTitle(R.string.editor_add_title);
            etScore.setText("1");
            selectDifficulty(3);
            selectType(Question.TYPE_SINGLE);
            etTitle.requestFocus();
        }
    }

    /* ---------- 读取已有题目 ---------- */

    private void bind(Question question) {
        if (question == null) {
            finish();
            return;
        }
        editing = question;
        bankId = question.bankId;

        etTitle.setText(question.title);
        etAnswer.setText(AnswerCodec.display(question.answer, question.type));
        etExplanation.setText(question.explanation);
        etChapter.setText(question.chapter);
        etTags.setText(question.tags);
        etScore.setText(String.valueOf(question.score <= 0 ? 1 : question.score));

        optionContainer.removeAllViews();
        if (question.type == Question.TYPE_SINGLE || question.type == Question.TYPE_MULTI) {
            for (String option : OptionCodec.decode(question.options)) {
                addOptionRow(option);
            }
        }
        selectType(question.type);
        selectDifficulty(question.difficulty);
        if (question.type == Question.TYPE_JUDGE) {
            chipGroupJudge.check(AnswerCodec.judgeValue(question.answer)
                    ? R.id.chipJudgeTrue : R.id.chipJudgeFalse);
        }
    }

    /* ---------- 选项行 ---------- */

    private void addOptionRow(String text) {
        View row = getLayoutInflater().inflate(R.layout.item_option_edit, optionContainer, false);
        EditText input = row.findViewById(R.id.etOption);
        input.setText(text == null ? "" : text);
        row.findViewById(R.id.btnRemoveOption).setOnClickListener(v -> {
            if (optionContainer.getChildCount() <= 2) {
                Toast.makeText(this, R.string.toast_option_needed, Toast.LENGTH_SHORT).show();
                return;
            }
            optionContainer.removeView(row);
        });
        optionContainer.addView(row);
    }

    private List<String> collectOptions() {
        List<String> options = new ArrayList<>();
        for (int i = 0; i < optionContainer.getChildCount(); i++) {
            EditText input = optionContainer.getChildAt(i).findViewById(R.id.etOption);
            String text = input == null || input.getText() == null
                    ? "" : input.getText().toString().trim();
            if (!text.isEmpty()) {
                options.add(text);
            }
        }
        return options;
    }

    /* ---------- 题型联动 ---------- */

    private int selectedType() {
        int checkedId = chipGroupType.getCheckedChipId();
        if (checkedId == View.NO_ID) {
            return Question.TYPE_SINGLE;
        }
        int position = chipGroupType.indexOfChild(chipGroupType.findViewById(checkedId));
        return Math.max(0, Math.min(position, TYPE_CHIPS.length - 1)) + 1;
    }

    private void selectType(int type) {
        int position = Math.max(0, Math.min(type - 1, TYPE_CHIPS.length - 1));
        chipGroupType.check(TYPE_CHIPS[position]);
    }

    private void applyTypeUi() {
        int type = selectedType();
        boolean choice = type == Question.TYPE_SINGLE || type == Question.TYPE_MULTI;
        boolean judge = type == Question.TYPE_JUDGE;

        optionSection.setVisibility(choice ? View.VISIBLE : View.GONE);
        chipGroupJudge.setVisibility(judge ? View.VISIBLE : View.GONE);
        tilAnswer.setVisibility(judge ? View.GONE : View.VISIBLE);

        if (type == Question.TYPE_SINGLE) {
            tilAnswer.setHint(R.string.hint_answer_single);
        } else if (type == Question.TYPE_MULTI) {
            tilAnswer.setHint(R.string.hint_answer_multi);
        } else {
            tilAnswer.setHint(R.string.hint_answer_text);
        }

        if (choice) {
            while (optionContainer.getChildCount() < 2) {
                addOptionRow("");
            }
        }
        if (judge && chipGroupJudge.getCheckedChipId() == View.NO_ID) {
            chipGroupJudge.check(R.id.chipJudgeTrue);
        }
    }

    private int selectedDifficulty() {
        int checkedId = chipGroupDifficulty.getCheckedChipId();
        if (checkedId == View.NO_ID) {
            return 3;
        }
        return chipGroupDifficulty.indexOfChild(chipGroupDifficulty.findViewById(checkedId)) + 1;
    }

    private void selectDifficulty(int difficulty) {
        int position = Math.max(0, Math.min(difficulty - 1, DIFFICULTY_CHIPS.length - 1));
        chipGroupDifficulty.check(DIFFICULTY_CHIPS[position]);
    }

    /* ---------- 保存 ---------- */

    private void save() {
        int type = selectedType();
        String title = text(etTitle);
        if (title.isEmpty()) {
            Toast.makeText(this, R.string.toast_need_title, Toast.LENGTH_SHORT).show();
            tilTitle.requestFocus();
            return;
        }
        if (editing == null) {
            if (bankId <= 0) {
                finish();
                return;
            }
            editing = Question.create(bankId, type);
        }

        // 先按题型校验，不通过就不落库
        if (type == Question.TYPE_SINGLE || type == Question.TYPE_MULTI) {
            List<String> options = collectOptions();
            if (options.size() < 2) {
                Toast.makeText(this, R.string.toast_option_needed, Toast.LENGTH_SHORT).show();
                return;
            }
            List<Integer> indexes = AnswerCodec.parseLetters(text(etAnswer));
            if (indexes.isEmpty()) {
                Toast.makeText(this, R.string.toast_answer_needed, Toast.LENGTH_SHORT).show();
                tilAnswer.requestFocus();
                return;
            }
            for (int index : indexes) {
                if (index >= options.size()) {
                    Toast.makeText(this, R.string.toast_answer_needed, Toast.LENGTH_SHORT).show();
                    tilAnswer.requestFocus();
                    return;
                }
            }
            editing.options = OptionCodec.encode(options);
            editing.answer = type == Question.TYPE_SINGLE
                    ? AnswerCodec.single(indexes.get(0))
                    : AnswerCodec.multi(indexes);
        } else if (type == Question.TYPE_JUDGE) {
            int checkedId = chipGroupJudge.getCheckedChipId();
            if (checkedId == View.NO_ID) {
                Toast.makeText(this, R.string.toast_answer_needed, Toast.LENGTH_SHORT).show();
                return;
            }
            editing.options = "";
            editing.answer = AnswerCodec.judge(checkedId == R.id.chipJudgeTrue);
        } else {
            editing.options = "";
            editing.answer = text(etAnswer);
        }

        editing.type = type;
        editing.title = title;
        editing.explanation = text(etExplanation);
        editing.chapter = text(etChapter);
        editing.tags = text(etTags);
        editing.difficulty = selectedDifficulty();
        editing.score = parseInt(text(etScore), 1);

        if (editing.id == 0) {
            DataStore.addQuestion(editing, this::onSaved);
        } else {
            DataStore.updateQuestion(editing, this::onSaved);
        }
    }

    private void onSaved() {
        Toast.makeText(this, R.string.toast_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private static String text(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
