# Färjetidtabell (Android Auto & Phone Companion)

An intuitive, driver-optimized Android application providing live, real-time ferry departures, queue forecasts, traffic cameras, and visual road status for multiple Swedish ferry routes:
* 🛳️ **Hönöleden** (**Hönö Pinan ⇄ Lilla Varholmen**) • ~12 min (Route 28)
* ⛴️ **Björköleden** (**Björkö Grönevik ⇄ Lilla Varholmen**) • ~6 min (Route 23)
* ⚓ **Svanesundsleden** (**Kolhättan ⇄ Svanesund**) • ~5 min (Route 35)
* 🚤 **Gullmarsleden** (**Finnsbo ⇄ Skår**) • ~10 min (Route 25)

---

## 🌟 Key Features

### 🚗 Android Auto (In-Car Screen)
* **Distraction-Free Glanceability**: Built strictly following Google's Android for Cars Design Guidelines (`androidx.car.app`).
* **Multi-Route Selection (`[Välj led]`)**: Tap `[Välj led]` on the car screen to quickly switch between **Hönöleden**, **Björköleden**, **Svanesundsleden**, or **Gullmarsleden**.
* **Next 3 Departures**: Large departure time display, live countdown badges (`om 4 min`, `Avgår nu!`), route crossing time, and cancellation flags (`[INSTÄLLD]`).
* **Smart Queue Forecast**: Shows live road delay and projected boarding:
  > *Fri väg (0 min kö) ➔ Du hinner med nästa färja!*  
  > *Bilkö: 8 min ➔ Prognos: Du hinner med 2:a färjan*
* **Visual Road Map (`[Vägkarta]`)**: Full dual-lane schematic map on the car display (`PaneTemplate`) dynamically adapting to the selected ferry route with real-time road speeds and status colors.
* **One-Tap Navigation (`[Navigera]`)**: Instantly launches turn-by-turn guidance to the active ferry slip in **Google Maps / Waze** directly on your car screen.
* **Auto-Refresh & Preference Sync**: Re-calculates and refreshes every 30 seconds automatically, and remembers your selected route across phone and car restarts.

### 📱 Phone Companion App (Jetpack Compose)
* **Route Selector Bar**: One-tap filter chips at the top of the screen to switch between any ferry route instantly.
* **Live Departures & Countdown**: Next departure hero card with prominent countdown badge.
* **Trafiköversikt & Vägkarta**: High-resolution rendered road schematic showing both lanes for the active route, live segment speeds (km/h), and pointer to where traffic becomes free-flow (*"HÄR BÖRJAR DET BLI GRÖNT"*).
* **Live Trafikverket CCTV Cameras**: Real-time camera feeds from Väg 155 with live timestamps.
* **Utvecklingsläge (Time Simulator)**: Quick-test chips (*07:15 Morgonrush*, *16:25 Eftermiddag*, *23:55 Midnatt*) to test any time of day instantly.

---

## 📡 Live Data Integrations

* **Trafikverket Ferry Route API (Routes 28, 23, 35, 25)**:
  * Schedules: `https://www.trafikverket.se/api/ferryRouteApi/schedules/?id=<routeId>&date=YYYY-MM-DD`
  * Dynamic harbor filtering and real-time deviations & cancellations.
* **Trafikverket TravelTimeRoute API (County 14)**:
  * Live congestion speeds and delay calculations along Väg 155 (*Bur ➔ Amhult ➔ Hästevik ➔ Hjuvik ➔ Lulles väg ➔ Färjeläget*).
* **Trafikverket Traffic Cameras**:
  * Live CCTV photos of the Route 155 corridor.
* **Offline Fallback Engine**:
  * If offline or when APIs are unreachable, automatically falls back to the high-precision embedded timetable calculation engine.

---

## 📁 Project Architecture

```
com.example.ferryschedule/
├── car/
│   ├── FerryCarAppService.kt         # Android Auto service entry point
│   ├── FerrySession.kt               # Car session lifecycle
│   ├── DeparturesScreen.kt           # In-car ListTemplate (Departures + Queue advice)
│   ├── RouteSelectionCarScreen.kt    # In-car ListTemplate for picking ferry route
│   └── RoadStatusCarScreen.kt        # In-car PaneTemplate (Visual road corridor map)
├── phone/
│   ├── MainActivity.kt               # Companion mobile activity
│   ├── DeparturesViewModel.kt        # State management, camera loader, ticker
│   └── ui/
│       ├── DeparturesPhoneScreen.kt  # Jetpack Compose UI (Route chips, Hero card, map)
│       └── theme/Theme.kt            # Nordic maritime Material 3 theme
├── domain/
│   ├── model/
│   │   ├── FerryDeparture.kt         # Time, countdown text, cancellations, queue
│   │   ├── RouteDirection.kt         # FerryRoute & RouteDirection models
│   │   ├── FerryScheduleState.kt     # Observable UI state
│   │   └── TrafficModels.kt          # Road segments, CongestionLevel, Cameras
│   └── repository/
│       └── FerryRepository.kt        # Repository abstraction
├── data/
│   ├── local/
│   │   ├── HonoTimetableEngine.kt    # 24/7 offline timetable & rollover engine
│   │   └── UserPreferences.kt        # SharedPreferences route persistence
│   ├── remote/
│   │   ├── TrafikverketService.kt    # Live Trafikverket schedules, queues, cameras
│   │   ├── NextJsDto.kt              # DTO schemas for Next.js API
│   │   └── NextJsApiClient.kt        # Retrofit client for Next.js server
│   └── repository/
│       └── FerryRepositoryImpl.kt    # Repository implementation with auto-fallback
└── util/
    └── RoadCorridorBitmapGenerator.kt # Renders dynamic dual-lane road schematic into a Bitmap
```

---

## 🚗 Testing on a Real Car

To enable this app to run on your actual car's Android Auto dashboard:

1. **On your phone**: Open **Settings** ➔ search for **Android Auto**.
2. Scroll to the bottom and tap **Version** **10 times in a row** to unlock Developer Mode.
3. Tap the **3 dots** (top right) ➔ tap **Developer settings** (*Utvecklarinställningar*).
4. Check the box for **"Unknown sources"** (*Okända källor*).
5. Plug your phone into your car's USB port (or connect via wireless Android Auto).
6. Tap the **App Launcher** icon on your car screen — **Hönöleden** will appear in the app grid!

---

## 💻 Testing with Desktop Head Unit (DHU Emulator)

1. Open Android Studio ➔ **Tools** ➔ **SDK Manager** ➔ **SDK Tools** ➔ install **Android Auto Desktop Head Unit Emulator**.
2. On your phone: In Android Auto settings ➔ tap 3 dots ➔ **Start head unit server**.
3. Connect phone via USB with USB debugging enabled.
4. In PowerShell, run:
   ```powershell
   adb forward tcp:5277 tcp:5277
   cd "$env:LOCALAPPDATA\Android\Sdk\extras\google\auto"
   .\desktop-head-unit.exe
   ```
5. The Android Auto window opens on your PC!

---

## 📱 Testing on Phone / Standard Emulator

1. Open the project in **Android Studio**.
2. Select your connected phone or an Android Virtual Device (AVD).
3. Click **Run (▶)** (`Shift + F10`).
4. The app launches with live departures, the rendered road schematic, queue advice, and live CCTV cameras.
