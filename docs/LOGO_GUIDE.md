# 🎨 App Logo & Branding Guide

This application is built with a single-source branding architecture, making it extremely easy to replace or customize the logo at any time.

---

## ⚡ Quick Guide: Replacing the Logo in 1 Step

### Method 1: Drop-in Replacement (Zero Code Changes)
1. Save your new logo image as a PNG file.
2. Overwrite the file at:
   ```
   app/src/main/res/drawable/app_logo.png
   ```
3. Done!
   * The in-app logo badge in the top bar and the About dialog will update immediately.
   * On modern Android devices (Android 8.0+ / API 26+), the adaptive launcher icon references this file directly via `res/mipmap-anydpi-v26/ic_launcher.xml` and will update automatically.

---

### Method 2: Regenerating Launcher Icons (Optional)
If you would also like to regenerate all legacy raster launcher mipmaps (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`, and round variants) from your new `app_logo.png`:

Open PowerShell in the project root and run:
```powershell
powershell -ExecutionPolicy Bypass -File scripts/generate_launcher_icons.ps1
```
Or specify a custom image source:
```powershell
powershell -ExecutionPolicy Bypass -File scripts/generate_launcher_icons.ps1 -SourceImage "path/to/my_new_logo.png"
```

---

### Method 3: Code-Level Configuration
All in-app branding settings are centralized in a single file:
[`com.example.ferryschedule.phone.ui.components.AppLogoConfig`](file:///c:/Users/johan/Downloads/ferryschedule/app/src/main/java/com/example/ferryschedule/phone/ui/components/AppLogo.kt):

```kotlin
object AppLogoConfig {
    // Change this to point to any other drawable in res/drawable/
    @DrawableRes
    val LOGO_RES_ID: Int = R.drawable.app_logo

    const val BRAND_NAME: String = "Westcoast Mobility"
    const val APP_TITLE: String = "Färjetidtabell"
    const val APP_VERSION: String = "1.0.0"
}
```

---

## 🧩 Reusable Logo Components

The following Jetpack Compose components are ready to use anywhere in the application:

* **`AppLogoBadge`**:
  A compact emblem badge with rounded corners and subtle border, optimized for the `TopAppBar`. Clicking it opens the About dialog.
* **`AppLogo`**:
  Standard logo composable with configurable `modifier`, `contentScale`, and click listener.
* **`AppLogoCard`**:
  A branded card displaying the logo, company name, and subtitle.
* **`AppAboutDialog`**:
  A modern Material 3 dialog presenting the high-resolution logo, route information, version, and data source credits.
