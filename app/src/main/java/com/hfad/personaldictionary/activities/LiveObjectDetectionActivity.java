package com.hfad.personaldictionary.activities;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;
import com.hfad.personaldictionary.views.GraphicOverlay;
import com.hfad.personaldictionary.R;

import java.util.ArrayList;
import java.util.List;

public class LiveObjectDetectionActivity extends AppCompatActivity {

    private PreviewView previewView;
    private TextView resultTextView;
    private ObjectDetector objectDetector;
    private GraphicOverlay graphicOverlay;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_object_detection);

        previewView = findViewById(R.id.previewView);
        graphicOverlay = findViewById(R.id.graphicOverlay);
        resultTextView = findViewById(R.id.resultTextView);

        // Load local custom model
        LocalModel localModel = new LocalModel.Builder()
                .setAssetFilePath("EfficientNet-lite.tflite")
                .build();

        CustomObjectDetectorOptions options = new CustomObjectDetectorOptions.Builder(localModel)
                .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build();

        objectDetector = ObjectDetection.getClient(options);

        startCamera();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                androidx.camera.core.Preview preview = new androidx.camera.core.Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
                    @SuppressLint("UnsafeOptInUsageError")
                    android.media.Image mediaImage = imageProxy.getImage();

                    if (mediaImage != null) {
                        InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

                        objectDetector.process(image)
                                .addOnSuccessListener(detectedObjects -> {
                                    StringBuilder resultText = new StringBuilder();
                                    List<Rect> boxes = new ArrayList<>();

                                    for (DetectedObject detectedObject : detectedObjects) {
                                        Rect boundingBox = detectedObject.getBoundingBox();
                                        boxes.add(boundingBox);

                                        List<DetectedObject.Label> labels = detectedObject.getLabels();
                                        if (!labels.isEmpty()) {
                                            DetectedObject.Label label = labels.get(0);
                                            float confidence = label.getConfidence();

                                            if (confidence >= 0.10f) {
                                                resultText.append(label.getText())
                                                        .append(" (")
                                                        .append(String.format("%.2f", confidence * 100))
                                                        .append("%)\n");
                                            }
                                        }
                                    }
                                    graphicOverlay.setImageSourceInfo(image.getWidth(), image.getHeight());
                                    graphicOverlay.setImageFlipped(false); // or true if using front camera

                                    graphicOverlay.setBoundingBoxes(boxes); // Update overlay

                                    if (resultText.length() == 0) {
                                        resultTextView.setText("No confident objects detected.");
                                    } else {
                                        resultTextView.setText(resultText.toString());
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    resultTextView.setText("Detection failed: " + e.getMessage());
                                    Log.e("LiveObjectDetection", "Detection failed", e);
                                })
                                .addOnCompleteListener(task -> imageProxy.close());

                    } else {
                        imageProxy.close();
                    }
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e("LiveObjectDetection", "Camera initialization failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }
}
