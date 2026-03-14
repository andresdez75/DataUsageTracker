# CLAUDE.md — Data Usage Tracker

## What is this project
Android app that measures mobile data usage differentiating **foreground** (app actively in use) and **background** (app in the background) for each user-installed app.

## Tech stack
- **Language**: Kotlin
- **Min SDK**: 23 (Android 6.0)
- **Architecture**: Empty Views Activity, no external libraries besides AndroidX + Material
- **No backend**: everything is processed locally on the device
- **Build**: Gradle with `.kts`, version catalog (`libs.versions.toml`)

## Project structure
```
app/src/main/java/com/datausage/tracker/
├── data/
│   └── NetworkStatsHelper.kt   # All OS query logic
├── model/
│   └── Models.kt               # AppUsageEntry, TotalUsageSummary, enums
├── ui/
│   ├── MainActivity.kt         # Main screen
│   └── AppUsageAdapter.kt      # RecyclerView adapter
└── util/
    └── ByteFormatter.kt        # Byte conversion → B/KB/MB/GB

app/src/main/res/
├── layout/
│   ├── activity_main.xml       # Main layout
│   └── item_app_usage.xml      # App list row
└── values/
    ├── colors.xml
    ├── strings.xml
    └── themes.xml
```

## Required permissions (AndroidManifest.xml)
```xml
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" tools:ignore="ProtectedPermissions"/>
<uses-permission android:name="android.permission.READ_PHONE_STATE"/>
```
The PACKAGE_USAGE_STATS permission is special — the user grants it manually in Settings → Apps → Special access → Usage data access.

## Key data model

### AppUsageEntry
Represents the usage of ONE app in a given period and network type:
- `fgRxBytes`, `fgTxBytes` — foreground bytes (received/sent)
- `bgRxBytes`, `bgTxBytes` — background bytes
- `fgTotalBytes`, `bgTotalBytes`, `totalBytes` — derived fields
- `bgRatio` — background ratio (0.0–1.0). If > 0.5, highlighted in orange

### TotalUsageSummary
Aggregate of all apps for the device summary.

### Enums
- `NetworkType`: ALL, MOBILE, WIFI
- `TimePeriod`: TODAY, WEEK (7 days), MONTH (30 days)

## Main logic — NetworkStatsHelper.kt
- Uses `NetworkStatsManager.querySummary()` to get data from the OS
- Separates foreground/background based on `bucket.state == STATE_FOREGROUND`
- For `NetworkType.ALL`, sums MOBILE + WIFI (the OS has no ALL constant)
- Mobile data may require `subscriberId` from `TelephonyManager`
- App filtering uses `Intent.ACTION_MAIN + CATEGORY_LAUNCHER` to get only apps with a launcher icon (user apps)

## Metrics displayed by the app
- Per app: FG usage, BG usage, total, % background
- Device summary: total FG, total BG, total, global BG %, app count
- App ranking by total usage (highest to lowest)
- Units: B / KB / MB / GB with 1 decimal place

## Important technical decisions
1. No external libraries — only native SDK + AndroidX + Material
2. No backend — fully offline, data from the OS itself
3. If complexity grows → evaluate ViewModel + LiveData
4. Android stores ~4 weeks of history in NetworkStatsManager

## How to build
Open in Android Studio, sync Gradle, and run on an Android 6.0+ device.
The user must grant the usage data access permission before seeing any data.
