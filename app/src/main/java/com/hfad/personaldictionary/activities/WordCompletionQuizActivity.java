package com.hfad.personaldictionary.activities; // Adjust package name as per your project structure

import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.flexbox.FlexboxLayout;
import com.hfad.personaldictionary.database.DatabaseHelper; // Ensure this import path is correct
import com.hfad.personaldictionary.R; // Ensure this import path is correct
import com.hfad.personaldictionary.fragments.QuizResultsDialogFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public class WordCompletionQuizActivity extends AppCompatActivity {

    private TextView tvQuestionCounter, tvScore, tvMeaning, tvInstruction, tvMaskedWord, tvFeedback;
    private FlexboxLayout llOptionsContainer;
    private Button btnNext;

    public static class WordCompletionQuestion {
        private String meaning;
        private String originalWord; // Stored as retrieved from DB (can be mixed case)
        private String maskedWordDisplay; // Stored in lowercase here for consistency
        private String correctMissingSequence; // Stored in lowercase here for consistency
        private List<String> options; // Stored in lowercase here for consistency

        public WordCompletionQuestion(String meaning, String originalWord,
                                      String maskedWordDisplay, String correctMissingSequence,
                                      List<String> options) {
            this.meaning = meaning;
            this.originalWord = originalWord;
            this.maskedWordDisplay = maskedWordDisplay;
            this.correctMissingSequence = correctMissingSequence;
            this.options = options;
        }

        public String getMeaning() { return meaning; }
        public String getOriginalWord() { return originalWord; }
        public String getMaskedWordDisplay() { return maskedWordDisplay; }
        public String getCorrectMissingSequence() { return correctMissingSequence; }
        public List<String> getOptions() { return options; }
    }

    private List<WordCompletionQuestion> questionList;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private boolean answerChecked = false;
    private String selectedOptionSequence = null;

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_completion_quiz);

        tvQuestionCounter = findViewById(R.id.tvWordCompletionQuestionCounter);
        tvScore = findViewById(R.id.tvWordCompletionScore);
        tvInstruction = findViewById(R.id.tvWordCompletionInstruction);
        tvMeaning = findViewById(R.id.tvWordCompletionMeaning);
        tvMaskedWord = findViewById(R.id.tvWordCompletionMaskedWord);
        llOptionsContainer = findViewById(R.id.llWordCompletionOptionsContainer);
        tvFeedback = findViewById(R.id.tvWordCompletionFeedback);
        btnNext = findViewById(R.id.btnWordCompletionNext);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText(getResources().getString(R.string.word_completion_quiz));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.arrow_back_24);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

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

        dbHelper = new DatabaseHelper(this);
        dbHelper.openDatabase();

        loadWordCompletionQuestionsFromDatabase();

        if (questionList.isEmpty()) {
            Toast.makeText(this, "No words with meanings available for word completion quiz! Please check database.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnNext.setOnClickListener(v -> showNextQuestion());

        displayQuestion();
    }

    private void loadWordCompletionQuestionsFromDatabase() {
        questionList = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = dbHelper.getWordsWithMeanings();

            if (cursor != null && cursor.moveToFirst()) {
                int wordColIndex = cursor.getColumnIndex("word");
                int meaningColIndex = cursor.getColumnIndex("urdu_meaning");

                if (wordColIndex == -1 || meaningColIndex == -1) {
                    Log.e("WordCompletionQuiz", "Required columns 'word' or 'urdu_meaning' not found in database.");
                    Toast.makeText(this, "Database error: missing required columns for quiz.", Toast.LENGTH_LONG).show();
                    return;
                }

                do {
                    String word = cursor.getString(wordColIndex);
                    String meaning = cursor.getString(meaningColIndex);

                    // Filter words: Must be at least 4 characters long to allow for 1-3 blanks
                    if (word != null && !word.trim().isEmpty() && meaning != null && !meaning.trim().isEmpty() && word.trim().length() >= 4) {
                        WordCompletionQuestion question = generateWordCompletionQuestion(word.trim(), meaning.trim());
                        if (question != null) {
                            questionList.add(question);
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("WordCompletionQuiz", "Error loading questions from database: " + e.getMessage(), e);
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
        Log.d("WordCompletionQuiz", "Loaded " + questionList.size() + " questions for quiz.");
    }

    private WordCompletionQuestion generateWordCompletionQuestion(String originalWord, String meaning) {
        String processedWord = originalWord.toLowerCase(Locale.getDefault());
        int wordLength = processedWord.length();

        Random random = new Random();
        int maxBlanks = Math.min(3, wordLength - 1);
        if (maxBlanks < 1) return null;

        int numBlanks = random.nextInt(maxBlanks) + 1;

        Set<Integer> blankPositions = new HashSet<>();
        while (blankPositions.size() < numBlanks) {
            blankPositions.add(random.nextInt(wordLength));
        }
        List<Integer> sortedBlankPositions = new ArrayList<>(blankPositions);
        Collections.sort(sortedBlankPositions);

        char[] maskedChars = processedWord.toCharArray();
        StringBuilder correctMissingSequenceBuilder = new StringBuilder();
        for (int pos : sortedBlankPositions) {
            correctMissingSequenceBuilder.append(processedWord.charAt(pos));
            maskedChars[pos] = '_';
        }
        String maskedWordDisplay = new String(maskedChars);

        String correctMissingSequence = correctMissingSequenceBuilder.toString();

        List<String> options = new ArrayList<>();
        options.add(correctMissingSequence);

        int numOptions = 4;
        while (options.size() < numOptions) {
            StringBuilder distractorBuilder = new StringBuilder();
            for (int i = 0; i < numBlanks; i++) {
                char randomChar = (char) ('A' + random.nextInt(26));
                distractorBuilder.append(Character.toLowerCase(randomChar));
            }
            String distractor = distractorBuilder.toString();

            if (!options.contains(distractor)) {
                options.add(distractor);
            }
        }
        Collections.shuffle(options);

        return new WordCompletionQuestion(meaning, originalWord, maskedWordDisplay, correctMissingSequence, options);
    }

    private void displayQuestion() {
        if (currentQuestionIndex >= questionList.size()) {
            showQuizResults();
            return;
        }

        answerChecked = false;
        selectedOptionSequence = null;
        tvFeedback.setVisibility(View.INVISIBLE);
        tvFeedback.setText("");
        btnNext.setVisibility(View.INVISIBLE);

        llOptionsContainer.removeAllViews();

        WordCompletionQuestion currentQuestion = questionList.get(currentQuestionIndex);

        tvQuestionCounter.setText("Word " + (currentQuestionIndex + 1) + "/" + questionList.size());
        tvScore.setText("Score: " + score);
        tvMeaning.setText(capitalizeFirstLetter(currentQuestion.getMeaning()));
        tvMaskedWord.setText(capitalizeFirstLetter(currentQuestion.getMaskedWordDisplay()));

        for (String optionSequence : currentQuestion.getOptions()) {
            Button optionButton = new Button(this);
            FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(
                    (int) getResources().getDimension(R.dimen.option_button_margin),
                    (int) getResources().getDimension(R.dimen.option_button_margin),
                    (int) getResources().getDimension(R.dimen.option_button_margin),
                    (int) getResources().getDimension(R.dimen.option_button_margin)
            );
            optionButton.setLayoutParams(params);

            optionButton.setText(capitalizeFirstLetter(formatOptionSequence(optionSequence)));
            optionButton.setTag(optionSequence);

            optionButton.setTextSize(18f);
            optionButton.setTextColor(ContextCompat.getColor(this, R.color.quiz_option_text_default));
            optionButton.setBackgroundResource(R.drawable.quiz_button_bg_selector);
            optionButton.setElevation(4f);
            optionButton.setPadding(20, 16, 20, 16);

            optionButton.setOnClickListener(v -> {
                if (!answerChecked) {
                    answerChecked = true;

                    String clickedOptionSequence = (String) v.getTag();

                    String correctMissingSequence = currentQuestion.getCorrectMissingSequence();

                    for (int i = 0; i < llOptionsContainer.getChildCount(); i++) {
                        View child = llOptionsContainer.getChildAt(i);
                        if (child instanceof Button) {
                            ((Button) child).setEnabled(false);
                            ((Button) child).setBackgroundResource(R.drawable.quiz_button_bg_selector);
                        }
                    }

                    int feedbackCorrectColor = ContextCompat.getColor(this, R.color.quiz_feedback_correct);
                    int feedbackIncorrectColor = ContextCompat.getColor(this, R.color.quiz_feedback_incorrect);
                    int quizTextPrimaryColor = ContextCompat.getColor(this, R.color.quiz_text_primary);

                    if (clickedOptionSequence.equalsIgnoreCase(correctMissingSequence)) {
                        score++;
                        tvScore.setText("Score: " + score);
                        tvFeedback.setText("Correct!");
                        tvFeedback.setTextColor(feedbackCorrectColor);
                        ((Button) v).setBackgroundColor(ContextCompat.getColor(this, R.color.quiz_highlight_correct));
                    } else {
                        String incorrectPart = "Incorrect.";
                        // --- IMPORTANT CHANGE: Format the correct sequence to ALL CAPS for feedback display ---
                        String formattedCorrectSequence = formatOptionSequence(correctMissingSequence).toUpperCase(Locale.getDefault());
                        String correctSpellingPart = "\nCorrect sequence: " + formattedCorrectSequence + "\nOriginal Word: " + capitalizeFirstLetter(currentQuestion.getOriginalWord());
                        String fullText = incorrectPart + correctSpellingPart;

                        SpannableString spannableString = new SpannableString(fullText);
                        spannableString.setSpan(new ForegroundColorSpan(feedbackIncorrectColor),
                                0,
                                incorrectPart.length(),
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        spannableString.setSpan(new ForegroundColorSpan(quizTextPrimaryColor),
                                incorrectPart.length(),
                                fullText.length(),
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        tvFeedback.setText(spannableString);

                        ((Button) v).setBackgroundColor(ContextCompat.getColor(this, R.color.quiz_highlight_incorrect));

                        for (int i = 0; i < llOptionsContainer.getChildCount(); i++) {
                            View child = llOptionsContainer.getChildAt(i);
                            if (child instanceof Button) {
                                Button potentialCorrectButton = (Button) child;
                                if (potentialCorrectButton.getTag() != null && potentialCorrectButton.getTag().toString().equalsIgnoreCase(correctMissingSequence)) {
                                    potentialCorrectButton.setBackgroundColor(ContextCompat.getColor(this, R.color.quiz_highlight_correct));
                                    break;
                                }
                            }
                        }
                    }
                    tvFeedback.setVisibility(View.VISIBLE);
                    btnNext.setVisibility(View.VISIBLE);
                }
            });

            llOptionsContainer.addView(optionButton);
        }
    }

    private String formatOptionSequence(String sequence) {
        if (sequence == null || sequence.length() <= 1) {
            return sequence;
        }
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < sequence.length(); i++) {
            formatted.append(sequence.charAt(i));
            if (i < sequence.length() - 1) {
                formatted.append(",");
            }
        }
        return formatted.toString();
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
        tvFeedback.setVisibility(View.INVISIBLE);
        tvScore.setText("Score: 0"); // Reset score display immediately
        btnNext.setVisibility(View.INVISIBLE);

        // 2. Load a new set of questions from the database
        // This will also shuffle them for a fresh quiz.
        loadWordCompletionQuestionsFromDatabase();

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

    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String trimmedText = text.trim();
        if (trimmedText.isEmpty()) {
            return trimmedText;
        }
        return trimmedText.substring(0, 1).toUpperCase(Locale.getDefault()) + trimmedText.substring(1).toLowerCase(Locale.getDefault());
    }
}