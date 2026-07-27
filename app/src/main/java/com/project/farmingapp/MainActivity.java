package com.project.farmingapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.Locale;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

public class MainActivity extends AppCompatActivity {

    private Button selectImageButton;
    private Button uploadImageButton;
    private Button toggleLanguageButton;
    private ImageView previewImageView;
    private ProgressBar uploadProgressBar;

    private Uri selectedImageUri;
    private SupabaseUploadHelper supabaseUploadHelper;
    private FirebaseHelper firebaseHelper;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectImageButton = findViewById(R.id.buttonSelectImage);
        uploadImageButton = findViewById(R.id.buttonUploadImage);
        toggleLanguageButton = findViewById(R.id.buttonToggleLanguage);
        previewImageView = findViewById(R.id.imageViewPreview);
        uploadProgressBar = findViewById(R.id.progressUpload);

        supabaseUploadHelper = new SupabaseUploadHelper();
        firebaseHelper = new FirebaseHelper();

        setupLaunchers();
        setupClicks();
        setLoading(false);
    }

    private void setupLaunchers() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            Glide.with(this)
                                    .load(selectedImageUri)
                                    .centerCrop()
                                    .into(previewImageView);
                        }
                    }
                }
        );

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (Boolean.TRUE.equals(granted)) {
                        openGallery();
                    } else {
                        Toast.makeText(this, "Gallery permission is required", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setupClicks() {
        selectImageButton.setOnClickListener(view -> checkGalleryPermissionThenOpen());
        uploadImageButton.setOnClickListener(view -> uploadSelectedImage());
        toggleLanguageButton.setOnClickListener(view -> toggleLanguage());
    }

    private void toggleLanguage() {
        Locale current = getResources().getConfiguration().locale;
        String lang = current.getLanguage().equals("si") ? "en" : "si";
        setLocale(lang);
    }

    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        recreate();
    }

    private void checkGalleryPermissionThenOpen() {
        String permission = getGalleryPermission();
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            permissionLauncher.launch(permission);
        }
    }

    private String getGalleryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_IMAGES;
        }
        return Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void uploadSelectedImage() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        supabaseUploadHelper.uploadImage(this, selectedImageUri, new SupabaseUploadHelper.UploadCallback() {
            @Override
            public void onSuccess(@NonNull String publicUrl) {
                long now = System.currentTimeMillis();
                firebaseHelper.saveImageRecord(publicUrl, now, new FirebaseHelper.SaveCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            setLoading(false);
                            Glide.with(MainActivity.this)
                                    .load(publicUrl)
                                    .centerCrop()
                                    .into(previewImageView);
                            Toast.makeText(MainActivity.this, "Upload successful", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(@NonNull String error) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                        });
                    }
                });
            }

            @Override
            public void onError(@NonNull String error) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        uploadProgressBar.setVisibility(loading ? ProgressBar.VISIBLE : ProgressBar.GONE);
        selectImageButton.setEnabled(!loading);
        uploadImageButton.setEnabled(!loading);
    }
}
