package com.hfad.personaldictionary.activities;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;


public class TimedQuizActivity extends AppCompatActivity {

    // --- UI Elements ---
    private TextView tvQuestionCounter, tvScore, tvTimer, tvInstruction, tvFeedback;
    private ProgressBar progressBarTimer;
    private Button btnNext;

    // Quiz Type Specific UI Containers (These are now LinearLayouts in XML)
    private LinearLayout mcSynAntQuizLayout, wcQuizLayout, spellingQuizLayout;

    // Multiple Choice / Synonyms/Antonyms UI
    private TextView tvMcSynAntWord;
    private LinearLayout flMcSynAntOptionsContainer;

    // Word Completion UI
    private TextView tvWcMeaning, tvWcMaskedWord;
    private LinearLayout flWcOptionsContainer;

    // Spelling Test UI (FlexboxLayout is still used here for wrapping letter boxes)
    private TextView tvSpellingChallengeMeaning;
    private FlexboxLayout flSpellingLetterBoxesContainer;
    private Button btnSpellingQuizSubmit;
    private Button btnSpellingHint;
    private List<EditText> letterEditTexts;

    // --- Quiz State Variables ---
    private List<QuizQuestion> questionList;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private boolean answerChecked = false; // Flag to prevent multiple checks for one question

    // --- Timer Variables ---
    private CountDownTimer countDownTimer;
    // Changed to overall quiz time limit
    private static final long OVERALL_QUIZ_TIME_LIMIT_MILLIS = 2 * 60 * 1000; // 3 minutes for the entire quiz
    private long timeLeftInMillis;

    // --- Database Helper ---
    private DatabaseHelper dbHelper;
    private Random random = new Random();

    // --- Constants for Quiz Types ---
    private static final int TYPE_WORD_COMPLETION = 0;
    private static final int TYPE_WORD_MEANING = 1;
    private static final int TYPE_SYNONYM_ANTONYM = 2;
    private static final int TYPE_SPELLING_TEST = 3;

    private static final int MAX_QUIZ_QUESTIONS = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timed_quiz);

        // --- UI Initialization ---
        tvQuestionCounter = findViewById(R.id.tvTimedQuizQuestionCounter);
        tvScore = findViewById(R.id.tvTimedQuizScore);
        tvTimer = findViewById(R.id.tvTimedQuizTimer);
        tvInstruction = findViewById(R.id.tvTimedQuizInstruction);
        tvFeedback = findViewById(R.id.tvTimedQuizFeedback);
        progressBarTimer = findViewById(R.id.progressBarTimer);
        btnNext = findViewById(R.id.btnTimedQuizNext);

        // Quiz type specific layout containers
        mcSynAntQuizLayout = findViewById(R.id.mcSynAntQuizLayout);
        wcQuizLayout = findViewById(R.id.wcQuizLayout);
        spellingQuizLayout = findViewById(R.id.spellingQuizLayout);

        // Multiple Choice / Synonyms/Antonyms views
        tvMcSynAntWord = findViewById(R.id.tvMcSynAntWord);
        flMcSynAntOptionsContainer = findViewById(R.id.flMcSynAntOptionsContainer);

        // Word Completion views
        tvWcMeaning = findViewById(R.id.tvWcMeaning);
        tvWcMaskedWord = findViewById(R.id.tvWcMaskedWord);
        flWcOptionsContainer = findViewById(R.id.flWcOptionsContainer);

        // Spelling Test views
        tvSpellingChallengeMeaning = findViewById(R.id.tvSpellingChallengeMeaning);
        flSpellingLetterBoxesContainer = findViewById(R.id.flSpellingLetterBoxesContainer);
        btnSpellingQuizSubmit = findViewById(R.id.btnSpellingQuizSubmit);
        btnSpellingHint = findViewById(R.id.btnSpellingHint);


        // --- Toolbar Setup ---
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText(getResources().getString(R.string.timed_quiz));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.arrow_back_24);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // --- Status Bar Color ---
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

        // --- Database Initialization ---
        dbHelper = new DatabaseHelper(this);
        dbHelper.openDatabase();

        // --- Load Questions ---
        loadQuizQuestionsFromDatabase();

        if (questionList.isEmpty()) {
            Toast.makeText(this, "Not enough words with sufficient data available for a comprehensive timed quiz. Please add more words with meanings, synonyms, and antonyms.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // --- Set Button Listeners ---
        btnNext.setOnClickListener(v -> showNextQuestion());
        btnSpellingQuizSubmit.setOnClickListener(v -> {
            // For spelling test, check answer directly
            checkAnswer(TYPE_SPELLING_TEST, null);
        });
        btnSpellingHint.setOnClickListener(v -> showHint()); // Set listener for Hint Button

        // --- Start overall timer ---
        startOverallTimer();

        // --- Display the first question ---
        displayQuestion();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (dbHelper != null) {
            dbHelper.closeDatabase();
        }
    }

    // --- Quiz Question Interface and Implementations ---

    public interface QuizQuestion {
        int getType();
        String getInstruction();
        String getQuestionText();
        List<String> getOptions();
        String getCorrectAnswer();
        String getOriginalWord();
        String getOriginalMeaning();
    }

    // 1. Word Meaning Quiz Question
    public static class WordMeaningQuestion implements QuizQuestion {
        private String word;
        private String correctMeaning;
        private List<String> meaningOptions;

        public WordMeaningQuestion(String word, String correctMeaning, List<String> meaningOptions) {
            this.word = word;
            this.correctMeaning = correctMeaning;
            this.meaningOptions = meaningOptions;
        }

        @Override
        public int getType() { return TYPE_WORD_MEANING; }
        @Override
        public String getInstruction() { return "What is the meaning of this word?"; }
        @Override
        public String getQuestionText() { return word; }
        @Override
        public List<String> getOptions() { return meaningOptions; }
        @Override
        public String getCorrectAnswer() { return correctMeaning; }
        @Override
        public String getOriginalWord() { return word; }
        @Override
        public String getOriginalMeaning() { return correctMeaning; }
    }

    // 2. Word Completion Quiz Question
    public static class WordCompletionQuestion implements QuizQuestion {
        private String meaning;
        private String originalWord;
        private String maskedWordDisplay;
        private String correctMissingSequence;
        private List<String> options;

        public WordCompletionQuestion(String meaning, String originalWord,
                                      String maskedWordDisplay, String correctMissingSequence,
                                      List<String> options) {
            this.meaning = meaning;
            this.originalWord = originalWord;
            this.maskedWordDisplay = maskedWordDisplay;
            this.correctMissingSequence = correctMissingSequence;
            this.options = options;
        }

        @Override
        public int getType() { return TYPE_WORD_COMPLETION; }
        @Override
        public String getInstruction() { return "Complete the word for this meaning:"; }
        @Override
        public String getQuestionText() { return meaning; }
        @Override
        public List<String> getOptions() { return options; }
        @Override
        public String getCorrectAnswer() { return correctMissingSequence; }
        @Override
        public String getOriginalWord() { return originalWord; }
        @Override
        public String getOriginalMeaning() { return meaning; }
        public String getMaskedWordDisplay() { return maskedWordDisplay; }
    }

    // 3. Synonym/Antonym Quiz Question
    public static class SynonymAntonymQuestion implements QuizQuestion {
        private String word;
        private String correctRelationWord;
        private List<String> options;
        private boolean isSynonym;

        public SynonymAntonymQuestion(String word, String correctRelationWord, List<String> options, boolean isSynonym) {
            this.word = word;
            this.correctRelationWord = correctRelationWord;
            this.options = options;
            this.isSynonym = isSynonym;
        }

        @Override
        public int getType() { return TYPE_SYNONYM_ANTONYM; }
        @Override
        public String getInstruction() {
            return isSynonym ? "Select the SYNONYM for:" : "Select the ANTONYM for:";
        }
        @Override
        public String getQuestionText() { return word; }
        @Override
        public List<String> getOptions() { return options; }
        @Override
        public String getCorrectAnswer() { return correctRelationWord; }
        @Override
        public String getOriginalWord() { return word; }
        @Override
        public String getOriginalMeaning() { return correctRelationWord; }
        public boolean isSynonym() { return isSynonym; }
    }

    // 4. Spelling Test Quiz Question
    public static class SpellingQuestion implements QuizQuestion {
        private String meaning;
        private String correctSpelling;

        public SpellingQuestion(String meaning, String correctSpelling) {
            this.meaning = meaning;
            this.correctSpelling = correctSpelling;
        }

        @Override
        public int getType() { return TYPE_SPELLING_TEST; }
        @Override
        public String getInstruction() { return "Spell the word for this meaning:"; }
        @Override
        public String getQuestionText() { return meaning; }
        @Override
        public List<String> getOptions() { return null; }
        @Override
        public String getCorrectAnswer() { return correctSpelling; }
        @Override
        public String getOriginalWord() { return correctSpelling; }
        @Override
        public String getOriginalMeaning() { return meaning; }
    }


    // --- Core Quiz Logic Methods ---

    private void loadQuizQuestionsFromDatabase() {
        questionList = new ArrayList<>();
        List<WordData> allWords = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = dbHelper.getWordsWithSynonymsAntonyms();

            if (cursor != null && cursor.moveToFirst()) {
                int wordColIndex = cursor.getColumnIndex("word");
                int meaningColIndex = cursor.getColumnIndex("urdu_meaning");
                int synonymColIndex = cursor.getColumnIndex("synonym");
                int antonymColIndex = cursor.getColumnIndex("antonym");

                if (wordColIndex == -1 || meaningColIndex == -1) {
                    Log.e("TimedQuizActivity", "Required columns 'word' or 'urdu_meaning' not found in database.");
                    Toast.makeText(this, "Database error: missing required columns for quiz.", Toast.LENGTH_LONG).show();
                    return;
                }

                do {
                    String word = cursor.getString(wordColIndex);
                    String meaning = cursor.getString(meaningColIndex);
                    String synonym = (synonymColIndex != -1) ? cursor.getString(synonymColIndex) : null;
                    String antonym = (antonymColIndex != -1) ? cursor.getString(antonymColIndex) : null;


                    if (word != null && !word.trim().isEmpty() && meaning != null && !meaning.trim().isEmpty()) {
                        allWords.add(new WordData(word.trim(), meaning.trim(), synonym, antonym));
                    }
                } while (cursor.moveToNext());
            } else {
                Log.d("TimedQuizActivity", "Cursor is empty or null from getWordsWithMeaningsSynonymsAntonyms().");
            }
        } catch (Exception e) {
            Log.e("TimedQuizActivity", "Error loading questions from database: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to load quiz questions.", Toast.LENGTH_LONG).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        Collections.shuffle(allWords);

        for (int i = 0; i < MAX_QUIZ_QUESTIONS; i++) {
            if (allWords.isEmpty()) break;

            WordData candidateWordData = allWords.get(0);

            QuizQuestion question = null;
            List<Integer> availableTypes = new ArrayList<>(Arrays.asList(
                    TYPE_WORD_COMPLETION, TYPE_WORD_MEANING, TYPE_SYNONYM_ANTONYM, TYPE_SPELLING_TEST));
            Collections.shuffle(availableTypes);

            for (int type : availableTypes) {
                switch (type) {
                    case TYPE_WORD_COMPLETION:
                        if (candidateWordData.word.length() >= 4) {
                            question = generateWordCompletionQuestion(candidateWordData.word, candidateWordData.meaning, allWords);
                        }
                        break;
                    case TYPE_WORD_MEANING:
                        question = generateWordMeaningQuestion(candidateWordData.word, candidateWordData.meaning, allWords);
                        break;
                    case TYPE_SYNONYM_ANTONYM:
                        question = generateSynonymAntonymQuestion(candidateWordData.word, candidateWordData.synonym, candidateWordData.antonym, allWords);
                        break;
                    case TYPE_SPELLING_TEST:
                        question = generateSpellingQuestion(candidateWordData.meaning, candidateWordData.word);
                        break;
                }
                if (question != null) {
                    break;
                }
            }

            if (question != null) {
                questionList.add(question);
                allWords.remove(0);
            } else {
                Log.w("TimedQuizActivity", "Could not generate any valid question for word: " + candidateWordData.word + ". Skipping this word.");
                allWords.remove(0);
            }
        }

        Collections.shuffle(questionList);

        Log.d("TimedQuizActivity", "Loaded " + questionList.size() + " mixed questions for quiz.");
        if (questionList.size() < MAX_QUIZ_QUESTIONS) {
            Log.w("TimedQuizActivity", "Could not generate " + MAX_QUIZ_QUESTIONS + " questions. Generated " + questionList.size());
        }
    }

    private static class WordData {
        String word;
        String meaning;
        String synonym;
        String antonym;

        WordData(String word, String meaning, String synonym, String antonym) {
            this.word = word;
            this.meaning = meaning;
            this.synonym = (synonym != null) ? synonym.trim() : null;
            this.antonym = (antonym != null) ? antonym.trim() : null;
        }
    }

    // --- Question Generation Logic ---

    private WordMeaningQuestion generateWordMeaningQuestion(String word, String correctMeaning, List<WordData> allWords) {
        List<String> options = new ArrayList<>();
        options.add(correctMeaning);

        Set<String> addedOptions = new HashSet<>();
        addedOptions.add(correctMeaning.toLowerCase(Locale.getDefault()));

        List<String> potentialDistractors = new ArrayList<>();
        for (WordData wd : allWords) {
            if (!wd.word.equalsIgnoreCase(word) && wd.meaning != null && !wd.meaning.trim().isEmpty()) {
                potentialDistractors.add(wd.meaning.trim());
            }
        }
        Collections.shuffle(potentialDistractors);

        for (String distractor : potentialDistractors) {
            if (options.size() >= 4) break;
            if (!addedOptions.contains(distractor.toLowerCase(Locale.getDefault()))) {
                options.add(distractor);
                addedOptions.add(distractor.toLowerCase(Locale.getDefault()));
            }
        }

        while(options.size() < 4) {
            options.add("Option " + (options.size() + 1));
        }

        Collections.shuffle(options);
        return new WordMeaningQuestion(word, correctMeaning, options);
    }

    private WordCompletionQuestion generateWordCompletionQuestion(String originalWord, String meaning, List<WordData> allWords) {
        String processedWord = originalWord.toLowerCase(Locale.getDefault());
        int wordLength = processedWord.length();

        if (wordLength < 4) return null;

        int minBlanks = 1;
        int maxBlanks = Math.min(3, wordLength / 2);
        if (maxBlanks < minBlanks) maxBlanks = minBlanks;

        int numBlanks = random.nextInt(maxBlanks - minBlanks + 1) + minBlanks;

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

        Set<String> addedOptions = new HashSet<>();
        addedOptions.add(correctMissingSequence.toLowerCase(Locale.getDefault()));

        int attempts = 0;
        while (options.size() < 4 && attempts < 20) {
            StringBuilder distractorBuilder = new StringBuilder();
            for (int i = 0; i < numBlanks; i++) {
                char randomChar = (char) ('a' + random.nextInt(26));
                distractorBuilder.append(randomChar);
            }
            String distractor = distractorBuilder.toString();

            if (!addedOptions.contains(distractor.toLowerCase(Locale.getDefault()))) {
                options.add(distractor);
                addedOptions.add(distractor.toLowerCase(Locale.getDefault()));
            }
            attempts++;
        }
        while(options.size() < 4) {
            options.add(UUID.randomUUID().toString().substring(0, Math.min(numBlanks, UUID.randomUUID().toString().length())));
        }

        Collections.shuffle(options);

        return new WordCompletionQuestion(meaning, originalWord, maskedWordDisplay, correctMissingSequence, options);
    }


    private SynonymAntonymQuestion generateSynonymAntonymQuestion(String word, String synonym, String antonym, List<WordData> allWords) {
        List<String> options = new ArrayList<>();
        boolean isSynonymQuiz = random.nextBoolean();

        String correctRelation = null;
        if (isSynonymQuiz && synonym != null && !synonym.isEmpty()) {
            correctRelation = synonym.trim();
        } else if (!isSynonymQuiz && antonym != null && !antonym.isEmpty()) {
            correctRelation = antonym.trim();
        }

        if (correctRelation == null || correctRelation.isEmpty()) {
            return null;
        }

        options.add(correctRelation);
        Set<String> addedOptions = new HashSet<>();
        addedOptions.add(correctRelation.toLowerCase(Locale.getDefault()));

        List<String> requiredTypeDistractors = new ArrayList<>();

        for (WordData wd : allWords) {
            if (!wd.word.equalsIgnoreCase(word)) {
                if (isSynonymQuiz) {
                    if (wd.synonym != null && !wd.synonym.trim().isEmpty()) {
                        requiredTypeDistractors.add(wd.synonym.trim());
                    }
                } else {
                    if (wd.antonym != null && !wd.antonym.trim().isEmpty()) {
                        requiredTypeDistractors.add(wd.antonym.trim());
                    }
                }
            }
        }

        Collections.shuffle(requiredTypeDistractors);
        for (String distractor : requiredTypeDistractors) {
            if (options.size() >= 4) break;
            if (!addedOptions.contains(distractor.toLowerCase(Locale.getDefault())) &&
                    !distractor.equalsIgnoreCase(word) &&
                    !distractor.equalsIgnoreCase(correctRelation)) {
                options.add(distractor);
                addedOptions.add(distractor.toLowerCase(Locale.getDefault()));
            }
        }

        while(options.size() < 4) {
            options.add("Related Term " + (options.size() + 1));
        }

        Collections.shuffle(options);
        return new SynonymAntonymQuestion(word, correctRelation, options, isSynonymQuiz);
    }

    private SpellingQuestion generateSpellingQuestion(String meaning, String word) {
        if (meaning == null || meaning.trim().isEmpty() || word == null || word.trim().isEmpty()) {
            return null;
        }
        return new SpellingQuestion(meaning, word);
    }


    // --- Display Question Logic ---
    private void displayQuestion() {
        if (currentQuestionIndex >= questionList.size()) {
            showQuizResults(true); // All questions finished successfully
            return;
        }

        // Timer continues to run from the overall timer.
        // No individual question timer to reset or start here.

        answerChecked = false;
        tvFeedback.setVisibility(View.INVISIBLE);
        tvFeedback.setText("");
        btnNext.setVisibility(View.INVISIBLE);
        btnSpellingQuizSubmit.setVisibility(View.GONE);
        btnSpellingHint.setVisibility(View.GONE); // Hide hint button by default

        // Hide all quiz-specific layouts
        mcSynAntQuizLayout.setVisibility(View.GONE);
        wcQuizLayout.setVisibility(View.GONE);
        spellingQuizLayout.setVisibility(View.GONE);

        // Clear previous options/inputs
        flMcSynAntOptionsContainer.removeAllViews();
        flWcOptionsContainer.removeAllViews();
        flSpellingLetterBoxesContainer.removeAllViews();
        letterEditTexts = new ArrayList<>();

        QuizQuestion currentQuestion = questionList.get(currentQuestionIndex);

        tvQuestionCounter.setText("Word " + (currentQuestionIndex + 1) + "/" + questionList.size());
        tvScore.setText("Score: " + score);
        tvInstruction.setText(currentQuestion.getInstruction());

        switch (currentQuestion.getType()) {
            case TYPE_WORD_MEANING:
            case TYPE_SYNONYM_ANTONYM:
                mcSynAntQuizLayout.setVisibility(View.VISIBLE);
                tvMcSynAntWord.setText(capitalizeFirstLetter(currentQuestion.getQuestionText()));

                for (String option : currentQuestion.getOptions()) {
                    Button optionButton = createOptionButton(option);
                    optionButton.setOnClickListener(v -> {
                        if (!answerChecked) {
                            checkAnswer(currentQuestion.getType(), (String) v.getTag());
                        }
                    });
                    flMcSynAntOptionsContainer.addView(optionButton);
                }
                break;

            case TYPE_WORD_COMPLETION:
                wcQuizLayout.setVisibility(View.VISIBLE);
                WordCompletionQuestion wcQuestion = (WordCompletionQuestion) currentQuestion;
                tvWcMeaning.setText(capitalizeFirstLetter(wcQuestion.getOriginalMeaning()));
                tvWcMaskedWord.setText(capitalizeFirstLetter(wcQuestion.getMaskedWordDisplay()));

                for (String option : wcQuestion.getOptions()) {
                    Button optionButton = createOptionButton(option);
                    optionButton.setText(formatLettersWithCommas((String)optionButton.getTag()));
                    optionButton.setOnClickListener(v -> {
                        if (!answerChecked) {
                            checkAnswer(currentQuestion.getType(), (String) v.getTag());
                        }
                    });
                    flWcOptionsContainer.addView(optionButton);
                }
                break;

            case TYPE_SPELLING_TEST:
                spellingQuizLayout.setVisibility(View.VISIBLE);
                btnSpellingQuizSubmit.setVisibility(View.VISIBLE);
                btnSpellingHint.setVisibility(View.VISIBLE); // Show hint button for spelling test
                btnSpellingHint.setEnabled(true); // Re-enable hint button for new question

                SpellingQuestion spellingQuestion = (SpellingQuestion) currentQuestion;
                tvSpellingChallengeMeaning.setText(capitalizeFirstLetter(spellingQuestion.getOriginalMeaning()));

                String correctSpelling = spellingQuestion.getCorrectAnswer();
                for (int i = 0; i < correctSpelling.length(); i++) {
                    EditText letterBox = createLetterBox(i, correctSpelling.length());
                    letterEditTexts.add(letterBox);
                    flSpellingLetterBoxesContainer.addView(letterBox);
                }

                // Request focus for the first ENABLED EditText
                boolean focused = false;
                for (EditText et : letterEditTexts) {
                    if (et.isEnabled()) {
                        et.requestFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
                        }
                        focused = true;
                        break;
                    }
                }
                if (!focused) { // If all boxes are disabled (e.g., all hinted)
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(flSpellingLetterBoxesContainer.getWindowToken(), 0);
                    }
                }
                break;
        }
    }

    private Button createOptionButton(String originalTextValue) {
        Button optionButton = new Button(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(
                (int) getResources().getDimension(R.dimen.option_button_margin),
                (int) getResources().getDimension(R.dimen.option_button_margin),
                (int) getResources().getDimension(R.dimen.option_button_margin),
                (int) getResources().getDimension(R.dimen.option_button_margin)
        );
        params.gravity = Gravity.CENTER_HORIZONTAL;

        optionButton.setLayoutParams(params);
        optionButton.setTag(originalTextValue);

        optionButton.setText(capitalizeFirstLetter(originalTextValue));

        optionButton.setTextSize(18f);
        optionButton.setTextColor(ContextCompat.getColor(this, R.color.quiz_option_text_default));
        optionButton.setBackgroundResource(R.drawable.quiz_button_bg_selector);
        optionButton.setElevation(4f);
        optionButton.setPadding(20, 16, 20, 16);
        return optionButton;
    }

    private EditText createLetterBox(final int currentIndex, final int wordLength) {
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
        letterBox.setEms(1);

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
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(letterBox.getWindowToken(), 0);
                        }
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0 && currentIndex > 0) {
                    // Find the previous ENABLED EditText
                    int prevFocusIndex = currentIndex - 1;
                    while (prevFocusIndex >= 0 && !letterEditTexts.get(prevFocusIndex).isEnabled()) {
                        prevFocusIndex--; // Skip disabled boxes backwards
                    }
                    if (prevFocusIndex >= 0) {
                        letterEditTexts.get(prevFocusIndex).requestFocus();
                        letterEditTexts.get(prevFocusIndex).setText(""); // Clear previous box if backspacing from empty one
                    }
                }
            }
        });

        letterBox.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                EditText currentEditText = (EditText) v;
                if (currentEditText.getText().length() == 0 && currentIndex > 0) {
                    // Find the previous ENABLED EditText
                    int prevFocusIndex = currentIndex - 1;
                    while (prevFocusIndex >= 0 && !letterEditTexts.get(prevFocusIndex).isEnabled()) {
                        prevFocusIndex--; // Skip disabled boxes backwards
                    }

                    if (prevFocusIndex >= 0) {
                        letterEditTexts.get(prevFocusIndex).requestFocus();
                        return true; // Consume the event to prevent default backspace from clearing
                    }
                }
            }
            return false;
        });

        return letterBox;
    }


    // --- Answer Checking Logic ---
    private void checkAnswer(int quizType, String selectedOption) {
        if (answerChecked) {
            return;
        }
        answerChecked = true;
        // The overall timer is NOT cancelled here, it keeps running.

        QuizQuestion currentQuestion = questionList.get(currentQuestionIndex);
        String correctAnswer = currentQuestion.getCorrectAnswer().toLowerCase(Locale.getDefault());
        boolean isCorrect = false;

        if (quizType == TYPE_SPELLING_TEST) {
            StringBuilder userAnswerBuilder = new StringBuilder();
            for (EditText et : letterEditTexts) {
                userAnswerBuilder.append(et.getText().toString().trim().toLowerCase(Locale.getDefault()));
            }
            String userAnswer = userAnswerBuilder.toString();
            isCorrect = userAnswer.equals(correctAnswer);

            // Disable hint button after checking answer for spelling test
            btnSpellingHint.setVisibility(View.GONE);

            for (int i = 0; i < letterEditTexts.size(); i++) {
                EditText et = letterEditTexts.get(i);
                et.setEnabled(false); // Disable editing after submission
                if (i < correctAnswer.length() && String.valueOf(correctAnswer.charAt(i)).toLowerCase(Locale.getDefault()).equals(et.getText().toString().toLowerCase(Locale.getDefault()))) {
                    et.setBackgroundResource(R.drawable.letter_box_correct_background);
                    et.setTextColor(Color.WHITE);
                } else {
                    et.setBackgroundResource(R.drawable.letter_box_incorrect_background);
                    et.setTextColor(Color.WHITE);
                }
            }
        } else { // Multiple choice or word completion
            LinearLayout optionsContainer = (quizType == TYPE_WORD_COMPLETION) ? flWcOptionsContainer : flMcSynAntOptionsContainer;

            for (int i = 0; i < optionsContainer.getChildCount(); i++) {
                Button optionButton = (Button) optionsContainer.getChildAt(i);
                optionButton.setEnabled(false);

                String optionValueFromTag = (String) optionButton.getTag();

                if (optionValueFromTag != null) {
                    if (optionValueFromTag.toLowerCase(Locale.getDefault()).equals(correctAnswer)) {
                        optionButton.setBackgroundColor(ContextCompat.getColor(this, R.color.quiz_highlight_correct));
                        optionButton.setTextColor(Color.WHITE);
                        if (selectedOption != null && selectedOption.toLowerCase(Locale.getDefault()).equals(correctAnswer)) {
                            isCorrect = true;
                        }
                    } else if (selectedOption != null && optionValueFromTag.toLowerCase(Locale.getDefault()).equals(selectedOption.toLowerCase(Locale.getDefault()))) {
                        optionButton.setBackgroundColor(ContextCompat.getColor(this, R.color.quiz_highlight_incorrect));
                        optionButton.setTextColor(Color.WHITE);
                    }
                }
            }
        }

        // --- Update Score and Feedback ---
        if (isCorrect) {
            score++;
            tvScore.setText("Score: " + score);
            tvFeedback.setText("Correct!");
            tvFeedback.setTextColor(ContextCompat.getColor(this, R.color.quiz_feedback_correct));
        } else {
            String correctAnsDisplay = capitalizeFirstLetter(currentQuestion.getCorrectAnswer());
            String incorrectPart = "Incorrect.\n";
            String correctAnswerPart = "Correct answer: " + correctAnsDisplay;

            SpannableString spannableFeedback = new SpannableString(incorrectPart + correctAnswerPart);

            // Apply red color to the "Incorrect." part
            spannableFeedback.setSpan(
                    new ForegroundColorSpan(ContextCompat.getColor(this, R.color.quiz_feedback_incorrect)),
                    0,
                    incorrectPart.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            // Apply black color to the "Correct answer: [answer]" part
            spannableFeedback.setSpan(
                    new ForegroundColorSpan(ContextCompat.getColor(this, R.color.black)),
                    incorrectPart.length(),
                    incorrectPart.length() + correctAnswerPart.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            tvFeedback.setText(spannableFeedback);
            tvFeedback.setTextColor(ContextCompat.getColor(this, R.color.quiz_feedback_incorrect));
        }
        tvFeedback.setVisibility(View.VISIBLE);
        btnNext.setVisibility(View.VISIBLE);
    }

    // --- Hint Logic for Spelling Test ---
    private void showHint() {
        if (answerChecked) { // Don't allow hints after answer is checked
            Toast.makeText(this, "Answer already submitted!", Toast.LENGTH_SHORT).show();
            return;
        }

        QuizQuestion currentQuestion = questionList.get(currentQuestionIndex);
        if (currentQuestion.getType() != TYPE_SPELLING_TEST) {
            // This should ideally not happen if btnSpellingHint is only visible for spelling test
            return;
        }

        String correctSpelling = currentQuestion.getCorrectAnswer();

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


    // --- Overall Timer Management ---
    private void startOverallTimer() {
        progressBarTimer.setMax((int) OVERALL_QUIZ_TIME_LIMIT_MILLIS);
        progressBarTimer.setProgress((int) OVERALL_QUIZ_TIME_LIMIT_MILLIS);
        timeLeftInMillis = OVERALL_QUIZ_TIME_LIMIT_MILLIS;

        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountDownText();
                progressBarTimer.setProgress((int) millisUntilFinished);
            }

            @Override
            public void onFinish() {
                timeLeftInMillis = 0;
                updateCountDownText();
                progressBarTimer.setProgress(0);
                // Quiz finishes when timer runs out, regardless of questions answered
                showQuizResults(false); // Quiz finished due to time limit
            }
        }.start();
    }

    private void updateCountDownText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        tvTimer.setText(timeFormatted);
    }

    // --- Navigation ---
    private void showNextQuestion() {
        currentQuestionIndex++;
        if (currentQuestionIndex < questionList.size()) {
            displayQuestion();
        } else {
            showQuizResults(true); // All questions finished successfully
        }
    }

    private void showQuizResults(boolean completedAllQuestions) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        QuizResultsDialogFragment dialogFragment = QuizResultsDialogFragment.newInstance(score, questionList.size());
        dialogFragment.show(getSupportFragmentManager(), "quiz_results_dialog");
    }
    // You can add this method anywhere within your TimedQuizActivity class
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
        loadQuizQuestionsFromDatabase();

        // Check if we were able to load new questions
        if (questionList.isEmpty()) {
            Toast.makeText(this, "Not enough words available to start a new quiz.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // 3. Start a new countdown timer for the entire quiz
        if (countDownTimer != null) {
            countDownTimer.cancel(); // Stop the old timer if it's still running
        }
        startOverallTimer();

        // 4. Display the very first question of the new quiz
        displayQuestion();
    }

    // --- Utility Methods ---

    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.substring(0, 1).toUpperCase(Locale.getDefault()) + text.substring(1);
    }

    private String formatLettersWithCommas(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            sb.append(input.charAt(i));
            if (i < input.length() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}