package com.hfad.personaldictionary.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.hfad.personaldictionary.database.DatabaseHelper;
import com.hfad.personaldictionary.database.FavoriteWordsDbHelper;
import com.hfad.personaldictionary.R;
import com.hfad.personaldictionary.models.FavoriteWord;

import java.util.Locale;
import java.util.Objects;

public class WordDetailsActivity extends AppCompatActivity {

    private static final String TAG = "WordDetailsActivity";

    LinearLayout definitionDetailsContainer;
    DatabaseHelper databaseHelper;

    TextToSpeech textToSpeech;

    private float currentFontSize = 16f;
    private final float MIN_FONT_SIZE = 12f;
    private final float MAX_FONT_SIZE = 28f;

    ImageButton favoriteBtn;
    private FavoriteWordsDbHelper favoriteWordsDbHelper;
    private String currentWord;
    private long currentWordId;
    private boolean isLanguageEnglish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_details);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.arrow_back_24);
        }

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));

        definitionDetailsContainer = findViewById(R.id.definitionDetailsContainer);
        databaseHelper = new DatabaseHelper(this);
        favoriteWordsDbHelper = new FavoriteWordsDbHelper(this);

        Intent intent = getIntent();
        currentWord = intent.getStringExtra("word");
        long tempWordId = intent.getLongExtra("word_id", -1);
        isLanguageEnglish = intent.getBooleanExtra("searchLangCode", true);

        Log.d(TAG, "Received Intent in onCreate:");
        Log.d(TAG, "  currentWord: '" + currentWord + "'");
        Log.d(TAG, "  tempWordId: " + tempWordId);
        Log.d(TAG, "  isLanguageEnglish: " + isLanguageEnglish);

        if (tempWordId == -1) {
            currentWordId = -1;
            Log.e(TAG, "Word ID invalid or out of integer range for favorites: " + tempWordId);
        } else {
            currentWordId = tempWordId;
        }

        if (currentWord == null || currentWord.isEmpty()) {
            Log.e(TAG, "Critical Error: Word data missing from intent. currentWord is null or empty.");
            finish();
            return;
        }

        StringBuilder shareContentBuilder = new StringBuilder();

        Cursor cursor = databaseHelper.getAllDefinitionsForWord(currentWord, isLanguageEnglish);

        if (cursor != null && cursor.moveToFirst()) {
            // Check if the current language is English to determine the display format
            if (isLanguageEnglish) {
                // --- ENGLISH WORD DISPLAY LOGIC (FULL DETAILS) ---
                String pronunciation = cursor.getString(cursor.getColumnIndexOrThrow("pronunciation"));
                String syllabic = cursor.getString(cursor.getColumnIndexOrThrow("syllabic_pronunciation"));
                String formsDegrees = cursor.getString(cursor.getColumnIndexOrThrow("forms/degrees"));

                // Create the Word Box
                LinearLayout wordBox = new LinearLayout(this);
                wordBox.setOrientation(LinearLayout.VERTICAL);
                wordBox.setBackground(ContextCompat.getDrawable(this, R.drawable.rounded_box));
                wordBox.setPadding(32, 32, 32, 32);
                LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                boxParams.setMargins(0, 0, 0, 20);
                wordBox.setLayoutParams(boxParams);

                LinearLayout horizontalLayout = new LinearLayout(this);
                horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
                horizontalLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                horizontalLayout.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

                LinearLayout leftTextLayout = new LinearLayout(this);
                leftTextLayout.setOrientation(LinearLayout.VERTICAL);
                leftTextLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                TextView wordText = new TextView(this);
                wordText.setText(currentWord.toUpperCase());
                wordText.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentFontSize + 6f);
                wordText.setTypeface(Typeface.DEFAULT_BOLD);
                wordText.setTextColor(Color.parseColor("#333333"));
                wordText.setPadding(0, 0, 0, 8);
                leftTextLayout.addView(wordText);

                StringBuilder pronLine = new StringBuilder();
                if (pronunciation != null && !pronunciation.isEmpty()) {
                    pronLine.append(pronunciation);
                }
                if (syllabic != null && !syllabic.isEmpty()) {
                    if (pronLine.length() > 0) pronLine.append(" | ");
                    pronLine.append(syllabic);
                }

                if (pronLine.length() > 0) {
                    TextView pronText = new TextView(this);
                    pronText.setTextColor(Color.parseColor("#666666"));
                    pronText.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentFontSize);
                    pronText.setText(pronLine.toString());
                    pronText.setPadding(0, 0, 0, 8);
                    leftTextLayout.addView(pronText);
                }

                if (formsDegrees != null && !formsDegrees.isEmpty()) {
                    TextView formsText = new TextView(this);
                    formsText.setText(formsDegrees);
                    formsText.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentFontSize);
                    formsText.setTypeface(null, Typeface.BOLD);
                    formsText.setTextColor(Color.parseColor("#7B1FA2"));
                    leftTextLayout.addView(formsText);
                }

                LinearLayout rightBtnLayout = new LinearLayout(this);
                rightBtnLayout.setOrientation(LinearLayout.VERTICAL);
                rightBtnLayout.setGravity(Gravity.END);
                rightBtnLayout.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

                ImageButton fontSizeIncreaseBtn = new ImageButton(this);
                fontSizeIncreaseBtn.setImageResource(R.drawable.text_increase_24dp);
                fontSizeIncreaseBtn.setBackgroundColor(Color.TRANSPARENT);
                rightBtnLayout.addView(fontSizeIncreaseBtn);

                ImageButton fontSizeDecreaseBtn = new ImageButton(this);
                fontSizeDecreaseBtn.setImageResource(R.drawable.text_decrease_24dp);
                fontSizeDecreaseBtn.setBackgroundColor(Color.TRANSPARENT);
                rightBtnLayout.addView(fontSizeDecreaseBtn);

                fontSizeIncreaseBtn.setOnClickListener(v -> {
                    if (currentFontSize < MAX_FONT_SIZE) {
                        updateFontSize(currentFontSize + 2f);
                    }
                });

                fontSizeDecreaseBtn.setOnClickListener(v -> {
                    if (currentFontSize > MIN_FONT_SIZE) {
                        updateFontSize(currentFontSize - 2f);
                    }
                });

                horizontalLayout.addView(leftTextLayout);
                horizontalLayout.addView(rightBtnLayout);
                wordBox.addView(horizontalLayout);

                LinearLayout mainContentContainer = findViewById(R.id.mainContentContainer);
                mainContentContainer.addView(wordBox, 0);

                shareContentBuilder.append(currentWord.toUpperCase()).append("\n");
                if (pronLine.length() > 0) shareContentBuilder.append(pronLine).append("\n");
                if (formsDegrees != null && !formsDegrees.isEmpty())
                    shareContentBuilder.append("Forms/Degrees: ").append(formsDegrees).append("\n\n");

                do {
                    String pos = cursor.getString(cursor.getColumnIndexOrThrow("pos"));
                    String urduMeaning = cursor.getString(cursor.getColumnIndexOrThrow("urdu_meaning"));
                    String urduDefinition = cursor.getString(cursor.getColumnIndexOrThrow("urdu_definition"));
                    String englishDefinition = cursor.getString(cursor.getColumnIndexOrThrow("english_definition"));
                    String synonym = cursor.getString(cursor.getColumnIndexOrThrow("synonym"));
                    String antonym = cursor.getString(cursor.getColumnIndexOrThrow("antonym"));

                    SpannableStringBuilder section = new SpannableStringBuilder();
                    if (pos != null && !pos.isEmpty()) {
                        section.append(pos).append(":\n");
                        section.setSpan(new StyleSpan(Typeface.BOLD), 0, pos.length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        section.setSpan(new ForegroundColorSpan(Color.parseColor("#005B96")), 0, pos.length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    if (urduMeaning != null && !urduMeaning.isEmpty()) appendMeaningOrDefinition(section, "Meaning", urduMeaning);
                    if (urduDefinition != null && !urduDefinition.isEmpty()) appendMeaningOrDefinition(section, "Definition (Urdu)", urduDefinition);
                    if (englishDefinition != null && !englishDefinition.isEmpty()) appendMeaningOrDefinition(section, "Definition (English)", englishDefinition);
                    if (synonym != null && !synonym.isEmpty()) appendLine(section, "Synonyms", synonym);
                    if (antonym != null && !antonym.isEmpty()) appendLine(section, "Antonyms", antonym);

                    section.append("\n");

                    LinearLayout posBox = new LinearLayout(this);
                    posBox.setOrientation(LinearLayout.VERTICAL);
                    posBox.setBackground(ContextCompat.getDrawable(this, R.drawable.rounded_box));
                    posBox.setPadding(32, 32, 32, 32);
                    LinearLayout.LayoutParams posParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    posParams.setMargins(0, 0, 0, 32);
                    posBox.setLayoutParams(posParams);

                    TextView sectionView = new TextView(this);
                    sectionView.setText(section);
                    sectionView.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentFontSize);
                    sectionView.setTextColor(Color.BLACK);

                    posBox.addView(sectionView);
                    definitionDetailsContainer.addView(posBox);
                    shareContentBuilder.append(section.toString());

                } while (cursor.moveToNext());
            } else {
                // --- URDU WORD DISPLAY LOGIC (SIMPLIFIED) ---

                String urduMeaning = cursor.getString(cursor.getColumnIndexOrThrow("urdu_meaning"));
                String englishWord = cursor.getString(cursor.getColumnIndexOrThrow("word"));

                // Create the Word Box with Urdu word as the title
                LinearLayout wordBox = new LinearLayout(this);
                wordBox.setOrientation(LinearLayout.VERTICAL);
                wordBox.setBackground(ContextCompat.getDrawable(this, R.drawable.rounded_box));
                wordBox.setPadding(32, 32, 32, 32);
                LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                boxParams.setMargins(0, 0, 0, 20);
                wordBox.setLayoutParams(boxParams);

                // Use a simple horizontal layout for the word and buttons
                LinearLayout horizontalLayout = new LinearLayout(this);
                horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
                horizontalLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                horizontalLayout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); // Set RTL for Urdu

                // --- Buttons on the left, text on the right for RTL layout ---
                LinearLayout leftBtnLayout = new LinearLayout(this);
                leftBtnLayout.setOrientation(LinearLayout.VERTICAL);
                leftBtnLayout.setGravity(Gravity.START);
                leftBtnLayout.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

                ImageButton fontSizeIncreaseBtn = new ImageButton(this);
                fontSizeIncreaseBtn.setImageResource(R.drawable.text_increase_24dp);
                fontSizeIncreaseBtn.setBackgroundColor(Color.TRANSPARENT);
                leftBtnLayout.addView(fontSizeIncreaseBtn);

                ImageButton fontSizeDecreaseBtn = new ImageButton(this);
                fontSizeDecreaseBtn.setImageResource(R.drawable.text_decrease_24dp);
                fontSizeDecreaseBtn.setBackgroundColor(Color.TRANSPARENT);
                leftBtnLayout.addView(fontSizeDecreaseBtn);

                fontSizeIncreaseBtn.setOnClickListener(v -> {
                    if (currentFontSize < MAX_FONT_SIZE) {
                        updateFontSize(currentFontSize + 2f);
                    }
                });

                fontSizeDecreaseBtn.setOnClickListener(v -> {
                    if (currentFontSize > MIN_FONT_SIZE) {
                        updateFontSize(currentFontSize - 2f);
                    }
                });

                LinearLayout rightTextLayout = new LinearLayout(this);
                rightTextLayout.setOrientation(LinearLayout.VERTICAL);
                rightTextLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                rightTextLayout.setGravity(Gravity.END); // Align text to the right for Urdu

                TextView wordText = new TextView(this);
                wordText.setText(urduMeaning);
                wordText.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentFontSize + 6f);
                wordText.setTypeface(Typeface.DEFAULT_BOLD);
                wordText.setTextColor(Color.parseColor("#333333"));
                wordText.setPadding(0, 0, 0, 8);
                rightTextLayout.addView(wordText);

                // Add text layout first, then button layout to get the correct order for RTL
                horizontalLayout.addView(rightTextLayout);
                horizontalLayout.addView(leftBtnLayout);

                wordBox.addView(horizontalLayout);

                LinearLayout mainContentContainer = findViewById(R.id.mainContentContainer);
                mainContentContainer.addView(wordBox, 0);

                shareContentBuilder.append(urduMeaning).append("\n\n");

                do {
                    // Fetch all relevant data for each part of speech
                    String pos = cursor.getString(cursor.getColumnIndexOrThrow("pos"));
                    englishWord = cursor.getString(cursor.getColumnIndexOrThrow("word"));
                    String urduDefinition = cursor.getString(cursor.getColumnIndexOrThrow("urdu_definition"));
                    String englishDefinition = cursor.getString(cursor.getColumnIndexOrThrow("english_definition"));

                    SpannableStringBuilder section = new SpannableStringBuilder();
                    if (pos != null && !pos.isEmpty()) {
                        section.append(pos).append(":\n");
                        section.setSpan(new StyleSpan(Typeface.BOLD), 0, pos.length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        section.setSpan(new ForegroundColorSpan(Color.parseColor("#005B96")), 0, pos.length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    // Display the English word as the "meaning"
                    if (englishWord != null && !englishWord.isEmpty()) appendMeaningOrDefinition(section, "Meaning", englishWord);
                    // Add the definitions right after the meaning
                    if (englishDefinition != null && !englishDefinition.isEmpty()) appendMeaningOrDefinition(section, "Definition (English)", englishDefinition);
                    if (urduDefinition != null && !urduDefinition.isEmpty()) appendMeaningOrDefinition(section, "Definition (Urdu)", urduDefinition);


                    section.append("\n");

                    LinearLayout posBox = new LinearLayout(this);
                    posBox.setOrientation(LinearLayout.VERTICAL);
                    posBox.setBackground(ContextCompat.getDrawable(this, R.drawable.rounded_box));
                    posBox.setPadding(32, 32, 32, 32);
                    LinearLayout.LayoutParams posParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    posParams.setMargins(0, 0, 0, 32);
                    posBox.setLayoutParams(posParams);

                    TextView sectionView = new TextView(this);
                    sectionView.setText(section);
                    sectionView.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentFontSize);
                    sectionView.setTextColor(Color.BLACK);

                    posBox.addView(sectionView);
                    definitionDetailsContainer.addView(posBox);
                    shareContentBuilder.append(section.toString());

                } while (cursor.moveToNext());
            }

            cursor.close();
        } else {
            TextView notFoundText = new TextView(this);
            notFoundText.setText("No details found for '" + currentWord + "'.");
            notFoundText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
            notFoundText.setTextColor(Color.DKGRAY);
            notFoundText.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 64, 0, 0);
            notFoundText.setLayoutParams(params);
            definitionDetailsContainer.addView(notFoundText);
            Log.w(TAG, "No definitions found for word: " + currentWord);
        }

        final String shareContent = shareContentBuilder.toString();

        ImageButton textToSpeechBtn = findViewById(R.id.textToSpeachBtn);
        textToSpeechBtn.setOnClickListener(view -> speakWord());

        favoriteBtn = findViewById(R.id.favoriteBtn);
        updateFavoriteButtonState();

        if (currentWordId != -1) {
            favoriteBtn.setOnClickListener(v -> toggleFavorite());
        } else {
            favoriteBtn.setEnabled(false);
            favoriteBtn.setColorFilter(Color.LTGRAY);
        }

        ImageButton shareBtn = findViewById(R.id.shareBtn);
        shareBtn.setOnClickListener(view -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

        ImageButton copyBtn = findViewById(R.id.copyBtn);
        copyBtn.setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("content", shareContent);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Copied to clipboard!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to copy: Clipboard service not available.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateFontSize(float newSize) {
        currentFontSize = newSize;
        LinearLayout mainContentContainer = findViewById(R.id.mainContentContainer);
        updateFontSizeRecursive(mainContentContainer);
    }

    private void updateFontSizeRecursive(View view) {
        if (view instanceof TextView) {
            // Check if the TextView's tag indicates it's a fixed-size element, like a button icon
            if (view.getTag() != null && view.getTag().toString().equals("fixed_size")) {
                return;
            }
            ((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_SP, currentFontSize);
        } else if (view instanceof LinearLayout) {
            LinearLayout layout = (LinearLayout) view;
            for (int i = 0; i < layout.getChildCount(); i++) {
                updateFontSizeRecursive(layout.getChildAt(i));
            }
        }
    }

    private void speakWord() {
        // If TTS is already initialized, shut it down to set a new locale
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }

        if (currentWord == null || currentWord.isEmpty()) {
            Toast.makeText(this, "No word to speak.", Toast.LENGTH_SHORT).show();
            return;
        }

        textToSpeech = new TextToSpeech(WordDetailsActivity.this, i -> {
            if (i == TextToSpeech.SUCCESS) {
                Locale locale = isLanguageEnglish ? Locale.US : new Locale("ur", "PK");
                int result = textToSpeech.setLanguage(locale);

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "This feature is either not supported on your device or requires voice data to be installed. Please check your Text-to-Speech (TTS) settings.", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "TTS language not supported or missing data.");
                } else {
                    textToSpeech.speak(currentWord, TextToSpeech.QUEUE_FLUSH, null, null);
                }
            } else {
                Toast.makeText(this, "TTS initialization failed.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "TTS initialization failed with code: " + i);
            }
        });
    }

    private void appendLine(SpannableStringBuilder builder, String label, String value) {
        if (value == null || value.isEmpty()) return;
        int start = builder.length();
        builder.append(label).append(": ");
        builder.setSpan(new StyleSpan(Typeface.BOLD), start, start + label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new ForegroundColorSpan(getColorForLabel(label)), start, start + label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append(value).append("\n");
    }

    private void appendMeaningOrDefinition(SpannableStringBuilder builder, String label, String value) {
        if (value == null || value.isEmpty()) return;
        int startLabel = builder.length();
        builder.append(label).append("\n");
        builder.setSpan(new StyleSpan(Typeface.BOLD), startLabel, startLabel + label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new ForegroundColorSpan(getColorForLabel(label)), startLabel, startLabel + label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        int startValue = builder.length();
        builder.append(value).append("\n\n");
        builder.setSpan(new StyleSpan(Typeface.NORMAL), startValue, startValue + value.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new ForegroundColorSpan(Color.parseColor("#222222")), startValue, startValue + value.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private int getColorForLabel(String label) {
        switch (label.toLowerCase()) {
            case "meaning":
                return Color.parseColor("#388E3C");
            case "definition (urdu)":
                return Color.parseColor("#1976D2");
            case "definition (english)":
                return Color.parseColor("#9C27B0");
            case "synonyms":
                return Color.parseColor("#F57C00");
            case "antonyms":
                return Color.parseColor("#D32F2F");
            case "forms/degrees":
                return Color.parseColor("#7B1FA2");
            default:
                return Color.parseColor("#444444");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            String caller = getIntent().getStringExtra("caller");
            Intent intent;
            if ("MainActivity".equals(caller)) {
                intent = new Intent(this, MainActivity.class);
            } else if ("FavoriteWordsActivity".equals(caller)) {
                intent = new Intent(this, FavoriteWordsActivity.class);
            } else if ("HistoryActivity".equals(caller)) {
                intent = new Intent(this, HistoryActivity.class);
            } else {
                onBackPressed();
                return true;
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateFavoriteButtonState() {
        if (currentWord != null && favoriteWordsDbHelper.isFavorite(currentWord)) {
            favoriteBtn.setColorFilter(Color.RED);
        } else {
            favoriteBtn.setColorFilter(Color.BLACK);
        }
    }

    private void toggleFavorite() {
        if (currentWord == null || currentWordId == -1) {
            Toast.makeText(this, "Cannot favorite. Word data is missing or invalid.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (favoriteWordsDbHelper.isFavorite(currentWord)) {
            int rowsAffected = favoriteWordsDbHelper.deleteFavoriteWord(currentWord);
            if (rowsAffected > 0) {
                Toast.makeText(this, "'" + currentWord + "' removed from favorites.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to remove " + currentWord + " from favorites.", Toast.LENGTH_SHORT).show();
            }
        } else {
            // The `FavoriteWord` constructor requires four arguments now.
            // Pass `isLanguageEnglish` and the current timestamp.
            long newRowId = favoriteWordsDbHelper.addFavoriteWord(new FavoriteWord(currentWord, currentWordId, isLanguageEnglish, System.currentTimeMillis()));
            if (newRowId != -1) {
                Toast.makeText(this, "'" + currentWord + "' added to favorites!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to add " + currentWord + " to favorites (might already exist).", Toast.LENGTH_SHORT).show();
            }
        }
        updateFavoriteButtonState();
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (databaseHelper != null) {
            databaseHelper.closeDatabase();
        }
        super.onDestroy();
    }
}
