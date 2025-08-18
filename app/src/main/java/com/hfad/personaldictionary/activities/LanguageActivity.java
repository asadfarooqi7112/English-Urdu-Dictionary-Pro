package com.hfad.personaldictionary.activities;

import android.content.Intent;
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
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.arrow_back_24);
        }

        Window window = getWindow();
        if (window != null) {
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        }

        ImageButton confirmSelectionBtn = findViewById(R.id.confirmSelectionBtn);

        LinearLayout englishOption = findViewById(R.id.englishOption);
        LinearLayout urduOption = findViewById(R.id.urduOption);
        RadioButton radioEnglish = findViewById(R.id.radioEnglish);
        RadioButton radioUrdu = findViewById(R.id.radioUrdu);

        // **UPDATED**: Check the current locale directly from AppCompatDelegate
        String currentLanguage = getLocaleLanguage();

        if ("en".equals(currentLanguage)) {
            radioEnglish.setChecked(true);
        } else {
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

        confirmSelectionBtn.setOnClickListener(view -> {
            String selectedLanguage = radioEnglish.isChecked() ? "en" : "ur";

            String currentLanguageCheck = getLocaleLanguage();

            if (selectedLanguage.equals(currentLanguageCheck)) {
                // No change, just close the activity
                finish();
            } else {
                // Set the new locale using AppCompatDelegate, which is the recommended method
                LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(selectedLanguage);
                AppCompatDelegate.setApplicationLocales(appLocale);
                finish();
            }
        });
    }

    private String getLocaleLanguage() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (locales.isEmpty()) {
            // Fallback to the system locale if no app locale is set
            return LocaleListCompat.getAdjustedDefault().get(0).getLanguage();
        }
        return locales.get(0).getLanguage();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}