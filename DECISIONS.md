# Technical Decisions

Record of relevant technical decisions for the project, with their context and reasoning. The goal is for anyone joining the project to understand **why** it was done this way, not just **what** was done.

---

## [001] Architecture: Empty Activity without external libraries

**Date:** 2026-03
**Status:** ✅ Adopted

### Context
The app has a single purpose: query and display foreground/background data usage per app. It does not require local persistence, external API calls, or complex navigation between screens.

### Decision
Use **Empty Activity** as the starting point, relying exclusively on the native Android SDK, without incorporating third-party libraries.

### Components used (all native)

| Component         | Purpose                                             |
|-------------------|-----------------------------------------------------|
| `NetworkStatsManager` | Query foreground/background usage from the OS   |
| `PackageManager`  | Get name and icon of each installed app             |
| `RecyclerView`    | List apps with their usage (included in AndroidX)   |
| `ViewBinding`     | Connect views with code (enabled in gradle)         |

### Reasoning
- The logic is a single query to the operating system — it does not justify introducing abstraction layers
- Fewer external dependencies = smaller error surface, fewer updates to maintain, and smaller APK size
- Makes it easy for anyone on the team to read and understand the code without knowing specific frameworks

### Consequences
- If data persistence is added in the future → evaluate **Room**
- If multiple screens with navigation are added → evaluate **Navigation Component**
- If state complexity grows → evaluate **ViewModel + LiveData**

---

## [002] Usage statistics access permission

**Date:** 2026-03
**Status:** ✅ Adopted

### Context
To query per-app data usage differentiating foreground and background, Android requires a special permission that the user must grant manually.

### Decision
Use the **`PACKAGE_USAGE_STATS`** permission, declared in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
    tools:ignore="ProtectedPermissions"/>
```

### Reasoning
- It is the standard permission for accessing `NetworkStatsManager` and `UsageStatsManager` from user apps
- The alternative permission (`READ_NETWORK_USAGE_HISTORY`) is system-level and only available for manufacturer apps — not valid for Play Store distribution
- Being a special permission (`appOp`), it is not requested at runtime like normal permissions: the user must be redirected to **Settings > Apps > Special access > Usage access**

### Consequences
- The app must detect whether the permission is granted at startup and show the settings screen if it is not
- On some manufacturers (Xiaomi, Huawei, Samsung) the settings path may vary — consider a visual guide in onboarding

---

## [003] No backend — 100% client-side processing

**Date:** 2026-03
**Status:** ✅ Adopted

### Context
Usage data is local to the device and does not need to be synced with any external server.

### Decision
The app operates completely **offline**, without any backend, remote database, or external analytics.

### Reasoning
- The queried data already resides on the device itself — there is no need to extract it
- Eliminates connectivity dependencies, latency, and infrastructure costs
- Simplifies the privacy model: no user data leaves the device

### Consequences
- Available history is limited to what Android retains in the OS (~4 weeks)
- If longer history or cross-device comparisons are desired in the future → would require introducing a backend and redesigning the data model
