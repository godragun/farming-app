package com.project.farmingapp;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseUploadHelper {

    public interface UploadCallback {
        void onSuccess(@NonNull String publicUrl);
        void onError(@NonNull String error);
    }

    private static final String SUPABASE_URL = "https://nkjzjbjkcpcbefkypvdl.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5ranpqYmprY3BjYmVma3lwdmRsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzY3ODMzMDQsImV4cCI6MjA5MjM1OTMwNH0.yI46mLkjTM9Qs3etBoJSCqCDjPRMuibUlvDSabFiG5w";
    private static final String BUCKET_NAME = "media";

    private final OkHttpClient okHttpClient;
    private final ExecutorService executorService;

    public SupabaseUploadHelper() {
        this.okHttpClient = new OkHttpClient();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void uploadImage(@NonNull Context context, @NonNull Uri imageUri, @NonNull UploadCallback callback) {
        executorService.execute(() -> {
            byte[] fileBytes;
            try {
                fileBytes = readBytes(context, imageUri);
            } catch (IOException e) {
                callback.onError("Failed to read image: " + e.getMessage());
                return;
            }

            String extension = resolveExtension(context.getContentResolver(), imageUri);
            String fileName = String.format(
                    Locale.US,
                    "uploads/%d_%s.%s",
                    System.currentTimeMillis(),
                    UUID.randomUUID().toString().replace("-", ""),
                    extension
            );
            String uploadUrl = SUPABASE_URL + "/storage/v1/object/" + BUCKET_NAME + "/" + fileName;
            MediaType mediaType = MediaType.parse(resolveMimeType(context.getContentResolver(), imageUri));
            RequestBody requestBody = RequestBody.create(fileBytes, mediaType);

            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .post(requestBody)
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                    .addHeader("x-upsert", "false")
                    .build();

            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    callback.onError("Upload failed: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try (response) {
                        if (!response.isSuccessful()) {
                            String body = response.body() != null ? response.body().string() : "";
                            callback.onError("Upload failed (" + response.code() + "): " + body);
                            return;
                        }
                        String publicUrl = SUPABASE_URL + "/storage/v1/object/public/" + BUCKET_NAME + "/" + fileName;
                        callback.onSuccess(publicUrl);
                    }
                }
            });
        });
    }

    private static byte[] readBytes(Context context, Uri uri) throws IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                throw new IOException("Input stream is null");
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = inputStream.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return buffer.toByteArray();
        }
    }

    private static String resolveMimeType(ContentResolver resolver, Uri uri) {
        String mimeType = resolver.getType(uri);
        if (TextUtils.isEmpty(mimeType)) {
            return "image/jpeg";
        }
        return mimeType;
    }

    private static String resolveExtension(ContentResolver resolver, Uri uri) {
        String mimeType = resolver.getType(uri);
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        if (TextUtils.isEmpty(extension)) {
            return "jpg";
        }
        return extension;
    }
}
