package com.example.examreborn;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.examreborn.adapter.BankAdapter;
import com.example.examreborn.data.DataStore;
import com.example.examreborn.dialog.AppDialogs;
import com.example.examreborn.model.QuestionBank;
import com.example.examreborn.util.GlassUtils;
import com.example.examreborn.util.InsetUtils;
import com.google.android.material.appbar.MaterialToolbar;

/** 查看库：题库列表 + 新建题库 + 批量导入 */
public class BankListActivity extends AppCompatActivity {

    private BankAdapter adapter;
    private TextView tvEmpty;

    /** SAF 选文件：txt / md / json 都能选（有些 ROM 把 .md 认成八位字节流，所以放宽 MIME） */
    private final ActivityResultLauncher<String[]> docPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) {
                    return;
                }
                try {
                    getContentResolver().takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {
                    // 有些 provider 不给持久权限，不影响本次读取
                }
                startActivity(ImportPreviewActivity.newIntent(this, uri));
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bank_list);
        InsetUtils.lightStatusBar(this, true);
        GlassUtils.blurMesh(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        InsetUtils.padTop(findViewById(R.id.toolbarBar));
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_add_bank) {
                AppDialogs.addBank(this, this::loadBanks);
                return true;
            }
            if (item.getItemId() == R.id.action_import) {
                docPicker.launch(new String[]{
                        "text/*", "application/json", "application/octet-stream"});
                return true;
            }
            return false;
        });

        tvEmpty = findViewById(R.id.tvEmpty);

        adapter = new BankAdapter(new BankAdapter.Listener() {
            @Override
            public void onBankClick(QuestionBank bank) {
                openQa(bank.id);
            }

            @Override
            public void onAddQuestion(QuestionBank bank) {
                startActivity(EditQuestionActivity.newIntentForAdd(BankListActivity.this, bank.id));
            }
        });

        RecyclerView list = findViewById(R.id.rvBanks);
        InsetUtils.padBottom(list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBanks();
    }

    private void loadBanks() {
        DataStore.loadBanks(list -> {
            adapter.setItems(list);
            tvEmpty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void openQa(long bankId) {
        Intent intent = new Intent(this, QaActivity.class);
        intent.putExtra(QaActivity.EXTRA_BANK_ID, bankId);
        startActivity(intent);
    }
}
