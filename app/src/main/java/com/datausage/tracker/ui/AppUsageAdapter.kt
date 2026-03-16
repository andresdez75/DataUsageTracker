package com.datausage.tracker.ui

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
 * Uses ListAdapter + DiffUtil for efficient updates.
 * Supports inline expandable bar chart on item click.
 */
class AppUsageAdapter(
    private val onItemClick: ((AppUsageEntry, FrameLayout) -> Unit)? = null
) : ListAdapter<AppUsageEntry, AppUsageAdapter.ViewHolder>(DiffCallback()) {

    private var expandedUid: Int? = null
    var chartAvailable: Boolean = false

    fun collapseAll() {
        expandedUid = null
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_usage, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = getItem(position)
        holder.bind(entry, position + 1)

        // Chart hint icon visibility
        holder.ivChartHint.visibility = if (chartAvailable && expandedUid != entry.uid) View.VISIBLE else View.GONE

        // Expand/collapse chart
        val container = holder.chartContainer
        val isExpanded = expandedUid == entry.uid
        container.visibility = if (isExpanded) View.VISIBLE else View.GONE
        if (!isExpanded) container.removeAllViews()

        holder.itemView.setOnClickListener {
            val wasExpanded = expandedUid == entry.uid
            val prevUid = expandedUid
            expandedUid = if (wasExpanded) null else entry.uid

            // Collapse previous
            if (prevUid != null && prevUid != entry.uid) {
                val prevPos = (0 until itemCount).firstOrNull { getItem(it).uid == prevUid }
                if (prevPos != null) notifyItemChanged(prevPos)
            }

            if (wasExpanded) {
                container.removeAllViews()
                container.visibility = View.GONE
                holder.ivChartHint.visibility = if (chartAvailable) View.VISIBLE else View.GONE
            } else {
                container.removeAllViews()
                container.visibility = View.VISIBLE
                holder.ivChartHint.visibility = View.GONE
                onItemClick?.invoke(entry, container)
            }
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivIcon:      ImageView   = view.findViewById(R.id.ivAppIcon)
        private val tvRank:      TextView    = view.findViewById(R.id.tvRank)
        private val tvName:      TextView    = view.findViewById(R.id.tvAppName)
        private val tvTotal:     TextView    = view.findViewById(R.id.tvTotalBytes)
        private val tvFg:        TextView    = view.findViewById(R.id.tvFgBytes)
        private val tvBg:        TextView    = view.findViewById(R.id.tvBgBytes)
        private val tvBgRatio:   TextView    = view.findViewById(R.id.tvBgRatio)
        private val progressBg:  ProgressBar = view.findViewById(R.id.progressBg)
        private val tvSessions:  TextView    = view.findViewById(R.id.tvSessions)
        val chartContainer: FrameLayout      = view.findViewById(R.id.chartContainer)
        val ivChartHint: ImageView           = view.findViewById(R.id.ivChartHint)

        fun bind(entry: AppUsageEntry, rank: Int) {
            ivIcon.setImageDrawable(getAppIcon(entry.packageName))
            tvRank.text = "#$rank"
            tvName.text = entry.appName
            tvTotal.text = ByteFormatter.format(entry.totalBytes)
            tvFg.text  = "FG: ${ByteFormatter.format(entry.fgTotalBytes)}"
            tvBg.text  = "BG: ${ByteFormatter.format(entry.bgTotalBytes)}"

            val bgPct = ByteFormatter.formatPercent(entry.bgRatio)
            tvBgRatio.text = "BG $bgPct"

            if (entry.totalSessions > 0) {
                tvSessions.visibility = View.VISIBLE
                tvSessions.text = "${entry.totalSessions} sessions (${entry.activeSessions} > 5s)"
            } else {
                tvSessions.visibility = View.GONE
            }

            progressBg.progress = (entry.bgRatio * 100).toInt()

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
