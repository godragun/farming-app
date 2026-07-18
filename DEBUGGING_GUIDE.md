# Debugging Guide - Farming App Survey System

## 🔍 Logcat Debugging

### Enable Verbose Logging in AndroidStudio

1. Open **Logcat** panel (bottom of Android Studio)
2. Filter by tag: `[SURVEY]`, `[CAMERA]`, `[LOCATION]`, `[FIREBASE]`, `[SYNC]`
3. Monitor these tags during operations

### Sample Logcat Output - Expected Behavior

#### Taking a Photo (Should be Fast - No Freezing)
```
[CAMERA] Requesting camera permission...
[CAMERA] Camera permission granted
[CAMERA] Launching camera: photoUri=content://com.project.farmingapp.fileprovider/pictures/SURVEY_20260423_091500.jpg, path=/data/user/0/com.project.farmingapp/files/Pictures/SURVEY_20260423_091500.jpg
[CAMERA] Photo captured successfully: /data/user/0/com.project.farmingapp/files/Pictures/SURVEY_20260423_091500.jpg
[CAMERA] Loading photo preview: /data/user/0/com.project.farmingapp/files/Pictures/SURVEY_20260423_091500.jpg (2048576 bytes)
```

**Result:** Fast (< 1 second), no ANR, image appears in preview ✅

---

#### Fetching Location (Should Complete in ~1-5 Seconds)
```
[LOCATION] Location permission granted
[LOCATION] Starting location fetch with 15s timeout...
[LOCATION] Requesting current location (timeout: 15000ms)...
[LOCATION] Location found: 6.9352° N, 79.8711° E
[LOCATION] Location updated: 6.9352, 79.8711
```

**Result:** Fast (< 5 seconds), UI responsive, accurate coordinates ✅

**If No Signal (Falls Back Immediately):**
```
[LOCATION] Location request timed out after 15000ms
[LOCATION] Failed to get location
[LOCATION] Using default location: Colombo (6.9271°N, 79.8612°E)
```

**Result:** Falls back in 15 seconds, no ANR ✅

---

#### Saving Survey (Should be Instant)
```
[SURVEY] Saving survey: farmerName=John Doe, crop=Rice, photo=true, audio=false
[SURVEY] Survey saved to local database
[SURVEY] Survey save successful: ID=1
```

**Result:** Instant (< 100ms) ✅

---

#### Syncing Survey - Online (Should Complete in 5-30 Seconds)
```
[SYNC] Starting survey sync...
[SYNC] Found 1 pending survey(ies) to sync
[SYNC] Syncing survey: John Doe (ID: 1)
[CAMERA] Starting compression: /data/user/0/com.project.farmingapp/files/Pictures/SURVEY_20260423_091500.jpg → size: 2048576 bytes
[CAMERA] Compression complete: 256000 bytes (12% of original)
[SYNC] Starting image upload: /data/user/0/com.project.farmingapp/files/Pictures/SURVEY_20260423_091500.jpg (size: 256000 bytes)
[SYNC] Image uploaded successfully: https://firebasestorage.googleapis.com/v0/b/...
[SYNC] Successfully uploaded survey to Firestore: John Doe
[SYNC] Sync completed: 1/1 surveys synced successfully
[SYNC] Sync successful: 1 survey(s)
```

**Result:** Success message shown, survey marked as synced ✅

---

#### Syncing Survey - Offline (Should Be Immediate)
```
[SYNC] Starting survey sync...
[SYNC] No internet connection. Surveys saved locally, will sync later.
[SYNC] No internet - saved locally
```

**Result:** Toast message, survey remains in "unsynced" state ✅

---

#### Syncing Survey - Error (Should Show Error Message)
```
[SYNC] Starting survey sync...
[SYNC] Found 1 pending survey(ies) to sync
[SYNC] Syncing survey: John Doe (ID: 1)
[SYNC] Starting image upload: /data/user/0/com.project.farmingapp/files/Pictures/SURVEY_20260423_091500.jpg (size: 256000 bytes)
[SYNC] Image upload failed: com.google.firebase.storage.StorageException: User does not have permission
[SYNC] Sync completed: 0/1 surveys synced successfully
[SYNC] Sync error: User does not have permission
```

**Result:** Error message shown, survey remains for retry ✅

---

## 🧪 Test Cases

### Test 1: Camera Crash Fix
**Steps:**
1. Open app → Go to Survey Form
2. Click "Capture Photo"
3. Grant permission if needed
4. Take a photo
5. Verify preview appears (without freezing)

**Expected:**
- No ANR
- Photo preview appears within 1 second
- Logcat shows: `[CAMERA] Photo captured successfully`

---

### Test 2: Location Freezing Fix
**Steps:**
1. Open Survey Form
2. Observe location fetching at top
3. Wait for location display to update

**Expected:**
- No ANR
- Location updates within 15 seconds OR falls back to "Colombo, Sri Lanka"
- Logcat shows: `[LOCATION] Location found` or `[LOCATION] Using default location`

---

### Test 3: Survey Offline→Online Sync
**Steps:**
1. **Offline:** Turn off WiFi + Mobile data
2. Fill survey form, click "Save Offline"
3. **Online:** Turn on WiFi/Mobile data
4. Go to "Saved Surveys"
5. Click "Sync All" button

**Expected:**
- Survey appears in list with orange "Pending" badge initially
- After sync: badge changes to green "Synced ✔"
- Logcat shows upload success
- Firestore shows new document in `users/{uid}/surveys/`

---

### Test 4: Multiple Submissions (No Overwriting)
**Steps:**
1. Create & save Survey #1: "John, Rice"
2. Create & save Survey #2: "Jane, Tea"
3. Create & save Survey #3: "Bob, Corn"
4. Go to "Saved Surveys"
5. Sync all
6. Check Firestore

**Expected:**
- All 3 appear in list as separate items
- Firestore shows 3 documents with unique IDs
- No overwriting occurred

---

### Test 5: Image Upload Success
**Steps:**
1. Create survey with photo
2. Save & sync online
3. Check Firestore document
4. Open `imageUrl` link in browser

**Expected:**
- `imageUrl` field has valid HTTPS link
- Image loads in browser
- Filename format: `users/{uid}/surveys/{remoteId}/images/{timestamp}_{uuid}.jpg`

---

## ❌ Common Issues & Solutions

### Issue: "Wait or Close App" (ANR) When Taking Photo

**Cause:** Image loading on main thread
**Solution:**
- Check Logcat for `[CAMERA]` tags
- If you see delay before `Photo captured successfully`, problem is in Glide loading
- Verify `override(1280, 720)` is set in Glide call

**Logcat check:**
```
✅ [CAMERA] Photo captured successfully
✅ [CAMERA] Loading photo preview...
❌ (5 second delay without logs)
```

---

### Issue: Location Takes 30+ Seconds or Freezes

**Cause:** Old LocationManager blocking main thread
**Solution:**
- Check Logcat: Should show `[LOCATION] Starting location fetch`
- Should timeout after 15 seconds maximum
- Check permission is granted

**Logcat check:**
```
✅ [LOCATION] Starting location fetch with 15s timeout...
✅ [LOCATION] Location found (or timeout)
❌ No location logs = not using new LocationHelper
```

---

### Issue: Data Not Syncing (Offline→Online)

**Cause:** Firebase offline persistence not enabled OR network detection failing
**Solution:**
1. **Check FarmingApp.kt** has offline persistence enabled
2. **Check network:** In SavedSurveysFragment, check "unsynced" count
3. **Check Logcat:** Look for `[SYNC]` tags

**Logcat check:**
```
✅ [SYNC] No internet connection. Surveys saved locally, will sync later.
✅ [SYNC] Starting survey sync...
❌ No sync logs = sync never called
```

**Fix:**
1. Verify `FirebaseFirestore.getInstance().firestoreSettings` in FarmingApp.kt
2. Check manifest has `INTERNET` permission
3. Check network connection is actually working

---

### Issue: Multiple Submissions Overwrite Each Other

**Cause:** Using `.set()` with same document ID
**Solution:**
- Each submission should create unique document in Firestore
- Check Logcat for UUID generation

**Logcat check:**
```
✅ [SYNC] Syncing survey: John (ID: 1)
✅ [SYNC] firestore.collection("users").document(uid).collection("surveys").document("{remoteId}").set(...)
✅ Remote ID is UUID: "a1b2c3d4-e5f6-4789-0abc-def123456789"
❌ All surveys have same remoteId
```

**Fix in FieldSurveyRepository.kt:**
```kotlin
// ✅ This should generate UUID if empty
val survey = if (original.remoteId.isBlank()) {
    original.copy(remoteId = UUID.randomUUID().toString())
} else {
    original
}
```

---

### Issue: Images Not Uploading

**Cause:** File doesn't exist, permissions missing, or storage path wrong
**Solution:**
1. Check file exists: `File(imagePath).exists()`
2. Check permissions: `READ_EXTERNAL_STORAGE` for Android < 12

**Logcat check:**
```
✅ [SYNC] Starting image upload: /path/to/file.jpg (size: 256000 bytes)
✅ [SYNC] Image uploaded successfully: https://...
❌ [SYNC] Image file not found or empty
❌ [SYNC] Image upload failed
```

**Fix:**
1. Ensure photo is saved before sync
2. Check `currentPhotoPath` is not empty
3. Verify file permissions in AndroidManifest.xml

---

## 🔧 Manual Testing Steps

### Step 1: Enable Logcat Filtering

In Android Studio Logcat:
```
Tag Filter: [SURVEY]|[CAMERA]|[LOCATION]|[FIREBASE]|[SYNC]
```

### Step 2: Monitor During Operations

Create a survey and observe logs:

```bash
# Expected sequence:
1. [SURVEY] Saving survey...
2. [SURVEY] Survey saved to local database
3. (If sync immediate) [SYNC] Starting survey sync...
4. [SYNC] Found X pending survey(ies)
5. [SYNC] Syncing survey: Name (ID: X)
6. (If image) [CAMERA] Starting compression...
7. [SYNC] Image uploaded successfully: https://...
8. [SYNC] Successfully uploaded survey to Firestore
9. [SYNC] Sync completed: X/X surveys synced successfully
```

### Step 3: Verify in Firestore

1. Open Firebase Console → Firestore Database
2. Navigate to: `users/{uid}/surveys/`
3. Should see documents with remote IDs
4. Each document should have:
   - `farmerName`
   - `cropType`
   - `imageUrl` (if image uploaded)
   - `audioUrl` (if audio uploaded)
   - `syncedAt` (timestamp)

### Step 4: Check File Storage

Images stored in:
```
Firebase Storage: users/{uid}/surveys/{remoteId}/images/{timestamp}_{uuid}.jpg
```

Audio stored in:
```
Firebase Storage: users/{uid}/surveys/{remoteId}/audio/{timestamp}_{uuid}.mp4
```

---

## 📊 Performance Benchmarks

| Operation | Target Time | Expected | Status |
|-----------|------------|----------|--------|
| Camera capture | < 1s | 0.5-0.8s | ✅ |
| Photo preview | < 1s | 0.3-0.5s | ✅ |
| Location fetch | < 15s | 2-5s (or timeout) | ✅ |
| Save survey | < 100ms | 50-80ms | ✅ |
| Sync online | < 30s | 5-20s | ✅ |
| Image upload | < 10s | 2-5s | ✅ |

---

## 🆘 Emergency Troubleshooting

If everything is broken:

1. **Clear App Data:**
   ```
   Settings → Apps → Farming App → Storage → Clear Cache & Storage
   ```

2. **Check Permissions in AndroidManifest.xml:**
   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   <uses-permission android:name="android.permission.CAMERA" />
   <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
   <uses-permission android:name="android.permission.RECORD_AUDIO" />
   ```

3. **Restart Firebase:**
   - Delete app from device
   - Rebuild & reinstall
   - Clear Firebase cache (requires Firestore Rules modification)

4. **Check Network:**
   ```
   adb shell: ping 8.8.8.8
   ```

5. **Verify Device Has Space:**
   ```
   adb shell: df -h
   ```

---

## 📞 Getting Help

When reporting issues, always include:
1. **Logcat output** (copy `[SURVEY]`, `[CAMERA]`, `[LOCATION]`, `[FIREBASE]`, `[SYNC]` tags)
2. **Device info:** Android version, manufacturer
3. **Network:** WiFi or cellular, online/offline
4. **Steps to reproduce**
5. **Expected vs actual result**

---

**Last Updated:** 2026-04-23
**Debugging Status:** Ready
