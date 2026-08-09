package com.fitness.training.ui.profile

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.fitness.training.data.entity.Achievement
import com.google.android.material.card.MaterialCardView

class AchievementAdapter : ListAdapter<Achievement, AchievementAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView as MaterialCardView
        private val tvIcon: TextView = itemView.findViewById(R.id.tv_icon)
        private val tvName: TextView = itemView.findViewById(R.id.tv_name)
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_description)
        private val tvProgress: TextView = itemView.findViewById(R.id.tv_progress)

        fun bind(achievement: Achievement) {
            tvIcon.text = achievement.icon
            tvName.text = achievement.name
            tvDescription.text = achievement.description
            
            if (achievement.isUnlocked) {
                // 已解锁 - 彩色显示
                tvIcon.alpha = 1f
                tvName.setTextColor(itemView.context.getColor(R.color.text_primary))
                tvProgress.text = "已解锁"
                tvProgress.setTextColor(itemView.context.getColor(R.color.primary))
                card.strokeColor = itemView.context.getColor(R.color.primary)
            } else {
                // 未解锁 - 灰色显示
                tvIcon.alpha = 0.4f
                tvName.setTextColor(itemView.context.getColor(R.color.text_hint))
                tvProgress.text = "${achievement.progress}/${achievement.targetValue}"
                tvProgress.setTextColor(itemView.context.getColor(R.color.text_hint))
                card.strokeColor = itemView.context.getColor(R.color.divider)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Achievement>() {
        override fun areItemsTheSame(oldItem: Achievement, newItem: Achievement): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Achievement, newItem: Achievement): Boolean {
            return oldItem == newItem
        }
    }
}
