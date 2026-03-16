package com.datausage.tracker.model

// ─── Enums ────────────────────────────────────────────────────────────────────

enum class NetworkType(val label: String) {
    ALL("All"),
    MOBILE("Mobile"),
    WIFI("Wi-Fi")
}

enum class TimePeriod(val label: String) {
    TODAY("Today"),
    WEEK("7 days"),
    MONTH("30 days"),
    CUSTOM("Customize")
}

enum class SortOrder(val label: String) {
    TOTAL_TRAFFIC("Total Traffic"),
    BG_TRAFFIC("BG Traffic"),
    BG_PERCENT("BG Percent (%)"),
    SESSION_ALL("Session All"),
    WITH_SESSION("With Session"),
    NO_SESSION("No Session"),
    SESSION_5S("Session > 5s"),
    NAME("Name A-Z")
}

// ─── AppUsageEntry ─────────────────────────────────────────────────────────────
// Represents the usage of ONE app in a given period and network type.
// Raw fields come from NetworkStatsManager; derived fields are calculated here.

data class AppUsageEntry(
    val packageName: String,
    val appName: String,
    val uid: Int,
    val networkType: NetworkType,
    val startTime: Long,
    val endTime: Long,

    // Raw bytes from the OS
    val fgRxBytes: Long,   // Received in foreground
    val fgTxBytes: Long,   // Sent in foreground
    val bgRxBytes: Long,   // Received in background
    val bgTxBytes: Long,   // Sent in background

    // Session data from UsageStatsManager
    val totalSessions: Int = 0,      // Total times the app was opened
    val activeSessions: Int = 0      // Sessions longer than 5 seconds
) {
    // Derived fields (DATA_MODEL.md § Derived fields)
    val fgTotalBytes: Long get() = fgRxBytes + fgTxBytes
    val bgTotalBytes: Long get() = bgRxBytes + bgTxBytes
    val totalBytes: Long   get() = fgTotalBytes + bgTotalBytes

    /** Proportion of usage that occurs in background (0.0 – 1.0) */
    val bgRatio: Float
        get() = if (totalBytes == 0L) 0f else bgTotalBytes.toFloat() / totalBytes.toFloat()
}

// ─── TotalUsageSummary ─────────────────────────────────────────────────────────
// Aggregate of ALL apps for a given period and network type.

data class TotalUsageSummary(
    val networkType: NetworkType,
    val startTime: Long,
    val endTime: Long,
    val fgTotalBytes: Long,
    val bgTotalBytes: Long,
    val appCount: Int
) {
    val totalBytes: Long get() = fgTotalBytes + bgTotalBytes

    /** Device-wide background % */
    val bgRatio: Float
        get() = if (totalBytes == 0L) 0f else bgTotalBytes.toFloat() / totalBytes.toFloat()
}
