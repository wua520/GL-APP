package com.fitness.training.ui.history

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R

class CalendarAdapter(
    private val onDayClick: (CalendarDay) -> Unit
) : ListAdapter<CalendarDay, CalendarAdapter.DayViewHolder>(DayDiffCallback()) {

    private var selectedDay: Int = -1

    fun setSelectedDay(day: Int) {
        val oldSelected = selectedDay
        selectedDay = day
        
        // 刷新旧选中和新选中的项
        currentList.forEachIndexed { index, calendarDay ->
            if (calendarDay.dayOfMonth == oldSelected || calendarDay.dayOfMonth == day) {
                notifyItemChanged(index)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(getItem(position), selectedDay)
    }

    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val layoutDay: LinearLayout = itemView.findViewById(R.id.layout_day)
        private val tvDay: TextView = itemView.findViewById(R.id.tv_day)

        fun bind(day: CalendarDay, selectedDay: Int) {
            if (day.dayOfMonth <= 0) {
                tvDay.text = ""
                tvDay.background = null
                layoutDay.isSelected = false
                layoutDay.setOnClickListener(null)
                return
            }

            tvDay.text = day.dayOfMonth.toString()
            
            val isSelected = day.dayOfMonth == selectedDay
            layoutDay.isSelected = isSelected
            
            when {
                isSelected -> {
                    // 选中状态：使用选中背景
                    tvDay.setBackgroundResource(R.drawable.bg_diet_day_selected)
                    tvDay.setTextColor(Color.WHITE)
                }
                day.hasWorkout -> {
                    // 有训练记录：灰色圆圈
                    tvDay.setBackgroundResource(R.drawable.bg_diet_day_ring)
                    tvDay.setTextColor(itemView.context.getColor(R.color.text_primary))
                }
                else -> {
                    // 普通状态
                    tvDay.background = null
                    tvDay.setTextColor(itemView.context.getColor(R.color.text_primary))
                }
            }
            
            layoutDay.setOnClickListener {
                onDayClick(day)
            }
        }
    }

    class DayDiffCallback : DiffUtil.ItemCallback<CalendarDay>() {
        override fun areItemsTheSame(oldItem: CalendarDay, newItem: CalendarDay): Boolean {
            return oldItem.dayOfMonth == newItem.dayOfMonth
        }

        override fun areContentsTheSame(oldItem: CalendarDay, newItem: CalendarDay): Boolean {
            return oldItem == newItem
        }
    }
}
