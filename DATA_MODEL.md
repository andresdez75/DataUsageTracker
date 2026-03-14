# Data Model

Description of the data the app queries, structures, and presents to the user. All data comes from the native Android API `NetworkStatsManager` and is obtained in real time — the app does not persist any of its own data in a local database.

---

## Data source

| Element               | Detail                                                   |
|-----------------------|----------------------------------------------------------|
| Android API           | `NetworkStatsManager` + `PackageManager`                 |
| Required permission   | `PACKAGE_USAGE_STATS`                                    |
| Availability          | Android 6.0 (API 23) and above                           |
| Own persistence       | None — direct query to the operating system              |
| OS retention          | Android keeps history for up to ~4 weeks                 |

---

## Main entity: `AppUsageEntry`

Represents the data usage of **one application** for a given **period and network type**.

| Field           | Type     | Description                                                  | Example                        |
|-----------------|----------|--------------------------------------------------------------|--------------------------------|
| `packageName`   | `String` | Unique app identifier in the Android system                  | `com.instagram.android`        |
| `appName`       | `String` | Visible app name (obtained via `PackageManager`)             | `Instagram`                    |
| `uid`           | `Int`    | Process User ID assigned by Android                          | `10085`                        |
| `networkType`   | `Enum`   | Network type queried: `ALL`, `MOBILE`, `WIFI`                | `MOBILE`                       |
| `startTime`     | `Long`   | Start of the queried period (epoch ms timestamp)             | `1741824000000`                |
| `endTime`       | `Long`   | End of the queried period (epoch ms timestamp)               | `1741910400000`                |
| `fgRxBytes`     | `Long`   | Bytes **received** in **foreground**                         | `52428800` (50 MB)             |
| `fgTxBytes`     | `Long`   | Bytes **sent** in **foreground**                             | `10485760` (10 MB)             |
| `bgRxBytes`     | `Long`   | Bytes **received** in **background**                         | `20971520` (20 MB)             |
| `bgTxBytes`     | `Long`   | Bytes **sent** in **background**                             | `5242880` (5 MB)               |

### Derived fields (calculated in app, not from the OS)

| Field             | Formula                                        | Description                        |
|-------------------|------------------------------------------------|------------------------------------|
| `fgTotalBytes`    | `fgRxBytes + fgTxBytes`                        | Total consumed in foreground       |
| `bgTotalBytes`    | `bgRxBytes + bgTxBytes`                        | Total consumed in background       |
| `totalBytes`      | `fgTotalBytes + bgTotalBytes`                  | Total consumed by the app          |
| `bgRatio`         | `bgTotalBytes / totalBytes`                    | % of usage that is background      |

---

## Aggregated entity: `TotalUsageSummary`

Represents the **total device usage** (sum of all apps) for a given period and network type. Calculated by aggregating all `AppUsageEntry` records.

| Field           | Type     | Description                                      |
|-----------------|----------|--------------------------------------------------|
| `networkType`   | `Enum`   | Network type: `ALL`, `MOBILE`, `WIFI`            |
| `startTime`     | `Long`   | Start of the period (epoch ms timestamp)         |
| `endTime`       | `Long`   | End of the period (epoch ms timestamp)            |
| `fgTotalBytes`  | `Long`   | Total foreground across all apps                 |
| `bgTotalBytes`  | `Long`   | Total background across all apps                 |
| `totalBytes`    | `Long`   | Total device usage in the period                 |
| `appCount`      | `Int`    | Number of apps with recorded usage               |

---

## Enum: `NetworkType`

| Value    | Android Constant                         | Description                        |
|----------|------------------------------------------|------------------------------------|
| `ALL`    | *(sum of MOBILE + WIFI)*                 | All usage regardless of network    |
| `MOBILE` | `ConnectivityManager.TYPE_MOBILE`        | Mobile data only                   |
| `WIFI`   | `ConnectivityManager.TYPE_WIFI`          | Wi-Fi only                         |

---

## Enum: `TimePeriod`

Predefined query periods offered to the user:

| Value     | Description                                      |
|-----------|--------------------------------------------------|
| `TODAY`   | From 00:00 of the current day until now           |
| `WEEK`    | Last 7 days                                      |
| `MONTH`   | Last 30 days                                     |

---

## Important notes

- **Rx = Received (download)** / **Tx = Transmitted (upload)** — standard Android terminology
- Bytes always come in `Long` format from the OS; conversion to KB/MB/GB is done in the presentation layer
- System apps also have a `uid` and appear in results; the app can choose to filter or display them
- If an app has no usage in the period, `NetworkStatsManager` does not return an entry for it (it does not appear with zeros)
- The `ALL` type is not a direct OS query — it is obtained by summing `MOBILE` + `WIFI` within the app
