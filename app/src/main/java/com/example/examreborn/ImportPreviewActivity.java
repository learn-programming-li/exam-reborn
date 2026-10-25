package com.example.examreborn;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.examreborn.adapter.ImportAdapter;
import com.example.examreborn.data.DataStore;
import com.example.examreborn.model.BankWithCount;
import com.example.examreborn.model.Question;
import com.example.examreborn.model.QuestionBank;
import com.example.examreborn.parse.PaperParser;
import com.example.examreborn.util.DocFileReader;
import com.example.examreborn.util.GlassUtils;
import com.example.examreborn.util.InsetUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 导入预览：先看识别结果，再决定导不导、导到哪个题库 */
public class ImportPreviewActivity extends AppCompatActivity {

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<QuestionBank> targets = new ArrayList<>();

    private ImportAdapter adapter;
    private TextInputEditText etPaperName;
    private TextInputEditText etDuration;
    private Spinner spTarget;
    private TextView tvSummary;
    private TextView tvWarnings;
    private TextView btnToggleAll;
    private MaterialButton btnImport;

    private PaperParser.Result parsed;

    public static Intent newIntent(Context context, Uri uri) {
        Intent intent = new Intent(context, ImportPreviewActivity.class);
        intent.setData(uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_import_preview);
        InsetUtils.lightStatusBar(this, true);
        GlassUtils.blurMesh(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        InsetUtils.padTop(findViewById(R.id.toolbarBar));
        toolbar.setNavigationOnClickListener(v -> finish());

        etPaperName = findViewById(R.id.etPaperName);
        etDuration = findViewById(R.id.etDuration);
        spTarget = findViewById(R.id.spTarget);
        tvSummary = findViewById(R.id.tvSummary);
        tvWarnings = findViewById(R.id.tvWarnings);
        btnToggleAll = findViewById(R.id.btnToggleAll);
        btnImport = findViewById(R.id.btnImport);

        adapter = new ImportAdapter();
        RecyclerView list = findViewById(R.id.rvPreview);
        InsetUtils.padBottom(list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        btnToggleAll.setOnClickListener(v -> {
            boolean selectAll = !adapter.allSelected();
            adapter.setAllSelected(selectAll);
            btnToggleAll.setText(selectAll ? R.string.import_unselect_all : R.string.import_select_all);
        });

        btnImport.setOnClickListener(v -> doImport());

        loadTargets();

        Uri uri = getIntent().getData();
        if (uri == null) {
            finish();
            return;
        }
        readAndParse(uri);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
    }

    private void readAndParse(Uri uri) {
        tvSummary.setText(R.string.import_reading);
        io.execute(() -> {
            String text;
            try {
                text = DocFileReader.read(getApplicationContext(), uri);
            } catch (Exception e) {
                main.post(() -> {
                    Toast.makeText(this, R.string.import_failed, Toast.LENGTH_LONG).show();
                    finish();
                });
                return;
            }
            String fileName = DocFileReader.displayName(getApplicationContext(), uri);
            PaperParser.Result result = PaperParser.parse(text, fileName);
            main.post(() -> show(result));
        });
    }

    private void show(PaperParser.Result result) {
        parsed = result;
        etPaperName.setText(result.paper.name);
        etDuration.setText(result.paper.duration > 0 ? String.valueOf(result.paper.duration) : "");

        adapter.setItems(result.questions);
        btnToggleAll.setText(R.string.import_select_all);
        btnImport.setEnabled(!result.questions.isEmpty());

        if (result.questions.isEmpty()) {
            tvSummary.setText(R.string.import_empty);
        } else {
            tvSummary.setText(getString(R.string.import_summary, result.questions.size())
                    + "\n" + getString(R.string.import_types,
                    result.countOf(Question.TYPE_SINGLE),
                    result.countOf(Question.TYPE_MULTI),
                    result.countOf(Question.TYPE_JUDGE),
                    result.countOf(Question.TYPE_FILL),
                    result.countOf(Question.TYPE_ESSAY)));
        }

        if (result.warnings.isEmpty()) {
            tvWarnings.setVisibility(View.GONE);
        } else {
            StringBuilder builder = new StringBuilder(
                    getString(R.string.import_warnings, result.warnings.size()));
            for (String warning : result.warnings) {
                builder.append("\n· ").append(warning);
            }
            tvWarnings.setText(builder.toString());
            tvWarnings.setVisibility(View.VISIBLE);
        }
    }

    private void loadTargets() {
        DataStore.loadBanks(list -> {
            List<String> labels = new ArrayList<>();
            labels.add(getString(R.string.import_target_new));
            targets.clear();
            if (list != null) {
                for (BankWithCount item : list) {
                    targets.add(item.bank);
                    labels.add(item.bank.name);
                }
            }
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, labels);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spTarget.setAdapter(spinnerAdapter);
        });
    }

    private void doImport() {
        List<Question> selected = adapter.selectedQuestions();
        if (selected.isEmpty()) {
            Toast.makeText(this, R.string.import_none_selected, Toast.LENGTH_SHORT).show();
            return;
        }
        String name = text(etPaperName);
        int position = spTarget.getSelectedItemPosition();
        boolean createNew = position <= 0;

        if (createNew && name.isEmpty()) {
            Toast.makeText(this, R.string.import_need_name, Toast.LENGTH_SHORT).show();
            return;
        }

        btnImport.setEnabled(false);
        if (createNew) {
            QuestionBank bank = QuestionBank.create(name, parseInt(text(etDuration)), "");
            DataStore.addBank(bank, id -> DataStore.addQuestions(id, selected, this::finishWithToast));
        } else {
            QuestionBank target = targets.get(Math.min(position - 1, targets.size() - 1));
            DataStore.addQuestions(target.id, selected, this::finishWithToast);
        }
    }

    private void finishWithToast(int count) {
        Toast.makeText(this, getString(R.string.import_done, count), Toast.LENGTH_SHORT).show();
        finish();
    }

    private static String text(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
