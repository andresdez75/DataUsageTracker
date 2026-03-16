package com.datausage.tracker.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Local SQLite database to persist daily session counts.
 * UsageStatsManager only keeps ~7-10 days of event history,
 * so we store daily aggregates to build long-term session data.
 */
class SessionDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "sessions.db"
        private const val DB_VERSION = 1
        private const val TABLE = "daily_sessions"
        private const val COL_PACKAGE = "package_name"
        private const val COL_DATE = "date"
        private const val COL_TOTAL = "total_sessions"
        private const val COL_ACTIVE = "active_sessions"
    }

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE (
                $COL_PACKAGE TEXT NOT NULL,
                $COL_DATE TEXT NOT NULL,
                $COL_TOTAL INTEGER NOT NULL DEFAULT 0,
                $COL_ACTIVE INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY ($COL_PACKAGE, $COL_DATE)
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    /**
     * Save or update session counts for a specific package and date.
     * Uses INSERT OR REPLACE to always keep the latest data.
     */
    fun saveDailySessions(packageName: String, dateMillis: Long, total: Int, active: Int) {
        val date = dateFmt.format(Date(dateMillis))
        val values = ContentValues().apply {
            put(COL_PACKAGE, packageName)
            put(COL_DATE, date)
            put(COL_TOTAL, total)
            put(COL_ACTIVE, active)
        }
        writableDatabase.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    /**
     * Save session counts for multiple packages at once for a given date.
     */
    fun saveBulkDailySessions(dateMillis: Long, sessions: Map<String, SessionStatsHelper.SessionCount>) {
        val db = writableDatabase
        val date = dateFmt.format(Date(dateMillis))
        db.beginTransaction()
        try {
            for ((packageName, count) in sessions) {
                val values = ContentValues().apply {
                    put(COL_PACKAGE, packageName)
                    put(COL_DATE, date)
                    put(COL_TOTAL, count.total)
                    put(COL_ACTIVE, count.active)
                }
                db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Get session counts for a package on a specific date.
     * Returns null if no data stored for that date.
     */
    fun getDailySessions(packageName: String, dateMillis: Long): SessionStatsHelper.SessionCount? {
        val date = dateFmt.format(Date(dateMillis))
        val cursor = readableDatabase.query(
            TABLE,
            arrayOf(COL_TOTAL, COL_ACTIVE),
            "$COL_PACKAGE = ? AND $COL_DATE = ?",
            arrayOf(packageName, date),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                SessionStatsHelper.SessionCount(it.getInt(0), it.getInt(1))
            } else null
        }
    }

    /**
     * Get all stored session dates for a package, sorted ascending.
     * Returns pairs of (dateString, SessionCount).
     */
    fun getAllDailySessions(packageName: String): List<Pair<String, SessionStatsHelper.SessionCount>> {
        val result = mutableListOf<Pair<String, SessionStatsHelper.SessionCount>>()
        val cursor = readableDatabase.query(
            TABLE,
            arrayOf(COL_DATE, COL_TOTAL, COL_ACTIVE),
            "$COL_PACKAGE = ?",
            arrayOf(packageName),
            null, null, "$COL_DATE ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val date = it.getString(0)
                val count = SessionStatsHelper.SessionCount(it.getInt(1), it.getInt(2))
                result.add(date to count)
            }
        }
        return result
    }

    /**
     * Get session counts for a package within a date range.
     */
    fun getDailySessionsInRange(
        packageName: String, startMillis: Long, endMillis: Long
    ): Map<String, SessionStatsHelper.SessionCount> {
        val startDate = dateFmt.format(Date(startMillis))
        val endDate = dateFmt.format(Date(endMillis - 1)) // endMillis is exclusive
        val result = mutableMapOf<String, SessionStatsHelper.SessionCount>()
        val cursor = readableDatabase.query(
            TABLE,
            arrayOf(COL_DATE, COL_TOTAL, COL_ACTIVE),
            "$COL_PACKAGE = ? AND $COL_DATE >= ? AND $COL_DATE <= ?",
            arrayOf(packageName, startDate, endDate),
            null, null, "$COL_DATE ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                result[it.getString(0)] = SessionStatsHelper.SessionCount(it.getInt(1), it.getInt(2))
            }
        }
        return result
    }

    fun parseDate(dateStr: String): Long {
        return dateFmt.parse(dateStr)?.time ?: 0L
    }

    fun formatDate(millis: Long): String = dateFmt.format(Date(millis))
}
