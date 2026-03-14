# Changelog

All notable versions of this project are documented here.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

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
