package com.hfad.personaldictionary.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.hfad.personaldictionary.R;
import com.hfad.personaldictionary.activities.OCRWithLivePreviewActivity;

import java.util.Locale;

public class ContentAction {
    public static final int SPEECH_REQUEST_CODE = 1;
    public static final int PICK_IMAGE_REQUEST = 2;
    public static final int REQUEST_CODE_OCR = 3;
    // Code for copying text to the clipboard
    public static void copyToClipboard(Context context,String text){
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText("content",text);
        clipboardManager.setPrimaryClip(clipData);
        Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show();
    }

    //Code for sharing text
    public static void shareText(Context context, String text){
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT,text);
        context.startActivity(Intent.createChooser(intent,"Share via"));
    }

    //Code for speech to text
    public static void startSpeechToText(Activity activity, boolean isInputEnglish){
        if (!isInternetAvailable(activity)) {
            Toast.makeText(activity, "Internet connection is required to use this feature.", Toast.LENGTH_SHORT).show();
            return;
        }
        if(isInputEnglish){
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.ENGLISH);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Speak Now");
            try {
                activity.startActivityForResult(intent, SPEECH_REQUEST_CODE);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(activity, "Your device doesn't support this feature", Toast.LENGTH_SHORT).show();
            }
        }else {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ur-PK");
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"اردو میں بولیں");
            try {
                activity.startActivityForResult(intent, SPEECH_REQUEST_CODE);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(activity, "Your device doesn't support this feature", Toast.LENGTH_SHORT).show();
            }
        }
    }
    //Code for opening OCRActivity
    public static void openOCR(Activity activity){
        if(ContextCompat.checkSelfPermission(activity, "android.permission.CAMERA") == PackageManager.PERMISSION_GRANTED){
            Intent intent = new Intent(activity, OCRWithLivePreviewActivity.class);
            activity.startActivityForResult(intent, REQUEST_CODE_OCR);
        }else{
            ActivityCompat.requestPermissions(activity,  new String[]{"android.permission.CAMERA"}, 10);
        }
    }
    //Code to open gallery
    public static void openGallery(Activity activity){
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");;
        activity.startActivityForResult(galleryIntent,PICK_IMAGE_REQUEST);
    }
    //Code to create and show a dialog with a custom layout
    public static void showDialog(Activity activity){
        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_image_selection,null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        Button cameraBtn = dialogView.findViewById(R.id.cameraBtn);
        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openOCR(activity);
                dialog.dismiss();
            }
        });
        Button galleryBtn = dialogView.findViewById(R.id.galleryBtn);
        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery(activity);
                dialog.dismiss();
            }
        });
        dialog.show();
    }
    //Code to check if internet is available
    public static boolean isInternetAvailable(Context context){
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
