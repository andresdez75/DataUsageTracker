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
│   ├── NetworkStatsHelper.kt   # Network usage queries (NetworkStatsManager)
│   └── SessionStatsHelper.kt   # App session counts (UsageStatsManager)
├── model/
│   └── Models.kt               # AppUsageEntry, TotalUsageSummary, enums (NetworkType, TimePeriod, SortOrder)
├── ui/
│   ├── MainActivity.kt         # Main screen
│   └── AppUsageAdapter.kt      # RecyclerView adapter
└── util/
    ├── ByteFormatter.kt        # Byte conversion → B/KB/MB/GB
    └── JsonExporter.kt         # JSON export logic

app/src/main/res/
├── drawable/
│   ├── bg_spinner.xml          # Spinner background with dropdown arrow
│   ├── ic_dropdown_arrow.xml   # Dropdown arrow icon
│   ├── ic_system.xml           # Gear icon for "System" entry
│   └── ic_tethering.xml        # Tethering/hotspot icon
├── layout/
│   ├── activity_main.xml       # Main layout
│   ├── item_app_usage.xml      # App list row
│   └── item_spinner.xml        # Custom spinner item layout
├── menu/
│   └── menu_drawer.xml         # Drawer menu items
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
- `totalSessions` — number of times the app was opened (from UsageStatsManager)
- `activeSessions` — sessions lasting more than 5 seconds

### TotalUsageSummary
Aggregate of all apps for the device summary.

### Enums
- `NetworkType`: ALL, MOBILE, WIFI
- `TimePeriod`: TODAY, WEEK (7 days), MONTH (30 days)
- `SortOrder`: USAGE, NAME, SESSIONS, WITH_SESSIONS, ACTIVE_5S

## Main logic — NetworkStatsHelper.kt
- Uses `NetworkStatsManager.querySummary()` to get data from the OS
- Separates foreground/background based on `bucket.state == STATE_FOREGROUND`
- For `NetworkType.ALL`, sums MOBILE + WIFI (the OS has no ALL constant)
- Mobile data may require `subscriberId` from `TelephonyManager`
- App filtering uses `Intent.ACTION_MAIN + CATEGORY_LAUNCHER` to get only apps with a launcher icon (user apps)
- Tethering (UID -5) is listed as its own entry with a dedicated icon
- All other system UIDs are aggregated into a single "System" entry (UID -999)

## Session tracking — SessionStatsHelper.kt
- Uses `UsageStatsManager.queryEvents()` to count FOREGROUND → BACKGROUND transitions
- Counts total sessions (app opened) and active sessions (duration > 5 seconds)
- Helps identify apps that consume data in background without the user ever opening them

## Metrics displayed by the app
- Per app: FG usage, BG usage, total, % background, sessions (total and active > 5s)
- Device summary: total FG, total BG, total, global BG %, app count, date range
- Filters via 3 dropdown spinners: Access (network), Date (period), Order (sort/filter)
- Units: B / KB / MB / GB with 1 decimal place

## Date range logic
- **Today**: from midnight today to now (current day only)
- **7 days**: 7 complete days before today (midnight to midnight, today excluded)
- **30 days**: 30 complete days before today (midnight to midnight, today excluded)
- Display shows the actual inclusive dates (e.g., "From 08/03/2025 to 14/03/2025" for 7 days)

## Important technical decisions
1. No external libraries — only native SDK + AndroidX + Material
2. No backend — fully offline, data from the OS itself
3. If complexity grows → evaluate ViewModel + LiveData
4. Android stores ~4 weeks of history in NetworkStatsManager

## How to build
Open in Android Studio, sync Gradle, and run on an Android 6.0+ device.
The user must grant the usage data access permission before seeing any data.

---

## v1.1.0 Changelog

### New features
1. **English UI** — all user-facing text translated to English
2. **Tethering** — included as its own entry with dedicated icon
3. **System traffic** — all non-listed system UIDs aggregated into a single "System" entry so totals match the device's real usage
4. **Date range** — device summary shows "From DD/MM/YYYY to DD/MM/YYYY"
5. **Sidebar menu** — hamburger/drawer menu with JSON export of current filter data (values in MB)
6. **Toolbar** — grey background with "Data Usage Tracker" title in white, no logo
7. **Dropdown filters** — replaced chips with 3 spinners: Access (All/Mobile/Wi-Fi), Date (Today/7 days/30 days), Order (5 options)
8. **Session tracking** — each app shows "X sessions (Y > 5s)" via UsageStatsManager
9. **Sort/filter options** — Usage ↓, Name A-Z, Sessions ↓, With sessions, Active > 5s

### Special classification rules

- **Tethering is always classified as foreground (FG)**: the OS reports tethering traffic as background because it runs as a system service with no UI. However, tethering is user-initiated traffic (the user explicitly enables the hotspot), so it is reclassified as FG to avoid inflating background metrics. Without this exception, tethering would distort the BG ratio for both the app entry and the device summary.

### JSON export structure

When the filter is **ALL**, the JSON splits data by network type:

```json
{
  "generated_at": "2026-03-15T10:30:00Z",
  "filter": {
    "network_type": "ALL",
    "period": "WEEK",
    "date_from": "2026-03-08",
    "date_to": "2026-03-14"
  },
  "summary": {
    "total_mb": 1234.5,
    "mobile": {
      "fg_total_mb": 500.0,
      "bg_total_mb": 234.5,
      "total_mb": 734.5
    },
    "wifi": {
      "fg_total_mb": 300.0,
      "bg_total_mb": 200.0,
      "total_mb": 500.0
    },
    "bg_ratio_pct": 35.2,
    "app_count": 12
  },
  "apps": [
    {
      "rank": 1,
      "package_name": "com.example.app",
      "app_name": "Example App",
      "mobile": {
        "fg_rx_mb": 60.0,
        "fg_tx_mb": 30.0,
        "fg_total_mb": 90.0,
        "bg_rx_mb": 20.0,
        "bg_tx_mb": 5.0,
        "bg_total_mb": 25.0,
        "total_mb": 115.0
      },
      "wifi": {
        "fg_rx_mb": 40.0,
        "fg_tx_mb": 20.0,
        "fg_total_mb": 60.0,
        "bg_rx_mb": 10.0,
        "bg_tx_mb": 5.0,
        "bg_total_mb": 15.0,
        "total_mb": 75.0
      },
      "total_mb": 190.0,
      "bg_ratio_pct": 21.1
    }
  ]
}
```

When the filter is **MOBILE** or **WIFI**, the JSON is flat (no split):

```json
{
  "generated_at": "2026-03-15T10:30:00Z",
  "filter": {
    "network_type": "MOBILE",
    "period": "WEEK",
    "date_from": "2026-03-08",
    "date_to": "2026-03-14"
  },
  "summary": {
    "total_mb": 734.5,
    "fg_total_mb": 500.0,
    "bg_total_mb": 234.5,
    "bg_ratio_pct": 31.9,
    "app_count": 10
  },
  "apps": [
    {
      "rank": 1,
      "package_name": "com.example.app",
      "app_name": "Example App",
      "fg_rx_mb": 60.0,
      "fg_tx_mb": 30.0,
      "fg_total_mb": 90.0,
      "bg_rx_mb": 20.0,
      "bg_tx_mb": 5.0,
      "bg_total_mb": 25.0,
      "total_mb": 115.0,
      "bg_ratio_pct": 21.7
    }
  ]
}
```
