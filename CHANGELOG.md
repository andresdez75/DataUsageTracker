# Changelog

All notable versions of this project are documented here.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [1.1.0] — 2026-03-15

### New features
- **English UI** — all user-facing text translated to English
- **Tethering entry** — tethering traffic shown as its own entry with dedicated icon
- **System traffic** — all non-listed system UIDs aggregated into a single "System" entry so totals match real device usage
- **Date range display** — device summary shows "From DD/MM/YYYY to DD/MM/YYYY"
- **Sidebar menu** — hamburger/drawer menu with JSON export of current filter data (values in MB)
- **Toolbar** — grey background with "Data Usage Tracker" title in white, no logo
- **Dropdown filters** — replaced chips with 3 spinners: Access (All/Mobile/Wi-Fi), Date (Today/7 days/30 days), Order (5 options)
- **Session tracking** — each app shows "X sessions (Y > 5s)" via UsageStatsManager
- **Sort/filter options** — Usage ↓, Name A-Z, Sessions ↓, With sessions, Active > 5s

### Special classification rules
- **Tethering is always classified as foreground (FG)**: the OS reports tethering as background because it runs as a system service with no UI, but it is user-initiated traffic so it is reclassified as FG to avoid inflating background metrics

### JSON export
- Export current filter data as JSON via sidebar menu
- Includes: generation timestamp, active filters, device summary, and per-app breakdown (all values in MB)

---

## [1.0.0] — 2026-03

### First functional version

#### Features
- List of user-installed apps with their icon and name
- Data usage differentiated by **foreground** and **background** for each app
- Aggregated device summary (total FG, total BG, total, global BG %, app count)
- App ranking sorted from highest to lowest total usage
- Network type filter: **All**, **Mobile**, **Wi-Fi**
- Time period filter: **Today**, **7 days**, **30 days**
- Visual indicator (orange) for apps with more than 50% background usage
- Permission request screen with redirect to Settings

#### Technical
- Native query to Android OS `NetworkStatsManager`
- User app filtering via `Intent.ACTION_MAIN + CATEGORY_LAUNCHER`
- `subscriberId` support for mobile data on devices that require it
- No backend — 100% offline, data from the device itself
- No external libraries — only AndroidX + Material
- Dark mode support via `values-night/colors.xml`
- Min SDK 23 (Android 6.0)

#### Known limitations
- Available history depends on the OS (Android stores up to ~4 weeks)
- Some manufacturers (Xiaomi, Huawei) may have additional restrictions
- Data usage over VPN networks may not be accurately reflected

#### Required permissions
- `PACKAGE_USAGE_STATS` — manually granted by the user in Settings
- `READ_PHONE_STATE` — to obtain `subscriberId` for mobile data
