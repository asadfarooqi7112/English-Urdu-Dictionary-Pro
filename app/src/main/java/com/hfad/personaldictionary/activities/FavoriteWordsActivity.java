package com.hfad.personaldictionary.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.DividerItemDecoration; // Import this class
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hfad.personaldictionary.R;
import com.hfad.personaldictionary.models.FavoriteWord;
import com.hfad.personaldictionary.database.FavoriteWordsDbHelper;
import com.hfad.personaldictionary.adapters.FavoriteWordsAdapter;

import java.util.List;
import java.util.ArrayList; // Used for initial adapter data
import java.util.Objects;

public class FavoriteWordsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvNoFavorites;
    private FavoriteWordsDbHelper dbHelper;
    private FavoriteWordsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_words);

        recyclerView = findViewById(R.id.rv_favorite_words);
        tvNoFavorites = findViewById(R.id.tv_no_favorites);

        // --- Toolbar Setup ---
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText(getResources().getString(R.string.favorites));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.arrow_back_24);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            if (window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.setStatusBarColor(ContextCompat.getColor(this, R.color.quiz_background));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    View decor = window.getDecorView();
                    if (decor != null) {
                        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(window, decor);
                        if (insetsController != null) {
                            insetsController.setAppearanceLightStatusBars(true);
                        }
                    }
                }
            }
        }

        dbHelper = new FavoriteWordsDbHelper(this);

        // Setup RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // --- ADDED: DividerItemDecoration for lines between items ---
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                recyclerView.getContext(),
                layoutManager.getOrientation()
        );
        recyclerView.addItemDecoration(dividerItemDecoration);
        // --- END ADDED ---

        // Initialize adapter once with an empty list
        adapter = new FavoriteWordsAdapter(new ArrayList<>(), new FavoriteWordsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(FavoriteWord word) {
                // Handle click on a favorite word (e.g., open its details)
                Intent intent = new Intent(FavoriteWordsActivity.this, WordDetailsActivity.class);
                intent.putExtra("word", word.getWord());
                intent.putExtra("word_id", word.getWordId());
                startActivity(intent);
            }

            @Override
            public void onRemoveClick(FavoriteWord word) {
                // Handle the 'remove' button click for a favorite word
                dbHelper.deleteFavoriteWord(word.getWord());
                Toast.makeText(FavoriteWordsActivity.this, "'"+word.getWord() + "' removed from favorites", Toast.LENGTH_SHORT).show();
                loadFavoriteWords(); // Reload list after removal
            }
        });
        recyclerView.setAdapter(adapter);

        // Load data initially
        loadFavoriteWords();
    }

    private void loadFavoriteWords() {
        List<FavoriteWord> favoriteWords = dbHelper.getAllFavoriteWords();

        if (favoriteWords.isEmpty()) {
            tvNoFavorites.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvNoFavorites.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.updateData(favoriteWords); // Update existing adapter data
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // It's good practice to reload data when the activity resumes,
        // in case favorites were added/removed from another screen.
        loadFavoriteWords();
    }
}