# Hönöleden Ferry Schedule (Android Auto & Phone Companion)

An intuitive, driver-optimized Android application providing real-time and scheduled ferry departures for the **Hönöleden** route (**Hönö Pinan ⇄ Lilla Varholmen**).

---

## 🌟 Features

- **Android Auto First-Class Support**:
  - Implements the official **Android for Cars App Library** (`androidx.car.app`).
  - Distraction-free, glanceable `ListTemplate` UI designed for in-car screens.
  - Shows the **next 3 upcoming rides** from the current time.
  - Large time readout, real-time countdown badge (`om 6 min`, `Avgår nu!`), and crossing time estimate (~12 min).
  - One-tap route direction toggle: `[⇄ Byt riktning]`.
  - Automatic 30-second refresh ticker while driving.
- **Phone Companion App (Jetpack Compose)**:
  - Material 3 design with responsive cards and quick route flipping.
  - Built-in **Utvecklingsläge (Time Simulator)** allowing instant testing of peak morning rush, afternoon rush, midnight rollover, and night hours without modifying device time.
- **Pluggable Data Architecture**:
  - **Phase 1 (Active)**: Embedded 24/7 timetable engine for Hönöleden with full midnight rollover logic and accurate interval frequencies.
  - **Phase 2 (Ready)**: Pre-configured Retrofit client ready to fetch live data from your Next.js backend.

---

## 📁 Project Architecture

```
com.example.ferryschedule/
├── car/
│   ├── FerryCarAppService.kt      # Android Auto service entry point
│   ├── FerrySession.kt            # Session lifecycle manager
│   └── DeparturesScreen.kt        # In-car ListTemplate UI
├── phone/
│   ├── MainActivity.kt            # Mobile activity entry point
│   ├── DeparturesViewModel.kt     # State management & coroutine ticker
│   └── ui/
│       ├── DeparturesPhoneScreen.kt  # Jetpack Compose mobile interface
│       └── theme/Theme.kt         # Nordic maritime Material 3 theme
├── domain/
│   ├── model/
│   │   ├── FerryDeparture.kt      # Domain model (time, countdown, status)
│   │   ├── RouteDirection.kt      # HONO_TO_VARHOLMEN vs VARHOLMEN_TO_HONO
│   │   └── FerryScheduleState.kt  # Observable UI state
│   └── repository/
│       └── FerryRepository.kt     # Repository contract
└── data/
    ├── local/
    │   └── HonoTimetableEngine.kt # 24/7 timetable calculation engine
    ├── repository/
    │   └── FerryRepositoryImpl.kt # Repository implementation with offline fallback
    └── remote/
        ├── NextJsDto.kt           # DTO schemas for Next.js API
        └── NextJsApiClient.kt     # Retrofit client for Next.js backend
```

---

## 🚗 Testing on Android Auto (Desktop Head Unit / DHU)

To test the in-car display on your computer:

1. **Install DHU in Android Studio**:
   - Open Android Studio ➔ **Tools** ➔ **SDK Manager** ➔ **SDK Tools** tab.
   - Check **Android Auto Desktop Head Unit Emulator** and click Apply.
   - The tool will be located at:
     ```
     %LOCALAPPDATA%\Android\Sdk\extras\google\auto\desktop-head-unit.exe
     ```

2. **Enable Android Auto Developer Mode on your Phone**:
   - On your Android phone, go to **Settings** ➔ **Apps** (or search "Android Auto").
   - Open Android Auto settings.
   - Scroll to the bottom and tap **Version** 10 times until Developer Mode is unlocked.
   - Tap the three dots (top right) ➔ select **Start head unit server**.

3. **Port Forward & Launch**:
   - Connect your phone via USB with USB debugging enabled.
   - In PowerShell, run:
     ```powershell
     adb forward tcp:5277 tcp:5277
     cd "$env:LOCALAPPDATA\Android\Sdk\extras\google\auto"
     .\desktop-head-unit.exe
     ```
   - The Android Auto window will pop up on your PC, and **Hönöleden** will appear in the app list!

---

## 📱 Testing on Phone / Standard Emulator

1. Open the project folder in **Android Studio**.
2. Select a connected phone or Android Virtual Device (AVD).
3. Click **Run** (`Shift + F10`).
4. The phone app launches with the next 3 departures. You can use the bottom chips (e.g. *07:15*, *23:55*) to test different times immediately.

---

## 🌐 Connecting your Next.js Backend (Phase 2)

When your Next.js server is ready, update `NextJsApiClient.kt`:

1. Set `BASE_URL`:
   ```kotlin
   // For local dev on Android Emulator:
   var BASE_URL = "http://10.0.2.2:3000/"

   // For deployed server:
   var BASE_URL = "https://your-nextjs-app.vercel.app/"
   ```
2. Ensure your Next.js API route returns the following JSON structure at `GET /api/ferry/departures?direction=HONO_LV`:
   ```json
   {
     "direction": "HONO_LV",
     "timestamp": "2026-09-30T22:30:00Z",
     "departures": [
       { "time": "22:40", "minutesUntil": 10, "status": "I tid", "isEstimated": false },
       { "time": "23:00", "minutesUntil": 30, "status": "I tid", "isEstimated": false },
       { "time": "23:30", "minutesUntil": 60, "status": "I tid", "isEstimated": false }
     ]
   }
   ```
