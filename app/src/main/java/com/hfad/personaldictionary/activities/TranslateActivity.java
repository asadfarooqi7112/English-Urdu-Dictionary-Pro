package com.hfad.personaldictionary.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;

import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.hfad.personaldictionary.utils.ContentAction;
import com.hfad.personaldictionary.R;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class TranslateActivity extends AppCompatActivity {
    private EditText inputText;
    private TextView outputText, inputLanguage, outputLanguage;
    private ImageButton clearAllText;
    ImageView inputLanguageLogo, outputLanguageLogo;
    private Translator translator;
    private boolean isInputEnglish = true;
    TextToSpeech textToSpeech;
    TextRecognizer recognizer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_translate);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText(getResources().getString(R.string.translate));

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.arrow_back_24);

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.black)); // or your desired color

        inputText = findViewById(R.id.inputText);
        inputText.setMovementMethod(new ScrollingMovementMethod()); //To enable scroll
        inputText.setTextIsSelectable(true); // Enables select all on long press

        outputText = findViewById(R.id.outputText);
        outputText.setMovementMethod(new ScrollingMovementMethod());
        Button translateButton = findViewById(R.id.translationBtn);

        inputLanguage = findViewById(R.id.inputLanguage);
        inputLanguageLogo = findViewById(R.id.inputLanguageLogo);
        outputLanguage = findViewById(R.id.outputLanguage);
        outputLanguageLogo = findViewById(R.id.outputLanguageLogo);

        clearAllText = findViewById(R.id.clearAllText);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) clearAllText.getLayoutParams();
        clearAllText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inputText.setText("");
            }
        });

        ImageButton swapBtn = findViewById(R.id.swapBtn);
        swapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isInputEnglish){
                    inputLanguage.setText(getResources().getString(R.string.urdu));
                    inputLanguageLogo.setImageResource(R.drawable.pakistan_flag_round_circle_icon);
                    outputLanguage.setText(getResources().getString(R.string.english));
                    outputLanguageLogo.setImageResource(R.drawable.uk_flag_round_circle_icon);

                    inputText.setText("");
                    outputText.setText("");

                    inputText.setTextDirection(View.TEXT_DIRECTION_RTL);
                    inputText.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                    inputText.setHint("یہاں لکھیں...");
                    params.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE);
                    params.addRule(RelativeLayout.ALIGN_PARENT_END, 0);

                }else{
                    inputLanguage.setText(getResources().getString(R.string.english));
                    inputLanguageLogo.setImageResource(R.drawable.uk_flag_round_circle_icon);
                    outputLanguage.setText(getResources().getString(R.string.urdu));
                    outputLanguageLogo.setImageResource(R.drawable.pakistan_flag_round_circle_icon);

                    inputText.setText("");
                    outputText.setText("");

                    inputText.setTextDirection(View.TEXT_DIRECTION_LTR);
                    inputText.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                    inputText.setHint("Type here...");
                    params.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
                    params.addRule(RelativeLayout.ALIGN_PARENT_START, 0); // remove conflicting rule if any
                }
                isInputEnglish = !isInputEnglish;
            }
        });

        inputText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() > 0) {
                    clearAllText.setVisibility(View.VISIBLE);
                } else {
                    clearAllText.setVisibility(View.GONE);
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });

        // Handle button click
        translateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String input = inputText.getText().toString();
                if(isInputEnglish){
                    translate(input, "en", "ur");
                }else {
                    translate(input, "ur", "en");
                }
            }
        });

        //Code for text to speech for output
        ImageButton urduTextToSpeechBtn = findViewById(R.id.textToSpeachBtn);
        urduTextToSpeechBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textToSpeech = new TextToSpeech(TranslateActivity.this, new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int i) {
                        if(i == TextToSpeech.SUCCESS && !outputText.getText().toString().trim().isEmpty()){
                            Locale locale = isInputEnglish ?  new Locale("ur", "PK"):Locale.US;
                            int result = textToSpeech.setLanguage(locale);

                            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                Toast.makeText(TranslateActivity.this,
                                        "This feature is either not supported on your device or requires voice data to be installed. Please check your Text-to-Speech (TTS) settings.",
                                        Toast.LENGTH_LONG).show();
                            }else {
                                // Only speak when TTS is ready and language is supported
                                textToSpeech.speak(outputText.getText().toString(), TextToSpeech.QUEUE_FLUSH, null, null);
                            }
                        }else{
                            Toast.makeText(TranslateActivity.this, "No text found. Please provide some text.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        //Code for copying output text to clipboard
        ImageButton copyUrduBtn = findViewById(R.id.copyUrduBtn);
        copyUrduBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(outputText.getText().toString().trim().isEmpty()){
                    Toast.makeText(TranslateActivity.this, "No text found. Please provide some text.", Toast.LENGTH_SHORT).show();
                }else{
                    ContentAction.copyToClipboard(TranslateActivity.this,outputText.getText().toString());
                }
            }
        });

        //Code for sharing output text
        ImageButton shareUrduBtn = findViewById(R.id.shareUrduBtn);
        shareUrduBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(outputText.getText().toString().trim().isEmpty()){
                    Toast.makeText(TranslateActivity.this, "No text found. Please provide some text.", Toast.LENGTH_SHORT).show();
                }else{
                    ContentAction.shareText(TranslateActivity.this,outputText.getText().toString());
                }
            }
        });
        //Code for speech to text
        ImageButton speechToTextBtn = findViewById(R.id.speechToTextBtn);
        speechToTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ContentAction.startSpeechToText(TranslateActivity.this, isInputEnglish);
            }
        });
        //Code for copying input text to clipboard
        ImageButton copyEnglishBtn = findViewById(R.id.copyEnglishBtn);
        copyEnglishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(inputText.getText().toString().trim().isEmpty()){
                    Toast.makeText(TranslateActivity.this, "No text found. Please provide some text.", Toast.LENGTH_SHORT).show();
                }else{
                    ContentAction.copyToClipboard(TranslateActivity.this, inputText.getText().toString());
                }
            }
        });
        //Code for image to text (OCR)
        ImageButton imageToTextBtn = findViewById(R.id.imageToTextBtn);
        imageToTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isInputEnglish){
                    ContentAction.showDialog(TranslateActivity.this);
                }else {
                    Toast.makeText(TranslateActivity.this, "Text recognition from images in Urdu is not yet supported.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        Intent intent = getIntent();
        String intentFrom = intent.getStringExtra("intent_from_main_activity");
        if ("main_activity".equals(intentFrom)) {
            String recievedText = intent.getStringExtra("extracted_text");
            if(recievedText != null && recievedText.isEmpty()){
                Toast.makeText(this, "No text extracted", Toast.LENGTH_SHORT).show();
            }
            inputText.setText(intent.getStringExtra("extracted_text"));
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode== ContentAction.SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data !=null){
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String spokenText = result.get(0);
            inputText.setText(spokenText);
            if(isInputEnglish){
                translate(spokenText, "en", "ur");
            }else {
                translate(spokenText, "ur", "en");
            }
        }
        if (requestCode == ContentAction.PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();

            UCrop.Options options = new UCrop.Options();
            options.setCircleDimmedLayer(false); // Disable circular crop area
            options.setShowCropFrame(true); // Show crop frame (rectangular boundary)
            options.setShowCropGrid(false); // Do not show the crop grid (no grid lines)
            options.setFreeStyleCropEnabled(true); // Allow free cropping without fixed aspect ratio

            // Use UCrop to crop the image
            Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "cropped_image.jpg"));
            UCrop.of(imageUri, destinationUri)
                    .withAspectRatio(1, 1)
                    .withOptions(options)
                    .start(this);
        }
        // Handle the result from uCrop
        if (requestCode == UCrop.REQUEST_CROP  && resultCode == RESULT_OK) {
            Uri croppedUri = UCrop.getOutput(data); // Get the cropped image URI

            // Use the cropped image for OCR
            try {
                InputImage image = InputImage.fromFilePath(this, croppedUri);

                recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

                recognizer.process(image)
                        .addOnSuccessListener(result -> {
                            String recognizedText = result.getText();
                            if (recognizedText.trim().isEmpty()) {
                                Toast.makeText(TranslateActivity.this, "No text extracted", Toast.LENGTH_SHORT).show();
                            }
                            ((TextView) findViewById(R.id.inputText)).setText(recognizedText);
                            translate(recognizedText, "en", "ur");
                        })
                        .addOnFailureListener(e -> {
                            e.printStackTrace();
                            Toast.makeText(this, "Failed to read text from image", Toast.LENGTH_SHORT).show();
                        });

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Image loading error", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == ContentAction.REQUEST_CODE_OCR && resultCode == RESULT_OK && data != null) {
            String ocrText = data.getStringExtra("OCR_RESULT");
            inputText.setText(ocrText);
            translate(ocrText, "en", "ur");
        }
    }

    //Code for translating from english to urdu
    private void translate(String text, String sourceLangCode, String targetLangCode) {

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.fromLanguageTag(sourceLangCode))
                .setTargetLanguage(TranslateLanguage.fromLanguageTag(targetLangCode))
                .build();

        translator = Translation.getClient(options);

        // Directly attempt to translate, assuming models are already downloaded
        translator.translate(text)
                .addOnSuccessListener(translatedText -> {
                    // Hide the ProgressBar and set the translated text
                    outputText.setText(translatedText);
                })
                .addOnFailureListener(e -> {
                    // Hide the ProgressBar and show error message
                    outputText.setText(getResources().getString(R.string.translation_failed, e.getMessage()));
                });
    }
    //This function is called after the permission dialog finishes
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 10) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ContentAction.showDialog(TranslateActivity.this);
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CAMERA)) {
                    ContentAction.showDialog(this);
                    new AlertDialog.Builder(this)
                            .setTitle("Camera Permission Required")
                            .setMessage("This app needs camera access to scan text. Please allow it to proceed.")
                            .setPositiveButton("Grant", (dialog, which) -> {
                                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 10);
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> finish())
                            .show();
                } else {
                    // Permission denied permanently
                    AlertDialog alertDialog = new AlertDialog.Builder(this)
                            .setTitle("Permission Denied")
                            .setMessage("Camera permission is permanently denied. You can enable it from app settings.")
                            .setPositiveButton("Open Settings", (dialog, which) -> {
                                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                                finish();
                            })
                            .setNegativeButton("Exit", (dialog, which) -> finish())
                            .show();

                    Window window = alertDialog.getWindow();
                    if (window != null) {
                        window.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#E6E6FA")));
                    }
                }
            }
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (translator != null) {
            translator.close(); // Close the translator to release resources
        }
    }
}