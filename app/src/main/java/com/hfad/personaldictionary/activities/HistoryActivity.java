package com.hfad.personaldictionary.activities; // Adjust to your actual package

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hfad.personaldictionary.R;
import com.hfad.personaldictionary.activities.WordDetailsActivity;
import com.hfad.personaldictionary.adapters.HistoryAdapter;
import com.hfad.personaldictionary.models.WordHistoryEntry;
import com.hfad.personaldictionary.database.HistoryDatabaseHelper; // NEW
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService; // NEW
import java.util.concurrent.Executors; // NEW

public class HistoryActivity extends AppCompatActivity
        implements HistoryAdapter.OnItemClickListener,
        HistoryAdapter.OnDeleteItemClickListener {

    private RecyclerView recyclerViewHistory;
    private HistoryAdapter historyAdapter;
    private TextView tvNoHistory;
    private Button btnClearHistory;

    // --- MODIFIED: Use a custom database helper ---
    private HistoryDatabaseHelper historyDbHelper;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText(getResources().getString(R.string.history));

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.arrow_back_24);

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));

        // --- NEW: Initialize the database helper ---
        historyDbHelper = new HistoryDatabaseHelper(this);

        tvNoHistory = findViewById(R.id.tvNoHistory);
        btnClearHistory = findViewById(R.id.btnClearHistory);
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);

        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));

        // Initial data load
        loadHistoryData();

        btnClearHistory.setOnClickListener(v -> {
            // --- MODIFIED: Call database helper on a background thread ---
            executorService.execute(() -> {
                historyDbHelper.clearHistory();
                runOnUiThread(() -> {
                    Toast.makeText(HistoryActivity.this, "History cleared!", Toast.LENGTH_SHORT).show();
                    loadHistoryData();
                });
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistoryData();
    }

    private void loadHistoryData() {
        // --- MODIFIED: Load data on a background thread ---
        executorService.execute(() -> {
            final List<WordHistoryEntry> history = historyDbHelper.getAllHistory();
            runOnUiThread(() -> {
                if (history.isEmpty()) {
                    tvNoHistory.setVisibility(View.VISIBLE);
                    recyclerViewHistory.setVisibility(View.GONE);
                    btnClearHistory.setEnabled(false);
                } else {
                    tvNoHistory.setVisibility(View.GONE);
                    recyclerViewHistory.setVisibility(View.VISIBLE);
                    btnClearHistory.setEnabled(true);

                    if (historyAdapter == null) {
                        historyAdapter = new HistoryAdapter(history, this, this);
                        recyclerViewHistory.setAdapter(historyAdapter);
                    } else {
                        historyAdapter.updateData(history);
                    }
                }
            });
        });
    }

    @Override
    public void onItemClick(WordHistoryEntry entry) {
        Intent intent = new Intent(this, WordDetailsActivity.class);
        intent.putExtra("word", entry.getWord());    // Pass the word
        intent.putExtra("word_id", entry.getId());   // Pass the ID!
        intent.putExtra("caller", "HistoryActivity");
        startActivity(intent);
    }

    @Override
    public void onDeleteItemClick(String word) {
        // --- MODIFIED: Call database helper on a background thread ---
        executorService.execute(() -> {
            historyDbHelper.deleteHistoryEntry(word);
            runOnUiThread(() -> {
                Toast.makeText(this, "'" + word + "' removed from history.", Toast.LENGTH_SHORT).show();
                loadHistoryData();
            });
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}