package com.fitness.training.ui.diet

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R

data class DietCalendarDay(
    val day: Int,
    val year: Int,
    val month: Int,
    val isCurrentMonth: Boolean,
    val hasRecord: Boolean,
    val isSelected: Boolean,
    val isToday: Boolean
)

class DietCalendarAdapter(
    private val onDayClick: (DietCalendarDay) -> Unit
) : RecyclerView.Adapter<DietCalendarAdapter.DayViewHolder>() {

    private var days: List<DietCalendarDay> = emptyList()

    fun submitList(newDays: List<DietCalendarDay>) {
        days = newDays
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diet_calendar_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount() = days.size

    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDay: TextView = itemView.findViewById(R.id.tv_day)

        fun bind(day: DietCalendarDay) {
            if (day.day == 0) {
                tvDay.text = ""
                tvDay.background = null
                itemView.isClickable = false
                return
            }

            tvDay.text = day.day.toString()
            itemView.isClickable = day.isCurrentMonth

            when {
                day.isSelected && day.hasRecord -> {
                    // 选中且有记录：浅灰填充+黑圈
                    tvDay.setBackgroundResource(R.drawable.bg_diet_day_selected_with_record)
                    tvDay.setTextColor(Color.WHITE)
                }
                day.isSelected -> {
                    // 仅选中：浅灰填充
                    tvDay.setBackgroundResource(R.drawable.bg_diet_day_selected)
                    tvDay.setTextColor(Color.WHITE)
                }
                day.hasRecord -> {
                    // 仅有记录：黑圈
                    tvDay.setBackgroundResource(R.drawable.bg_diet_day_ring)
                    tvDay.setTextColor(itemView.context.getColor(R.color.text_primary))
                }
                else -> {
                    tvDay.background = null
                    tvDay.setTextColor(
                        if (day.isCurrentMonth) 
                            itemView.context.getColor(R.color.text_primary)
                        else 
                            itemView.context.getColor(R.color.text_hint)
                    )
                }
            }

            if (day.isCurrentMonth && day.day > 0) {
                itemView.setOnClickListener { onDayClick(day) }
            }
        }
    }
}
