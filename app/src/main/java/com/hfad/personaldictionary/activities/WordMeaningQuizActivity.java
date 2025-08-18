package com.hfad.personaldictionary.activities;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.hfad.personaldictionary.database.DatabaseHelper;
import com.hfad.personaldictionary.R;
import com.hfad.personaldictionary.fragments.QuizResultsDialogFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale; // Import Locale
import java.util.Objects;
import java.util.Random;

public class WordMeaningQuizActivity extends AppCompatActivity {

    private TextView tvQuestionCounter, tvScore, tvWord, tvFeedback;
    private RadioGroup radioGroupOptions;
    private RadioButton rbOptionA, rbOptionB, rbOptionC, rbOptionD;
    private Button btnNext;

    private List<Question> questionList;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private boolean answerChecked = false;

    private DatabaseHelper dbHelper;

    // Store the listener to remove/add it
    private RadioGroup.OnCheckedChangeListener radioGroupChangeListener;

    public static class Question {
        private String word;
        private String correctAnswer;
        private List<String> allOptions;

        public Question(String word, String correctAnswer, List<String> allOptions) {
            this.word = word;
            this.correctAnswer = correctAnswer;
            this.allOptions = new ArrayList<>(allOptions);
            if (!this.allOptions.contains(correctAnswer)) {
                Log.e("Question", "Correct answer '" + correctAnswer + "' not found in options for word: " + word);
                this.allOptions.add(correctAnswer);
                while (this.allOptions.size() > 4) {
                    this.allOptions.remove(0);
                }
            }
        }

        public String getWord() {
            return word;
        }

        public String getCorrectAnswer() {
            return correctAnswer;
        }

        public List<String> getAllOptions() {
            Collections.shuffle(allOptions);
            return allOptions;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_meaning_quiz);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText(getResources().getString(R.string.wordMeaningQuiz));

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.arrow_back_24);

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));

        tvQuestionCounter = findViewById(R.id.tvQuestionCounter);
        tvScore = findViewById(R.id.tvScore);
        tvWord = findViewById(R.id.tvWord);
        tvFeedback = findViewById(R.id.tvFeedback);
        radioGroupOptions = findViewById(R.id.radioGroupOptions);
        rbOptionA = findViewById(R.id.rbOptionA);
        rbOptionB = findViewById(R.id.rbOptionB);
        rbOptionC = findViewById(R.id.rbOptionC);
        rbOptionD = findViewById(R.id.rbOptionD);
        btnNext = findViewById(R.id.btnNext);

        dbHelper = new DatabaseHelper(this);
        dbHelper.openDatabase();

        loadQuestionsFromDatabase();

        if (questionList.isEmpty()) {
            Toast.makeText(this, "No quiz questions available! Please check database.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize the listener once
        radioGroupChangeListener = new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // Only proceed if a valid RadioButton was actually selected by the user
                if (checkedId != -1 && !answerChecked) {
                    RadioButton selectedRadioButton = findViewById(checkedId);
                    if (selectedRadioButton != null) {
                        checkAnswer(selectedRadioButton);
                    }
                }
            }
        };

        // Set the listener after initial setup of the UI
        radioGroupOptions.setOnCheckedChangeListener(radioGroupChangeListener);

        displayQuestion(); // Display the first question
    }

    private void loadQuestionsFromDatabase() {
        questionList = new ArrayList<>();
        List<String> allMeanings = new ArrayList<>();
        Cursor cursorAllMeanings = null;
        Cursor cursorWords = null;

        try {
            cursorAllMeanings = dbHelper.getAllWords();
            if (cursorAllMeanings != null && cursorAllMeanings.moveToFirst()) {
                int meaningColIndex = cursorAllMeanings.getColumnIndex("urdu_meaning");
                if (meaningColIndex != -1) {
                    do {
                        String meaning = cursorAllMeanings.getString(meaningColIndex);
                        if (meaning != null && !meaning.isEmpty()) {
                            allMeanings.add(meaning);
                        }
                    } while (cursorAllMeanings.moveToNext());
                } else {
                    Log.e("QuizActivity", "Column 'urdu_meaning' not found in cursor for all meanings.");
                }
            }

            Collections.shuffle(allMeanings);

            cursorWords = dbHelper.getAllWords();
            if (cursorWords != null && cursorWords.moveToFirst()) {
                int wordColIndex = cursorWords.getColumnIndex("word");
                int meaningColIndex = cursorWords.getColumnIndex("urdu_meaning");

                if (wordColIndex == -1 || meaningColIndex == -1) {
                    Log.e("QuizActivity", "Columns 'word' or 'urdu_meaning' not found in cursor for words.");
                    return;
                }

                Random random = new Random();

                do {
                    String word = cursorWords.getString(wordColIndex);
                    String correctMeaning = cursorWords.getString(meaningColIndex);

                    if (word == null || word.isEmpty() || correctMeaning == null || correctMeaning.isEmpty()) {
                        continue;
                    }

                    List<String> options = new ArrayList<>();
                    options.add(correctMeaning);

                    int attempts = 0;
                    int maxAttempts = allMeanings.size() * 2;
                    while (options.size() < 4 && attempts < maxAttempts) {
                        if (allMeanings.isEmpty()) {
                            Log.w("QuizActivity", "Not enough meanings in database for incorrect options. Using duplicates.");
                            break;
                        }
                        String randomMeaning = allMeanings.get(random.nextInt(allMeanings.size()));
                        if (!options.contains(randomMeaning)) {
                            options.add(randomMeaning);
                        }
                        attempts++;
                    }

                    if (options.size() == 4) {
                        questionList.add(new Question(word, correctMeaning, options));
                    } else {
                        Log.w("QuizActivity", "Could not generate 4 distinct options for word: " + word + ". Options generated: " + options.size());
                    }

                } while (cursorWords.moveToNext());
            }

        } catch (Exception e) {
            Log.e("QuizActivity", "Error loading questions from database: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to load quiz questions.", Toast.LENGTH_LONG).show();
        } finally {
            if (cursorAllMeanings != null) {
                cursorAllMeanings.close();
            }
            if (cursorWords != null) {
                cursorWords.close();
            }
        }
        Collections.shuffle(questionList);

        int MAX_QUIZ_QUESTIONS = 10;
        if (questionList.size() > MAX_QUIZ_QUESTIONS) {
            questionList = questionList.subList(0, MAX_QUIZ_QUESTIONS);
        }
        Log.d("QuizActivity", "Loaded " + questionList.size() + " quiz questions.");
    }

    private void displayQuestion() {
        answerChecked = false;
        tvFeedback.setVisibility(View.INVISIBLE);
        tvFeedback.setText(""); // IMPORTANT: Clear the feedback text from previous question
        btnNext.setVisibility(View.INVISIBLE);

        // Temporarily remove the listener to prevent it from firing due to programmatic changes
        radioGroupOptions.setOnCheckedChangeListener(null);
        radioGroupOptions.clearCheck(); // Clear any previously checked radio button
        // Reapply the default background resource to all options
        resetOptionBackgrounds();
        // Re-add the listener
        radioGroupOptions.setOnCheckedChangeListener(radioGroupChangeListener);


        Question currentQuestion = questionList.get(currentQuestionIndex);

        tvQuestionCounter.setText("Question " + (currentQuestionIndex + 1) + "/" + questionList.size());
        tvScore.setText("Score: " + score);

        // --- MODIFICATION HERE ---
        // Capitalize the first letter of the word before setting it
        tvWord.setText(capitalizeFirstLetter(currentQuestion.getWord()));
        // --- END MODIFICATION ---

        List<String> options = currentQuestion.getAllOptions();

        rbOptionA.setText(options.get(0));
        rbOptionB.setText(options.get(1));
        rbOptionC.setText(options.get(2));
        rbOptionD.setText(options.get(3));

        rbOptionA.setEnabled(true);
        rbOptionB.setEnabled(true);
        rbOptionC.setEnabled(true);
        rbOptionD.setEnabled(true);

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (answerChecked) {
                    currentQuestionIndex++;
                    if (currentQuestionIndex < questionList.size()) {
                        displayQuestion();
                    } else {
                        showQuizResults();
                    }
                } else {
                    Toast.makeText(WordMeaningQuizActivity.this, "Please select an answer first!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void checkAnswer(RadioButton selectedRadioButton) {
        answerChecked = true;
        String selectedAnswer = selectedRadioButton.getText().toString();
        String correctAnswer = questionList.get(currentQuestionIndex).getCorrectAnswer();

        rbOptionA.setEnabled(false);
        rbOptionB.setEnabled(false);
        rbOptionC.setEnabled(false);
        rbOptionD.setEnabled(false);

        int correctHighlightColor = ContextCompat.getColor(this, R.color.quiz_option_correct_highlight);
        int incorrectHighlightColor = ContextCompat.getColor(this, R.color.quiz_option_incorrect_highlight);
        int feedbackCorrectColor = ContextCompat.getColor(this, R.color.quiz_feedback_correct);
        int feedbackIncorrectColor = ContextCompat.getColor(this, R.color.quiz_feedback_incorrect);

        if (selectedAnswer.equals(correctAnswer)) {
            score++;
            tvScore.setText("Score: " + score);
            tvFeedback.setText("Correct!");
            tvFeedback.setTextColor(feedbackCorrectColor);
            selectedRadioButton.setBackgroundColor(correctHighlightColor);
        } else {
            tvFeedback.setText("Incorrect.");
            tvFeedback.setTextColor(feedbackIncorrectColor);
            selectedRadioButton.setBackgroundColor(incorrectHighlightColor);

            RadioButton[] radioButtons = {rbOptionA, rbOptionB, rbOptionC, rbOptionD};
            for (RadioButton rb : radioButtons) {
                if (rb.getText().toString().equals(correctAnswer)) {
                    rb.setBackgroundColor(correctHighlightColor);
                    break;
                }
            }
        }
        tvFeedback.setVisibility(View.VISIBLE);
        btnNext.setVisibility(View.VISIBLE);
    }

    private void resetOptionBackgrounds() {
        rbOptionA.setBackgroundResource(R.drawable.option_background_selector);
        rbOptionB.setBackgroundResource(R.drawable.option_background_selector);
        rbOptionC.setBackgroundResource(R.drawable.option_background_selector);
        rbOptionD.setBackgroundResource(R.drawable.option_background_selector);
    }

    private void showQuizResults() {
        QuizResultsDialogFragment dialogFragment = QuizResultsDialogFragment.newInstance(score, questionList.size());
        dialogFragment.show(getSupportFragmentManager(), "quiz_results_dialog");
    }
    public void restartQuiz() {
        // 1. Reset all your quiz state variables to their initial state
        score = 0;
        currentQuestionIndex = 0;

        // Clear any previous feedback or UI state
        tvFeedback.setVisibility(View.INVISIBLE);
        tvScore.setText("Score: 0"); // Reset score display immediately
        btnNext.setVisibility(View.INVISIBLE);

        // 2. Load a new set of questions from the database
        // This will also shuffle them for a fresh quiz.
        loadQuestionsFromDatabase();

        // Check if we were able to load new questions
        if (questionList.isEmpty()) {
            Toast.makeText(this, "Not enough words available to start a new quiz.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 3. Display the very first question of the new quiz
        displayQuestion();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.closeDatabase();
        }
    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    // --- NEW HELPER METHOD ---
    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        // Handle potential leading/trailing spaces if necessary, though it's usually better to trim before saving to DB
        String trimmedText = text.trim();
        if (trimmedText.isEmpty()) {
            return trimmedText;
        }
        // Capitalize the first character and append the rest of the string
        return trimmedText.substring(0, 1).toUpperCase(Locale.getDefault()) + trimmedText.substring(1);
    }
    // --- END NEW HELPER METHOD ---
}