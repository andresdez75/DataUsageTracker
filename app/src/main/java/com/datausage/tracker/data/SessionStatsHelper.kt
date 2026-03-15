package com.datausage.tracker.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

/**
 * Queries UsageStatsManager for app session counts.
 * A "session" is each MOVE_TO_FOREGROUND → MOVE_TO_BACKGROUND pair.
 * An "active session" is one lasting more than 5 seconds.
 */
class SessionStatsHelper(context: Context) {

    private val usageManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    companion object {
        private const val ACTIVE_THRESHOLD_MS = 5_000L
    }

    data class SessionCount(val total: Int, val active: Int)

    /**
     * Returns session counts per package name for the given time range.
     */
    fun getSessionCounts(startTime: Long, endTime: Long): Map<String, SessionCount> {
        val foregroundTimestamps = mutableMapOf<String, Long>()
        val totalMap = mutableMapOf<String, Int>()
        val activeMap = mutableMapOf<String, Int>()

        val events = usageManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    foregroundTimestamps[event.packageName] = event.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val fgTime = foregroundTimestamps.remove(event.packageName) ?: continue
                    val duration = event.timeStamp - fgTime
                    totalMap[event.packageName] = (totalMap[event.packageName] ?: 0) + 1
                    if (duration >= ACTIVE_THRESHOLD_MS) {
                        activeMap[event.packageName] = (activeMap[event.packageName] ?: 0) + 1
                    }
                }
            }
        }

        val result = mutableMapOf<String, SessionCount>()
        for (pkg in totalMap.keys) {
            result[pkg] = SessionCount(
                total = totalMap[pkg] ?: 0,
                active = activeMap[pkg] ?: 0
            )
        }
        return result
    }
}
