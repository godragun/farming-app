package com.project.farmingapp;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FirebaseHelper {

    public interface SaveCallback {
        void onSuccess();
        void onError(@NonNull String error);
    }

    private final FirebaseFirestore firestore;

    public FirebaseHelper() {
        firestore = FirebaseFirestore.getInstance();
    }

    public void saveImageRecord(@NonNull String imageUrl, long timestamp, @NonNull SaveCallback callback) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("imageUrl", imageUrl);
        payload.put("timestamp", timestamp);

        firestore.collection("uploaded_images")
                .add(payload)
                .addOnSuccessListener(documentReference -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError("Failed to save to Firestore: " + e.getMessage()));
    }
}
