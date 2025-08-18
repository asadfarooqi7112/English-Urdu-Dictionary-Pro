package com.hfad.personaldictionary.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;
import com.hfad.personaldictionary.R;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class ObjectRecognition extends AppCompatActivity {
    private enum ActionType {
        LABELING,
        DETECTION
    }
    private ActionType selectedAction;
    public static final int PICK_IMAGE_REQUEST = 1;
    Button imageLabelling, imageRecognition;
    TextView labelResultTextView;
    ImageView recognizedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_object_recognition);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText(getResources().getString(R.string.objectRecognition));

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.arrow_back_24);

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));

        recognizedImage = findViewById(R.id.recognizedImage);
        labelResultTextView = findViewById(R.id.labelResultTextView);
        imageLabelling = findViewById(R.id.imageLabelling);
        imageRecognition = findViewById(R.id.imageRecognition);

        imageLabelling.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedAction = ActionType.LABELING;
                openDialoge();
            }
        });
        imageRecognition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedAction = ActionType.DETECTION;
                openDialoge();
            }
        });
    }

    private void openDialoge() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_image_selection, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        Button cameraBtn = dialogView.findViewById(R.id.cameraBtn);
        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCamera();
                dialog.dismiss();
            }
        });
        Button galleryBtn = dialogView.findViewById(R.id.galleryBtn);
        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();
                dialog.dismiss();
            }
        });
        dialog.show();
    }
    public void openCamera(){
        if(selectedAction == ActionType.LABELING){
            Intent intent = new Intent(ObjectRecognition.this, LiveLabelingActivity.class);
            startActivity(intent);
        }else{
            Intent intent = new Intent(ObjectRecognition.this, LiveObjectDetectionActivity.class);
            startActivity(intent);
        }
    }
    public void openGallery(){
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");;
        startActivityForResult(galleryIntent,PICK_IMAGE_REQUEST);
    }
    private void runImageLabeling(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);

        labeler.process(image)
                .addOnSuccessListener(labels -> {
                    StringBuilder resultText = new StringBuilder();
                    for (ImageLabel label : labels) {
                        float confidence = label.getConfidence();
                        if (confidence >= 0.70) {  // Only include labels with ≥70% confidence
                            String text = label.getText();
                            resultText.append(text)
                                    .append(" (")
                                    .append(String.format("%.2f", confidence * 100))
                                    .append("%)\n");
                        }
                    }

                    if (resultText.length() == 0) {
                        labelResultTextView.setText("No labels above 70% confidence.");
                    } else {
                        labelResultTextView.setText(resultText.toString());
                    }
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    labelResultTextView.setText("Failed to label image.");
                });
    }

    private void runObjectDetection(Bitmap originalBitmap) {
        // Load custom model from assets folder
        LocalModel localModel = new LocalModel.Builder()
                .setAssetFilePath("EfficientNet-lite.tflite")
                .build();

        // Set custom detector options
        CustomObjectDetectorOptions customObjectDetectorOptions =
                new CustomObjectDetectorOptions.Builder(localModel)
                        .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
                        .enableMultipleObjects()
                        .enableClassification()  // Enables labels (like animal, fruit, etc.)
                        .build();

        ObjectDetector objectDetector = ObjectDetection.getClient(customObjectDetectorOptions);
        InputImage image = InputImage.fromBitmap(originalBitmap, 0);

        objectDetector.process(image)
                .addOnSuccessListener(detectedObjects -> {
                    if (detectedObjects.isEmpty()) {
                        labelResultTextView.setText("No objects detected.");
                        recognizedImage.setImageBitmap(originalBitmap); // show image anyway
                    } else {
                        StringBuilder resultText = new StringBuilder();

                        Bitmap mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                        Canvas canvas = new Canvas(mutableBitmap);
                        Paint boxPaint = new Paint();
                        boxPaint.setColor(Color.RED);
                        boxPaint.setStrokeWidth(5f);
                        boxPaint.setStyle(Paint.Style.STROKE);

                        Paint textPaint = new Paint();
                        textPaint.setColor(Color.YELLOW);
                        textPaint.setTextSize(40f);
                        textPaint.setStyle(Paint.Style.FILL);

                        for (DetectedObject object : detectedObjects) {
                            List<DetectedObject.Label> labels = object.getLabels();

                            if (!labels.isEmpty()) {
                                DetectedObject.Label topLabel = labels.get(0);
                                float confidence = topLabel.getConfidence();

                                // Filter by confidence >= 10%
                                if (confidence >= 0.10f) {
                                    Rect bounds = object.getBoundingBox();
                                    canvas.drawRect(bounds, boxPaint);

                                    String labelText = topLabel.getText();
                                    canvas.drawText(labelText, bounds.left, bounds.top - 10, textPaint);

                                    resultText.append("Label: ").append(labelText)
                                            .append(" (confidence: ").append(String.format("%.2f", confidence * 100)).append("%)\n");
                                    resultText.append("Bounds: ").append(bounds.toString()).append("\n\n");
                                }
                            }
                            // If you want, you can handle objects without labels here or just skip
                        }

                        recognizedImage.setImageBitmap(mutableBitmap);
                        labelResultTextView.setText(resultText.toString());
                    }
                })
                .addOnFailureListener(e -> {
                    labelResultTextView.setText("Detection failed: " + e.getMessage());
                    Log.e("ObjectRecognition", "Object detection failed", e);
                });

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null){
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
        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            Uri croppedUri = UCrop.getOutput(data);

            if (croppedUri != null) {
                try {
                    Bitmap croppedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), croppedUri);

                    if (selectedAction == ActionType.LABELING) {
                        runImageLabeling(croppedBitmap);
                    } else if (selectedAction == ActionType.DETECTION) {
                        runObjectDetection(croppedBitmap);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}