# Agri Lanka — Farming Companion App

<p align="center">
  <img src="output%20images%20of%20app/front%20page%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/front%20page%20%202%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/sigingin%20%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/signing%20up%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/navigation%20panel%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/light%20mode%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/weather%20forcasting%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/ecommerce%20%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/product%20description%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/check%20out%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/Aricales%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/details%20of%20product%20and%20market%20prices.png" width="200" />
  <img src="output%20images%20of%20app/socila%20media%20post%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/survare%20for%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/saved%20survay%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/front%20statite%20view.png" width="200" />
  <img src="output%20images%20of%20app/satalite%20view%20farming%20app.png" width="200" />
  <img src="output%20images%20of%20app/Agri%20Chatbot.png" width="200" />
  <img src="output%20images%20of%20app/login%20page%202.png" width="200" />
  <img src="output%20images%20of%20app/front%20page%203%20.png" width="200" />
  <img src="output%20images%20of%20app/front%20page%204.png" width="200" />
  <img src="output%20images%20of%20app/front%20page%205.png" width="200" />
  <img src="output%20images%20of%20app/front%20page%206.png" width="200" />
  <img src="output%20images%20of%20app/front%20page%207.png" width="200" />
</p>

An Android fieldwork application built for Sri Lankan **research assistants and data collectors**. It includes **satellite map overlays**, **GPS-based weather**, **offline-first field surveys** (photo + voice notes), agriculture articles, an input catalog/cost baseline module, and a field observation log.

This repository is built to be **demo-friendly**: core UI keeps working even when Firebase or third‑party APIs are unavailable (offline products, offline articles, demo weather, guest mode).

---

## Table of Contents

- [What the App Does](#what-the-app-does)
- [Architecture & Tech Stack](#architecture--tech-stack)
- [Features In Detail](#features-in-detail)
- [How the Map Works](#how-the-map-works)
- [How Weather Works](#how-weather-works)
- [How Field Surveys Work (Offline + Sync)](#how-field-surveys-work-offline--sync)
- [Authentication & User Accounts](#authentication--user-accounts)
- [Offline / Guest Mode](#offline--guest-mode)
- [Dark & Light Mode](#dark--light-mode)
- [Running the App](#running-the-app)
- [Project Structure](#project-structure)
- [External APIs & Services](#external-apis--services)
- [Acknowledgements & References](#acknowledgements--references)
- [Known Quirks & Notes](#known-quirks--notes)
- [Viva Preparation Guide](#viva-preparation-guide--agri-lanka)

---

## What the App Does

Agri Lanka is a one-stop mobile app for **field researchers** working in Sri Lanka. From the moment you open the app, you can:

- Check live weather for your exact GPS location (or Colombo as a default)
- Review an agricultural input catalog and estimate baseline costs (fertilizers, pesticides, machinery) for supply-chain auditing
- Read daily market/APMC price updates to make better selling decisions
- Post field observations and anomalies and coordinate with other research assistants
- Learn from crop-specific agriculture articles
- Get advice from a built-in rule-based chatbot covering rice, tea, chilli, coconut, pests, and fertilizer
- Manage your own farmer profile with photo and bio

The app is designed to work in Sri Lanka specifically — currency is LKR, the default location is Colombo, and crop guidance is tuned for local agriculture.

---

## Architecture & Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | XML layouts + ViewBinding / DataBinding |
| Architecture | MVVM (ViewModel + LiveData) |
| Cloud backend | Firebase Firestore |
| Local database | Room (SQLite) |
| Auth | Firebase Authentication (email + Google Sign-In) |
| File Storage | Firebase Storage (profile/background images, post images) |
| Media storage (survey uploads) | Supabase Storage (OkHttp upload helper) |
| Weather | OpenWeatherMap API via Retrofit |
| Image loading | Glide, Picasso |
| Build system | Gradle (Kotlin DSL) |
| Min SDK | API 23 (Android 6.0) |
| Target SDK | API 34 |

The app follows a single-Activity / multi-Fragment pattern. `DashboardActivity` is the main host and all screens (weather, e-commerce, community, etc.) are `Fragment`s swapped in and out of a single `frame_layout` container. Navigation state is preserved correctly across configuration changes (theme switches, rotations).

---

## Features In Detail

### 1. Dashboard (Home Screen)
The home screen is the main hub. It shows:
- A **live weather card** — current temperature, humidity, wind speed, and a weather icon pulled from OpenWeatherMap based on device GPS. Falls back to Colombo coordinates if location is unavailable.
- A **satellite map preview** (Leaflet inside a `WebView`) that is reliable across devices. Tapping it opens the full-screen satellite experience in the APMC screen.
- A **product grid** — 4 randomly selected products from the e-commerce catalog (or offline demo products if Firebase is unavailable).
- **Category shortcuts** to Agriculture Articles.
- A **cart button** in the top corner for quick access to the cart.
- A **floating chatbot button** (bottom-right) that opens the Agri Chatbot from any screen except the social feed and chatbot itself.

### 2. Weather Screen
Full weather detail screen:
- **Hero card** — temperature (°C), condition description, feels-like, humidity, wind speed, min/max for the day.
- **Hourly forecast** — horizontal scrollable list of today's hourly entries.
- **5-day forecast** — daily noon-based entries in a vertical list.
- Data comes from the OpenWeatherMap 5-day / 3-hour forecast API, keyed to GPS coordinates. City name is displayed at the top.
- An offline fallback demo dataset is used if the API call fails.

### 3. E-Commerce (Shop)
A full shopping flow:
- **Product catalog** — grid layout with category filter chips (All, Fertilizer, Pesticide, Machine) and a live search bar that filters by product name and short description.
- **Product detail screen** — full description, images, attribute selection (e.g. size/weight variants), add-to-cart.
- **Cart** — list of selected items with quantities, totals in LKR, and a checkout button.
- **Checkout / Payment** — pre-payment summary screen feeding into a Razorpay payment activity.
- **My Orders** — history of placed orders.
- When Firebase is unavailable, a built-in set of offline demo products is shown so the UI is always usable.

### 4. Market Prices (APMC)
Displays agricultural commodity market prices. Useful for research assistants when capturing **economic context** alongside field yields.

In the article detail screen, each crop/article also shows a **Market price trend** line chart (MPAndroidChart). If the Firestore article document contains a `marketPrices: [..]` field, it will be used; otherwise a stable offline fallback series is shown.

### 5. Field Observation & Anomaly Log (Social Feed)
A research-focused observation log:
- **Post list** — scrollable list of community posts, each optionally with an image.
- **Create post** — text and image posts. Images are uploaded to Firebase Storage before the post is saved to Firestore.
- Posts are saved locally as drafts via `LocalPostDraftStore` so nothing is lost if the app is closed mid-composition.
- User's own posts are visible on their profile and can be deleted from there.

### 6. Agriculture Articles
Educational content section:
- **Article list** — overview of agriculture topics.
- **Fruits / Crops sub-section** — crop-specific articles (e.g. rice, tea, coconut, vegetables).
- Content sourced from Firestore.
- The article detail screen (`FruitsFragment`) includes a **Market price trend** chart and fixed layouts to avoid text overlap on smaller devices.

### 7. Field Classification Assistant
A lightweight, rule-based classification helper:
- Responds to questions on **weather**, **fertilizer**, **pests/disease**, **rice**, **tea**, **chilli**, **coconut**, **how to grow** (any crop), and more.
- Remembers the last topic discussed within the session (stored in SharedPreferences) so follow-up messages like "tell me more" are context-aware.
- Chat bubbles scroll automatically as the conversation grows.
- Available from any screen via the floating action button, and from the side navigation drawer.

### 8. User Profile
Every registered user has a profile page:
- Profile photo and background cover image (uploaded to Firebase Storage).
- Display name, city, and a short bio — all editable inline.
- Post count and a scrollable list of the user's own community posts.
- Tap any of your own posts to view or delete them.

### 9. Intro / Onboarding
Users see a multi-screen intro slider (ViewPager2) **before** the login screen. Slide images live under `app/src/main/assets/images/` (`front_page_1.jpeg` … `front_page_6.jpeg`, `front_page_8.jpeg`, plus `onboarding.png` for the seventh story). The pager loads them with Glide from `file:///android_asset/…` (see `IntroAdapter`, `AgriAssetPaths`). Whether onboarding runs is controlled by `SharedPreferences` (`MyPrefs`): **`intro_schema_version`** must be at least `IntroActivity.INTRO_SCHEMA_VERSION` (bump that constant when you change slides so everyone sees the new tour once). `firstTime` is still written when onboarding completes for older code paths.

---

## Authentication & User Accounts

Three ways to get in:

| Method | How it works |
|---|---|
| Email + Password | Standard Firebase Auth. Signup creates a Firestore user document. |
| Google Sign-In | OAuth via Firebase + Google Sign-In SDK. |
| Guest / Offline mode | Skips Firebase entirely. Uses a fixed demo identity (`harendrathennakoon@gmail.com`). All UI screens open normally. |

Password reset is available from the login screen (sends a Firebase reset email).

The `AuthHelper` singleton manages the signed-in state across the app. It persists guest mode to SharedPreferences so the app survives restarts without asking you to log in again.

---

## Offline / Guest Mode

The app is built to run even without a live Firebase connection:

- **Guest mode** — tap "Continue as Guest" on the login screen. All screens open. Firebase-dependent data (your real posts, orders, profile) won't load, but the layout and navigation are fully testable.
- **Offline e-commerce** — if Firestore returns empty results, a hardcoded set of demo agricultural products is shown automatically. Product detail, cart, and checkout flows all work with these demo products.
- **Offline weather** — if the OpenWeatherMap API call fails, a demo weather dataset is displayed so the weather card and screen always show something.
- **Draft posts** — community post drafts are saved locally so they survive app restarts.
- **Cart persistence** — cart items are stored in `LocalCartStore` (SharedPreferences) so the cart survives app restarts without needing Firebase.

---

## Dark & Light Mode

A dark/light mode toggle is in the **side navigation drawer** ("Switch to Dark Mode" / "Switch to Light Mode"). The preference is saved to SharedPreferences and applied at app startup via the `FarmingApp` Application class, so the chosen theme is restored every time the app opens.

Toggling the theme causes Android to recreate `DashboardActivity` to apply the new theme resources. The app correctly saves and restores your current screen and navigation state during this recreation — you stay on whatever screen you were on, now re-rendered in the new theme.

---

## Running the App

### Requirements
- Android Studio Hedgehog (2023.1) or newer
- A physical device or emulator running **API 33 or 34** (API 36 emulators have known instability issues with this project)
- A `google-services.json` file in `app/` — get this from your Firebase project console

### Steps
1. Clone or download this repository.
2. Open the `Farming-App` folder in Android Studio.
3. Place your `google-services.json` in the `app/` directory.
4. Let Gradle sync fully.
5. Open **Device Manager**, create/start an emulator (API 33 or 34 recommended).
6. Run the `app` configuration.

> **No Firebase?** You can still run the app. On the login screen, tap "Continue as Guest" to enter offline/guest mode. All UI screens, offline products, demo weather data, and the chatbot will work without any Firebase setup.

### Fixing a Stuck Emulator
If you see `Timed out after 300 seconds waiting for emulator to come online`, run these from PowerShell inside the `scripts/` folder:

```powershell
.\fix-emulator.ps1
.\start-emulator-cursor.ps1 -Avd "YOUR_AVD_NAME"
```

Wait until the Android home screen is fully loaded before running the app again.

---

## Project Structure

```
app/src/main/
├── java/com/project/farmingapp/
│   ├── FarmingApp.kt                    # Application class — applies dark/light theme on startup
│   ├── adapter/                         # RecyclerView adapters for all lists
│   │   ├── EcommerceAdapter.kt
│   │   ├── OfflineEcommerceAdapter.kt
│   │   ├── WeatherAdapter.kt
│   │   ├── CurrentWeatherAdapter.kt
│   │   ├── CartItemsAdapter.kt
│   │   ├── SMPostListAdapter.kt
│   │   ├── DashboardEcomItemAdapter.kt
│   │   └── ...
│   ├── model/                           # Data models and API definitions
│   │   ├── WeatherApi.kt                # Retrofit interface for OpenWeatherMap
│   │   ├── AuthRepository.kt
│   │   └── data/                        # Data classes (Weather, Orders, etc.)
│   ├── utilities/                       # Helpers and extensions
│   │   ├── AuthHelper.kt                # Singleton for auth state (guest + Firebase)
│   │   ├── LocalCartStore.kt            # SharedPreferences-backed cart persistence
│   │   ├── MicroInteractions.kt         # Tap feedback animations
│   │   └── ViewUtils.kt
│   ├── view/                            # All screens (Activities + Fragments)
│   │   ├── auth/                        # LoginActivity, SignupActivity
│   │   ├── dashboard/                   # DashboardActivity (main host), DashboardFragment
│   │   ├── weather/                     # WeatherFragment
│   │   ├── ecommerce/                   # EcommerceFragment, EcommerceItemFragment,
│   │   │                                #   CartFragment, PaymentFragment, MyOrdersFragment,
│   │   │                                #   RazorPayActivity, OfflineEcommerceData
│   │   ├── apmc/                        # ApmcFragment (market prices)
│   │   ├── socialmedia/                 # SocialMediaPostsFragment, SMCreatePostFragment
│   │   ├── articles/                    # ArticleListFragment, FruitsFragment
│   │   ├── chatbot/                     # ChatbotFragment
│   │   ├── user/                        # UserFragment (profile)
│   │   └── introscreen/                 # IntroActivity (onboarding)
│   └── viewmodel/                       # ViewModels (MVVM)
│       ├── AuthViewModel.kt
│       ├── WeatherViewModel.kt
│       ├── EcommViewModel.kt
│       ├── SocialMediaViewModel.kt
│       ├── UserDataViewModel.kt
│       └── ...
└── res/
    ├── layout/                          # XML layouts for all screens and list items
    ├── anim/                            # Slide-in / slide-out fragment transition animations
    ├── drawable/                        # Icons, backgrounds, shapes
    ├── menu/                            # Navigation drawer menu, bottom nav menu, toolbar menus
    └── values/                          # Colors, strings, themes (light + dark)
```

---

## External APIs & Services

| Service | Purpose | Notes |
|---|---|---|
| Firebase Authentication | User login (email + Google) | Required for real accounts; guest mode bypasses this |
| Firebase Firestore | Products, posts, user data, orders, articles | App falls back to offline data if unavailable |
| Firebase Storage | Profile photos, background images, post images | Only used when signed in as a real user |
| OpenWeatherMap | 5-day / 3-hour weather forecast | API key configured in the weather ViewModel; demo fallback if unavailable |
| Google Maps (WebView) | Satellite map tile on home screen | Uses `google_maps_key` from strings |
| Razorpay | Payment processing for orders | Runs in `RazorPayActivity` |
| Google Sign-In | OAuth login | Requires `default_web_client_id` from `google-services.json` |

---

## Acknowledgements & References

This project was built by **[godragun](https://github.com/godragun)** as an academic project. During development, the following open-source GitHub repositories were studied for guidance on architecture patterns, Firebase integration, and UI structure. No code was directly copied — these projects served as learning references.

| Repository | Author | What I learned from it |
|---|---|---|
| [farming-app (hetsuthar028)](https://github.com/hetsuthar028) | hetsuthar028 | MVVM structure, Firebase Firestore integration patterns, e-commerce flow |
| [farming-app (sirvi3014)](https://github.com/sirvi3014) | sirvi3014 | UI layout approaches, navigation drawer setup, weather API integration |

All new features in this repository — the **Field Survey offline module** (Room database, MediaRecorder, FileProvider, Firestore sync), the **light mode crash fix**, the **chatbot FAB visibility logic**, and this documentation — were designed and implemented independently.

---

## Known Quirks & Notes

- **Currency & region** — all prices are in LKR (Sri Lankan Rupees). Location defaults to Colombo (6.9271°N, 79.8612°E) when GPS is unavailable.
- **Location permission** — the app requests `ACCESS_FINE_LOCATION` at runtime. If denied, weather data defaults to Colombo. The app never blocks functionality because of a denied location permission.
- **First-run intro** — shown until `intro_schema_version` in `MyPrefs` matches `IntroActivity.INTRO_SCHEMA_VERSION`. To force the tour again, clear app data or remove/adjust that int (and optionally `firstTime`).
- **Dashboard satellite preview** — **Leaflet** runs inside a `WebView` and loads Leaflet JS/CSS from local app assets (no CDN). Tile images are fetched via a `/tiles/…` proxy handled by `LeafletMapWebViewHelper` (OkHttp + caching) for consistent behavior across devices.
- **OpenWeather JSON** — forecast `main.humidity`, `temp_min`, and `temp_max` are numeric in JSON; the Kotlin model must use numeric types so Gson can parse responses (see `WeatherMain.kt`).
- **Google Sign-In** — requires a valid `google-services.json` and a correctly configured OAuth client ID in Firebase Console. Without it, Google Sign-In will fail at runtime (email login and guest mode will still work).
- **API 36 emulators** — known to be unstable with this project. Use API 33 or 34.
- **Razorpay** — a valid Razorpay key must be configured for payment to complete. The pre-payment and cart flows work without it.

---

---

# Viva Preparation Guide — Agri Lanka

> This section is written for a student presenting this project in a viva or technical interview. Read it like you are explaining your own work to an examiner. Everything below is tied directly to code and features that exist in this app.

---

## 1. What Problem Does This App Solve?

### The Problem (say this first in your viva)

Farmers in Sri Lanka — and in most developing countries — face four daily challenges:

1. **No easy access to weather information** relevant to their field location.
2. **No single platform** to audit local input pricing (fertilizer, pesticides, machinery) in a consistent, repeatable way.
3. **Isolation** — research assistants working across districts cannot easily share observations or get rapid peer feedback in the field.
4. **Unreliable internet** — rural areas have poor or no connectivity, so apps that require constant internet are useless.

### How This App Solves It

Agri Lanka is a **single Android application** that addresses all four problems:

| Problem | Solution in the App |
|---|---|
| No weather access | Live GPS-based weather from OpenWeatherMap, with offline fallback data |
| No marketplace | E-commerce module for fertilizers, pesticides, machinery with offline demo products |
| Isolation | Community social feed, Agri Chatbot, agriculture articles |
| Poor internet | Offline-first design — Room database, SharedPreferences cache, demo data fallbacks |

**Examiner tip:** Say "The core design decision was offline-first. Every feature has a fallback so a farmer with no internet can still use the app."

---

## 2. How the Architecture Works (MVVM + Data Flow)

### What is MVVM?

MVVM stands for **Model — View — ViewModel**. It is an architectural pattern that separates the app into three layers so each layer has one job and they don't directly depend on each other.

```
USER INTERACTION
      ↓
   VIEW (Fragment / Activity)
      ↓  observes LiveData      ↑ calls functions
   VIEWMODEL
      ↓  requests data          ↑ returns data
   MODEL (Repository → Firebase / Room / Retrofit)
```

### How It Works in This App — Concrete Example

Take the **Weather feature** as an example:

1. **Model layer** — `WeatherApi.kt` (Retrofit interface) defines the HTTP call to OpenWeatherMap. `WeatherViewModel` holds the `WeatherRepository` which fetches data.

2. **ViewModel layer** — `WeatherViewModel.kt` holds a `MutableLiveData<WeatherRootList>` called `newDataTrial`. It exposes this as an immutable `LiveData` to the View. It also holds coordinates via `getCoordinates()` / `updateCoordinates()`.

3. **View layer** — `WeatherFragment.kt` calls `viewModel.newDataTrial.observe(viewLifecycleOwner) { weatherData -> ... }`. Whenever the ViewModel posts new weather data, the Fragment automatically updates the UI — temperature, humidity, wind, icon.

**Why this is good:** The Fragment does NOT make the network call. If the screen rotates, the ViewModel survives (it is scoped to the Activity), so the data is not lost and no extra network request is made.

### Key Android Architecture Concepts

| Concept | What it means | Where in this app |
|---|---|---|
| `ViewModel` | Survives screen rotation. Holds UI data. | `WeatherViewModel`, `EcommViewModel`, `FieldSurveyViewModel`, etc. |
| `LiveData` | Observable data holder. Automatically updates the UI. | `newDataTrial`, `ecommLiveData`, `allSurveys`, `userliveData` |
| `Repository` | Single source of truth for data. Hides whether data comes from network or local DB. | `FieldSurveyRepository`, `AuthRepository` |
| `Fragment` | A reusable UI screen that lives inside an Activity. | Every screen is a Fragment inside `DashboardActivity` |
| `ViewBinding` | Type-safe access to XML layout views without `findViewById`. | Used in every Fragment |

### Single Activity Pattern

This app uses **one Activity (`DashboardActivity`) as a host** and every screen is a `Fragment` swapped into a `frame_layout` container. This is a recommended modern Android pattern because:

- Fragments share the same ViewModel scope (the Activity), so data flows between screens without complicated callbacks.
- The back stack is managed by the `FragmentManager` — pressing back naturally restores the previous screen.
- Navigation animations are applied at the transaction level.

---

## 3. How Each Major Feature Works Internally

### Authentication

**Flow:**
```
LoginActivity → FirebaseAuth.signInWithEmailAndPassword()
                        OR
             → GoogleSignIn → FirebaseAuth.signInWithCredential()
                        OR
             → "Guest" button → AuthHelper.enterGuestMode()
                        ↓
             DashboardActivity (main app)
```

- `AuthViewModel` holds the business logic (login, signup, Google sign-in).
- `AuthHelper` is a Kotlin `object` (singleton) that holds the guest mode flag in memory AND in `SharedPreferences` so it survives app restarts.
- On every screen that needs a user identity (social feed, profile, orders), the code calls `AuthHelper.requireEmail()` — it returns either the real Firebase email or `guest@offline.local`.

**Key point for examiner:** "I used a singleton `AuthHelper` class to centralise all authentication state so no Fragment needs to directly import Firebase Auth — it just asks `AuthHelper`."

---

### Weather Feature

**Flow:**
```
DashboardActivity (GPS location)
        ↓ updateCoordinates([lat, lon, city])
WeatherViewModel (LiveData<List<String>> coordinates)
        ↓ updateNewData() → Retrofit API call
OpenWeatherMap API → WeatherRootList (data class)
        ↓ postValue(weatherData)
WeatherFragment / DashboardFragment → UI updates
```

- The GPS location is fetched in `DashboardActivity` using `LocationManager` + `FusedLocationApi`.
- Location is published via `WeatherViewModel.updateCoordinates()` — any Fragment observing this ViewModel automatically gets the coordinates.
- The actual HTTP request uses **Retrofit 2** with a **Gson converter** to parse JSON directly into Kotlin data classes (`WeatherRootList`, `WeatherList`, `Main`, `Wind`, etc.).
- If GPS is unavailable or permission is denied, the app defaults to **Colombo, Sri Lanka (6.9271°N, 79.8612°E)** — a hardcoded fallback.
- If the API call fails entirely, the ViewModel's LiveData is never updated, so the UI shows the last known value or the initial placeholder.

---

### E-Commerce Feature

**Flow:**
```
Firestore collection "products"
        ↓ EcommViewModel.loadAllEcommItems()
        ↓ or getSpecificCategoryItems("fertilizer")
EcommerceFragment → RecyclerView (EcommerceAdapter)
        ↓ user taps product
EcommerceItemFragment (product details + add to cart)
        ↓ add to cart
LocalCartStore (SharedPreferences) ← stored offline
        ↓ checkout
PrePaymentFragment → RazorPayActivity
```

- Products are stored in **Firebase Firestore** and fetched as `DocumentSnapshot` objects.
- If Firestore returns empty (no internet or no data), `OfflineEcommerceData` provides a hardcoded list of demo products so the shopping flow never breaks.
- The cart is stored in **SharedPreferences** (`LocalCartStore`) — this means the cart persists even if the app is killed, with no Firebase needed.
- Category filtering is done by querying Firestore with a `.whereEqualTo("type", category)` clause.
- Search filtering is done **client-side** — the full list is loaded and filtered in memory using Kotlin's `.filter { }` function.

**Key point for examiner:** "I separated online and offline adapters — `EcommerceAdapter` for Firestore data and `OfflineEcommerceAdapter` for demo data — so both render identically to the user."

---

### Field Survey Module (Offline-First)

This is the most technically complete offline feature in the app.

**Flow:**
```
SurveyFormFragment
  → GPS location (LocationManager)
  → Camera photo (FileProvider + TakePicture contract)
  → Voice recording (MediaRecorder → .mp4 file)
  → User fills form fields
        ↓
FieldSurveyViewModel.saveSurvey(survey)
        ↓ coroutine (suspend function)
FieldSurveyDao.insert(survey) → Room SQLite database
        ↓
LiveData<List<FieldSurvey>> auto-updates
SavedSurveysFragment → shows updated list
        ↓ user taps "Sync"
FieldSurveyRepository.syncAll()
  → checks internet (ConnectivityManager)
  → if online: Firestore.collection("field_surveys").set(doc)
  → marks isSynced = true in Room
```

- **Room** is Android's official SQLite ORM (Object Relational Mapper). It maps a Kotlin data class (`FieldSurvey`) to a database table via the `@Entity` annotation.
- **DAO (Data Access Object)** — `FieldSurveyDao` provides `@Insert`, `@Update`, `@Delete`, and `@Query` functions. Room generates all the SQL at compile time.
- `getAllSurveys()` returns a `LiveData<List<FieldSurvey>>` — Room automatically re-queries the database whenever data changes and posts the new list to LiveData, so the UI updates without any manual refresh.
- The `isSynced` boolean flag in the `FieldSurvey` entity tracks whether this record has been uploaded to Firestore.
- `syncAll()` only uploads records where `isSynced = false`, then marks them synced. If there is no internet, a `SyncResult.NoInternet` is returned and the data stays safe in Room.

---

### Community Social Feed

```
SMCreatePostFragment
  → image picked from gallery (Intent.ACTION_PICK)
  → FirebaseStorage.upload(image) → returns download URL
  → Firestore.collection("posts").add({ text, imageUrl, email, timestamp })
        ↓
SocialMediaPostsFragment
  → Firestore.collection("posts").get() → list of posts
  → SMPostListAdapter → RecyclerView
```

- Post drafts (text only) are saved to `LocalPostDraftStore` (SharedPreferences) so if the user closes the app while writing, the draft is restored.
- Image uploads happen asynchronously — a progress bar is shown during upload.
- Each post is a Firestore document with fields: author email, text, image URL, timestamp.

---

### Agri Chatbot

The chatbot is **fully offline** — it requires no API, no internet, no server.

```
User types message
        ↓
ChatbotFragment.generateResponse(prompt)
  → prompt.lowercase()
  → when { text.contains("rice") → ... }
        ↓
Returns hardcoded string response
  → displayed as chat bubble (TextView added to LinearLayout)
```

- It is a **rule-based system** — `when` (switch) on keywords.
- It maintains context using `SharedPreferences` — the last topic (`"last_topic"` key) is stored and used to answer follow-up questions like "tell me more".
- The chat UI is built dynamically — each message is a `TextView` added programmatically to a `LinearLayout` inside a `ScrollView`.

**Key point for examiner:** "I chose a rule-based approach because field researchers repeatedly need consistent, standardised classifications (symptom labels, capture checklists, category selection). A server-dependent ML approach would be risky in low-connectivity environments."

---

## 4. How Offline Storage Is Handled

The app uses **three different offline storage mechanisms** depending on what kind of data needs to be stored:

| Storage Type | Technology | Used For |
|---|---|---|
| Structured data (surveys) | **Room** (SQLite ORM) | Field surveys — full CRUD, sync status, queries |
| Simple key-value data | **SharedPreferences** | Cart items, auth state, dark mode, chatbot last topic, post drafts, first-run flag |
| Files (images, audio) | **Internal/External files** | Survey photos (`.jpg`), voice notes (`.mp4`) via `getExternalFilesDir()` |

### Room Database — How It Works Step by Step

1. **Define the entity** — `@Entity(tableName = "field_surveys") data class FieldSurvey(...)` — each field becomes a database column.
2. **Define the DAO** — `interface FieldSurveyDao` with `@Insert`, `@Query`, etc. — Room generates the actual SQL implementation at compile time using `kapt`.
3. **Build the database** — `Room.databaseBuilder(context, FieldSurveyDatabase::class.java, "field_survey_db").build()` — this creates the SQLite file on the device.
4. **Observe with LiveData** — `dao.getAllSurveys()` returns `LiveData<List<FieldSurvey>>`. Whenever any row in the table changes, Room re-runs the query and delivers the fresh list to all active observers automatically.
5. **Write with coroutines** — `insert`, `update`, `delete` are `suspend` functions, called inside `viewModelScope.launch { }` so they run on a background thread without blocking the UI.

### Why Not Just Use SharedPreferences for Everything?

SharedPreferences stores simple key-value pairs (String, Boolean, Int). It cannot:
- Store complex structured data (like a list of surveys with 14 fields each)
- Query data (e.g. "give me all unsynced surveys")
- Observe changes automatically

Room (SQLite) handles all of these, which is why it is used for the Field Survey module.

---

## 5. How Data Synchronization Works

### The Sync Strategy — "Offline First, Sync When Online"

This app follows an **offline-first** strategy:

1. **Write locally first** — every survey is saved to Room immediately when the user taps "Save Offline". The user never waits for a network response.
2. **Mark as unsynced** — the `isSynced = false` flag is set.
3. **Sync when online** — when the user taps "Submit & Sync" or "Sync All", the app:
   - Checks internet via `ConnectivityManager.getNetworkCapabilities()`
   - If offline: returns `SyncResult.NoInternet` — user is informed, data stays safe
   - If online: uploads each unsynced survey to Firestore, then marks `isSynced = true` in Room

```
No Internet?          Yes Internet?
     ↓                      ↓
Show warning        Upload to Firestore
Keep in Room        Mark isSynced = true in Room
                    Update LiveData → UI reflects "Synced"
```

### Conflict Handling
Each device generates surveys independently, so **local Room IDs are not safe across devices**. To prevent multi-device overwrites, every survey also has a globally unique `remoteId` (UUID) that is used as the Firestore document ID. This enables multiple devices to submit without overwriting each other.

Repeated sync of the same local record is still idempotent: the same `remoteId` is reused.

---

## How the Map Works

The app provides a satellite map in two places: a preview on the Dashboard and a full experience in the APMC screen.

### Dashboard map preview (Leaflet WebView)

- The preview is a `WebView` that loads `app/src/main/assets/leaflet_map.html`.
- Leaflet JS/CSS are bundled locally under `app/src/main/assets/leaflet/` (no external CDN dependency).
- Tile requests are routed through `https://appassets.androidplatform.net/tiles/{z}/{y}/{x}`.
  - `LeafletMapWebViewHelper` intercepts `/tiles/…` and fetches real tiles with OkHttp.
  - OkHttp has disk + memory caching so scroll/zoom stays smooth.

### APMC (full-screen satellite + overlays)

`ApmcFragment` supports two implementations:

- **Google Maps (preferred)** when a valid `google_maps_key` exists:
  - Map type: `SATELLITE`
  - District boundary polygon is drawn with a green stroke + light fill.
  - Crop suitability zones are drawn as circles (HIGH/MEDIUM/LOW) with **stroke + fill**.

- **Leaflet WebView fallback** when no Google Maps key is present:
  - Uses the same `leaflet_map.html` and tile proxy.
  - Boundary and crop zones are rendered by calling JavaScript functions:
    - `showDistrictBoundary(boundaryJson)`
    - `showCropZones(zonesJson)`
  - Crop zones do **not** clear the boundary; only circles are replaced so the border stays visible.

## How Weather Works

- Weather data is fetched from OpenWeatherMap using Retrofit (`WeatherApi`).
- `WeatherViewModel` holds current coordinates (lat/lon/city) and exposes weather via `newDataTrial`.
- Requests are **debounced** and any in-flight call is cancelled when a new request is made, to prevent request storms and UI jank.
- If network fails, a demo fallback dataset is shown so the UI never goes blank.

## How Field Surveys Work (Offline + Sync)

Field surveys are designed as an **offline-first workflow**.

### Create a survey (offline)

In `SurveyFormFragment` the farmer can:
- Capture a **photo** (camera + FileProvider)
- Record a **voice note** (MediaRecorder → `.mp4`)
- Save agronomy fields (crop type, soil condition, irrigation type, notes, yield, GPS coords)

The survey is stored locally in **Room** as a `FieldSurvey` row.

### View and sync surveys

- `SavedSurveysFragment` lists local surveys using LiveData from Room.
- `SurveyDetailFragment` shows full detail; it loads Room data on a background dispatcher and plays audio using `prepareAsync()` to avoid UI freezes.

### Sync pipeline (multi-device safe)

`FieldSurveyRepository.syncAll()`:

1. Checks internet availability.
2. Uploads media to **Supabase Storage** (if local paths exist and URLs are empty).
3. Uploads the survey document to Firestore collection `field_surveys` using the survey’s **`remoteId`** as the document id.
4. Marks the local record as `isSynced = true` and persists uploaded media URLs locally.

This supports **multiple devices submitting surveys** without overwriting each other.

### Real-Time vs On-Demand Sync

| Feature | Sync Type | Why |
|---|---|---|
| Firestore data (products, posts) | Real-time (Firestore listener) | Data changes on the server need to show immediately |
| Field surveys | On-demand (user taps "Sync") | Surveys are created offline; no need for real-time |
| Weather | On-demand (every time the screen opens) | Weather changes over time but doesn't need real-time streaming |
| Cart | Never synced | Cart is a local concern — only uploaded when an order is placed |

---

## 6. What Technologies Are Used and Why

### Kotlin
- Official language for Android development since 2017.
- **Why over Java:** Null safety (`?` / `!!` operators), extension functions, coroutines (for async code), data classes (concise model definitions), `when` expressions.
- **In this app:** Every file is Kotlin. Coroutines are used for all database operations in the Field Survey module.

### Firebase Firestore
- A **NoSQL cloud database** that stores data as documents inside collections.
- **Why:** Real-time sync, no backend server needed, scales automatically, integrates directly with Firebase Auth.
- **In this app:** Used for products, posts, user profiles, orders, articles, and field survey sync.
- **Document structure example:** `users/{email}` → `{ name, city, bio, profileImage, posts: [] }`

### Firebase Authentication
- **Why:** Handles all security (password hashing, token management, OAuth) without building a backend.
- **In this app:** Supports email/password and Google Sign-In. `AuthHelper` wraps it for clean access throughout the app.

### Firebase Storage
- Object storage for binary files (images, videos).
- **Why:** Images uploaded by users (profile photos, post images) cannot go into Firestore (size limit). Storage provides a URL that Firestore can reference.

### Retrofit 2
- HTTP client library for Android.
- **Why:** Converts a Kotlin interface with annotations into a full HTTP client. Handles JSON parsing via Gson automatically.
- **In this app:** `WeatherApi.kt` defines `@GET("forecast")` with query parameters. Retrofit generates the actual implementation.

### Room (SQLite ORM)
- Official Android persistence library that wraps SQLite.
- **Why:** Type-safe database access with compile-time query validation, LiveData integration, coroutine support.
- **In this app:** Field Survey module — stores all survey data locally before syncing to Firestore.

### LiveData
- Lifecycle-aware observable data holder from Android Jetpack.
- **Why:** Automatically stops delivering data when the Fragment is not visible (prevents memory leaks and crashes on null views). Resumes delivery when the Fragment becomes active again.
- **In this app:** Used in every ViewModel to communicate data changes to the UI.

### Glide
- Image loading and caching library.
- **Why:** Handles downloading images from URLs asynchronously, caches them to disk, handles placeholder/error images, and automatically cancels requests when the view is recycled.
- **In this app:** Used to load product images, profile photos, weather icons from URLs.

### ViewBinding / DataBinding
- **ViewBinding** generates a type-safe binding class for each XML layout.
- **Why:** Eliminates `findViewById` calls and provides null-safe, type-safe access to every view.
- **In this app:** Every Fragment and Activity uses ViewBinding (`_binding = FragmentXyzBinding.inflate(...)`).

### MediaRecorder
- Android's built-in API for recording audio.
- **Why:** No external library needed. Supports `.mp4` / `.aac` output. Runs in-process.
- **In this app:** Used in the Field Survey form to record voice notes for crop conditions.

### FileProvider
- Android security component for sharing file URIs between apps.
- **Why:** On Android 7.0+, you cannot pass raw `file://` URIs to the camera app — it will crash. FileProvider converts local file paths to `content://` URIs that the camera accepts.
- **In this app:** Used in the Field Survey's photo capture to pass the output file URI to the camera intent.

---

## 7. What to Say to the Examiner

### Opening Statement (memorise this)

> "This project is a **Comprehensive Data Collection & Environmental Auditing Suite** for field researchers and research assistants conducting fieldwork. The core design principle is **offline‑first reliability**: the system must remain operational in low‑connectivity environments, capture high‑fidelity evidence (structured forms + GPS + photo + audio), and synchronise to a central dataset when connectivity returns. Technically, it uses **MVVM** with Kotlin, **Room** for local persistence, and **Firebase (Auth + Firestore + Storage)** as the cloud backend, with Retrofit for environmental context feeds (weather). I’ll explain the key technical decisions and why each module contributes to research methodology and data integrity."

---

### Likely Examiner Questions and How to Answer Them

**Q: Why did you use MVVM?**
> "MVVM enforces a clear separation of concerns: the View (Activity/Fragment) renders UI, the ViewModel owns state and orchestrates workflows, and the Model/Repository layer abstracts data sources (Room, Firestore, Retrofit). This is important in field research because the UI must remain responsive while background work (I/O, uploads, sync) continues safely. ViewModels also survive configuration changes, so rotating the device or navigating between screens does not restart data collection flows or duplicate network calls."

**Q: What is LiveData and why did you use it?**
> "LiveData is a lifecycle‑aware observable data holder. The ViewModel exposes immutable LiveData streams, and Fragments observe them to update UI reactively. Lifecycle awareness matters in field contexts: if the app is backgrounded during a capture or sync, LiveData prevents updates from targeting a destroyed view, reducing crashes and preventing partial UI states. When the screen becomes active again, it immediately receives the most recent state."

**Q: How does offline work in your app?**
> "Offline operation is layered:
>
> 1) **SharedPreferences** for lightweight state (authentication mode, UI settings, cached selections).
> 2) **Room (SQLite)** for structured research records in the Field Survey module — each submission is committed locally first, including pointers to evidence files, then synchronised later.
> 3) **Graceful degradation** for context feeds: if Firestore or the weather endpoint is unreachable, the UI continues with cached or fallback datasets so fieldwork is never blocked by connectivity."

**Q: What is Room and how is it different from SharedPreferences?**
> "SharedPreferences stores simple key‑value pairs (Booleans, Strings, Ints) and is not suitable for relational queries or structured datasets. Room is an ORM over SQLite that provides:
>
> - Schema‑defined entities (`@Entity`) for structured research records
> - Queryable DAOs (e.g., retrieve all unsynced submissions)
> - LiveData integration for reactive UI
> - Coroutine support so database I/O runs off the main thread
>
> This makes Room appropriate for offline‑first field collection where records must be consistent, queryable, and durable."

**Q: How does data sync work?**
> "The suite uses an **offline‑first, append‑only synchronisation strategy**:
>
> - Each field submission is written to Room immediately with `isSynced = false` so the researcher never waits on the network.
> - Evidence files (photo/audio) are captured locally and referenced by path.
> - When connectivity is available, sync uploads media (Supabase Storage where configured) and writes a metadata document to Firestore, then marks the local record as synced.
> - Multi‑device safety is handled by a stable, globally unique `remoteId` (UUID) used as the Firestore document id to prevent cross‑device overwrites.
>
> This guarantees that a research assistant can complete audits in remote locations and synchronise later without data loss."

**Q: What is Firebase Firestore?**
> "Firestore is a cloud NoSQL document database. It stores data in collections and documents (JSON‑like structures) and supports real‑time listeners. In this project it functions as the **central research dataset**:
>
> - Field survey submissions are synchronised as documents (linked to evidence URLs).
> - Auxiliary datasets (catalog items, observation logs) are stored as documents for consistent querying and sharing across devices.
> - User identity metadata supports provenance and accountability in a research workflow."

**Q: Why did you choose Firebase over a custom backend?**
> "Building a custom REST API backend requires a server, deployment, security setup, and maintenance. Firebase provides all of this out of the box — authentication, database, file storage, and real-time sync. For a prototype or academic project, this lets me focus on the Android app rather than backend infrastructure. In production, I could migrate to a custom backend gradually."

**Q: What is Retrofit?**
> "Retrofit is an HTTP client library. I define typed API interfaces (e.g., `@GET("forecast")`) and Retrofit generates the client implementation. Responses are parsed into Kotlin data classes using Gson. In this suite, Retrofit provides environmental context (weather) that can be attached to field submissions to improve the analytical value of the dataset."

**Q: How did you handle camera and microphone in the survey?**
> "For evidence capture I used Android’s modern, permission‑safe APIs:
>
> - **Camera**: `ActivityResultContracts.TakePicture()` writes to an app‑owned file. A `FileProvider` issues a `content://` URI so the camera app can write securely (required on Android 7.0+).
> - **Audio**: `MediaRecorder` captures voice notes as `.mp4` files for consistent storage and later upload.
>
> This design supports traceable, high‑fidelity field evidence alongside structured form data."

**Q: What is the single-Activity pattern?**
> "All screens in this app are Fragments hosted inside one Activity — `DashboardActivity`. The Activity has a `FrameLayout` container and each screen is a Fragment that replaces the previous one. This pattern is recommended by Google because Fragments are lighter than Activities, they can share ViewModels scoped to the Activity, and navigation (back stack) is managed cleanly by the FragmentManager."

**Q: How does authentication work?**
> "There are three access modes:
>
> - **Email/password** via Firebase Auth
> - **Google Sign‑In** via Firebase credentials
> - **Guest mode** for offline demonstration or constrained deployments (controlled by `AuthHelper` and persisted in SharedPreferences)
>
> Identity is used for **data provenance**: every submission is attributable to a specific research assistant account (or a guest identity in demo mode), supporting accountability and audit trails."

**Q: How does the “Field Classification Assistant” work?**
> "The Field Classification Assistant is an offline, rule‑based triage component. It standardises terminology and provides consistent classification guidance (e.g., symptom patterns, disease categories, recommended capture fields) before the research assistant logs a formal record. Implementation‑wise, it uses a keyword/rule matching pipeline (`when` logic) and stores lightweight conversational context in SharedPreferences to support follow‑ups. It is intentionally offline to remain available in remote field locations and to ensure consistent language across data collectors."

**Q: Why does the suite include an “Agricultural Input Supply Catalog & Cost Estimator” (shopping/products module)?**
> "Fieldwork often requires capturing **economic baselines** alongside biophysical observations. The input catalog module is repurposed as a **supply‑chain auditing instrument**: research assistants can document prevailing prices for fertilisers, pesticides, and machinery rentals in each district. This contextual metadata supports later analysis (e.g., whether input affordability correlates with observed plant health outcomes or yield estimates). Technically, the catalog is a structured dataset (Firestore when available; offline fallback data when not) rendered via RecyclerView adapters, allowing consistent data capture even under weak connectivity."

**Q: Why include “Economic Context Auditing” (market prices / APMC module)?**
> "Market dynamics can strongly influence local practices and the interpretation of yield and cost data. The APMC module provides an **economic reference layer** so investigators can cross‑reference field observations against prevailing commodity price signals. In research terms, this improves dataset interpretability by separating agronomic effects from economic incentives. The app treats this as a context feed rather than a consumer feature."

**Q: Why include a “Real‑Time Field Observation & Anomaly Log” (social feed)?**
> "This module functions as a decentralised **anomaly reporting channel** for research teams operating across districts. It supports rapid dissemination of unexpected observations (e.g., emerging pest clusters, disease outbreaks, flood damage) and helps standardise incident reporting. The design is intentionally lightweight (text + optional media), with asynchronous uploads and local draft persistence to reduce data loss in the field."

**Q: Why include satellite maps and geospatial overlays?**
> "Spatial context is essential for field research. The satellite view supports **site verification**, helps confirm district boundaries, and enables consistent visual context during audits. The APMC screen overlays boundaries and suitability zones to support repeatable site selection and to reduce ambiguity when multiple investigators work in adjacent regions. The implementation includes both Google Maps (when a key is configured) and a WebView‑Leaflet fallback to preserve functionality across devices and deployment environments."

**Q: Why include weather if the core task is data collection?**
> "Weather is a primary confounder in field observations. Attaching a consistent environmental context feed (temperature, humidity, wind) improves analytical value and reduces retrospective ambiguity during data cleaning. In the implementation, weather requests are debounced and have offline fallback data so that the UI remains responsive and fieldwork is not blocked by transient network failures."

**Q: Why include articles/knowledge content in a research suite?**
> "In fieldwork, classification consistency matters. The articles provide a **standard reference baseline** for terminology, disease indicators, and process guidance so that multiple data collectors use the same language. This reduces inter‑rater variance and improves label quality before records are finalised in the Field Survey module. The article detail screen also includes a market‑trend visual to support context-aware interpretation."

**Q: Why include user profiles in an academic data collection workflow?**
> "User identity supports **data provenance and auditor accountability**. In a research context, each submission should be attributable to a verified research assistant to maintain chain‑of‑custody for evidence (photos/audio) and to support later audit/review. The suite uses Firebase Auth when available, and a controlled guest mode for demonstrations."

**Q: The brief requires a field data collection app. How do you justify additional modules under Rule 7?**
> "Each module is positioned as a **research‑value component**: it either improves data quality (classification assistant, reference content), improves interpretability (weather + market context), or improves operational reliability (offline fallbacks, observation log, geospatial verification). Features that did not clearly add research value were deliberately removed from the codebase to keep scope aligned with the assignment brief."

**Q: How do you handle dark and light mode?**
> "The user toggles dark/light mode from the navigation drawer. The choice is saved to SharedPreferences. In the `FarmingApp` Application class — which runs before any Activity — I read this preference and call `AppCompatDelegate.setDefaultNightMode()`. This applies the theme before the first Activity starts. When the mode is toggled, Android recreates the Activity to reload themed resources. I fixed a bug where this recreation was navigating the user back to the home screen — I solved it by guarding the initial fragment transaction with `if (savedInstanceState == null)` so recreations restore the saved state instead of resetting navigation."

---

### Things to Emphasise in Your Viva

1. **Offline-first is the core design decision** — not an afterthought.
2. **MVVM keeps the code maintainable** — changing the UI never touches the data layer.
3. **LiveData + ViewModel = no memory leaks, no null pointer crashes from stale views.**
4. **Room + Firestore together** = best of both worlds: local speed + cloud sync.
5. **Every feature has a fallback** — the app never shows a blank screen.
6. **Sri Lanka context** — LKR currency, Colombo default location, crops like tea/rice/coconut show domain understanding.
7. **Security** — FileProvider for camera, Firebase Auth tokens, no hardcoded credentials.

---

### Quick Feature → Technology Map (for last-minute revision)

| Feature | Key Technology |
|---|---|
| Login / Signup | Firebase Authentication, AuthHelper singleton |
| Weather | Retrofit + OpenWeatherMap API, GPS via LocationManager |
| E-Commerce | Firestore, OfflineEcommerceData fallback, SharedPreferences cart |
| Community Feed | Firestore + Firebase Storage, LocalPostDraftStore |
| Field Survey | Room (SQLite), MediaRecorder, FileProvider, Firestore sync |
| Chatbot | Rule-based `when` expression, SharedPreferences for context |
| Market Prices | Firestore |
| Articles | Firestore |
| User Profile | Firestore + Firebase Storage, Glide image loading |
| Dark/Light Mode | SharedPreferences, AppCompatDelegate, FarmingApp Application class |
| Navigation | Single Activity + Fragment back stack, custom slide animations |
| Image loading | Glide (URL images), FileProvider (local camera images) |
