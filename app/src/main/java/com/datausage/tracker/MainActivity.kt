package com.datausage.tracker

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.datausage.tracker.data.NetworkStatsHelper
import com.datausage.tracker.model.NetworkType
import com.datausage.tracker.model.TimePeriod
import com.datausage.tracker.ui.AppUsageAdapter
import com.datausage.tracker.util.ByteFormatter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class MainActivity : AppCompatActivity() {

    private lateinit var layoutPermission: LinearLayout
    private lateinit var layoutContent: LinearLayout
    private lateinit var btnGrantPermission: Button
    private lateinit var chipGroupNetwork: ChipGroup
    private lateinit var chipGroupPeriod: ChipGroup
    private lateinit var rvApps: RecyclerView
    private lateinit var tvTotalFg: TextView
    private lateinit var tvTotalBg: TextView
    private lateinit var tvTotalAll: TextView
    private lateinit var tvBgGlobal: TextView
    private lateinit var tvAppCount: TextView
    private lateinit var tvEmptyState: TextView

    private val helper by lazy { NetworkStatsHelper(this) }
    private val adapter by lazy { AppUsageAdapter() }

    private var selectedNetwork = NetworkType.MOBILE
    private var selectedPeriod = TimePeriod.TODAY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        setupRecyclerView()
        setupChips()
        debugApps()
        btnGrantPermission.setOnClickListener { openUsageSettings() }
    }

    override fun onResume() {
        super.onResume()
        if (helper.hasUsagePermission()) {
            showContent()
            loadData()
        } else {
            showPermissionScreen()
        }
    }

    private fun debugApps() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = packageManager.queryIntentActivities(intent, 0)
        apps.forEach { resolve ->
            val pkg = resolve.activityInfo.packageName
            try {
                val info = packageManager.getApplicationInfo(pkg, 0)
                Log.d("DEBUG_APPS", "App: $pkg | UID: ${info.uid}")
            } catch (e: Exception) {}
        }
    }

    private fun bindViews() {
        layoutPermission   = findViewById(R.id.layoutPermission)
        layoutContent      = findViewById(R.id.layoutContent)
        btnGrantPermission = findViewById(R.id.btnGrantPermission)
        chipGroupNetwork   = findViewById(R.id.chipGroupNetwork)
        chipGroupPeriod    = findViewById(R.id.chipGroupPeriod)
        rvApps             = findViewById(R.id.rvApps)
        tvTotalFg          = findViewById(R.id.tvTotalFg)
        tvTotalBg          = findViewById(R.id.tvTotalBg)
        tvTotalAll         = findViewById(R.id.tvTotalAll)
        tvBgGlobal         = findViewById(R.id.tvBgGlobal)
        tvAppCount         = findViewById(R.id.tvAppCount)
        tvEmptyState       = findViewById(R.id.tvEmptyState)
    }

    private fun setupRecyclerView() {
        rvApps.layoutManager = LinearLayoutManager(this)
        rvApps.adapter = adapter
    }

    private fun setupChips() {
        listOf(
            Pair(R.id.chipAll,    NetworkType.ALL),
            Pair(R.id.chipMobile, NetworkType.MOBILE),
            Pair(R.id.chipWifi,   NetworkType.WIFI)
        ).forEach { (id, type) ->
            findViewById<Chip>(id).setOnCheckedChangeListener { _, checked ->
                if (checked) { selectedNetwork = type; loadData() }
            }
        }

        listOf(
            Pair(R.id.chipToday, TimePeriod.TODAY),
            Pair(R.id.chipWeek,  TimePeriod.WEEK),
            Pair(R.id.chipMonth, TimePeriod.MONTH)
        ).forEach { (id, period) ->
            findViewById<Chip>(id).setOnCheckedChangeListener { _, checked ->
                if (checked) { selectedPeriod = period; loadData() }
            }
        }
    }

    private fun loadData() {
        val entries = helper.getAppUsageEntries(selectedPeriod, selectedNetwork)
        val summary = helper.getTotalUsageSummary(entries, selectedPeriod, selectedNetwork)

        adapter.submitList(entries)

        tvEmptyState.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        rvApps.visibility       = if (entries.isEmpty()) View.GONE else View.VISIBLE

        tvTotalFg.text  = ByteFormatter.format(summary.fgTotalBytes)
        tvTotalBg.text  = ByteFormatter.format(summary.bgTotalBytes)
        tvTotalAll.text = ByteFormatter.format(summary.totalBytes)
        tvBgGlobal.text = ByteFormatter.formatPercent(summary.bgRatio)
        tvAppCount.text = "${summary.appCount} apps"
    }

    private fun showPermiss