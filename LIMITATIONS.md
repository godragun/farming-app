# FieldGuard AI — limitations

- **Node and sensors are simulated** in the demo (`get_sensor_readings`, on-screen LED/buzzer panel labelled “Simulated node”). There is no physical Agri Lanka IoT hardware in this submission.
- **Scenario images are placeholders** (see `ASSETS.md`). Vision quality depends entirely on the connected model and the frames you provide.
- **No accuracy claim.** Confidence shown in the UI/tools is always labelled as the **model's estimate**, never as measured accuracy.
- **Not field-tested** with real elephant encounters, rural networks, or production Meta Model API SLAs.
- **Alerts** use Android local notifications; delivery can fail without notification permission (Android 13+).
- **Offline queue** retries via WorkManager; success still requires a later working notification path — it does not magically reach neighbours without connectivity.
- **Cleartext** remains enabled for local Muse Glimmer / existing Agri Lanka backends; prefer HTTPS (`https://api.meta.ai/v1`) when using Meta Model API.
- On model/HTTP failure the app **shows the error** and does **not** invent fake AI answers.
