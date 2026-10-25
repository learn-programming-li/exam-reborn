package com.example.examreborn.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.examreborn.R;
import com.example.examreborn.model.BankWithCount;
import com.example.examreborn.model.QuestionBank;

import java.util.ArrayList;
import java.util.List;

/** 题库列表：点卡片进问答，点右边圆圈加题目 */
public class BankAdapter extends RecyclerView.Adapter<BankAdapter.BankHolder> {

    public interface Listener {
        void onBankClick(QuestionBank bank);

        void onAddQuestion(QuestionBank bank);
    }

    private final List<BankWithCount> items = new ArrayList<>();
    private final Listener listener;

    public BankAdapter(Listener listener) {
        this.listener = listener;
    }

    /** 从数据库读回来的数据整体替换 */
    public void setItems(List<BankWithCount> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BankHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bank, parent, false);
        return new BankHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BankHolder holder, int position) {
        BankWithCount item = items.get(position);
        QuestionBank bank = item.bank;

        holder.name.setText(bank.name);

        String countText = holder.itemView.getContext()
                .getString(R.string.bank_question_count, item.questionCount);
        String meta = bank.duration > 0
                ? holder.itemView.getContext().getString(R.string.paper_meta_duration, bank.duration)
                + " · " + countText
                : countText;
        String note = bank.note == null ? "" : bank.note.trim();
        holder.note.setText(note.isEmpty()
                ? meta
                : holder.itemView.getContext().getString(R.string.bank_meta_join, note, meta));

        holder.itemView.setOnClickListener(v -> listener.onBankClick(bank));
        holder.expand.setOnClickListener(v -> listener.onAddQuestion(bank));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class BankHolder extends RecyclerView.ViewHolder {

        final TextView name;
        final TextView note;
        final ImageView expand;

        BankHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvBankName);
            note = itemView.findViewById(R.id.tvBankNote);
            expand = itemView.findViewById(R.id.btnExpand);
        }
    }
}
