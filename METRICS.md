# Metrics

Definition of the metrics the app calculates and presents to the user from data obtained via `NetworkStatsManager`. All metrics are calculated client-side, in real time, without persistence.

---

## Analysis dimensions

All metrics can be queried by combining these two dimensions:

| Dimension     | Possible values                 |
|---------------|---------------------------------|
| `NetworkType` | `ALL`, `MOBILE`, `WIFI`         |
| `TimePeriod`  | `TODAY`, `WEEK`, `MONTH`        |

---

## Per-app view

Metrics calculated for **each app individually**.

| Metric                | Description                                                  | Source                          |
|-----------------------|--------------------------------------------------------------|---------------------------------|
| Foreground usage      | Total bytes (rx + tx) while the app was on screen            | `fgRxBytes + fgTxBytes`         |
| Background usage      | Total bytes (rx + tx) while the app was in the background    | `bgRxBytes + bgTxBytes`         |
| Total usage           | Sum of foreground + background                               | `fgTotalBytes + bgTotalBytes`   |
| % background          | Proportion of usage that occurs in background                | `bgTotalBytes / totalBytes`     |
| Usage ranking         | Apps sorted from highest to lowest total usage               | `totalBytes` desc               |

---

## Aggregated view (device total)

Metrics calculated by summing **all apps** on the device.

| Metric                     | Description                                                       | Source                              |
|----------------------------|-------------------------------------------------------------------|-------------------------------------|
| Device total foreground    | Sum of foreground across all apps                                 | `Σ fgTotalBytes`                    |
| Device total background    | Sum of background across all apps                                 | `Σ bgTotalBytes`                    |
| Device total usage         | Sum of total usage across all apps                                | `Σ totalBytes`                      |
| Global % background        | What proportion of total device usage is background               | `Σ bgTotalBytes / Σ totalBytes`     |
| Apps with usage            | How many apps generated traffic in the period                     | `appCount`                          |

---

## Value presentation

OS bytes are converted in the presentation layer according to these rules:

| Range                | Displayed unit | Example        |
|----------------------|----------------|----------------|
| < 1,024 bytes        | B              | `512 B`        |
| 1,024 – 1,048,575   | KB             | `768 KB`       |
| 1,048,576 – 1 GB    | MB             | `45.3 MB`      |
| > 1 GB              | GB             | `1.2 GB`       |

> Values are rounded to **1 decimal place** for display.

---

## Notes

- **% background** is the product's key metric: it allows detecting apps that silently consume data
- An app with `bgRatio > 0.5` consumes more in background than foreground — candidate for review or restriction
- Apps with no usage in the period do not appear in any view
- The `ALL` aggregate in `NetworkType` may differ slightly from `MOBILE + WIFI` if other network types are active (VPN, USB Ethernet)
