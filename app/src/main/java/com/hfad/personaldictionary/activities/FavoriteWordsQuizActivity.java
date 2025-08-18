package com.hfad.personaldictionary.activities;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
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
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.flexbox.FlexboxLayout;
import com.hfad.personaldictionary.database.DatabaseHelper;
import com.hfad.personaldictionary.database.FavoriteWordsDbHelper;
import com.hfad.personaldictionary.R;
import com.hfad.personaldictionary.fragments.QuizResultsDialogFragment;
import com.hfad.personaldictionary.models.FavoriteWord;

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


public class FavoriteWordsQuizActivity extends AppCompatActivity {

    // --- UI Elements ---
    private TextView tvQuestionCounter, tvScore, tvInstruction, tvFeedback;
    private Button btnNext;

    // Quiz Type Specific UI Containers
    private LinearLayout mcQuizLayout, wcQuizLayout, spellingQuizLayout;
    private TextView tvMcWord;
    private LinearLayout flMcOptionsContainer;

    // Word Completion UI
    private TextView tvWcMeaning, tvWcMaskedWord;
    private LinearLayout flWcOptionsContainer;

    // Spelling Test UI
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

    // --- Database Helper ---
    private DatabaseHelper dbHelper;
    private Random random = new Random();

    // --- Constants for Quiz Types ---
    private static final int TYPE_WORD_COMPLETION = 0;
    private static final int TYPE_WORD_MEANING = 1;
    private static final int TYPE_SPELLING_TEST = 3;

    private static final int MAX_QUIZ_QUESTIONS = 10; // This defines the exact number of questions required

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_words_quiz);

        // --- UI Initialization ---
        tvQuestionCounter = findViewById(R.id.tvFavoriteQuizQuestionCounter);
        tvScore = findViewById(R.id.tvFavoriteQuizScore);
        tvInstruction = findViewById(R.id.tvFavoriteQuizInstruction);
        tvFeedback = findViewById(R.id.tvFavoriteQuizFeedback);
        btnNext = findViewById(R.id.btnFavoriteQuizNext);

        // Quiz type specific layout containers
        mcQuizLayout = findViewById(R.id.mcSynAntQuizLayout);
        wcQuizLayout = findViewById(R.id.wcQuizLayout);
        spellingQuizLayout = findViewById(R.id.spellingQuizLayout);

        // Multiple Choice (now only for Word Meaning) views
        tvMcWord = findViewById(R.id.tvMcSynAntWord);
        flMcOptionsContainer = findViewById(R.id.flMcSynAntOptionsContainer);

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
        toolbarTitle.setText(getResources().getString(R.string.favorite_words_quiz));

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
                window.setStatusBarColor(ContextCompat.getColor(this, R.color.favorite_quiz_background));
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

        // Strict check: If we don't have exactly MAX_QUIZ_QUESTIONS, don't start the quiz.
        if (questionList.size() < MAX_QUIZ_QUESTIONS) {
            Toast.makeText(this, "Add at least " + MAX_QUIZ_QUESTIONS + " words to favorites to start this quiz.", Toast.LENGTH_LONG).show();
            finish(); // End the activity if the exact number of questions cannot be met.
            return; // Stop further execution of onCreate.
        }

        // --- Set Button Listeners (only if quiz is ready) ---
        btnNext.setOnClickListener(v -> showNextQuestion());
        btnSpellingQuizSubmit.setOnClickListener(v -> {
            checkAnswer(TYPE_SPELLING_TEST, null);
        });
        btnSpellingHint.setOnClickListener(v -> showHint());

        // --- Display the first question (only if quiz is ready) ---
        displayQuestion();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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

    public static class WordMeaningQuestion implements QuizQuestion {
        private String word;
        private String correctMeaning;
        private List<String> meaningOptions;

        public WordMeaningQuestion(String word, String correctMeaning, List<String> meaningOptions) {
            this.word = word;
            this.correctMeaning = correctMeaning;
            this.meaningOptions = meaningOptions;
        }

        @Override public int getType() { return TYPE_WORD_MEANING; }
        @Override public String getInstruction() { return "What is the meaning of this word?"; }
        @Override public String getQuestionText() { return word; }
        @Override public List<String> getOptions() { return meaningOptions; }
        @Override public String getCorrectAnswer() { return correctMeaning; }
        @Override public String getOriginalWord() { return word; }
        @Override public String getOriginalMeaning() { return correctMeaning; }
    }

    public static class WordCompletionQuestion implements QuizQuestion {
        private String meaning;
        private String originalWord;
        private String maskedWordDisplay;
        private String correctMissingSequence;
        private List<String> options;

        public WordCompletionQuestion(String meaning, String originalWord, String maskedWordDisplay, String correctMissingSequence, List<String> options) {
            this.meaning = meaning;
            this.originalWord = originalWord;
            this.maskedWordDisplay = maskedWordDisplay;
            this.correctMissingSequence = correctMissingSequence;
            this.options = options;
        }

        @Override public int getType() { return TYPE_WORD_COMPLETION; }
        @Override public String getInstruction() { return "Complete the word for this meaning:"; }
        @Override public String getQuestionText() { return meaning; }
        @Override public List<String> getOptions() { return options; }
        @Override public String getCorrectAnswer() { return correctMissingSequence; }
        @Override public String getOriginalWord() { return originalWord; }
        @Override public String getOriginalMeaning() { return meaning; }
        public String getMaskedWordDisplay() { return maskedWordDisplay; }
    }

    public static class SpellingQuestion implements QuizQuestion {
        private String meaning;
        private String correctSpelling;

        public SpellingQuestion(String meaning, String correctSpelling) {
            this.meaning = meaning;
            this.correctSpelling = correctSpelling;
        }

        @Override public int getType() { return TYPE_SPELLING_TEST; }
        @Override public String getInstruction() { return "Spell the word for this meaning:"; }
        @Override public String getQuestionText() { return meaning; }
        @Override public List<String> getOptions() { return null; }
        @Override public String getCorrectAnswer() { return correctSpelling; }
        @Override public String getOriginalWord() { return correctSpelling; }
        @Override public String getOriginalMeaning() { return meaning; }
    }

    // --- Core Quiz Logic Methods ---

    private void loadQuizQuestionsFromDatabase() {
        questionList = new ArrayList<>(); // Initialize the main list
        List<WordData> allWordsWithMeaning = new ArrayList<>();

        // 1. Get a list of all favorite words from the new helper
        FavoriteWordsDbHelper favoriteWordsDbHelper = new FavoriteWordsDbHelper(this);
        List<FavoriteWord> favoriteWordList = favoriteWordsDbHelper.getAllFavoriteWords();

        // Check if we even have any favorite words
        if (favoriteWordList.isEmpty()) {
            Log.d("FavoriteQuizActivity", "No favorite words found to generate quiz.");
            return; // Return with an empty list
        }

        // 2. Loop through the favorite words and get their meanings from the main dictionary
        Cursor meaningCursor = null;
        try {
            for (FavoriteWord favoriteWord : favoriteWordList) {
                // Filter out any non-English words as requested
                if (!favoriteWord.isLanguageEnglish()) {
                    continue;
                }

                String word = favoriteWord.getWord();
                boolean isEnglish = favoriteWord.isLanguageEnglish(); // Get the language from the model

                // Pass the correct language flag from the FavoriteWord object
                meaningCursor = dbHelper.getAllDefinitionsForWord(word, isEnglish);
                if (meaningCursor != null && meaningCursor.moveToFirst()) {
                    String meaning = meaningCursor.getString(meaningCursor.getColumnIndexOrThrow("urdu_meaning"));

                    if (word != null && !word.trim().isEmpty() && meaning != null && !meaning.trim().isEmpty()) {
                        allWordsWithMeaning.add(new WordData(word.trim(), meaning.trim(), null, null));
                    }
                }
                if (meaningCursor != null) {
                    meaningCursor.close();
                }
            }
        } catch (Exception e) {
            Log.e("FavoriteQuizActivity", "Error loading words from database for quiz: " + e.getMessage(), e);
            return;
        }

        // Now, continue with your existing quiz generation logic
        if (allWordsWithMeaning.size() < MAX_QUIZ_QUESTIONS) {
            Log.d("FavoriteQuizActivity", "Not enough English favorite words with meanings to generate quiz.");
            return;
        }

        // Shuffle the pool of available favorite words
        Collections.shuffle(allWordsWithMeaning);

        List<QuizQuestion> tempQuestions = new ArrayList<>();

        for (int i = 0; i < allWordsWithMeaning.size() && tempQuestions.size() < MAX_QUIZ_QUESTIONS; i++) {
            WordData candidateWordData = allWordsWithMeaning.get(i);
            QuizQuestion question = null;

            List<Integer> availableTypes = new ArrayList<>(Arrays.asList(
                    TYPE_WORD_COMPLETION, TYPE_WORD_MEANING, TYPE_SPELLING_TEST));
            Collections.shuffle(availableTypes);

            for (int type : availableTypes) {
                switch (type) {
                    case TYPE_WORD_COMPLETION:
                        if (candidateWordData.word.length() >= 4) {
                            question = generateWordCompletionQuestion(candidateWordData.word, candidateWordData.meaning, allWordsWithMeaning);
                        }
                        break;
                    case TYPE_WORD_MEANING:
                        question = generateWordMeaningQuestion(candidateWordData.word, candidateWordData.meaning, allWordsWithMeaning);
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
                tempQuestions.add(question);
            } else {
                Log.w("FavoriteQuizActivity", "Could not generate any valid question for favorite word: '" + candidateWordData.word + "'. Skipping this word from quiz pool.");
            }
        }

        if (tempQuestions.size() == MAX_QUIZ_QUESTIONS) {
            questionList.addAll(tempQuestions);
            Collections.shuffle(questionList);
            Log.d("FavoriteQuizActivity", "Successfully generated " + questionList.size() + " quiz questions.");
        } else {
            Log.d("FavoriteQuizActivity", "Could not generate exactly " + MAX_QUIZ_QUESTIONS + " questions. Generated: " + tempQuestions.size());
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

    private WordMeaningQuestion generateWordMeaningQuestion(String word, String correctMeaning, List<WordData> allWordsForDistractors) {
        List<String> options = new ArrayList<>();
        options.add(correctMeaning);

        Set<String> addedOptions = new HashSet<>();
        addedOptions.add(correctMeaning.toLowerCase(Locale.getDefault()));

        List<String> potentialDistractors = new ArrayList<>();
        // Collect distractors from all available words, excluding the current word's meaning
        for (WordData wd : allWordsForDistractors) {
            if (!wd.word.equalsIgnoreCase(word) && wd.meaning != null && !wd.meaning.trim().isEmpty()) {
                potentialDistractors.add(wd.meaning.trim());
            }
        }
        Collections.shuffle(potentialDistractors);

        // Add up to 3 unique distractors
        for (String distractor : potentialDistractors) {
            if (options.size() >= 4) break; // We need 1 correct + 3 distractors = 4 options
            if (!addedOptions.contains(distractor.toLowerCase(Locale.getDefault()))) {
                options.add(distractor);
                addedOptions.add(distractor.toLowerCase(Locale.getDefault()));
            }
        }

        // If not enough unique distractors, fill with generic options
        while(options.size() < 4) {
            options.add("Another Meaning " + UUID.randomUUID().toString().substring(0,4));
        }

        Collections.shuffle(options);
        return new WordMeaningQuestion(word, correctMeaning, options);
    }

    private WordCompletionQuestion generateWordCompletionQuestion(String originalWord, String meaning, List<WordData> allWordsForDistractors) {
        String processedWord = originalWord.toLowerCase(Locale.getDefault());
        int wordLength = processedWord.length();

        if (wordLength < 4) return null; // Can't make a meaningful completion question from very short words

        int minBlanks = 1;
        int maxBlanks = Math.min(3, wordLength / 2); // Max 3 blanks or half the word length
        if (maxBlanks < minBlanks) maxBlanks = minBlanks; // Ensure maxBlanks is at least minBlanks

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

        // Generate distractors
        int attempts = 0;
        while (options.size() < 4 && attempts < 20) { // Limit attempts to prevent infinite loops
            StringBuilder distractorBuilder = new StringBuilder();
            for (int i = 0; i < numBlanks; i++) {
                char randomChar = (char) ('a' + random.nextInt(26)); // Random lowercase English letter
                distractorBuilder.append(randomChar);
            }
            String distractor = distractorBuilder.toString();

            if (!addedOptions.contains(distractor.toLowerCase(Locale.getDefault()))) {
                options.add(distractor);
                addedOptions.add(distractor.toLowerCase(Locale.getDefault()));
            }
            attempts++;
        }
        // Fallback: If still not 4 unique options (e.g., due to short length of missing sequence), add generic
        while(options.size() < 4) {
            options.add(UUID.randomUUID().toString().substring(0, Math.min(numBlanks, UUID.randomUUID().toString().length())));
        }

        Collections.shuffle(options);

        return new WordCompletionQuestion(meaning, originalWord, maskedWordDisplay, correctMissingSequence, options);
    }

    private SpellingQuestion generateSpellingQuestion(String meaning, String word) {
        if (meaning == null || meaning.trim().isEmpty() || word == null || word.trim().isEmpty()) {
            return null; // A word must have a meaning and the word itself to be a spelling question
        }
        return new SpellingQuestion(meaning, word);
    }


    // --- Display Question Logic ---
    private void displayQuestion() {
        if (currentQuestionIndex >= questionList.size()) {
            showQuizResults();
            return;
        }

        answerChecked = false;
        tvFeedback.setVisibility(View.INVISIBLE);
        tvFeedback.setText("");
        btnNext.setVisibility(View.INVISIBLE);
        btnSpellingQuizSubmit.setVisibility(View.GONE);
        btnSpellingHint.setVisibility(View.GONE);

        // Hide all quiz-specific layouts
        mcQuizLayout.setVisibility(View.GONE);
        wcQuizLayout.setVisibility(View.GONE);
        spellingQuizLayout.setVisibility(View.GONE);

        // Clear previous options/inputs
        flMcOptionsContainer.removeAllViews();
        flWcOptionsContainer.removeAllViews();
        flSpellingLetterBoxesContainer.removeAllViews();
        letterEditTexts = new ArrayList<>();

        QuizQuestion currentQuestion = questionList.get(currentQuestionIndex);

        tvQuestionCounter.setText("Word " + (currentQuestionIndex + 1) + "/" + questionList.size());
        tvScore.setText("Score: " + score);
        tvInstruction.setText(currentQuestion.getInstruction());

        switch (currentQuestion.getType()) {
            case TYPE_WORD_MEANING:
                mcQuizLayout.setVisibility(View.VISIBLE);
                tvMcWord.setText(capitalizeFirstLetter(currentQuestion.getQuestionText()));

                for (String option : currentQuestion.getOptions()) {
                    Button optionButton = createOptionButton(option);
                    optionButton.setOnClickListener(v -> {
                        if (!answerChecked) {
                            checkAnswer(currentQuestion.getType(), (String) v.getTag());
                        }
                    });
                    flMcOptionsContainer.addView(optionButton);
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
                btnSpellingHint.setVisibility(View.VISIBLE);
                btnSpellingHint.setEnabled(true);

                SpellingQuestion spellingQuestion = (SpellingQuestion) currentQuestion;
                tvSpellingChallengeMeaning.setText(capitalizeFirstLetter(spellingQuestion.getOriginalMeaning()));

                String correctSpelling = spellingQuestion.getCorrectAnswer();
                for (int i = 0; i < correctSpelling.length(); i++) {
                    EditText letterBox = createLetterBox(i, correctSpelling.length());
                    letterEditTexts.add(letterBox);
                    flSpellingLetterBoxesContainer.addView(letterBox);
                }

                // Request focus on the first editable letter box
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
                if (!focused) { // If no editable box, hide keyboard
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
                    int nextFocusIndex = currentIndex + 1;
                    while (nextFocusIndex < letterEditTexts.size() && !letterEditTexts.get(nextFocusIndex).isEnabled()) {
                        nextFocusIndex++;
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
                    int prevFocusIndex = currentIndex - 1;
                    while (prevFocusIndex >= 0 && !letterEditTexts.get(prevFocusIndex).isEnabled()) {
                        prevFocusIndex--;
                    }
                    if (prevFocusIndex >= 0) {
                        letterEditTexts.get(prevFocusIndex).requestFocus();
                        letterEditTexts.get(prevFocusIndex).setText("");
                    }
                }
            }
        });

        letterBox.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                EditText currentEditText = (EditText) v;
                if (currentEditText.getText().length() == 0 && currentIndex > 0) {
                    int prevFocusIndex = currentIndex - 1;
                    while (prevFocusIndex >= 0 && !letterEditTexts.get(prevFocusIndex).isEnabled()) {
                        prevFocusIndex--;
                    }
                    if (prevFocusIndex >= 0) {
                        letterEditTexts.get(prevFocusIndex).requestFocus();
                        return true;
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

            btnSpellingHint.setVisibility(View.GONE);

            for (int i = 0; i < letterEditTexts.size(); i++) {
                EditText et = letterEditTexts.get(i);
                et.setEnabled(false); // Disable all letter boxes
                if (i < correctAnswer.length() && String.valueOf(correctAnswer.charAt(i)).toLowerCase(Locale.getDefault()).equals(et.getText().toString().toLowerCase(Locale.getDefault()))) {
                    et.setBackgroundResource(R.drawable.letter_box_correct_background);
                    et.setTextColor(Color.WHITE);
                } else {
                    et.setBackgroundResource(R.drawable.letter_box_incorrect_background);
                    et.setTextColor(Color.WHITE);
                }
            }
        } else { // Multiple choice (now only for word meaning/completion)
            LinearLayout optionsContainer = (quizType == TYPE_WORD_COMPLETION) ? flWcOptionsContainer : flMcOptionsContainer;

            for (int i = 0; i < optionsContainer.getChildCount(); i++) {
                Button optionButton = (Button) optionsContainer.getChildAt(i);
                optionButton.setEnabled(false); // Disable all option buttons

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

            spannableFeedback.setSpan(
                    new ForegroundColorSpan(ContextCompat.getColor(this, R.color.quiz_feedback_incorrect)),
                    0,
                    incorrectPart.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            spannableFeedback.setSpan(
                    new ForegroundColorSpan(ContextCompat.getColor(this, R.color.black)),
                    incorrectPart.length(),
                    incorrectPart.length() + correctAnswerPart.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            tvFeedback.setText(spannableFeedback);
        }
        tvFeedback.setVisibility(View.VISIBLE);
        btnNext.setVisibility(View.VISIBLE);
    }

    // --- Hint Logic for Spelling Test ---
    private void showHint() {
        if (answerChecked) {
            Toast.makeText(this, "Answer already submitted!", Toast.LENGTH_SHORT).show();
            return;
        }

        QuizQuestion currentQuestion = questionList.get(currentQuestionIndex);
        if (currentQuestion.getType() != TYPE_SPELLING_TEST) {
            return; // Hint only applies to spelling test
        }

        String correctSpelling = currentQuestion.getCorrectAnswer();

        List<Integer> possibleHintIndices = new ArrayList<>();
        for (int i = 0; i < letterEditTexts.size(); i++) {
            // Find empty and enabled letter boxes
            if (letterEditTexts.get(i).getText().toString().isEmpty() && letterEditTexts.get(i).isEnabled()) {
                possibleHintIndices.add(i);
            }
        }

        if (possibleHintIndices.isEmpty()) {
            Toast.makeText(this, "No more empty boxes for hints!", Toast.LENGTH_SHORT).show();
            btnSpellingHint.setEnabled(false); // Disable hint button if no more hints can be given
            return;
        }

        Collections.shuffle(possibleHintIndices);
        int hintIndex = possibleHintIndices.get(0); // Pick one random empty box

        EditText hintedBox = letterEditTexts.get(hintIndex);
        char correctLetter = correctSpelling.charAt(hintIndex);

        hintedBox.setText(String.valueOf(correctLetter).toUpperCase(Locale.getDefault()));
        hintedBox.setEnabled(false); // Disable the hinted box
        hintedBox.setBackgroundResource(R.drawable.letter_box_correct_background); // Indicate it's filled
        hintedBox.setTextColor(ContextCompat.getColor(this, R.color.quiz_text_primary));

        // After hint, check if any boxes are still empty and enabled. If not, disable hint button.
        boolean anyRemainingEmptyEditableBoxes = false;
        for (EditText et : letterEditTexts) {
            if (et.getText().toString().isEmpty() && et.isEnabled()) {
                anyRemainingEmptyEditableBoxes = true;
                break;
            }
        }
        if (!anyRemainingEmptyEditableBoxes) {
            btnSpellingHint.setEnabled(false);
        }

        // Move focus to the next empty editable box if any
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
        if (!focused) { // If no more editable boxes, hide keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(hintedBox.getWindowToken(), 0);
            }
        }
    }


    // --- Navigation ---
    private void showNextQuestion() {
        currentQuestionIndex++;
        if (currentQuestionIndex < questionList.size()) {
            displayQuestion();
        } else {
            showQuizResults();
        }
    }

    // --- Quiz Results ---
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
        loadQuizQuestionsFromDatabase();

        // Check if we were able to load new questions
        if (questionList.isEmpty()) {
            Toast.makeText(this, "Not enough English words available to start a new quiz.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 3. Display the very first question of the new quiz
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
