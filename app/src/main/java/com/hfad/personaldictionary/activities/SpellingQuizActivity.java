package com.hfad.personaldictionary.activities;

import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.flexbox.FlexboxLayout;
import com.hfad.personaldictionary.database.DatabaseHelper;
import com.hfad.personaldictionary.R;
import com.hfad.personaldictionary.fragments.QuizResultsDialogFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class SpellingQuizActivity extends AppCompatActivity {

    private TextView tvSpellingQuestionCounter, tvSpellingScore, tvSpellingChallengeMeaning,
            tvSpellingInstruction, tvSpellingFeedback;
    private FlexboxLayout llLetterBoxesContainer;
    private List<EditText> letterEditTexts;
    private Button btnSpellingSubmit, btnSpellingNext, btnSpellingHint;

    private List<SpellingQuestion> questionList;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private boolean answerChecked = false;

    private DatabaseHelper dbHelper;

    public static class SpellingQuestion {
        private String meaning;
        private String correctSpelling;

        public SpellingQuestion(String meaning, String correctSpelling) {
            this.meaning = meaning;
            this.correctSpelling = correctSpelling;
        }

        public String getMeaning() {
            return meaning;
        }

        public String getCorrectSpelling() {
            return correctSpelling;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spelling_quiz);

        // Initialize UI elements
        tvSpellingQuestionCounter = findViewById(R.id.tvSpellingQuestionCounter);
        tvSpellingScore = findViewById(R.id.tvSpellingScore);
        tvSpellingInstruction = findViewById(R.id.tvSpellingInstruction);
        tvSpellingChallengeMeaning = findViewById(R.id.tvSpellingChallengeMeaning);
        llLetterBoxesContainer = findViewById(R.id.llLetterBoxesContainer);
        btnSpellingSubmit = findViewById(R.id.btnSpellingSubmit);
        tvSpellingFeedback = findViewById(R.id.tvSpellingFeedback);
        btnSpellingNext = findViewById(R.id.btnSpellingNext);
        btnSpellingHint = findViewById(R.id.btnSpellingHint);

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText(getResources().getString(R.string.spellingQuiz));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.arrow_back_24);
        }

        // Status Bar Color and Icon Appearance
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            if (window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.setStatusBarColor(ContextCompat.getColor(this, R.color.quiz_light_background));

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

        dbHelper = new DatabaseHelper(this);
        dbHelper.openDatabase();

        loadSpellingQuestionsFromDatabase();

        if (questionList.isEmpty()) {
            Toast.makeText(this, "No words with meanings available for spelling quiz! Please check database.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Set up button listeners
        btnSpellingSubmit.setOnClickListener(v -> checkAnswer());
        btnSpellingNext.setOnClickListener(v -> showNextQuestion());
        btnSpellingHint.setOnClickListener(v -> showHint());

        displayQuestion();
    }

    private void loadSpellingQuestionsFromDatabase() {
        questionList = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = dbHelper.getWordsWithMeanings();

            if (cursor != null && cursor.moveToFirst()) {
                int wordColIndex = cursor.getColumnIndex("word");
                // Assuming 'urdu_meaning' is the column you want for the meaning for the quiz
                int meaningColIndex = cursor.getColumnIndex("urdu_meaning");

                if (wordColIndex == -1) {
                    Log.e("SpellingQuizActivity", "Column 'word' not found in database.");
                    Toast.makeText(this, "Database error: 'word' column missing.", Toast.LENGTH_LONG).show();
                    return;
                }
                if (meaningColIndex == -1) {
                    Log.w("SpellingQuizActivity", "Column 'urdu_meaning' not found. Spelling quiz requires it.");
                    Toast.makeText(this, "Missing 'urdu_meaning' column. Spelling quiz requires it.", Toast.LENGTH_LONG).show();
                    return;
                }

                do {
                    String word = cursor.getString(wordColIndex);
                    String meaning = cursor.getString(meaningColIndex);

                    if (word != null && !word.trim().isEmpty() && meaning != null && !meaning.trim().isEmpty()) {
                        questionList.add(new SpellingQuestion(meaning.trim(), word.trim()));
                    }
                } while (cursor.moveToNext());
            } else {
                Log.d("SpellingQuizActivity", "Cursor is empty or null from getWordsWithMeanings().");
            }
        } catch (Exception e) {
            Log.e("SpellingQuizActivity", "Error loading spelling questions: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to load quiz questions.", Toast.LENGTH_LONG).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        Collections.shuffle(questionList);

        int MAX_QUIZ_QUESTIONS = 10;
        if (questionList.size() > MAX_QUIZ_QUESTIONS) {
            questionList = questionList.subList(0, MAX_QUIZ_QUESTIONS);
        }

        Log.d("SpellingQuizActivity", "Loaded " + questionList.size() + " spelling questions.");
    }

    private void displayQuestion() {
        if (currentQuestionIndex >= questionList.size()) {
            showQuizResults();
            return;
        }

        answerChecked = false;

        tvSpellingFeedback.setVisibility(View.INVISIBLE);
        tvSpellingFeedback.setText("");
        btnSpellingSubmit.setVisibility(View.VISIBLE);
        btnSpellingNext.setVisibility(View.INVISIBLE);
        btnSpellingHint.setVisibility(View.VISIBLE); // Ensure hint button is visible
        btnSpellingHint.setEnabled(true); // Always re-enable hint button for new question

        llLetterBoxesContainer.removeAllViews();
        letterEditTexts = new ArrayList<>();

        SpellingQuestion currentQuestion = questionList.get(currentQuestionIndex);
        String correctSpelling = currentQuestion.getCorrectSpelling();

        for (int i = 0; i < correctSpelling.length(); i++) {
            EditText letterBox = new EditText(this);
            FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                    (int) getResources().getDimension(R.dimen.letter_box_size),
                    (int) getResources().getDimension(R.dimen.letter_box_size)
            );
            params.setMargins(
                    (int) getResources().getDimension(R.dimen.letter_box_margin),
                    (int) getResources().getDimension(R.dimen.letter_box_margin),
                    (int) getResources().getDimension(R.dimen.letter_box_margin),
                    (int) getResources().getDimension(R.dimen.letter_box_margin)
            );
            letterBox.setLayoutParams(params);
            letterBox.setBackgroundResource(R.drawable.letter_box_selector);
            letterBox.setGravity(Gravity.CENTER);
            letterBox.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            letterBox.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});
            letterBox.setTextSize(20f);
            letterBox.setTextColor(ContextCompat.getColor(this, R.color.quiz_text_primary));
            letterBox.setCursorVisible(true);
            letterBox.setSelectAllOnFocus(true);

            final int currentIndex = i;

            letterBox.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1) {
                        // Find the next ENABLED EditText
                        int nextFocusIndex = currentIndex + 1;
                        while (nextFocusIndex < letterEditTexts.size() && !letterEditTexts.get(nextFocusIndex).isEnabled()) {
                            nextFocusIndex++; // Skip disabled boxes
                        }

                        if (nextFocusIndex < letterEditTexts.size()) {
                            letterEditTexts.get(nextFocusIndex).requestFocus();
                        } else {
                            // If no more enabled boxes, hide keyboard
                            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                            if (imm != null) {
                                imm.hideSoftInputFromWindow(letterBox.getWindowToken(), 0);
                            }
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            letterBox.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (letterBox.getText().toString().isEmpty() && currentIndex > 0) {
                        // Find the previous ENABLED EditText
                        int prevFocusIndex = currentIndex - 1;
                        while (prevFocusIndex >= 0 && !letterEditTexts.get(prevFocusIndex).isEnabled()) {
                            prevFocusIndex--; // Skip disabled boxes backwards
                        }

                        if (prevFocusIndex >= 0) {
                            letterEditTexts.get(prevFocusIndex).requestFocus();
                            letterEditTexts.get(prevFocusIndex).setText(""); // Clear previous box if backspacing from empty one
                            return true; // Consume event
                        }
                    }
                }
                return false; // Let system handle other key events
            });

            letterEditTexts.add(letterBox);
            llLetterBoxesContainer.addView(letterBox);
        }

        tvSpellingQuestionCounter.setText("Spelling " + (currentQuestionIndex + 1) + "/" + questionList.size());
        tvSpellingScore.setText("Score: " + score);
        tvSpellingChallengeMeaning.setText(capitalizeFirstLetter(currentQuestion.getMeaning()));

        // Request focus for the first ENABLED EditText
        boolean focused = false;
        for (EditText et : letterEditTexts) {
            if (et.isEnabled()) {
                et.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
                }
                focused = true;
                break;
            }
        }
        if (!focused) { // If all boxes are disabled (e.g., all hinted)
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(llLetterBoxesContainer.getWindowToken(), 0);
            }
        }
    }

    private void showHint() {
        if (answerChecked) { // Don't allow hints after answer is checked
            Toast.makeText(this, "Answer already submitted!", Toast.LENGTH_SHORT).show();
            return;
        }

        SpellingQuestion currentQuestion = questionList.get(currentQuestionIndex);
        String correctSpelling = currentQuestion.getCorrectSpelling();

        List<Integer> possibleHintIndices = new ArrayList<>();
        for (int i = 0; i < letterEditTexts.size(); i++) {
            // A box is eligible for a hint if it's currently empty AND enabled
            if (letterEditTexts.get(i).getText().toString().isEmpty() && letterEditTexts.get(i).isEnabled()) {
                possibleHintIndices.add(i);
            }
        }

        if (possibleHintIndices.isEmpty()) {
            Toast.makeText(this, "No more empty boxes for hints!", Toast.LENGTH_SHORT).show();
            btnSpellingHint.setEnabled(false); // Disable if no more hints possible
            return;
        }

        // Pick a random empty box to hint from the available ones
        Collections.shuffle(possibleHintIndices);
        int hintIndex = possibleHintIndices.get(0); // Get the first (random) empty index

        EditText hintedBox = letterEditTexts.get(hintIndex);
        char correctLetter = correctSpelling.charAt(hintIndex);

        hintedBox.setText(String.valueOf(correctLetter).toUpperCase(Locale.getDefault()));
        hintedBox.setEnabled(false); // Disable the hinted box so user can't change it
        hintedBox.setBackgroundResource(R.drawable.letter_box_correct_background); // Visually indicate it's a correct, hinted letter
        hintedBox.setTextColor(ContextCompat.getColor(this, R.color.quiz_text_primary)); // Ensure hinted text color is correct

        // After revealing, check again if there are any remaining empty boxes.
        // If not, disable the hint button.
        List<Integer> remainingEmptyBoxes = new ArrayList<>();
        for (int i = 0; i < letterEditTexts.size(); i++) {
            if (letterEditTexts.get(i).getText().toString().isEmpty() && letterEditTexts.get(i).isEnabled()) {
                remainingEmptyBoxes.add(i);
            }
        }

        if (remainingEmptyBoxes.isEmpty()) {
            btnSpellingHint.setEnabled(false); // Disable hint button if no more hints are possible
        }

        // Move focus to the next available enabled empty box, or hide keyboard
        boolean focused = false;
        for(int i = 0; i < letterEditTexts.size(); i++) {
            if(letterEditTexts.get(i).isEnabled() && letterEditTexts.get(i).getText().toString().isEmpty()) {
                letterEditTexts.get(i).requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(letterEditTexts.get(i), InputMethodManager.SHOW_IMPLICIT);
                }
                focused = true;
                break;
            }
        }
        if (!focused) { // If all boxes are disabled (e.g., all hinted or filled)
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(hintedBox.getWindowToken(), 0);
            }
        }
    }

    private void checkAnswer() {
        if (answerChecked) return;

        StringBuilder userAnswerBuilder = new StringBuilder();
        for (EditText et : letterEditTexts) {
            userAnswerBuilder.append(et.getText().toString());
        }
        String userAnswer = userAnswerBuilder.toString().trim();
        String correctSpelling = questionList.get(currentQuestionIndex).getCorrectSpelling();

        if (userAnswer.isEmpty()) {
            Toast.makeText(this, "Please type an answer!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (userAnswer.length() != correctSpelling.length()) {
            Toast.makeText(this, "Please fill all letter boxes!", Toast.LENGTH_SHORT).show();
            return;
        }

        answerChecked = true;
        for (EditText et : letterEditTexts) {
            et.setEnabled(false); // Disable all boxes after submission
        }
        btnSpellingSubmit.setVisibility(View.GONE); // Use GONE to collapse space
        btnSpellingHint.setVisibility(View.GONE); // Hide hint button after submission

        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(llLetterBoxesContainer.getWindowToken(), 0);
        }

        int feedbackCorrectColor = ContextCompat.getColor(this, R.color.quiz_feedback_correct);
        int feedbackIncorrectColor = ContextCompat.getColor(this, R.color.quiz_feedback_incorrect);
        int quizTextPrimaryColor = ContextCompat.getColor(this, R.color.quiz_text_primary);

        if (userAnswer.equalsIgnoreCase(correctSpelling)) {
            score++;
            tvSpellingScore.setText("Score: " + score);
            tvSpellingFeedback.setText("Correct!");
            tvSpellingFeedback.setTextColor(feedbackCorrectColor);
            // Highlight all boxes green for correct answer
            for (EditText et : letterEditTexts) {
                et.setBackgroundResource(R.drawable.letter_box_correct_background);
            }
        } else {
            // Define the two parts of the text for mixed colors
            String incorrectPart = "Incorrect.";
            String correctSpellingPart = "\nCorrect spelling: " + capitalizeFirstLetter(correctSpelling);
            String fullText = incorrectPart + correctSpellingPart;

            // Create a SpannableString to apply different colors
            SpannableString spannableString = new SpannableString(fullText);

            // Apply red color to "Incorrect."
            spannableString.setSpan(new ForegroundColorSpan(feedbackIncorrectColor),
                    0,
                    incorrectPart.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Apply primary color to the rest of the text
            spannableString.setSpan(new ForegroundColorSpan(quizTextPrimaryColor),
                    incorrectPart.length(),
                    fullText.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Set the SpannableString to the TextView
            tvSpellingFeedback.setText(spannableString);

            // Highlight boxes based on correctness, but keep user's input
            for (int i = 0; i < correctSpelling.length(); i++) {
                if (i < userAnswer.length() && Character.toUpperCase(userAnswer.charAt(i)) == Character.toUpperCase(correctSpelling.charAt(i))) {
                    letterEditTexts.get(i).setBackgroundResource(R.drawable.letter_box_correct_background);
                    // User's correct letter remains
                } else {
                    letterEditTexts.get(i).setBackgroundResource(R.drawable.letter_box_incorrect_background);
                    // User's incorrect input remains, no change to text
                    letterEditTexts.get(i).setTextColor(ContextCompat.getColor(this, R.color.quiz_text_primary));
                }
            }
        }
        tvSpellingFeedback.setVisibility(View.VISIBLE);
        btnSpellingNext.setVisibility(View.VISIBLE);
    }

    private void showNextQuestion() {
        currentQuestionIndex++;
        displayQuestion();
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
        tvSpellingFeedback.setVisibility(View.INVISIBLE);
        tvSpellingScore.setText("Score: 0"); // Reset score display immediately
        btnSpellingNext.setVisibility(View.INVISIBLE);

        // 2. Load a new set of questions from the database
        // This will also shuffle them for a fresh quiz.
        loadSpellingQuestionsFromDatabase();

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

    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String trimmedText = text.trim();
        if (trimmedText.isEmpty()) {
            return trimmedText;
        }
        return trimmedText.substring(0, 1).toUpperCase(Locale.getDefault()) + trimmedText.substring(1);
    }
}