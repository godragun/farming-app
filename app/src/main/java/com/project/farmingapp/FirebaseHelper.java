package com.project.farmingapp;

import androidx.annotation.NonNull;

import com.project.farmingapp.supabase.SupabaseConfig;
import com.project.farmingapp.supabase.SupabaseSession;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Saves uploaded image metadata to the Supabase {@code uploaded_images} table.
 */
public class FirebaseHelper {

    public interface SaveCallback {
        void onSuccess();
        void onError(@NonNull String error);
    }

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final ExecutorService executor;

    public FirebaseHelper() {
        client = new OkHttpClient();
        executor = Executors.newSingleThreadExecutor();
    }

    public void saveImageRecord(@NonNull String imageUrl, long timestamp, @NonNull SaveCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("image_url", imageUrl);
                payload.put("timestamp", timestamp);

                String bearer = SupabaseSession.INSTANCE.bearerToken();
                Request request = new Request.Builder()
                        .url(SupabaseConfig.INSTANCE.getRestUrl() + "/uploaded_images")
                        .post(RequestBody.create(payload.toString(), JSON))
                        .addHeader("apikey", SupabaseConfig.ANON_KEY)
                        .addHeader("Authorization", "Bearer " + bearer)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=minimal")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        String body = response.body() != null ? response.body().string() : "";
                        callback.onError("Failed to save image record (" + response.code() + "): " + body);
                    }
                }
            } catch (IOException e) {
                callback.onError("Failed to save image record: " + e.getMessage());
            } catch (Exception e) {
                callback.onError("Failed to save image record: " + e.getMessage());
            }
        });
    }
}
