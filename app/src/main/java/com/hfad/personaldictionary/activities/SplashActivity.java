package com.hfad.personaldictionary.activities;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter; // Import for IntentFilter
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.hfad.personaldictionary.R;
import com.hfad.personaldictionary.utils.NetworkChangeReceiver; // Import your new receiver

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SplashActivity extends AppCompatActivity implements NetworkChangeReceiver.NetworkChangeListener {

    private static final String TAG = "SplashActivity";
    private static final long SPLASH_DISPLAY_LENGTH = 500;

    ProgressBar progressBar;
    TextView loadingText;

    private final AtomicInteger modelsLoadedSuccessfullyCount = new AtomicInteger(0);
    private final AtomicInteger modelCheckOrDownloadAttempts = new AtomicInteger(0);
    private static final int TOTAL_MODELS = 2;

    private Translator translator;
    private TextRecognizer textRecognizer;

    private AtomicBoolean modelsInitializedAndReady = new AtomicBoolean(false);

    private Snackbar currentSnackbar;
    private boolean isNetworkSnackbarShowing = false;

    // Declare the NetworkChangeReceiver
    private NetworkChangeReceiver networkChangeReceiver;

    // Flag to indicate if model downloads are currently active
    private AtomicBoolean isDownloadInProgress = new AtomicBoolean(false);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        progressBar = findViewById(R.id.progressBar);
        loadingText = findViewById(R.id.loadingText);

        // Initialize and register the NetworkChangeReceiver
        networkChangeReceiver = new NetworkChangeReceiver(this); // 'this' refers to the SplashActivity as it implements the listener interface
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, intentFilter);

        initializeModels();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Closing ML Kit resources and unregistering receiver.");
        if (translator != null) {
            translator.close();
        }
        if (textRecognizer != null) {
            textRecognizer.close();
        }
        if (currentSnackbar != null && currentSnackbar.isShown()) {
            currentSnackbar.dismiss();
            isNetworkSnackbarShowing = false;
        }

        // Unregister the receiver to prevent memory leaks
        unregisterReceiver(networkChangeReceiver);
    }


    // Implement the NetworkChangeListener interface method
    @Override
    public void onNetworkAvailabilityChanged(boolean isAvailable) {
        // If a download is in progress and network becomes unavailable, show the message
        if (isDownloadInProgress.get() && !isAvailable) {
            Log.d(TAG, "Network lost during download. Showing error.");
            runOnUiThread(() -> showSnackbarWithRetry(getString(R.string.internet_required_retry), true));
        } else if (isAvailable && currentSnackbar != null && currentSnackbar.isShown() && isNetworkSnackbarShowing) {
            // If network becomes available while a network error snackbar is shown,
            // we could potentially dismiss it automatically or keep it to let the user retry.
            // For now, let's keep it to allow user to explicitly retry, but you could
            // add currentSnackbar.dismiss() here if you want it to go away automatically.
            Log.d(TAG, "Network reconnected. User can now retry if desired.");
        }
    }


    private void initializeModels() {
        progressBar.setVisibility(View.VISIBLE);
        loadingText.setVisibility(View.VISIBLE);

        modelsLoadedSuccessfullyCount.set(0);
        modelCheckOrDownloadAttempts.set(0);
        modelsInitializedAndReady.set(false);

        // This is the key change. We no longer set isDownloadInProgress here.
        // It will be set only if a download is actually needed.
        isDownloadInProgress.set(false);

        // Dismiss any existing snackbar when starting a new initialization
        if (currentSnackbar != null && currentSnackbar.isShown()) {
            currentSnackbar.dismiss();
        }
        isNetworkSnackbarShowing = false; // Reset the flag

        checkAndInitializeTranslationModel();
        checkAndInitializeOCRModel();
    }

    private void checkAndInitializeTranslationModel() {
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.URDU)
                .build();
        translator = Translation.getClient(options);

        RemoteModelManager modelManager = RemoteModelManager.getInstance();
        TranslateRemoteModel englishUrduModel = new TranslateRemoteModel.Builder(TranslateLanguage.URDU).build();

        modelManager.isModelDownloaded(englishUrduModel)
                .addOnSuccessListener(isDownloaded -> {
                    if (isDownloaded) {
                        Log.d(TAG, "Translation model is already downloaded.");
                        modelReady(true, null);
                    } else {
                        Log.d(TAG, "Translation model not downloaded. Attempting download.");
                        attemptDownloadTranslationModel(options);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check translation model download status: " + e.getMessage());
                    attemptDownloadTranslationModel(options);
                });
    }

    private void attemptDownloadTranslationModel(TranslatorOptions options) {
        // Here's where we set the flag, because we are now actively trying to download.
        isDownloadInProgress.set(true);

        // Only proceed with actual download if network is available.
        // The network receiver will catch changes mid-download.
        if (!isNetworkAvailable()) {
            handleModelDownloadFailure(new MlKitException("No network for translation model download.", 13 /* NETWORK_ISSUE */));
            modelReady(false, null);
            return;
        }

        DownloadConditions conditions = new DownloadConditions.Builder().build();

        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Translation model downloaded successfully.");
                    modelReady(true, null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Translation model download failed: " + e.getMessage());
                    handleModelDownloadFailure(e);
                    modelReady(false, e);
                });
    }

    private void checkAndInitializeOCRModel() {
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        Bitmap dummyBitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        InputImage dummyImage = InputImage.fromBitmap(dummyBitmap, 0);

        textRecognizer.process(dummyImage)
                .addOnSuccessListener(text -> {
                    Log.d(TAG, "OCR model is ready (processed dummy image successfully).");
                    modelReady(true, null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "OCR model preload failed: " + e.getMessage());

                    // We can also set the flag here, as a download may be implicitly needed.
                    isDownloadInProgress.set(true);

                    if (e instanceof MlKitException) {
                        int errorCode = ((MlKitException) e).getErrorCode();
                        // 13 is NETWORK_ISSUE, 14 is MODEL_NOT_DOWNLOADED, 101 is NOT_ENOUGH_SPACE
                        if (errorCode == 13 || errorCode == 14 || errorCode == MlKitException.NOT_ENOUGH_SPACE) {
                            // Re-check network here for explicit messaging
                            if (!isNetworkAvailable()) {
                                handleModelDownloadFailure(new MlKitException("No network for OCR model download.", 13 /* NETWORK_ISSUE */));
                            } else if (errorCode == MlKitException.NOT_ENOUGH_SPACE) {
                                handleModelDownloadFailure(new MlKitException("Not enough storage for OCR model download.", MlKitException.NOT_ENOUGH_SPACE));
                            }
                            else {
                                handleModelDownloadFailure(e); // General failure for other ML Kit issues
                            }
                        } else {
                            handleModelDownloadFailure(e);
                        }
                    } else if (!isNetworkAvailable()) { // Fallback check for network if not MlKitException
                        handleModelDownloadFailure(new MlKitException("No network for OCR model download.", 13 /* NETWORK_ISSUE */));
                    } else {
                        handleModelDownloadFailure(e);
                    }
                    modelReady(false, e);
                });
    }

    private void modelReady(boolean success, Exception e) {
        if (success) {
            modelsLoadedSuccessfullyCount.incrementAndGet();
        }
        modelCheckOrDownloadAttempts.incrementAndGet();

        Log.d(TAG, "Models successfully loaded count: " + modelsLoadedSuccessfullyCount.get() + "/" + TOTAL_MODELS);
        Log.d(TAG, "Model check/download attempts completed: " + modelCheckOrDownloadAttempts.get() + "/" + TOTAL_MODELS);


        if (modelCheckOrDownloadAttempts.get() == TOTAL_MODELS) {
            isDownloadInProgress.set(false); // Downloads have completed (successfully or failed)
            if (modelsLoadedSuccessfullyCount.get() == TOTAL_MODELS) {
                Log.d(TAG, "All models successfully loaded. Proceeding to MainActivity.");
                modelsInitializedAndReady.set(true);

                progressBar.setVisibility(View.GONE);
                loadingText.setVisibility(View.GONE);

                // Dismiss any active Snackbar related to network/download issues
                if (currentSnackbar != null && currentSnackbar.isShown()) {
                    currentSnackbar.dismiss();
                }
                isNetworkSnackbarShowing = false; // Reset the flag

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (!isFinishing()) {
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        finish();
                    }
                }, SPLASH_DISPLAY_LENGTH);
            } else {
                Log.d(TAG, "Not all models loaded successfully. Waiting for user action or retry.");
            }
        }
    }

    private void handleModelDownloadFailure(Exception e) {
        if (!modelsInitializedAndReady.get()) {
            runOnUiThread(() -> {
                String message;
                boolean isNetworkIssue = false;

                if (e instanceof MlKitException) {
                    int errorCode = ((MlKitException) e).getErrorCode();
                    if (errorCode == 13 /* MlKitException.NETWORK_ISSUE */) {
                        message = getString(R.string.internet_required_retry);
                        isNetworkIssue = true;
                    } else if (errorCode == MlKitException.NOT_ENOUGH_SPACE) {
                        message = getString(R.string.not_enough_space_retry);
                        isNetworkIssue = false;
                    } else {
                        message = getString(R.string.download_failed_retry);
                        isNetworkIssue = false;
                    }
                } else if (!isNetworkAvailable()) { // Fallback for general exceptions + no network
                    message = getString(R.string.internet_required_retry);
                    isNetworkIssue = true;
                } else {
                    message = getString(R.string.download_failed_retry);
                    isNetworkIssue = false;
                }

                if (isNetworkIssue && isNetworkSnackbarShowing) {
                    Log.d(TAG, "Network Snackbar already shown. Skipping duplicate.");
                    return;
                }
                if (!isNetworkIssue && isNetworkSnackbarShowing && currentSnackbar != null && currentSnackbar.isShown()) {
                    currentSnackbar.dismiss();
                }

                showSnackbarWithRetry(message, isNetworkIssue);
            });
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    private void showSnackbarWithRetry(String message, boolean isNetworkIssue) {
        View rootView = findViewById(R.id.main);
        if (rootView == null) {
            Log.e(TAG, "Root view (R.id.main) not found for Snackbar.");
            return;
        }

        if (currentSnackbar != null && currentSnackbar.isShown()) {
            currentSnackbar.dismiss();
        }

        currentSnackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_INDEFINITE);
        isNetworkSnackbarShowing = isNetworkIssue;

        currentSnackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                super.onDismissed(transientBottomBar, event);
                if (transientBottomBar == currentSnackbar) {
                    isNetworkSnackbarShowing = false;
                    currentSnackbar = null;
                }
            }
        });

        currentSnackbar.setAction(R.string.retry_button, view -> {
            if (!modelsInitializedAndReady.get()) {
                initializeModels();
            }
        });
        currentSnackbar.show();
    }
}