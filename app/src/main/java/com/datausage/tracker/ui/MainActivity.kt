package com.datausage.tracker.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.datausage.tracker.R
import com.datausage.tracker.data.NetworkStatsHelper
import com.datausage.tracker.model.NetworkType
import com.datausage.tracker.model.TimePeriod
import com.datausage.tracker.util.ByteFormatter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * Pantalla principal de la app.
 * Flujo:
 *   1. Comprueba permiso PACKAGE_USAGE_STATS (Decisión [002])
 *   2. Si no hay permiso → muestra panel de aviso y redirige a Ajustes
 *   3. Si hay permiso → carga y muestra datos
 *
 * Sin ViewModel en v1.0 (Decisión [001] — sin librerías externas).
 * Si la complejidad crece → migrar a ViewModel + LiveData.
 */
class MainActivity : AppCompatActivity() {

    // ─── Views ────────────────────────────────────────────────────────────────
    private lateinit var layoutPermission:    LinearLayout
    private lateinit var layoutContent:       LinearLayout
    private lateinit var btnGrantPermission:  Button
    private lateinit var chipGroupNetwork:    ChipGroup
    private lateinit var chipGroupPeriod:     ChipGroup
    private lateinit var rvApps:              RecyclerView
    private lateinit var tvTotalFg:           TextView
    private lateinit var tvTotalBg:           TextView
    private lateinit var tvTotalAll:          TextView
    private lateinit var tvBgGlobal:          TextView
    private lateinit var tvAppCount:          TextView
    private lateinit var tvEmptyState:        TextView

    // ─── State ────────────────────────────────────────────────────────────────
    private val helper  by lazy { NetworkStatsHelper(this) }
    private val adapter by lazy { AppUsageAdapter() }

    private var selectedNetwork = NetworkType.MOBILE
    private var selectedPeriod  = TimePeriod.TODAY

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        setupRecyclerView()
        setupChips()
        btnGrantPermission.setOnClickListener { openUsageSettings() }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permiso al volver de Ajustes
        if (helper.hasUsagePermission()) {
            showContent()
            loadData()
        } else {
            showPermissionScreen()
        }
    }

    // ─── UI Setup ─────────────────────────────────────────────────────────────

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
        // Chips de tipo de red
        val networkChips = listOf(
            Pair(R.id.chipAll,    NetworkType.ALL),
            Pair(R.id.chipMobile, NetworkType.MOBILE),
            Pair(R.id.chipWifi,   NetworkType.WIFI)
        )
        networkChips.forEach { (id, type) ->
            findViewById<Chip>(id).setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    selectedNetwork = type
                    loadData()
                }
            }
        }

        // Chips de periodo
        val periodChips = listOf(
            Pair(R.id.chipToday, TimePeriod.TODAY),
            Pair(R.id.chipWeek,  TimePeriod.WEEK),
            Pair(R.id.chipMonth, TimePeriod.MONTH)
        )
        periodChips.forEach { (id, period) ->
            findViewById<Chip>(id).setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    selectedPeriod = period
                    loadData()
                }
            }
        }
    }

    // ─── Datos ────────────────────────────────────────────────────────────────

    /**
     * Carga los datos del SO en el hilo principal.
     * NOTA: NetworkStatsManager es rápido para periodos cortos.
     * Si se observan ANR → mover a coroutine o AsyncTask.
     */
    private fun loadData() {
        val entries = helper.getAppUsageEntries(selectedPeriod, selectedNetwork)
        val summary = helper.getTotalUsageSummary(entries, selectedPeriod, selectedNetwork)

        // Lista de apps
        adapter.submitList(entries)

        // Empty state
        tvEmptyState.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        rvApps.visibility       = if (entries.isEmpty()) View.GONE else View.VISIBLE

        // Resumen agregado (METRICS.md § Vista agregada)
        tvTotalFg.text  = ByteFormatter.format(summary.fgTotalBytes)
        tvTotalBg.text  = ByteFormatter.format(summary.bgTotalBytes)
        tvTotalAll.text = ByteFormatter.format(summary.totalBytes)
        tvBgGlobal.text = ByteFormatter.formatPercent(summary.bgRatio)
        tvAppCount.text = "${summary.appCount} apps"
    }

    // ─── Permiso ──────────────────────────────────────────────────────────────

    private fun showPermissionScreen() {
        layoutPermission.visibility = View.VISIBLE
        layoutContent.visibility    = View.GONE
    }

    private fun showContent() {
        layoutPermission.visibility = View.GONE
        layoutContent.visibility    = View.VISIBLE
    }

    /** Redirige al usuario a Ajustes > Apps > Acceso especial > Acceso a uso */
    private fun openUsageSettings() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }
}
