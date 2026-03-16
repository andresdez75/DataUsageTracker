package com.datausage.tracker.ui

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.datausage.tracker.R

class OnboardingActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "onboarding"
        private const val KEY_COMPLETED = "completed"
    }

    private lateinit var container: FrameLayout
    private lateinit var btnAction: Button
    private lateinit var btnSkip: Button
    private lateinit var dot1: View
    private lateinit var dot2: View
    private lateinit var dot3: View

    private var currentStep = 0
    private val handler = Handler(Looper.getMainLooper())
    private var switchAnimator: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Skip if already completed
        if (isOnboardingCompleted()) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_onboarding)
        container = findViewById(R.id.stepContainer)
        btnAction = findViewById(R.id.btnAction)
        btnSkip = findViewById(R.id.btnSkip)
        dot1 = findViewById(R.id.dot1)
        dot2 = findViewById(R.id.dot2)
        dot3 = findViewById(R.id.dot3)

        btnAction.setOnClickListener { onActionClick() }
        btnSkip.setOnClickListener { onSkipClick() }

        showStep(0)
    }

    override fun onResume() {
        super.onResume()
        // Update permission status when returning from Settings
        when (currentStep) {
            1 -> updateUsagePermissionStatus()
            2 -> updateBatteryStatus()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        switchAnimator?.let { handler.removeCallbacks(it) }
    }

    // ─── Steps ──────────────────────────────────────────────────────────────

    private fun showStep(step: Int) {
        currentStep = step
        container.removeAllViews()
        switchAnimator?.let { handler.removeCallbacks(it) }

        when (step) {
            0 -> showSplash()
            1 -> showUsagePermission()
            2 -> showBatteryPermission()
        }
        updateDots()
    }

    private fun showSplash() {
        val view = LayoutInflater.from(this).inflate(R.layout.step_splash, container, false)
        container.addView(view)

        btnAction.text = "Get Started"
        btnSkip.visibility = View.GONE

        // Animate logo, name, and tagline
        val logo = view.findViewById<ImageView>(R.id.ivLogo)
        val name = view.findViewById<TextView>(R.id.tvAppName)
        val tagline = view.findViewById<TextView>(R.id.tvTagline)

        // Logo: scale + fade in
        logo.scaleX = 0.3f
        logo.scaleY = 0.3f
        logo.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(800)
            .setInterpolator(OvershootInterpolator(1.5f))
            .start()

        // App name: fade in + slide up
        name.translationY = 40f
        name.animate()
            .alpha(1f).translationY(0f)
            .setDuration(600)
            .setStartDelay(400)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Tagline: fade in
        tagline.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(700)
            .start()
    }

    private fun showUsagePermission() {
        val view = LayoutInflater.from(this).inflate(R.layout.step_usage_permission, container, false)
        container.addView(view)

        btnAction.text = "Open Settings"
        btnSkip.visibility = View.GONE

        // Animate the demo switch ON/OFF repeatedly
        val switchDemo = view.findViewById<SwitchCompat>(R.id.switchDemo)
        startSwitchAnimation(switchDemo)

        updateUsagePermissionStatus()
    }

    private fun showBatteryPermission() {
        val view = LayoutInflater.from(this).inflate(R.layout.step_battery_permission, container, false)
        container.addView(view)

        btnAction.text = "Disable Optimization"
        btnSkip.visibility = View.VISIBLE
        btnSkip.text = "Skip for now"

        updateBatteryStatus()
    }

    // ─── Switch Animation ───────────────────────────────────────────────────

    private fun startSwitchAnimation(switchView: SwitchCompat) {
        var isOn = false
        val runnable = object : Runnable {
            override fun run() {
                isOn = !isOn
                switchView.isChecked = isOn
                handler.postDelayed(this, 1200)
            }
        }
        switchAnimator = runnable
        handler.postDelayed(runnable, 800)
    }

    // ─── Actions ────────────────────────────────────────────────────────────

    private fun onActionClick() {
        when (currentStep) {
            0 -> showStep(1)
            1 -> {
                if (hasUsagePermission()) {
                    showStep(2)
                } else {
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            }
            2 -> {
                if (isBatteryOptimized()) {
                    requestBatteryExemption()
                } else {
                    finishOnboarding()
                }
            }
        }
    }

    private fun onSkipClick() {
        when (currentStep) {
            2 -> finishOnboarding()
        }
    }

    // ─── Permission checks ──────────────────────────────────────────────────

    private fun hasUsagePermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun isBatteryOptimized(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return !pm.isIgnoringBatteryOptimizations(packageName)
    }

    @SuppressLint("BatteryLife")
    private fun requestBatteryExemption() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    private fun updateUsagePermissionStatus() {
        val tvStatus = container.findViewById<TextView>(R.id.tvUsageStatus) ?: return
        if (hasUsagePermission()) {
            tvStatus.text = "Permission granted"
            tvStatus.setTextColor(getColor(R.color.primary_blue))
            btnAction.text = "Continue"
            // Stop switch animation
            switchAnimator?.let { handler.removeCallbacks(it) }
            container.findViewById<SwitchCompat>(R.id.switchDemo)?.isChecked = true
        } else {
            tvStatus.text = "Permission not granted yet"
            tvStatus.setTextColor(getColor(R.color.warning_orange))
            btnAction.text = "Open Settings"
        }
    }

    private fun updateBatteryStatus() {
        val tvStatus = container.findViewById<TextView>(R.id.tvBatteryStatus) ?: return
        if (!isBatteryOptimized()) {
            tvStatus.text = "Battery optimization disabled"
            tvStatus.setTextColor(getColor(R.color.primary_blue))
            btnAction.text = "Finish Setup"
            btnSkip.visibility = View.GONE
        } else {
            tvStatus.text = "Battery optimization is active"
            tvStatus.setTextColor(getColor(R.color.warning_orange))
            btnAction.text = "Disable Optimization"
        }
    }

    // ─── Dots ───────────────────────────────────────────────────────────────

    private fun updateDots() {
        val dots = listOf(dot1, dot2, dot3)
        dots.forEachIndexed { index, dot ->
            dot.setBackgroundResource(
                if (index == currentStep) R.drawable.bg_step_indicator_active
                else R.drawable.bg_step_indicator_inactive
            )
            // Animate active dot
            if (index == currentStep) {
                dot.scaleX = 0.5f
                dot.scaleY = 0.5f
                dot.animate().scaleX(1f).scaleY(1f).setDuration(300).start()
            }
        }
    }

    // ─── Finish ─────────────────────────────────────────────────────────────

    private fun finishOnboarding() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit().putBoolean(KEY_COMPLETED, true).apply()
        goToMain()
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun isOnboardingCompleted(): Boolean {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getBoolean(KEY_COMPLETED, false)
    }
}
