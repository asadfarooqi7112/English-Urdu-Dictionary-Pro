    package com.hfad.personaldictionary.activities;

    import android.annotation.SuppressLint;
    import android.Manifest;
    import android.content.Intent;
    import android.content.pm.PackageManager;
    import android.graphics.Color;
    import android.graphics.drawable.ColorDrawable;
    import android.media.Image;
    import android.net.Uri;
    import android.os.Bundle;
    import android.view.Window;
    import android.widget.TextView;
    import android.widget.Toast;

    import androidx.activity.EdgeToEdge;
    import androidx.annotation.NonNull;
    import androidx.appcompat.app.AlertDialog;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.camera.core.CameraSelector;
    import androidx.camera.core.ImageAnalysis;
    import androidx.camera.core.Preview;
    import androidx.camera.lifecycle.ProcessCameraProvider;
    import androidx.camera.view.PreviewView;
    import androidx.core.app.ActivityCompat;
    import androidx.core.content.ContextCompat;
    import androidx.core.graphics.Insets;
    import androidx.core.view.ViewCompat;
    import androidx.core.view.WindowInsetsCompat;

    import com.google.common.util.concurrent.ListenableFuture;
    import com.google.mlkit.vision.common.InputImage;
    import com.google.mlkit.vision.text.TextRecognition;
    import com.google.mlkit.vision.text.TextRecognizer;
    import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
    import com.hfad.personaldictionary.utils.ContentAction;
    import com.hfad.personaldictionary.R;

    public class OCRWithLivePreviewActivity extends AppCompatActivity {
        private String recognizedText = "";

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_ocrwith_live_preview);
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.ocrActivityMain), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            if (ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{"android.permission.CAMERA"}, 10);
            }

            findViewById(R.id.translateOCRBtn).setOnClickListener(v -> {
                if (recognizedText.trim().isEmpty()) {
                    Toast.makeText(OCRWithLivePreviewActivity.this, "Please scan English text.", Toast.LENGTH_SHORT).show();
                }else{
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("OCR_RESULT", recognizedText);
                    setResult(RESULT_OK, returnIntent);
                    finish();
                }
            });
        }
        private void startCamera() {
            ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                    ProcessCameraProvider.getInstance(this);

            cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                    PreviewView previewView = findViewById(R.id.previewView);
                    Preview preview = new Preview.Builder().build();
                    CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                    preview.setSurfaceProvider(previewView.getSurfaceProvider());

                    // OCR: Image Analysis Use Case
                    ImageAnalysis imageAnalysis =
                            new ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build();

                    TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
                        @SuppressLint("UnsafeOptInUsageError")
                        Image mediaImage = imageProxy.getImage();
                        if (mediaImage != null) {
                            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                            recognizer.process(image)
                                    .addOnSuccessListener(result -> {
                                        recognizedText = result.getText();
                                        ((TextView)findViewById(R.id.textOutput)).setText(recognizedText);
                                    })
                                    .addOnCompleteListener(task -> imageProxy.close());
                        }
                    });

                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle(
                            this,
                            cameraSelector,
                            preview,
                            imageAnalysis
                    );

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, ContextCompat.getMainExecutor(this));
        }

        //This function is called after the permission dialog finishes
        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                               @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

            if (requestCode == 10) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCamera();
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                        ContentAction.showDialog(this);
                        AlertDialog alertDialog = new AlertDialog.Builder(this)
                                .setTitle("Camera Permission Required")
                                .setMessage("This app needs camera access to scan text. Please allow it to proceed.")
                                .setPositiveButton("Grant", (dialog, which) -> {
                                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 10);
                                })
                                .setNegativeButton("Cancel", (dialog, which) -> finish())
                                .show();

                        Window window = alertDialog.getWindow();
                        if (window != null) {
                            window.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#E6E6FA")));
                        }
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


    }