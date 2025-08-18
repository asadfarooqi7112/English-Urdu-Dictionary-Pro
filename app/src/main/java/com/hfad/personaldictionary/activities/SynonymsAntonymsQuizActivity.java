package com.hfad.personaldictionary.activities;

import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.hfad.personaldictionary.database.DatabaseHelper;
import com.hfad.personaldictionary.R;
import com.hfad.personaldictionary.fragments.QuizResultsDialogFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

public class SynonymsAntonymsQuizActivity extends AppCompatActivity {

    private TextView tvSynAntQuestionCounter, tvSynAntScore, tvChallengeWord, tvSynAntFeedback, tvSynAntInstruction;
    private RadioGroup radioGroupSynAntOptions;
    private RadioButton rbSynAntOptionA, rbSynAntOptionB, rbSynAntOptionC, rbSynAntOptionD;
    private Button btnSynAntNext;

    private List<SynAntQuestion> questionList;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private boolean answerChecked = false;

    private DatabaseHelper dbHelper;

    // Store the listener to remove/add it
    private RadioGroup.OnCheckedChangeListener radioGroupChangeListener;

    // Enum to define challenge type
    private enum ChallengeType {
        SYNONYM,
        ANTONYM
    }

    public static class SynAntQuestion {
        private String challengeWord;
        private ChallengeType questionType;
        private String correctAnswer;
        private List<String> allOptions; // Shuffled list of options

        public SynAntQuestion(String challengeWord, ChallengeType questionType, String correctAnswer, List<String> allOptions) {
            this.challengeWord = challengeWord;
            this.questionType = questionType;
            this.correctAnswer = correctAnswer;
            this.allOptions = new ArrayList<>(allOptions); // Defensive copy
            Collections.shuffle(this.allOptions); // Shuffle options when creating the question
        }

        public String getChallengeWord() {
            return challengeWord;
        }

        public ChallengeType getQuestionType() {
            return questionType;
        }

        public String getCorrectAnswer() {
            return correctAnswer;
        }

        public List<String> getAllOptions() {
            // Options are already shuffled in the constructor, just return them
            return allOptions;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_synonyms_antonyms_quiz);

        // Initialize UI elements
        tvSynAntQuestionCounter = findViewById(R.id.tvSynAntQuestionCounter);
        tvSynAntScore = findViewById(R.id.tvSynAntScore);
        tvChallengeWord = findViewById(R.id.tvChallengeWord); // Corrected ID to match XML
        tvSynAntInstruction = findViewById(R.id.tvSynAntInstruction);
        tvSynAntFeedback = findViewById(R.id.tvSynAntFeedback);
        radioGroupSynAntOptions = findViewById(R.id.radioGroupSynAntOptions);
        rbSynAntOptionA = findViewById(R.id.rbSynAntOptionA);
        rbSynAntOptionB = findViewById(R.id.rbSynAntOptionB);
        rbSynAntOptionC = findViewById(R.id.rbSynAntOptionC);
        rbSynAntOptionD = findViewById(R.id.rbSynAntOptionD);
        btnSynAntNext = findViewById(R.id.btnSynAntNext);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText(getResources().getString(R.string.synonymsAntonymsQuiz));

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.arrow_back_24);

        // Status Bar Color and Icon Appearance
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.quiz_light_background));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decor = window.getDecorView();
                WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(window, decor);
                if (insetsController != null) {
                    insetsController.setAppearanceLightStatusBars(true);
                }
            }
        }

        dbHelper = new DatabaseHelper(this);
        dbHelper.openDatabase();

        loadChallengeQuestionsFromDatabase();

        if (questionList.isEmpty()) {
            finish();
            return;
        }

        radioGroupChangeListener = (group, checkedId) -> {
            if (checkedId != -1 && !answerChecked) {
                RadioButton selectedRadioButton = findViewById(checkedId);
                if (selectedRadioButton != null) {
                    checkAnswer(selectedRadioButton);
                }
            }
        };

        radioGroupSynAntOptions.setOnCheckedChangeListener(radioGroupChangeListener);

        displayChallengeQuestion();
    }

    private void loadChallengeQuestionsFromDatabase() {
        questionList = new ArrayList<>();
        List<String> allMeaningsSynonymsAntonyms = new ArrayList<>();
        Cursor cursorAllData = null;

        try {
            cursorAllData = dbHelper.getWordsWithSynonymsAntonyms();

            if (cursorAllData != null && cursorAllData.moveToFirst()) {
                // Get column indices once and store them
                int wordColIndex = cursorAllData.getColumnIndex("word");
                int synonymColIndex = cursorAllData.getColumnIndex("synonym");
                int antonymColIndex = cursorAllData.getColumnIndex("antonym");

                // Essential column check: 'word' must exist
                if (wordColIndex == -1) {
                    Log.e("SynAntQuizActivity", "Column 'word' not found in cursor.");
                    return;
                }

                // Log warnings if optional columns are missing
                if (synonymColIndex == -1) {
                    Log.w("SynAntQuizActivity", "Column 'synonym' not found in cursor. Synonym questions may be limited.");
                }
                if (antonymColIndex == -1) {
                    Log.w("SynAntQuizActivity", "Column 'antonym' not found in cursor. Antonym questions may be limited.");
                }


                // --- First pass: Populate a list of all possible terms for distractors ---
                cursorAllData.moveToPosition(-1); // Reset cursor to before first for re-use
                while (cursorAllData.moveToNext()) {
                    String word = cursorAllData.getString(wordColIndex);
                    // Use the stored indices and check for -1 before retrieving
                    String synonym = (synonymColIndex != -1) ? cursorAllData.getString(synonymColIndex) : null;
                    String antonym = (antonymColIndex != -1) ? cursorAllData.getString(antonymColIndex) : null;

                    if (word != null && !word.trim().isEmpty()) allMeaningsSynonymsAntonyms.add(word.trim());
                    if (synonym != null && !synonym.trim().isEmpty()) allMeaningsSynonymsAntonyms.add(synonym.trim());
                    if (antonym != null && !antonym.trim().isEmpty()) allMeaningsSynonymsAntonyms.add(antonym.trim());
                }
                Collections.shuffle(allMeaningsSynonymsAntonyms);


                // --- Second pass: Generate actual questions ---
                cursorAllData.moveToPosition(-1); // Reset cursor again
                Random random = new Random();
                int wordsProcessed = 0;
                int MAX_WORDS_FOR_QUIZ_GEN = 30;

                while (cursorAllData.moveToNext() && wordsProcessed < MAX_WORDS_FOR_QUIZ_GEN) {
                    String word = cursorAllData.getString(wordColIndex);
                    String synonym = (synonymColIndex != -1) ? cursorAllData.getString(synonymColIndex) : null;
                    String antonym = (antonymColIndex != -1) ? cursorAllData.getString(antonymColIndex) : null;

                    if (word == null || word.trim().isEmpty()) {
                        continue;
                    }

                    word = word.trim();
                    synonym = (synonym != null) ? synonym.trim() : null;
                    antonym = (antonym != null) ? antonym.trim() : null;

                    ChallengeType type = null;
                    String correctAnswer = null;
                    boolean hasSynonym = (synonym != null && !synonym.isEmpty());
                    boolean hasAntonym = (antonym != null && !antonym.isEmpty());

                    if (hasSynonym && hasAntonym) {
                        if (random.nextBoolean()) {
                            type = ChallengeType.SYNONYM;
                            correctAnswer = synonym;
                        } else {
                            type = ChallengeType.ANTONYM;
                            correctAnswer = antonym;
                        }
                    } else if (hasSynonym) {
                        type = ChallengeType.SYNONYM;
                        correctAnswer = synonym;
                    } else if (hasAntonym) {
                        type = ChallengeType.ANTONYM;
                        correctAnswer = antonym;
                    } else {
                        continue;
                    }

                    if (correctAnswer != null && !correctAnswer.isEmpty()) {
                        List<String> options = new ArrayList<>();
                        options.add(correctAnswer);

                        int attempts = 0;
                        int maxAttempts = allMeaningsSynonymsAntonyms.size() * 3;
                        // Ensure distractors are not the challenge word itself or the correct answer
                        while (options.size() < 4 && attempts < maxAttempts) {
                            if (allMeaningsSynonymsAntonyms.isEmpty()) {
                                Log.w("SynAntQuizActivity", "Distractor pool is empty. Cannot generate enough options.");
                                break;
                            }
                            String randomTerm = allMeaningsSynonymsAntonyms.get(random.nextInt(allMeaningsSynonymsAntonyms.size()));
                            if (!options.contains(randomTerm) && !randomTerm.equalsIgnoreCase(word) && !randomTerm.equalsIgnoreCase(correctAnswer)) {
                                options.add(randomTerm);
                            }
                            attempts++;
                        }

                        if (options.size() == 4) {
                            questionList.add(new SynAntQuestion(word, type, correctAnswer, options));
                        } else {
                            Log.w("SynAntQuizActivity", "Could not generate 4 distinct options for word: '" + word + "'. Options generated: " + options.size() + ". Correct Answer: '" + correctAnswer + "'.");
                        }
                    }
                    wordsProcessed++;
                }

            } else {
                Log.d("SynAntQuizActivity", "Cursor is empty or null from getWordsWithSynonymsAntonyms().");
            }

        } catch (Exception e) {
            Log.e("SynAntQuizActivity", "Error loading challenge questions: " + e.getMessage(), e);
        } finally {
            if (cursorAllData != null) {
                cursorAllData.close();
            }
        }
        Collections.shuffle(questionList);

        int MAX_QUIZ_QUESTIONS = 10;
        if (questionList.size() > MAX_QUIZ_QUESTIONS) {
            questionList = questionList.subList(0, MAX_QUIZ_QUESTIONS);
        }
        Log.d("SynAntQuizActivity", "Loaded " + questionList.size() + " challenge questions.");
    }


    private void displayChallengeQuestion() {
        answerChecked = false;
        tvSynAntFeedback.setVisibility(View.INVISIBLE);
        tvSynAntFeedback.setText("");
        btnSynAntNext.setVisibility(View.INVISIBLE);

        radioGroupSynAntOptions.setOnCheckedChangeListener(null);
        radioGroupSynAntOptions.clearCheck();
        resetOptionBackgrounds();
        radioGroupSynAntOptions.setOnCheckedChangeListener(radioGroupChangeListener);

        if (currentQuestionIndex >= questionList.size()) {
            showQuizResults();
            return;
        }

        SynAntQuestion currentQuestion = questionList.get(currentQuestionIndex);

        tvSynAntQuestionCounter.setText("Challenge " + (currentQuestionIndex + 1) + "/" + questionList.size());
        tvSynAntScore.setText("Score: " + score);
        tvChallengeWord.setText(capitalizeFirstLetter(currentQuestion.getChallengeWord()));

        if (currentQuestion.getQuestionType() == ChallengeType.SYNONYM) {
            tvSynAntInstruction.setText("Find the Synonym:");
        } else {
            tvSynAntInstruction.setText("Find the Antonym:");
        }

        List<String> options = currentQuestion.getAllOptions();

        if (options.size() == 4) {
            rbSynAntOptionA.setText(options.get(0));
            rbSynAntOptionB.setText(options.get(1));
            rbSynAntOptionC.setText(options.get(2));
            rbSynAntOptionD.setText(options.get(3));
        } else {
            Log.e("SynAntQuizActivity", "Less than 4 options generated for word: " + currentQuestion.getChallengeWord() + ". Skipping question.");
            // Skip to next question if options are insufficient
            currentQuestionIndex++;
            displayChallengeQuestion(); // Try to display the next question
            return; // Exit this call to avoid further processing of invalid question
        }

        rbSynAntOptionA.setEnabled(true);
        rbSynAntOptionB.setEnabled(true);
        rbSynAntOptionC.setEnabled(true);
        rbSynAntOptionD.setEnabled(true);

        btnSynAntNext.setOnClickListener(v -> {
            if (answerChecked) {
                currentQuestionIndex++;
                if (currentQuestionIndex < questionList.size()) {
                    displayChallengeQuestion();
                } else {
                    showQuizResults();
                }
            } else {
                Toast.makeText(SynonymsAntonymsQuizActivity.this, "Please select an answer first!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAnswer(RadioButton selectedRadioButton) {
        answerChecked = true;
        String selectedAnswer = selectedRadioButton.getText().toString();
        String correctAnswer = questionList.get(currentQuestionIndex).getCorrectAnswer();

        rbSynAntOptionA.setEnabled(false);
        rbSynAntOptionB.setEnabled(false);
        rbSynAntOptionC.setEnabled(false);
        rbSynAntOptionD.setEnabled(false);

        int correctHighlightColor = ContextCompat.getColor(this, R.color.quiz_option_correct_highlight);
        int incorrectHighlightColor = ContextCompat.getColor(this, R.color.quiz_option_incorrect_highlight);
        int feedbackCorrectColor = ContextCompat.getColor(this, R.color.quiz_feedback_correct);
        int feedbackIncorrectColor = ContextCompat.getColor(this, R.color.quiz_feedback_incorrect);


        if (selectedAnswer.equals(correctAnswer)) {
            score++;
            tvSynAntScore.setText("Score: " + score);
            tvSynAntFeedback.setText("Correct!");
            tvSynAntFeedback.setTextColor(feedbackCorrectColor);
            selectedRadioButton.setBackgroundColor(correctHighlightColor);
        } else {
            tvSynAntFeedback.setText("Incorrect.");
            tvSynAntFeedback.setTextColor(feedbackIncorrectColor);
            selectedRadioButton.setBackgroundColor(incorrectHighlightColor);

            RadioButton[] radioButtons = {rbSynAntOptionA, rbSynAntOptionB, rbSynAntOptionC, rbSynAntOptionD};
            for (RadioButton rb : radioButtons) {
                if (rb.getText().toString().equals(correctAnswer)) {
                    rb.setBackgroundColor(correctHighlightColor);
                    break;
                }
            }
        }
        tvSynAntFeedback.setVisibility(View.VISIBLE);
        btnSynAntNext.setVisibility(View.VISIBLE);
    }

    private void resetOptionBackgrounds() {
        rbSynAntOptionA.setBackgroundResource(R.drawable.option_background_selector);
        rbSynAntOptionB.setBackgroundResource(R.drawable.option_background_selector);
        rbSynAntOptionC.setBackgroundResource(R.drawable.option_background_selector);
        rbSynAntOptionD.setBackgroundResource(R.drawable.option_background_selector);
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
        tvSynAntFeedback.setVisibility(View.INVISIBLE);
        tvSynAntScore.setText("Score: 0"); // Reset score display immediately
        btnSynAntNext.setVisibility(View.INVISIBLE);

        // 2. Load a new set of questions from the database
        // This will also shuffle them for a fresh quiz.
        loadChallengeQuestionsFromDatabase();

        // Check if we were able to load new questions
        if (questionList.isEmpty()) {
            Toast.makeText(this, "Not enough words available to start a new quiz.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 3. Display the very first question of the new quiz
        displayChallengeQuestion();
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