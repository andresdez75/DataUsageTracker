package com.datausage.tracker.data

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Process
import android.telephony.TelephonyManager
import android.util.Log
import com.datausage.tracker.model.AppUsageEntry
import com.datausage.tracker.model.NetworkType
import com.datausage.tracker.model.TimePeriod
import com.datausage.tracker.model.TotalUsageSummary
import java.util.Calendar

class NetworkStatsHelper(private val context: Context) {

    private val statsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
    private val packageManager = context.packageManager

    companion object {
        /** UID used by the OS to track tethering/hotspot traffic */
        const val UID_TETHERING = -5
        const val TETHERING_PACKAGE = "android.tethering"
        const val TETHERING_LABEL = "Tethering / Hotspot"

        /** Virtual UID to aggregate all non-listed system traffic */
        const val UID_SYSTEM = -999
        const val SYSTEM_PACKAGE = "android.system"
        const val SYSTEM_LABEL = "System"
    }

    // Only apps with a launcher icon = user-installed apps
    private fun getUserInstalledUids(): Map<Int, String> {
        val result = mutableMapOf<Int, String>()
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val launcherApps = packageManager.queryIntentActivities(intent, 0)

            for (resolveInfo in launcherApps) {
                val packageName = resolveInfo.activityInfo.packageName
                try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    result[appInfo.uid] = packageName
                } catch (e: Exception) {
                    continue
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkStats", "Error getting launcher apps", e)
        }
        return result
    }

    fun hasUsagePermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getTimeRange(period: TimePeriod): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayMidnight = cal.timeInMillis

        return when (period) {
            TimePeriod.TODAY -> Pair(todayMidnight, System.currentTimeMillis())
            TimePeriod.WEEK  -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                Pair(cal.timeInMillis, todayMidnight)
            }
            TimePeriod.MONTH -> {
                cal.timeInMillis = todayMidnight
                cal.add(Calendar.DAY_OF_YEAR, -30)
                Pair(cal.timeInMillis, todayMidnight)
            }
        }
    }

    fun getAppUsageEntries(period: TimePeriod, networkType: NetworkType): List<AppUsageEntry> {
        val (start, end) = getTimeRange(period)
        val userUids = getUserInstalledUids()

        return when (networkType) {
            NetworkType.MOBILE -> queryEntries(start, end, ConnectivityManager.TYPE_MOBILE, networkType, userUids)
            NetworkType.WIFI   -> queryEntries(start, end, ConnectivityManager.TYPE_WIFI, networkType, userUids)
            NetworkType.ALL    -> mergeEntries(
                queryEntries(start, end, ConnectivityManager.TYPE_MOBILE, NetworkType.ALL, userUids),
                queryEntries(start, end, ConnectivityManager.TYPE_WIFI, NetworkType.ALL, userUids)
            )
        }.sortedByDescending { it.totalBytes }
    }

    fun getTotalUsageSummary(
        entries: List<AppUsageEntry>,
        period: TimePeriod,
        networkType: NetworkType
    ): TotalUsageSummary {
        val (start, end) = getTimeRange(period)
        return TotalUsageSummary(
            networkType  = networkType,
            startTime    = start,
            endTime      = end,
            fgTotalBytes = entries.sumOf { it.fgTotalBytes },
            bgTotalBytes = entries.sumOf { it.bgTotalBytes },
            appCount     = entries.size
        )
    }

    private fun getSubscriberId(): String? {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            tm.subscriberId
        } catch (e: Exception) { null }
    }

    private fun queryEntries(
        start: Long,
        end: Long,
        connectivityType: Int,
        networkType: NetworkType,
        userUids: Map<Int, String>
    ): List<AppUsageEntry> {
        val result = mutableMapOf<Int, AppUsageEntry>()

        try {
            val subscriberId = if (connectivityType == ConnectivityManager.TYPE_MOBILE)
                getSubscriberId() else null

            val stats = statsManager.querySummary(connectivityType, subscriberId, start, end)
            val bucket = NetworkStats.Bucket()

            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)

                val rawUid = bucket.uid

                // Determine effective UID and package name:
                // - Tethering keeps its own UID
                // - User apps keep their UID
                // - Everything else is aggregated under UID_SYSTEM
                val (uid, packageName) = when {
                    rawUid == UID_TETHERING -> rawUid to TETHERING_PACKAGE
                    rawUid >= 0 && userUids.containsKey(rawUid) -> rawUid to userUids[rawUid]!!
                    else -> UID_SYSTEM to SYSTEM_PACKAGE
                }

                // Tethering exception: always classify as foreground
                val isForeground = uid == UID_TETHERING ||
                    bucket.state == NetworkStats.Bucket.STATE_FOREGROUND

                val entry = result.getOrPut(uid) {
                    val appName = when (uid) {
                        UID_TETHERING -> TETHERING_LABEL
                        UID_SYSTEM -> SYSTEM_LABEL
                        else -> try {
                            val info = packageManager.getApplicationInfo(packageName, 0)
                            packageManager.getApplicationLabel(info).toString()
                        } catch (e: Exception) { packageName }
                    }

                    AppUsageEntry(
                        packageName = packageName,
                        appName     = appName,
                        uid         = uid,
                        networkType = networkType,
                        startTime   = start,
                        endTime     = end,
                        fgRxBytes   = 0L,
                        fgTxBytes   = 0L,
                        bgRxBytes   = 0L,
                        bgTxBytes   = 0L
                    )
                }

                result[uid] = if (isForeground) {
                    entry.copy(
                        fgRxBytes = entry.fgRxBytes + bucket.rxBytes,
                        fgTxBytes = entry.fgTxBytes + bucket.txBytes
                    )
                } else {
                    entry.copy(
                        bgRxBytes = entry.bgRxBytes + bucket.rxBytes,
                        bgTxBytes = entry.bgTxBytes + bucket.txBytes
                    )
                }
            }
            stats.close()

        } catch (e: Exception) {
            Log.e("NetworkStats", "Error querying stats", e)
        }

        return result.values.filter { it.totalBytes > 0 }
    }

    /**
     * Returns a list of AppUsageEntry, one per day, for a specific app UID.
     * Used to show daily breakdown when the user taps on an app card.
     */
    fun getDailyBreakdown(uid: Int, packageName: String, appName: String,
                          period: TimePeriod, networkType: NetworkType): List<AppUsageEntry> {
        val (start, end) = getTimeRange(period)
        val dayMs = 24L * 60 * 60 * 1000
        val days = mutableListOf<AppUsageEntry>()
        val userUids = getUserInstalledUids()

        var dayStart = start
        while (dayStart < end) {
            val dayEnd = minOf(dayStart + dayMs, end)

            val dayEntry = when (networkType) {
                NetworkType.MOBILE -> queryEntriesForUid(uid, packageName, appName, dayStart, dayEnd,
                    ConnectivityManager.TYPE_MOBILE, networkType, userUids)
                NetworkType.WIFI -> queryEntriesForUid(uid, packageName, appName, dayStart, dayEnd,
                    ConnectivityManager.TYPE_WIFI, networkType, userUids)
                NetworkType.ALL -> {
                    val mobile = queryEntriesForUid(uid, packageName, appName, dayStart, dayEnd,
                        ConnectivityManager.TYPE_MOBILE, NetworkType.ALL, userUids)
                    val wifi = queryEntriesForUid(uid, packageName, appName, dayStart, dayEnd,
                        ConnectivityManager.TYPE_WIFI, NetworkType.ALL, userUids)
                    mobile.copy(
                        fgRxBytes = mobile.fgRxBytes + wifi.fgRxBytes,
                        fgTxBytes = mobile.fgTxBytes + wifi.fgTxBytes,
                        bgRxBytes = mobile.bgRxBytes + wifi.bgRxBytes,
                        bgTxBytes = mobile.bgTxBytes + wifi.bgTxBytes
                    )
                }
            }
            days.add(dayEntry)
            dayStart = dayEnd
        }
        return days
    }

    private fun queryEntriesForUid(
        targetUid: Int, packageName: String, appName: String,
        start: Long, end: Long, connectivityType: Int,
        networkType: NetworkType, userUids: Map<Int, String>
    ): AppUsageEntry {
        var fgRx = 0L; var fgTx = 0L; var bgRx = 0L; var bgTx = 0L
        try {
            val subscriberId = if (connectivityType == ConnectivityManager.TYPE_MOBILE)
                getSubscriberId() else null
            val stats = statsManager.querySummary(connectivityType, subscriberId, start, end)
            val bucket = NetworkStats.Bucket()

            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                val rawUid = bucket.uid
                val effectiveUid = when {
                    rawUid == UID_TETHERING -> UID_TETHERING
                    rawUid >= 0 && userUids.containsKey(rawUid) -> rawUid
                    else -> UID_SYSTEM
                }
                if (effectiveUid != targetUid) continue

                val isForeground = effectiveUid == UID_TETHERING ||
                    bucket.state == NetworkStats.Bucket.STATE_FOREGROUND
                if (isForeground) {
                    fgRx += bucket.rxBytes; fgTx += bucket.txBytes
                } else {
                    bgRx += bucket.rxBytes; bgTx += bucket.txBytes
                }
            }
            stats.close()
        } catch (e: Exception) {
            Log.e("NetworkStats", "Error querying daily stats", e)
        }
        return AppUsageEntry(
            packageName = packageName, appName = appName, uid = targetUid,
            networkType = networkType, startTime = start, endTime = end,
            fgRxBytes = fgRx, fgTxBytes = fgTx, bgRxBytes = bgRx, bgTxBytes = bgTx
        )
    }

    private fun mergeEntries(
        mobile: List<AppUsageEntry>,
        wifi: List<AppUsageEntry>
    ): List<AppUsageEntry> {
        val map = mutableMapOf<Int, AppUsageEntry>()

        fun accumulate(entry: AppUsageEntry) {
            val existing = map[entry.uid]
            map[entry.uid] = if (existing == null) {
                entry.copy(networkType = NetworkType.ALL)
            } else {
                existing.copy(
                    fgRxBytes = existing.fgRxBytes + entry.fgRxBytes,
                    fgTxBytes = existing.fgTxBytes + entry.fgTxBytes,
                    bgRxBytes = existing.bgRxBytes + entry.bgRxBytes,
                    bgTxBytes = existing.bgTxBytes + entry.bgTxBytes
                )
            }
        }

        mobile.forEach { accumulate(it) }
        wifi.forEach   { accumulate(it) }
        return map.values.toList()
    }
}