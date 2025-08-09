// src/main/java/com/hfad/personaldictionary/activities/MainActivity.java
package com.hfad.personaldictionary.activities;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.hfad.personaldictionary.utils.ContentAction;
import com.hfad.personaldictionary.database.DatabaseHelper;
import com.hfad.personaldictionary.utils.LocaleHelper;
import com.hfad.personaldictionary.R;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import com.hfad.personaldictionary.database.FavoriteWordsDbHelper;
import com.hfad.personaldictionary.models.FavoriteWord;
import com.hfad.personaldictionary.database.HistoryDatabaseHelper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private EditText searchEditText;
    private ListView searchResult;
    private ImageButton clearSearchButton;
    private RelativeLayout searchBarRelativeLayout;
    private LinearLayout searchLinearLayout;
    SimpleCursorAdapter cursorAdapter;
    private Cursor wordsCursor;
    private DatabaseHelper databaseHelper;
    String wordExtracted;
    int todaysWordId;
    TextToSpeech textToSpeech;
    private static final int REQUEST_IMAGE_CAPTURE = 4;
    private Uri photoUri;
    private CharSequence displayedWordMeaning;

    private static final float WORD_OF_DAY_FONT_SIZE_SP = 18f;

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1002;
    private static final String TAG = "MainActivity";
    private boolean isInputLanguageEnglish = true;

    private RadioGroup languageRadioGroup;
    private RadioButton radioEnglish;
    private RadioButton radioUrdu;
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String KEY_IS_ENGLISH_LANGUAGE = "isEnglishLanguage";

    private ImageButton favoriteBtn;
    private FavoriteWordsDbHelper favoriteWordsDbHelper;

    private HistoryDatabaseHelper historyDbHelper;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleHelper.setLocale(this, LocaleHelper.getLanguage(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,
                drawerLayout,
                toolbar,
                R.string.nav_open_drawer,
                R.string.nav_close_drawer);
        toggle.getDrawerArrowDrawable().setColor(Color.BLACK);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if(id==R.id.nav_chooseLanguage){
                Intent intent = new Intent(MainActivity.this, LanguageActivity.class);
                startActivity(intent);
            }else if(id==R.id.nav_translate) {
                Intent intent = new Intent(MainActivity.this, TranslateActivity.class);
                startActivity(intent);
            } else if (id==R.id.nav_ocr) {
                checkCameraPermissionAndShowDialog();
            } else if (id==R.id.nav_object_recognition) {
                Intent intent = new Intent(MainActivity.this, ObjectRecognition.class);
                startActivity(intent);
            }else if(id==R.id.nav_quiz_options){
                Intent intent = new Intent(MainActivity.this, QuizOptionsActivity.class);
                startActivity(intent);
            }else if(id==R.id.nav_favorite){
                Intent intent = new Intent(MainActivity.this, FavoriteWordsActivity.class);
                startActivity(intent);
            }else if(id==R.id.nav_history){
                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                startActivity(intent);
            } else if (id==R.id.nav_exit) {
                finish();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        searchResult = findViewById(R.id.searchResult);
        searchEditText = findViewById(R.id.searchEditText);
        searchBarRelativeLayout = findViewById(R.id.searchBarRelativeLayout);
        searchLinearLayout = findViewById(R.id.searchLinearLayout);

        languageRadioGroup = findViewById(R.id.languageRadioGroup);
        radioEnglish = findViewById(R.id.radioEnglish);
        radioUrdu = findViewById(R.id.radioUrdu);

        historyDbHelper = new HistoryDatabaseHelper(this);

        loadLanguagePreference();

        languageRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioEnglish) {
                isInputLanguageEnglish = true;
                saveLanguagePreference(true);
                searchEditText.setText("");
                searchBarRelativeLayout.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                searchLinearLayout.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                searchResult.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                searchEditText.setTextDirection(View.TEXT_DIRECTION_LTR);
                searchEditText.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                searchEditText.setHint("Type Here...");
            } else if (checkedId == R.id.radioUrdu) {
                isInputLanguageEnglish = false;
                saveLanguagePreference(false);
                searchEditText.setText("");
                searchBarRelativeLayout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                searchLinearLayout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                searchResult.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                searchEditText.setTextDirection(View.TEXT_DIRECTION_RTL);
                searchEditText.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                searchEditText.setHint("یہاں لکھیں…");
            }
        });

        databaseHelper = new DatabaseHelper(this);
        favoriteWordsDbHelper = new FavoriteWordsDbHelper(this);
        try {
            databaseHelper.copyDatabaseIfNeeded();
        } catch (IOException e) {
            Log.e("MainActivity", "Failed to copy database", e);
        }

        SharedPreferences sharedPreferences = getSharedPreferences("prevDateCache",MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        favoriteBtn = findViewById(R.id.favoriteBtn);
        TextView todaysWordTextView = findViewById(R.id.todaysWord);

        String prevDate = sharedPreferences.getString("prevDate", null);
        String dateString;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDate currentDate =  LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            dateString = currentDate.format(formatter);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            dateString = sdf.format(java.util.Calendar.getInstance().getTime());
        }

        if(!dateString.equals(prevDate)){
            Log.d("MainActivity", "New day, fetching new word from database.");
            Map<String, Object> todaysWordData = databaseHelper.getTodaysWord();

            if (todaysWordData == null || todaysWordData.isEmpty()) {
                Log.e("MainActivity", "getTodaysWord returned an empty or null map. Cannot display word.");
                displayedWordMeaning = new SpannableStringBuilder("Error: Could not load word.");
                wordExtracted = "";
                todaysWordId = -1;
            } else {
                Object wordIdObj = todaysWordData.get("word_id");
                if (wordIdObj instanceof Long) {
                    todaysWordId = ((Long) wordIdObj).intValue();
                } else if (wordIdObj instanceof Integer) {
                    todaysWordId = (int) wordIdObj;
                } else {
                    todaysWordId = -1;
                }

                displayedWordMeaning = (SpannableStringBuilder) todaysWordData.get("wordString");
                wordExtracted = (String) todaysWordData.get("plainWord");

                if (displayedWordMeaning == null) {
                    displayedWordMeaning = new SpannableStringBuilder("Error: Data missing.");
                }
                if (wordExtracted == null) {
                    wordExtracted = "";
                }

                editor.putString("prevDate", dateString);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    editor.putString("wordString", Html.toHtml((Spanned) displayedWordMeaning, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE));
                } else {
                    editor.putString("wordString", Html.toHtml((Spanned) displayedWordMeaning));
                }
                editor.putString("plainWord", wordExtracted);
                editor.putInt("wordEnglishId", todaysWordId);
                editor.apply();

                Log.d("MainActivity", "Saved new word to SharedPreferences.");
            }
        } else {
            Log.d("MainActivity", "Same day, retrieving word from SharedPreferences.");
            String storedHtmlWordString = sharedPreferences.getString("wordString", null);
            String storedPlainWord = sharedPreferences.getString("plainWord", null);
            todaysWordId = sharedPreferences.getInt("wordEnglishId", -1);

            if (storedHtmlWordString != null && storedPlainWord != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    displayedWordMeaning = Html.fromHtml(storedHtmlWordString, Html.FROM_HTML_MODE_LEGACY);
                } else {
                    displayedWordMeaning = Html.fromHtml(storedHtmlWordString);
                }
                wordExtracted = storedPlainWord;
            } else {
                Log.e("MainActivity", "SharedPreferences data is null on same day. Re-fetching word from database.");
                Map<String, Object> todaysWordData = databaseHelper.getTodaysWord();

                if (todaysWordData == null || todaysWordData.isEmpty()) {
                    Log.e("MainActivity", "Fallback fetch failed. Returning to a safe state.");
                    displayedWordMeaning = new SpannableStringBuilder("Error: Could not load word.");
                    wordExtracted = "";
                    todaysWordId = -1;
                } else {
                    Object wordIdObj = todaysWordData.get("word_id");
                    if (wordIdObj instanceof Long) {
                        todaysWordId = ((Long) wordIdObj).intValue();
                    } else if (wordIdObj instanceof Integer) {
                        todaysWordId = (int) wordIdObj;
                    } else {
                        todaysWordId = -1;
                    }

                    displayedWordMeaning = (SpannableStringBuilder) todaysWordData.get("wordString");
                    wordExtracted = (String) todaysWordData.get("plainWord");

                    if (displayedWordMeaning == null) {
                        displayedWordMeaning = new SpannableStringBuilder("Error: Data missing.");
                    }
                    if (wordExtracted == null) {
                        wordExtracted = "";
                    }

                    editor.putString("prevDate", dateString);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        editor.putString("wordString", Html.toHtml((Spanned) displayedWordMeaning, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE));
                    } else {
                        editor.putString("wordString", Html.toHtml((Spanned) displayedWordMeaning));
                    }
                    editor.putString("plainWord", wordExtracted);
                    editor.putInt("wordEnglishId", todaysWordId);
                    editor.apply();
                }
            }
        }
        todaysWordTextView.setText(displayedWordMeaning);
        todaysWordTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, WORD_OF_DAY_FONT_SIZE_SP);

        searchResult.setOnItemClickListener((adapterView, view, position, id) -> {
            Cursor clickedCursor = (Cursor) cursorAdapter.getItem(position);
            String word = clickedCursor.getString(clickedCursor.getColumnIndexOrThrow("word"));
            long wordId = clickedCursor.getLong(clickedCursor.getColumnIndexOrThrow("_id"));

            // MODIFIED: Add both the word and the correct wordId to history
            executorService.execute(() -> {
                historyDbHelper.addHistoryEntry(word, wordId);
            });

            Intent intent = new Intent(MainActivity.this, WordDetailsActivity.class);
            intent.putExtra("word", word);
            intent.putExtra("word_id", wordId);
            intent.putExtra("caller", "MainActivity");

            Log.d("MainActivity", "Launching WordDetailsActivity with:");
            Log.d("MainActivity", "  word: '" + word + "'");
            Log.d("MainActivity", "  wordId: " + wordId);
            Log.d("MainActivity", "  caller: " + "MainActivity");
            startActivity(intent);
        });

        clearSearchButton = findViewById(R.id.clearSearchButton);
        clearSearchButton.setOnClickListener(view -> searchEditText.setText(""));

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String newText = s.toString();
                if (newText.isEmpty()) {
                    clearSearchButton.setVisibility(View.GONE);
                    searchResult.setVisibility(View.GONE);
                    if (cursorAdapter != null) {
                        cursorAdapter.changeCursor(null);
                    }
                } else {
                    clearSearchButton.setVisibility(View.VISIBLE);
                    searchResult.setVisibility(View.VISIBLE);
                }

                String searchLanguageCode;
                if (isInputLanguageEnglish) {
                    searchLanguageCode = "en";
                } else {
                    searchLanguageCode = "ur";
                }

                Cursor cursor = databaseHelper.readFromDatabase(newText, searchLanguageCode);
                if (cursorAdapter == null) {
                    cursorAdapter = new SimpleCursorAdapter(
                            MainActivity.this,
                            R.layout.search_list_item,
                            cursor,
                            new String[]{"word"},
                            new int[]{R.id.wordText},
                            0
                    );
                    searchResult.setAdapter(cursorAdapter);
                } else {
                    cursorAdapter.changeCursor(cursor);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (wordExtracted == null) {
            Log.w("MainActivity", "wordExtracted was null, attempting to derive from displayedWordMeaning.");
            if (displayedWordMeaning != null) {
                String tempPlain = displayedWordMeaning.toString();
                int firstSpaceIndex = tempPlain.indexOf(" ");
                if (firstSpaceIndex != -1) {
                    wordExtracted = tempPlain.substring(0, firstSpaceIndex).trim();
                } else {
                    wordExtracted = tempPlain.trim();
                }
            } else {
                wordExtracted = "";
            }
        }

        ImageButton textToSpeechBtn = findViewById(R.id.textToSpeachBtn);
        textToSpeech = new TextToSpeech(this, i -> {
            if (i == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.US);
            } else {
                Log.e("TTS", "Initialization failed!");
            }
        });
        textToSpeechBtn.setOnClickListener(view -> {
            if(textToSpeech != null && wordExtracted != null && !wordExtracted.isEmpty()){
                textToSpeech.speak(wordExtracted, TextToSpeech.QUEUE_FLUSH,null,null);
            } else {
                Toast.makeText(MainActivity.this, "No word to speak or TTS not ready.", Toast.LENGTH_SHORT).show();
                if (wordExtracted == null || wordExtracted.isEmpty()) {
                    Log.w("TTS", "wordExtracted is null or empty. Cannot speak.");
                }
            }
        });

        updateFavoriteButtonState();

        favoriteBtn.setOnClickListener(view -> toggleFavorite());
        ImageButton copyBtn = findViewById(R.id.copyBtn);
        copyBtn.setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (displayedWordMeaning != null) {
                ClipData clip = ClipData.newPlainText("content", displayedWordMeaning);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, "Copied to clipboard!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Nothing to copy.", Toast.LENGTH_SHORT).show();
            }
        });

        ImageButton shareBtn = findViewById(R.id.shareBtn);
        shareBtn.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            if (displayedWordMeaning != null) {
                intent.putExtra(Intent.EXTRA_TEXT, displayedWordMeaning);
                startActivity(Intent.createChooser(intent, "Share via"));
            } else {
                Toast.makeText(MainActivity.this, "Nothing to share.", Toast.LENGTH_SHORT).show();
            }
        });

        MaterialCardView translatorBtn = findViewById(R.id.translatorBtn);
        ImageView translatorIcon = translatorBtn.findViewById(R.id.card_icon);
        TextView translatorText = translatorBtn.findViewById(R.id.card_text);
        translatorIcon.setImageResource(R.drawable.translate_24dp);
        translatorText.setText(R.string.translate);
        translatorBtn.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, TranslateActivity.class);
            startActivity(intent);
        });

        MaterialCardView OCRBtn = findViewById(R.id.OCRBtn);
        ImageView OCRIcon = OCRBtn.findViewById(R.id.card_icon);
        TextView OCRText = OCRBtn.findViewById(R.id.card_text);
        OCRIcon.setImageResource(R.drawable.document_scanner_24dp);
        OCRText.setText(R.string.OCR);
        OCRBtn.setOnClickListener(view -> checkCameraPermissionAndShowDialog());

        MaterialCardView favoritesBtn = findViewById(R.id.favoritesBtn);
        ImageView favoritesIcon = favoritesBtn.findViewById(R.id.card_icon);
        TextView favoritesText = favoritesBtn.findViewById(R.id.card_text);
        favoritesIcon.setImageResource(R.drawable.favorite_24dp);
        favoritesText.setText(R.string.favorites);
        favoritesBtn.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, FavoriteWordsActivity.class);
            startActivity(intent);
        });
        MaterialCardView historyBtn = findViewById(R.id.historyBtn);
        ImageView historyIcon = historyBtn.findViewById(R.id.card_icon);
        TextView historyText = historyBtn.findViewById(R.id.card_text);
        historyIcon.setImageResource(R.drawable.history_24dp);
        historyText.setText(R.string.history);
        historyBtn.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });
        MaterialCardView objectRecognitionBtn = findViewById(R.id.objectRecognitionBtn);
        ImageView objectRecognitionIcon = objectRecognitionBtn.findViewById(R.id.card_icon);
        TextView objectRecognitionText = objectRecognitionBtn.findViewById(R.id.card_text);
        objectRecognitionText.setGravity(Gravity.CENTER);
        objectRecognitionIcon.setImageResource(R.drawable.image_search_24dp);
        objectRecognitionText.setText(R.string.object_recognition2);
        objectRecognitionBtn.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, ObjectRecognition.class);
            startActivity(intent);
        });

        MaterialCardView quizzesBtn = findViewById(R.id.quizzesBtn);
        ImageView quizzesIcon = quizzesBtn.findViewById(R.id.card_icon);
        TextView quizzesText = quizzesBtn.findViewById(R.id.card_text);
        quizzesIcon.setImageResource(R.drawable.quiz_24dp);
        quizzesText.setText(R.string.quizzes);
        quizzesBtn.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, QuizOptionsActivity.class);
            startActivity(intent);
        });

        ImageButton speechToTextBtn = findViewById(R.id.speechToTextBtn);
        speechToTextBtn.setOnClickListener(view -> ContentAction.startSpeechToText(MainActivity.this,isInputLanguageEnglish));
    }

    @Override
    public void onBackPressed(){
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        if(drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(GravityCompat.START);
        }else {
            super.onBackPressed();
        }
    }

    private void checkCameraPermissionAndShowDialog() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            showDialog(MainActivity.this);
        }
    }

    public void openCamera(){
        File photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "captured.jpg");
        photoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(this, "No camera app found.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No camera app found to handle ACTION_IMAGE_CAPTURE intent.");
        }
    }

    public void openGallery(){
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(galleryIntent,ContentAction.PICK_IMAGE_REQUEST);
    }

    private void runTextRecognition(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String extractedText = visionText.getText();
                    Intent intent = new Intent(MainActivity.this, TranslateActivity.class);
                    intent.putExtra("extracted_text", extractedText);
                    intent.putExtra("intent_from_main_activity","main_activity");
                    startActivity(intent);
                })
                .addOnFailureListener(
                        e -> {
                            e.printStackTrace();
                            Log.e("OCR_ERROR", "Failed to recognize text", e);
                        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode== ContentAction.SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data !=null){
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String spokenText = "";
            if(result != null && !result.isEmpty()) {
                spokenText = result.get(0);
            }
            searchEditText.setText(spokenText);
        }
        if(requestCode == ContentAction.PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null){
            Uri imageUri = data.getData();

            UCrop.Options options = new UCrop.Options();
            options.setCircleDimmedLayer(false);
            options.setShowCropFrame(true);
            options.setShowCropGrid(false);
            options.setFreeStyleCropEnabled(true);

            Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "cropped_image.jpg"));
            UCrop.of(imageUri, destinationUri)
                    .withOptions(options)
                    .start(this);
        }
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if (photoUri != null) {
                Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "cropped.jpg"));

                UCrop.Options options = new UCrop.Options();
                options.setCircleDimmedLayer(false);
                options.setShowCropFrame(true);
                options.setShowCropGrid(false);
                options.setFreeStyleCropEnabled(true);

                UCrop.of(photoUri, destinationUri)
                        .withOptions(options)
                        .start(this);
            }
        }
        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            Uri croppedUri = UCrop.getOutput(data);

            if (croppedUri != null) {
                try {
                    Bitmap croppedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), croppedUri);
                    runTextRecognition(croppedBitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }else if (resultCode == UCrop.RESULT_ERROR) {
            Throwable cropError = UCrop.getError(data);
            if (cropError != null) {
                cropError.printStackTrace();
                Toast.makeText(this, "Image cropping failed: " + cropError.getMessage(), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Image cropping failed with unknown error.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted.", Toast.LENGTH_SHORT).show();
                showDialog(MainActivity.this);
            } else {
                Toast.makeText(this, "Camera permission is required to use OCR from camera.", Toast.LENGTH_LONG).show();
                Log.w(TAG, "Camera permission denied by user.");
            }
        }
    }

    public void showDialog(Activity activity){
        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_image_selection,null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        Button cameraBtn = dialogView.findViewById(R.id.cameraBtn);
        cameraBtn.setOnClickListener(view -> {
            openCamera();
            dialog.dismiss();
        });
        Button galleryBtn = dialogView.findViewById(R.id.galleryBtn);
        galleryBtn.setOnClickListener(view -> {
            openGallery();
            dialog.dismiss();
        });
        dialog.show();
    }
    @Override
    protected void attachBaseContext(Context newBase) {
        String lang = LocaleHelper.getLanguage(newBase);
        Context context = LocaleHelper.setLocale(newBase, lang);
        super.attachBaseContext(context);
    }

    private void saveLanguagePreference(boolean isEnglish) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(KEY_IS_ENGLISH_LANGUAGE, isEnglish);
        editor.apply();
    }

    private void loadLanguagePreference() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        isInputLanguageEnglish = settings.getBoolean(KEY_IS_ENGLISH_LANGUAGE, true);

        if (isInputLanguageEnglish) {
            radioEnglish.setChecked(true);
            searchBarRelativeLayout.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            searchLinearLayout.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            searchResult.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            searchEditText.setTextDirection(View.TEXT_DIRECTION_LTR);
            searchEditText.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            searchEditText.setHint("Type Here...");
        } else {
            radioUrdu.setChecked(true);
            searchBarRelativeLayout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            searchLinearLayout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            searchResult.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            searchEditText.setTextDirection(View.TEXT_DIRECTION_RTL);
            searchEditText.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            searchEditText.setHint("یہاں لکھیں…");
        }
    }

    private void toggleFavorite() {
        if (wordExtracted == null || wordExtracted.isEmpty() || todaysWordId == -1) {
            Toast.makeText(this, "Cannot favorite. No word selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (favoriteWordsDbHelper.isFavorite(wordExtracted)) {
            int rowsAffected = favoriteWordsDbHelper.deleteFavoriteWord(wordExtracted);
            if (rowsAffected > 0) {
                Toast.makeText(this, wordExtracted + " removed from favorites.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to remove " + wordExtracted + " from favorites.", Toast.LENGTH_SHORT).show();
            }
        } else {
            long newRowId = favoriteWordsDbHelper.addFavoriteWord(new FavoriteWord(wordExtracted, todaysWordId));
            if (newRowId != -1) {
                Toast.makeText(this, wordExtracted + " added to favorites!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to add " + wordExtracted + " to favorites (might already exist).", Toast.LENGTH_SHORT).show();
            }
        }
        updateFavoriteButtonState();
    }

    private void updateFavoriteButtonState() {
        if (wordExtracted != null && favoriteWordsDbHelper.isFavorite(wordExtracted)) {
            favoriteBtn.setColorFilter(Color.RED);
        } else {
            favoriteBtn.setColorFilter(Color.BLACK);
        }
    }

    @Override
    protected void onDestroy() {
        if (wordsCursor != null && !wordsCursor.isClosed()) {
            wordsCursor.close();
        }
        databaseHelper.closeDatabase();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
        executorService.shutdown();
        historyDbHelper.close();
    }
}