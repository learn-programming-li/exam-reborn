package com.example.examreborn;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.examreborn.adapter.BankAdapter;
import com.example.examreborn.data.DataStore;
import com.example.examreborn.model.QuestionBank;
import com.example.examreborn.util.GlassUtils;
import com.example.examreborn.util.InsetUtils;

/** 首页「博问」：开始问答 / 查看题库 / 我的题库 */
public class HomeActivity extends AppCompatActivity {

    private BankAdapter adapter;
    private TextView tvStartHint;
    private long firstBankId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        InsetUtils.lightStatusBar(this, true);
        InsetUtils.padAll(findViewById(R.id.home));
        GlassUtils.blurMesh(this);

        adapter = new BankAdapter(new BankAdapter.Listener() {
            @Override
            public void onBankClick(QuestionBank bank) {
                openQa(bank.id);
            }

            @Override
            public void onAddQuestion(QuestionBank bank) {
                startActivity(EditQuestionActivity.newIntentForAdd(HomeActivity.this, bank.id));
            }
        });

        RecyclerView list = findViewById(R.id.rvBanks);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        tvStartHint = findViewById(R.id.tvStartHint);

        View btnStart = findViewById(R.id.btnStartQa);
        btnStart.setOnClickListener(v -> {
            if (firstBankId > 0) {
                openQa(firstBankId);
            }
        });

        View btnView = findViewById(R.id.btnViewBanks);
        btnView.setOnClickListener(v -> startActivity(new Intent(this, BankListActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBanks();
    }

    private void loadBanks() {
        DataStore.loadBanks(list -> {
            adapter.setItems(list);
            if (list == null || list.isEmpty()) {
                firstBankId = -1;
                tvStartHint.setText("");
                return;
            }
            QuestionBank first = list.get(0).bank;
            firstBankId = first.id;
            tvStartHint.setText(getString(R.string.home_start_hint, first.name));
        });
    }

    private void openQa(long bankId) {
        Intent intent = new Intent(this, QaActivity.class);
        intent.putExtra(QaActivity.EXTRA_BANK_ID, bankId);
        startActivity(intent);
    }
}
