package com.datausage.tracker.ui

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.datausage.tracker.R
import com.datausage.tracker.model.AppUsageEntry
import com.datausage.tracker.util.ByteFormatter

/**
 * Adapter for the main app list.
 * Uses ListAdapter + DiffUtil for efficient updates
 * when the user changes period or network type.
 */
class AppUsageAdapter(
    private val onItemClick: ((AppUsageEntry) -> Unit)? = null
) : ListAdapter<AppUsageEntry, AppUsageAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_usage, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = getItem(position)
        holder.bind(entry, position + 1)
        holder.itemView.setOnClickListener { onItemClick?.invoke(entry) }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivIcon:      ImageView  = view.findViewById(R.id.ivAppIcon)
        private val tvRank:      TextView   = view.findViewById(R.id.tvRank)
        private val tvName:      TextView   = view.findViewById(R.id.tvAppName)
        private val tvTotal:     TextView   = view.findViewById(R.id.tvTotalBytes)
        private val tvFg:        TextView   = view.findViewById(R.id.tvFgBytes)
        private val tvBg:        TextView   = view.findViewById(R.id.tvBgBytes)
        private val tvBgRatio:   TextView   = view.findViewById(R.id.tvBgRatio)
        private val progressBg:  ProgressBar = view.findViewById(R.id.progressBg)
        private val tvSessions:  TextView   = view.findViewById(R.id.tvSessions)

        fun bind(entry: AppUsageEntry, rank: Int) {
            // App icon
            ivIcon.setImageDrawable(getAppIcon(entry.packageName))

            // Ranking position
            tvRank.text = "#$rank"

            // Name
            tvName.text = entry.appName

            // Total bytes
            tvTotal.text = ByteFormatter.format(entry.totalBytes)

            // Foreground / Background
            tvFg.text  = "FG: ${ByteFormatter.format(entry.fgTotalBytes)}"
            tvBg.text  = "BG: ${ByteFormatter.format(entry.bgTotalBytes)}"

            // % background — key product metric (METRICS.md)
            val bgPct = ByteFormatter.formatPercent(entry.bgRatio)
            tvBgRatio.text = "BG $bgPct"

            // Sessions
            if (entry.totalSessions > 0) {
                tvSessions.visibility = View.VISIBLE
                tvSessions.text = "${entry.totalSessions} sessions (${entry.activeSessions} > 5s)"
            } else {
                tvSessions.visibility = View.GONE
            }

            // Visual progress bar for % background
            progressBg.progress = (entry.bgRatio * 100).toInt()

            // Apps with bgRatio > 50% visually highlighted (METRICS.md note)
            val ctx = itemView.context
            val color = if (entry.bgRatio > 0.5f) {
                ContextCompat.getColor(ctx, R.color.warning_orange)
            } else {
                ContextCompat.getColor(ctx, R.color.primary_blue)
            }
            tvBgRatio.setTextColor(color)
            progressBg.progressTintList = android.content.res.ColorStateList.valueOf(color)
        }

        private fun getAppIcon(packageName: String): Drawable? {
            if (packageName == com.datausage.tracker.data.NetworkStatsHelper.TETHERING_PACKAGE) {
                return ContextCompat.getDrawable(itemView.context, R.drawable.ic_tethering)
            }
            if (packageName == com.datausage.tracker.data.NetworkStatsHelper.SYSTEM_PACKAGE) {
                return ContextCompat.getDrawable(itemView.context, R.drawable.ic_system)
            }
            return try {
                itemView.context.packageManager.getApplicationIcon(packageName)
            } catch (e: PackageManager.NameNotFoundException) {
                ContextCompat.getDrawable(itemView.context, android.R.drawable.sym_def_app_icon)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AppUsageEntry>() {
        override fun areItemsTheSame(old: AppUsageEntry, new: AppUsageEntry) =
            old.uid == new.uid && old.networkType == new.networkType
        override fun areContentsTheSame(old: AppUsageEntry, new: AppUsageEntry) =
            old == new
    }
}
