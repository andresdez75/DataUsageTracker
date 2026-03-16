package com.datausage.tracker.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.appcompat.app.ActionBarDrawerToggle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.datausage.tracker.R
import com.datausage.tracker.data.NetworkStatsHelper
import com.datausage.tracker.model.AppUsageEntry
import com.datausage.tracker.model.NetworkType
import com.datausage.tracker.model.SortOrder
import com.datausage.tracker.model.TimePeriod
import com.datausage.tracker.model.TotalUsageSummary
import com.datausage.tracker.util.ByteFormatter
import com.datausage.tracker.util.JsonExporter
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.navigation.NavigationView

/**
 * Main screen of the app.
 * Flow:
 *   1. Check PACKAGE_USAGE_STATS permission (Decision [002])
 *   2. If no permission → show notice panel and redirect to Settings
 *   3. If permission granted → load and display data
 *
 * No ViewModel in v1.0 (Decision [001] — no external libraries).
 * If complexity grows → migrate to ViewModel + LiveData.
 */
class MainActivity : AppCompatActivity() {

    // ─── Views ────────────────────────────────────────────────────────────────
    private lateinit var drawerLayout:        DrawerLayout
    private lateinit var navView:             NavigationView
    private lateinit var toolbar:             Toolbar
    private lateinit var layoutPermission:    LinearLayout
    private lateinit var layoutContent:       LinearLayout
    private lateinit var btnGrantPermission:  Button
    private lateinit var spinnerNetwork:      Spinner
    private lateinit var spinnerPeriod:       Spinner
    private lateinit var spinnerSort:         Spinner
    private lateinit var rvApps:              RecyclerView
    private lateinit var tvTotalFg:           TextView
    private lateinit var tvTotalBg:           TextView
    private lateinit var tvTotalAll:          TextView
    private lateinit var tvBgGlobal:          TextView
    private lateinit var tvAppCount:          TextView
    private lateinit var tvDateRange:         TextView
    private lateinit var tvFilteredCount:     TextView
    private lateinit var tvEmptyState:        TextView

    // ─── State ────────────────────────────────────────────────────────────────
    private val helper         by lazy { NetworkStatsHelper(this) }
    private val sessionHelper  by lazy { com.datausage.tracker.data.SessionStatsHelper(this) }
    private val sessionDb      by lazy { com.datausage.tracker.data.SessionDatabase(this) }
    private val adapter        by lazy { AppUsageAdapter { entry, container -> showDailyBreakdown(entry, container) } }

    private var selectedNetwork = NetworkType.MOBILE
    private var selectedPeriod  = TimePeriod.TODAY
    private var selectedSort    = SortOrder.TOTAL_TRAFFIC

    // Cached data for export
    private var lastEntries: List<AppUsageEntry> = emptyList()
    private var lastSummary: TotalUsageSummary? = null
    private var lastMobileEntries: List<AppUsageEntry>? = null
    private var lastWifiEntries: List<AppUsageEntry>? = null

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        setupToolbar()
        setupDrawer()
        setupRecyclerView()
        setupSpinners()
        btnGrantPermission.setOnClickListener { openUsageSettings() }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permission when returning from Settings
        if (helper.hasUsagePermission()) {
            showContent()
            loadData()
        } else {
            showPermissionScreen()
        }
    }

    // ─── UI Setup ─────────────────────────────────────────────────────────────

    private fun bindViews() {
        drawerLayout       = findViewById(R.id.drawerLayout)
        navView            = findViewById(R.id.navView)
        toolbar            = findViewById(R.id.toolbar)
        layoutPermission   = findViewById(R.id.layoutPermission)
        layoutContent      = findViewById(R.id.layoutContent)
        btnGrantPermission = findViewById(R.id.btnGrantPermission)
        spinnerNetwork     = findViewById(R.id.spinnerNetwork)
        spinnerPeriod      = findViewById(R.id.spinnerPeriod)
        spinnerSort        = findViewById(R.id.spinnerSort)
        rvApps             = findViewById(R.id.rvApps)
        tvTotalFg          = findViewById(R.id.tvTotalFg)
        tvTotalBg          = findViewById(R.id.tvTotalBg)
        tvTotalAll         = findViewById(R.id.tvTotalAll)
        tvBgGlobal         = findViewById(R.id.tvBgGlobal)
        tvAppCount         = findViewById(R.id.tvAppCount)
        tvDateRange        = findViewById(R.id.tvDateRange)
        tvFilteredCount    = findViewById(R.id.tvFilteredCount)
        tvEmptyState       = findViewById(R.id.tvEmptyState)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
    }

    private fun setupDrawer() {
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.drawer_open, R.string.drawer_close
        )
        toggle.drawerArrowDrawable.color = getColor(android.R.color.white)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_export_json -> exportJson()
            }
            drawerLayout.closeDrawers()
            true
        }
    }

    private fun setupRecyclerView() {
        rvApps.layoutManager = LinearLayoutManager(this)
        rvApps.adapter = adapter
    }

    private fun setupSpinners() {
        // Access (network type)
        val networkValues = NetworkType.values()
        spinnerNetwork.adapter = ArrayAdapter(
            this, R.layout.item_spinner,
            networkValues.map { it.label }
        ).also { it.setDropDownViewResource(R.layout.item_spinner) }
        spinnerNetwork.setSelection(networkValues.indexOf(selectedNetwork))
        spinnerNetwork.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedNetwork = networkValues[pos]
                loadData()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Date (time period)
        val periodValues = TimePeriod.values()
        spinnerPeriod.adapter = ArrayAdapter(
            this, R.layout.item_spinner,
            periodValues.map { it.label }
        ).also { it.setDropDownViewResource(R.layout.item_spinner) }
        spinnerPeriod.setSelection(periodValues.indexOf(selectedPeriod))
        spinnerPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val period = periodValues[pos]
                if (period == TimePeriod.CUSTOM) {
                    showDateRangePicker()
                } else {
                    selectedPeriod = period
                    loadData()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Sort order
        val sortValues = SortOrder.values()
        spinnerSort.adapter = ArrayAdapter(
            this, R.layout.item_spinner,
            sortValues.map { it.label }
        ).also { it.setDropDownViewResource(R.layout.item_spinner) }
        spinnerSort.setSelection(sortValues.indexOf(selectedSort))
        spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedSort = sortValues[pos]
                loadData()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // ─── Data ─────────────────────────────────────────────────────────────────

    /**
     * Loads data from the OS on the main thread.
     * NOTE: NetworkStatsManager is fast for short periods.
     * If ANRs are observed → move to coroutine or AsyncTask.
     */
    private fun loadData() {
        val rawEntries = helper.getAppUsageEntries(selectedPeriod, selectedNetwork)

        // Enrich with session data
        val (start, end) = helper.getTimeRange(selectedPeriod)
        val sessions = sessionHelper.getSessionCounts(start, end)
        val enriched = rawEntries.map { entry ->
            val s = sessions[entry.packageName]
            if (s != null) entry.copy(totalSessions = s.total, activeSessions = s.active)
            else entry
        }

        // Persist daily sessions to local DB for long-term history
        persistDailySessions(start, end)

        val entries = when (selectedSort) {
            SortOrder.TOTAL_TRAFFIC -> enriched.sortedByDescending { it.totalBytes }
            SortOrder.BG_TRAFFIC    -> enriched.sortedByDescending { it.bgTotalBytes }
            SortOrder.BG_PERCENT    -> enriched.sortedByDescending { it.bgRatio }
            SortOrder.SESSION_ALL   -> enriched.sortedByDescending { it.totalSessions }
            SortOrder.WITH_SESSION  -> enriched.filter { it.totalSessions > 0 }.sortedByDescending { it.totalBytes }
            SortOrder.NO_SESSION    -> enriched.filter { it.totalSessions == 0 }.sortedByDescending { it.totalBytes }
            SortOrder.SESSION_5S    -> enriched.filter { it.activeSessions > 0 }.sortedByDescending { it.totalBytes }
            SortOrder.NAME          -> enriched.sortedBy { it.appName.lowercase() }
        }
        val summary = helper.getTotalUsageSummary(entries, selectedPeriod, selectedNetwork)

        // Cache for export (separate mobile/wifi for ALL network split)
        lastEntries = entries
        lastSummary = summary
        if (selectedNetwork == NetworkType.ALL) {
            lastMobileEntries = helper.getAppUsageEntries(selectedPeriod, NetworkType.MOBILE)
            lastWifiEntries = helper.getAppUsageEntries(selectedPeriod, NetworkType.WIFI)
        } else {
            lastMobileEntries = null
            lastWifiEntries = null
        }

        // App list — collapse any expanded chart and scroll to top after list updates
        adapter.chartAvailable = selectedPeriod != TimePeriod.TODAY
        adapter.collapseAll()
        adapter.submitList(entries) {
            rvApps.scrollToPosition(0)
        }

        // Filtered count
        tvFilteredCount.text = "Applications: ${entries.size}"

        // Empty state
        tvEmptyState.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        rvApps.visibility       = if (entries.isEmpty()) View.GONE else View.VISIBLE

        // Aggregate summary (METRICS.md § Aggregate view)
        tvTotalFg.text  = ByteFormatter.format(summary.fgTotalBytes)
        tvTotalBg.text  = ByteFormatter.format(summary.bgTotalBytes)
        tvTotalAll.text = ByteFormatter.format(summary.totalBytes)
        tvBgGlobal.text = ByteFormatter.formatPercent(summary.bgRatio)
        tvAppCount.text = "${summary.appCount} apps"

        // Date range — for WEEK/MONTH/CUSTOM the end is midnight, so display the previous day
        val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val displayEnd = if (selectedPeriod == TimePeriod.TODAY) {
            summary.endTime
        } else {
            summary.endTime - 24L * 60 * 60 * 1000
        }
        tvDateRange.text = "From ${fmt.format(Date(summary.startTime))} to ${fmt.format(Date(displayEnd))}"
    }

    // ─── Export ─────────────────────────────────────────────────────────────────

    private fun exportJson() {
        val summary = lastSummary
        if (summary == null || lastEntries.isEmpty()) {
            Toast.makeText(this, R.string.export_no_data, Toast.LENGTH_SHORT).show()
            return
        }

        val json = JsonExporter.export(summary, lastEntries, selectedPeriod, lastMobileEntries, lastWifiEntries)
        val file = File(cacheDir, "data_usage_export.json")
        file.writeText(json)

        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.menu_export_json)))
    }

    // ─── Date Range Picker ─────────────────────────────────────────────────────

    private fun showDateRangePicker() {
        val constraintsBuilder = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())

        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("\uD83D\uDCC5  Select Date Range")
            .setCalendarConstraints(constraintsBuilder.build())
            .setTheme(R.style.Theme_DataUsageTracker_DatePicker)
            .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val startUtc = selection.first ?: return@addOnPositiveButtonClickListener
            val endUtc = selection.second ?: return@addOnPositiveButtonClickListener

            // MaterialDatePicker returns UTC midnight — add 1 day to end to make it inclusive
            val endInclusive = endUtc + 24L * 60 * 60 * 1000

            helper.setCustomRange(startUtc, endInclusive)
            selectedPeriod = TimePeriod.CUSTOM
            loadData()
        }

        picker.addOnNegativeButtonClickListener {
            revertPeriodSpinner()
        }

        picker.addOnCancelListener {
            revertPeriodSpinner()
        }

        picker.show(supportFragmentManager, "dateRangePicker")
    }

    private fun revertPeriodSpinner() {
        val periodValues = TimePeriod.values()
        spinnerPeriod.setSelection(periodValues.indexOf(selectedPeriod))
    }

    // ─── Session Persistence ───────────────────────────────────────────────────

    /**
     * Persist daily session counts from UsageStatsManager to local DB.
     * Iterates day by day and saves each day's session data.
     */
    private fun persistDailySessions(start: Long, end: Long) {
        val dayMs = 24L * 60 * 60 * 1000
        var dayStart = start
        while (dayStart < end) {
            val dayEnd = minOf(dayStart + dayMs, end)
            val daySessions = sessionHelper.getSessionCounts(dayStart, dayEnd)
            if (daySessions.isNotEmpty()) {
                sessionDb.saveBulkDailySessions(dayStart, daySessions)
            }
            dayStart = dayEnd
        }
    }

    // ─── Daily Breakdown ──────────────────────────────────────────────────────

    private fun showDailyBreakdown(entry: AppUsageEntry, container: FrameLayout) {
        if (selectedPeriod == TimePeriod.TODAY) return

        val days = helper.getDailyBreakdown(
            entry.uid, entry.packageName, entry.appName,
            selectedPeriod, selectedNetwork
        )

        val dateFmt = SimpleDateFormat("dd/MM", Locale.getDefault())
        val isSessionFilter = selectedSort in listOf(
            SortOrder.SESSION_ALL, SortOrder.WITH_SESSION,
            SortOrder.NO_SESSION, SortOrder.SESSION_5S
        )

        // Build bar data
        val dayMs = 24L * 60 * 60 * 1000
        val barData: List<DailyBarChartView.BarData>

        if (isSessionFilter) {
            // Use local DB for session history (covers beyond OS ~7-day limit)
            val dbData = sessionDb.getDailySessionsInRange(
                entry.packageName, days.first().startTime, days.last().endTime
            )
            val allBars = days.mapNotNull { day ->
                val dateKey = sessionDb.formatDate(day.startTime)
                val label = dateFmt.format(Date(day.startTime))
                // Try DB first, then live data
                val sessions = dbData[dateKey]
                    ?: sessionHelper.getSessionCountForPackage(entry.packageName, day.startTime, day.endTime)
                val count = if (selectedSort == SortOrder.SESSION_5S) sessions.active else sessions.total
                Triple(DailyBarChartView.BarData(label, count.toFloat(), "$count"),
                    sessions.total > 0 || dbData.containsKey(dateKey), day)
            }
            // Show from first day with data
            val firstWithData = allBars.indexOfFirst { it.second }
            barData = if (firstWithData == -1) emptyList()
            else allBars.subList(firstWithData, allBars.size).map { it.first }
        } else {
            // Traffic data — trim leading/trailing empty days
            val allBars = days.map { day ->
                val label = dateFmt.format(Date(day.startTime))
                val (value, valueLabel) = when (selectedSort) {
                    SortOrder.BG_TRAFFIC -> day.bgTotalBytes.toFloat() to ByteFormatter.format(day.bgTotalBytes)
                    SortOrder.BG_PERCENT -> (day.bgRatio * 100f) to "%.0f%%".format(day.bgRatio * 100f)
                    else -> day.totalBytes.toFloat() to ByteFormatter.format(day.totalBytes)
                }
                DailyBarChartView.BarData(label, value, valueLabel)
            }
            val firstNonZero = allBars.indexOfFirst { it.value > 0 }
            val lastNonZero = allBars.indexOfLast { it.value > 0 }
            barData = if (firstNonZero == -1) emptyList()
            else allBars.subList(firstNonZero, lastNonZero + 1)
        }

        if (barData.isEmpty()) return

        // Use the exact filter label as title
        val title = selectedSort.label
        val iconRes = if (isSessionFilter) R.drawable.ic_chart_sessions else R.drawable.ic_chart_traffic

        // Calculate aggregated total
        val totalLabel = if (isSessionFilter) {
            val totalSessions = barData.sumOf { it.value.toInt() }
            "$totalSessions sessions"
        } else {
            when (selectedSort) {
                SortOrder.BG_TRAFFIC -> {
                    val total = days.sumOf { it.bgTotalBytes }
                    ByteFormatter.format(total)
                }
                SortOrder.BG_PERCENT -> {
                    val totalAll = days.sumOf { it.totalBytes }
                    val totalBg = days.sumOf { it.bgTotalBytes }
                    if (totalAll > 0) "%.1f%%".format(totalBg.toFloat() / totalAll * 100f) else "0%"
                }
                else -> {
                    val total = days.sumOf { it.totalBytes }
                    ByteFormatter.format(total)
                }
            }
        }

        val chartView = DailyBarChartView(this)
        chartView.setTitle(title)
        chartView.setIcon(iconRes)
        chartView.setTotalLabel(totalLabel)
        if (isSessionFilter) {
            chartView.setSubtitle(
                "Session history starts from install.",
                "(~7 days initially, more over time)"
            )
        }
        chartView.setData(barData)
        container.addView(chartView)
    }

    // ─── Permission ──────────────────────────────────────────────────────────

    private fun showPermissionScreen() {
        layoutPermission.visibility = View.VISIBLE
        layoutContent.visibility    = View.GONE
    }

    private fun showContent() {
        layoutPermission.visibility = View.GONE
        layoutContent.visibility    = View.VISIBLE
    }

    /** Redirects the user to Settings > Apps > Special access > Usage data access */
    private fun openUsageSettings() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }
}
