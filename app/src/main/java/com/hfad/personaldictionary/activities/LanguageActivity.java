package com.hfad.personaldictionary.activities;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.hfad.personaldictionary.utils.LocaleHelper;
import com.hfad.personaldictionary.R;

import java.util.Objects;

public class LanguageActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_language);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText(getResources().getString(R.string.change_app_language));

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.arrow_back_24);

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));

        ImageButton confirmSelectionBtn = findViewById(R.id.confirmSelectionBtn);

        LinearLayout englishOption = findViewById(R.id.englishOption);
        LinearLayout urduOption = findViewById(R.id.urduOption);
        RadioButton radioEnglish = findViewById(R.id.radioEnglish);
        RadioButton radioUrdu = findViewById(R.id.radioUrdu);

        if(LocaleHelper.getLanguage(this).equals("en")){
            radioEnglish.setChecked(true);
        }else{
            radioUrdu.setChecked(true);
        }

        englishOption.setOnClickListener(v -> {
            radioEnglish.setChecked(true);
            radioUrdu.setChecked(false);
        });

        urduOption.setOnClickListener(v -> {
            radioUrdu.setChecked(true);
            radioEnglish.setChecked(false);
        });

        radioEnglish.setOnClickListener(v -> {
            radioEnglish.setChecked(true);
            radioUrdu.setChecked(false);
        });

        radioUrdu.setOnClickListener(v -> {
            radioUrdu.setChecked(true);
            radioEnglish.setChecked(false);
        });

        confirmSelectionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String selectedLanguage = radioEnglish.isChecked() ? "en" : "ur";
                if(selectedLanguage.equals(LocaleHelper.getLanguage(LanguageActivity.this))){
                    finish();
                }else{
                    LocaleHelper.setLocale(LanguageActivity.this, selectedLanguage);
                    Toast.makeText(LanguageActivity.this, "Please restart the app", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }
}