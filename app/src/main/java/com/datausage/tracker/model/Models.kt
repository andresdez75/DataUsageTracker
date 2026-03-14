package com.datausage.tracker.model

// ─── Enums ────────────────────────────────────────────────────────────────────

enum class NetworkType(val label: String) {
    ALL("Todo"),
    MOBILE("Móvil"),
    WIFI("Wi-Fi")
}

enum class TimePeriod(val label: String) {
    TODAY("Hoy"),
    WEEK("7 días"),
    MONTH("30 días")
}

// ─── AppUsageEntry ─────────────────────────────────────────────────────────────
// Representa el consumo de UNA app en un periodo y tipo de red determinados.
// Campos raw vienen de NetworkStatsManager; los derivados se calculan aquí.

data class AppUsageEntry(
    val packageName: String,
    val appName: String,
    val uid: Int,
    val networkType: NetworkType,
    val startTime: Long,
    val endTime: Long,

    // Raw bytes desde el SO
    val fgRxBytes: Long,   // Recibidos en foreground
    val fgTxBytes: Long,   // Enviados en foreground
    val bgRxBytes: Long,   // Recibidos en background
    val bgTxBytes: Long    // Enviados en background
) {
    // Campos derivados (DATA_MODEL.md § Campos derivados)
    val fgTotalBytes: Long get() = fgRxBytes + fgTxBytes
    val bgTotalBytes: Long get() = bgRxBytes + bgTxBytes
    val totalBytes: Long   get() = fgTotalBytes + bgTotalBytes

    /** Proporción del consumo que ocurre en background (0.0 – 1.0) */
    val bgRatio: Float
        get() = if (totalBytes == 0L) 0f else bgTotalBytes.toFloat() / totalBytes.toFloat()
}

// ─── TotalUsageSummary ─────────────────────────────────────────────────────────
// Agregado de TODAS las apps para un periodo y tipo de red.

data class TotalUsageSummary(
    val networkType: NetworkType,
    val startTime: Long,
    val endTime: Long,
    val fgTotalBytes: Long,
    val bgTotalBytes: Long,
    val appCount: Int
) {
    val totalBytes: Long get() = fgTotalBytes + bgTotalBytes

    /** % background global del dispositivo */
    val bgRatio: Float
        get() = if (totalBytes == 0L) 0f else bgTotalBytes.toFloat() / totalBytes.toFloat()
}
