package com.datausage.tracker.util

import com.datausage.tracker.model.AppUsageEntry
import com.datausage.tracker.model.NetworkType
import com.datausage.tracker.model.TimePeriod
import com.datausage.tracker.model.TotalUsageSummary
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Builds the JSON export string from current filter data.
 * Uses org.json (Android SDK) — no external dependencies.
 * All byte values are converted to MB with 1 decimal place.
 */
object JsonExporter {

    private const val BYTES_PER_MB = 1_048_576.0

    /**
     * Export with optional mobile/wifi breakdown.
     * When networkType is ALL and mobileEntries/wifiEntries are provided,
     * the JSON splits data into "mobile" and "wifi" objects.
     */
    fun export(
        summary: TotalUsageSummary,
        entries: List<AppUsageEntry>,
        period: TimePeriod,
        mobileEntries: List<AppUsageEntry>? = null,
        wifiEntries: List<AppUsageEntry>? = null
    ): String {
        val root = JSONObject()
        val splitByNetwork = summary.networkType == NetworkType.ALL
                && mobileEntries != null && wifiEntries != null

        // generated_at — ISO 8601 UTC
        val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        isoFmt.timeZone = TimeZone.getTimeZone("UTC")
        root.put("generated_at", isoFmt.format(Date()))

        // filter
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val startDate = Date(summary.startTime)
        val endDate = Date(summary.endTime)

        root.put("filter", JSONObject().apply {
            put("network_type", summary.networkType.name)
            put("period", period.name)
            put("date_from", dateFmt.format(startDate))
            put("date_to", dateFmt.format(endDate))
        })

        // summary
        if (splitByNetwork) {
            val mobileSummary = buildSummaryFromEntries(mobileEntries!!)
            val wifiSummary = buildSummaryFromEntries(wifiEntries!!)
            root.put("summary", JSONObject().apply {
                put("total_mb", toMb(summary.totalBytes))
                put("mobile", buildNetworkSummaryJson(mobileSummary))
                put("wifi", buildNetworkSummaryJson(wifiSummary))
                put("bg_ratio_pct", round1(summary.bgRatio * 100))
                put("app_count", summary.appCount)
            })
        } else {
            root.put("summary", JSONObject().apply {
                put("total_mb", toMb(summary.totalBytes))
                put("fg_total_mb", toMb(summary.fgTotalBytes))
                put("bg_total_mb", toMb(summary.bgTotalBytes))
                put("bg_ratio_pct", round1(summary.bgRatio * 100))
                put("app_count", summary.appCount)
            })
        }

        // apps
        val apps = JSONArray()
        if (splitByNetwork) {
            val mobileMap = mobileEntries!!.associateBy { it.packageName }
            val wifiMap = wifiEntries!!.associateBy { it.packageName }
            entries.forEachIndexed { index, entry ->
                apps.put(JSONObject().apply {
                    put("rank", index + 1)
                    put("package_name", entry.packageName)
                    put("app_name", entry.appName)
                    put("mobile", buildNetworkEntryJson(mobileMap[entry.packageName]))
                    put("wifi", buildNetworkEntryJson(wifiMap[entry.packageName]))
                    put("total_mb", toMb(entry.totalBytes))
                    put("bg_ratio_pct", round1(entry.bgRatio * 100))
                })
            }
        } else {
            entries.forEachIndexed { index, entry ->
                apps.put(JSONObject().apply {
                    put("rank", index + 1)
                    put("package_name", entry.packageName)
                    put("app_name", entry.appName)
                    put("fg_rx_mb", toMb(entry.fgRxBytes))
                    put("fg_tx_mb", toMb(entry.fgTxBytes))
                    put("fg_total_mb", toMb(entry.fgTotalBytes))
                    put("bg_rx_mb", toMb(entry.bgRxBytes))
                    put("bg_tx_mb", toMb(entry.bgTxBytes))
                    put("bg_total_mb", toMb(entry.bgTotalBytes))
                    put("total_mb", toMb(entry.totalBytes))
                    put("bg_ratio_pct", round1(entry.bgRatio * 100))
                })
            }
        }
        root.put("apps", apps)

        return root.toString(2)
    }

    private data class NetworkTotals(
        val fgTotalBytes: Long,
        val bgTotalBytes: Long,
        val totalBytes: Long
    )

    private fun buildSummaryFromEntries(entries: List<AppUsageEntry>): NetworkTotals {
        val fg = entries.sumOf { it.fgTotalBytes }
        val bg = entries.sumOf { it.bgTotalBytes }
        return NetworkTotals(fg, bg, fg + bg)
    }

    private fun buildNetworkSummaryJson(totals: NetworkTotals): JSONObject = JSONObject().apply {
        put("fg_total_mb", toMb(totals.fgTotalBytes))
        put("bg_total_mb", toMb(totals.bgTotalBytes))
        put("total_mb", toMb(totals.totalBytes))
    }

    private fun buildNetworkEntryJson(entry: AppUsageEntry?): JSONObject = JSONObject().apply {
        if (entry != null) {
            put("fg_rx_mb", toMb(entry.fgRxBytes))
            put("fg_tx_mb", toMb(entry.fgTxBytes))
            put("fg_total_mb", toMb(entry.fgTotalBytes))
            put("bg_rx_mb", toMb(entry.bgRxBytes))
            put("bg_tx_mb", toMb(entry.bgTxBytes))
            put("bg_total_mb", toMb(entry.bgTotalBytes))
            put("total_mb", toMb(entry.totalBytes))
        } else {
            put("fg_rx_mb", 0.0)
            put("fg_tx_mb", 0.0)
            put("fg_total_mb", 0.0)
            put("bg_rx_mb", 0.0)
            put("bg_tx_mb", 0.0)
            put("bg_total_mb", 0.0)
            put("total_mb", 0.0)
        }
    }

    private fun toMb(bytes: Long): Double = round1(bytes / BYTES_PER_MB)

    private fun round1(value: Double): Double =
        Math.round(value * 10.0) / 10.0

    private fun round1(value: Float): Double = round1(value.toDouble())
}
