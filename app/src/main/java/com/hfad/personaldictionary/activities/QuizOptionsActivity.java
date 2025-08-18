package com.hfad.personaldictionary.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.hfad.personaldictionary.R;

import java.util.Objects;

public class QuizOptionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_options); // You'll create this XML layout


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText(getResources().getString(R.string.quizOptions));

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.arrow_back_24);

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));

        // Initialize buttons
        Button btnWordMeaning = findViewById(R.id.btnWordMeaning);
        Button btnSynonymsAntonyms = findViewById(R.id.btnSynonymsAntonyms);
        Button btnSpellingTest = findViewById(R.id.btnSpellingTest);
        Button wordCompletionQuiz = findViewById(R.id.btnWordCompletionQuiz);
        Button btnTimedQuiz = findViewById(R.id.btnTimedQuiz);
        Button btnFavoriteWordsQuiz = findViewById(R.id.btnFavoriteWordsQuiz);

        // Set click listeners for each button
        btnWordMeaning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start WordMeaningQuizActivity
                Intent intent = new Intent(QuizOptionsActivity.this, WordMeaningQuizActivity.class);
                startActivity(intent);
            }
        });

        btnSynonymsAntonyms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start SynonymsAntonymsChallengeActivity
                Intent intent = new Intent(QuizOptionsActivity.this, SynonymsAntonymsQuizActivity.class);
                startActivity(intent);
            }
        });

        btnSpellingTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start SpellingTestActivity
                Intent intent = new Intent(QuizOptionsActivity.this, SpellingQuizActivity.class);
                startActivity(intent);
            }
        });
        wordCompletionQuiz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start SpellingTestActivity
                Intent intent = new Intent(QuizOptionsActivity.this, WordCompletionQuizActivity.class);
                startActivity(intent);
            }
        });

        btnTimedQuiz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start TimedQuizActivity
                Intent intent = new Intent(QuizOptionsActivity.this, TimedQuizActivity.class);
                startActivity(intent);
            }
        });
        btnFavoriteWordsQuiz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start FavoriteWordsQuizActivity
                Intent intent = new Intent(QuizOptionsActivity.this, FavoriteWordsQuizActivity.class);
                startActivity(intent);
            }
        });
    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}