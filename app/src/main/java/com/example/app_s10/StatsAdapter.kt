package com.example.app_s10

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StatsAdapter(
    private val stats: List<Any> // Puede ser GenreStats o PlatformStats
) : RecyclerView.Adapter<StatsAdapter.StatsViewHolder>() {

    class StatsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStatName: TextView = view.findViewById(R.id.tvStatName)
        val tvStatCount: TextView = view.findViewById(R.id.tvStatCount)
        val tvStatPercentage: TextView = view.findViewById(R.id.tvStatPercentage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stat, parent, false)
        return StatsViewHolder(view)
    }

    override fun onBindViewHolder(holder: StatsViewHolder, position: Int) {
        when (val stat = stats[position]) {
            is GenreStats -> {
                holder.tvStatName.text = stat.genre
                holder.tvStatCount.text = stat.count.toString()
                holder.tvStatPercentage.text = String.format("%.1f%%", stat.percentage)
            }
            is PlatformStats -> {
                holder.tvStatName.text = stat.platform
                holder.tvStatCount.text = stat.count.toString()
                holder.tvStatPercentage.text = String.format("%.1f%%", stat.percentage)
            }
        }
    }

    override fun getItemCount(): Int = stats.size
}
