package com.example.examreborn.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.examreborn.R;
import com.example.examreborn.data.DataStore;
import com.example.examreborn.model.Question;
import com.example.examreborn.model.QuestionBank;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * 还有两个短表单用对话框就够：新建题库、编辑笔记。
 * 添加/编辑题目已经改成全屏编辑页 EditQuestionActivity。
 */
public final class AppDialogs {

    private AppDialogs() {
    }

    public interface OnChanged {
        void onChanged();
    }

    /** 新建题库 */
    public static void addBank(Context context, OnChanged callback) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_bank, null, false);
        EditText etName = view.findViewById(R.id.etBankName);
        EditText etNote = view.findViewById(R.id.etBankNote);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(view)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_confirm, null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = text(etName);
            if (name.isEmpty()) {
                Toast.makeText(context, R.string.toast_need_name, Toast.LENGTH_SHORT).show();
                return;
            }
            DataStore.addBank(QuestionBank.create(name, text(etNote)), id -> {
                if (callback != null) {
                    callback.onChanged();
                }
            });
            dialog.dismiss();
        });
    }

    /** 编辑当前题的解析（写回数据库，新开编辑页时能读到） */
    public static void editNote(Context context, Question question, OnChanged callback) {
        if (question == null) {
            return;
        }
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_note, null, false);
        EditText etNote = view.findViewById(R.id.etNote);
        etNote.setText(question.explanation == null ? "" : question.explanation);
        etNote.setSelection(etNote.getText().length());

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(view)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save, null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            question.explanation = text(etNote);
            DataStore.updateQuestion(question, () -> {
                if (callback != null) {
                    callback.onChanged();
                }
            });
            dialog.dismiss();
        });
    }

    private static String text(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
