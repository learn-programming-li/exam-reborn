package com.example.examreborn.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.examreborn.R;
import com.example.examreborn.model.Question;
import com.example.examreborn.util.QuestionFormat;

import java.util.ArrayList;
import java.util.List;

/** 导入预览列表：每题一张卡，可勾选 */
public class ImportAdapter extends RecyclerView.Adapter<ImportAdapter.Holder> {

    public static class Item {
        public final Question question;
        public boolean selected = true;

        Item(Question question) {
            this.question = question;
        }
    }

    private final List<Item> items = new ArrayList<>();

    public void setItems(List<Question> questions) {
        items.clear();
        if (questions != null) {
            for (Question question : questions) {
                items.add(new Item(question));
            }
        }
        notifyDataSetChanged();
    }

    public int selectedCount() {
        int count = 0;
        for (Item item : items) {
            if (item.selected) {
                count++;
            }
        }
        return count;
    }

    public boolean allSelected() {
        if (items.isEmpty()) {
            return false;
        }
        for (Item item : items) {
            if (!item.selected) {
                return false;
            }
        }
        return true;
    }

    public void setAllSelected(boolean selected) {
        for (Item item : items) {
            item.selected = selected;
        }
        notifyDataSetChanged();
    }

    public List<Question> selectedQuestions() {
        List<Question> result = new ArrayList<>();
        for (Item item : items) {
            if (item.selected) {
                result.add(item.question);
            }
        }
        return result;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_import_question, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Item item = items.get(position);
        holder.type.setText(QuestionFormat.typeName(holder.itemView.getContext(), item.question.type));
        holder.title.setText(item.question.title);

        String answer = QuestionFormat.answerText(holder.itemView.getContext(), item.question);
        holder.answer.setText(holder.itemView.getContext().getString(R.string.qa_correct_label) + answer);

        holder.check.setOnCheckedChangeListener(null);
        holder.check.setChecked(item.selected);
        holder.check.setOnCheckedChangeListener((buttonView, isChecked) -> item.selected = isChecked);
        holder.itemView.setOnClickListener(v -> {
            item.selected = !item.selected;
            holder.check.setChecked(item.selected);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {

        final CheckBox check;
        final TextView type;
        final TextView title;
        final TextView answer;

        Holder(@NonNull View itemView) {
            super(itemView);
            check = itemView.findViewById(R.id.cbSelected);
            type = itemView.findViewById(R.id.tvType);
            title = itemView.findViewById(R.id.tvTitle);
            answer = itemView.findViewById(R.id.tvAnswer);
        }
    }
}
