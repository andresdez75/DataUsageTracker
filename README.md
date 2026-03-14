# Data Usage Tracker

Android app that measures and differentiates mobile data usage between **foreground** (app actively in use) and **background** (app running in the background).

---

## What is it for?

It allows the user to know precisely how much data each installed application consumes on their device, distinguishing whether the usage occurred while the app was in the foreground or background. This helps identify apps that silently and unnecessarily consume data.

---

## Main Features

### Per-app view
| | All | Mobile | Wi-Fi |
|---|---|---|---|
| Foreground | ✅ | ✅ | ✅ |
| Background | ✅ | ✅ | ✅ |

### Aggregated view (device total)
| | All | Mobile | Wi-Fi |
|---|---|---|---|
| Foreground | ✅ | ✅ | ✅ |
| Background | ✅ | ✅ | ✅ |

- Time period filter (day, week, month)
- No backend: all data is processed and stored locally on the device

---

## Requirements

| Requirement       | Detail                           |
|-------------------|----------------------------------|
| Minimum Android   | Android 6.0 (API 23)            |
| Required permission | `PACKAGE_USAGE_STATS`          |
| Network connection | Not required                    |
| Backend           | None — works offline             |

> ⚠️ The `PACKAGE_USAGE_STATS` permission must be manually granted by the user in **Settings > Apps > Special access > Usage access**.

---

## How it works

The app uses the native Android API `NetworkStatsManager` to query the operating system's network statistics. Android natively records usage separated by:

- **Foreground**: bytes transferred while the app was visible to the user
- **Background**: bytes transferred while the app was running in the background

No data is captured or transmitted outside the device.

---

## Installation and build

### Option A — Install APK directly
1. Download the `.apk` file from the [Releases](./releases) section
2. On your Android device, enable *Install from unknown sources*
3. Open the `.apk` and install

### Option B — Build from source
```bash
# Clone the repository
git clone https://github.com/andresdez75/DataUsageTracker.git

# Open in Android Studio and run on device or emulator
```

Environment requirements:
- Android Studio Hedgehog or later
- JDK 17
- Gradle 8+

---

## Project structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/         # App logic (Kotlin/Java)
│   │   ├── res/          # Layouts, strings, icons
│   │   └── AndroidManifest.xml
├── build.gradle
README.md
DATA_MODEL.md             # Schema of collected data
METRICS.md                # Definition of calculated metrics
```

---

## Known limitations

- Available historical data depends on the operating system (Android stores up to 4 weeks by default)
- Some manufacturers (Xiaomi, Huawei) may have additional permission restrictions
- Data usage over VPN networks may not be accurately reflected

---

## Project status

| Status       | Version |
|--------------|---------|
| 🟢 Active   | v1.0.0  |

---

## Contact and maintenance

Project maintained by the data product team.
For questions or improvements, open an issue in this repository.
