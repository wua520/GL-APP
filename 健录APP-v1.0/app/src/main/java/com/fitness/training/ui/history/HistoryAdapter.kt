package com.fitness.training.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.fitness.training.data.entity.Workout
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private val onItemClick: (Workout) -> Unit,
    private val onDeleteClick: (Workout) -> Unit
) : ListAdapter<HistoryItem, HistoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_workout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvWorkoutName: TextView = itemView.findViewById(R.id.tv_workout_name)
        private val tvWorkoutDate: TextView = itemView.findViewById(R.id.tv_workout_date)
        private val tvDuration: TextView = itemView.findViewById(R.id.tv_duration)
        private val tvExerciseCount: TextView = itemView.findViewById(R.id.tv_exercise_count)
        private val tvVolume: TextView = itemView.findViewById(R.id.tv_volume)
        private val tvExercisesSummary: TextView = itemView.findViewById(R.id.tv_exercises_summary)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)

        fun bind(item: HistoryItem) {
            val workout = item.workout
            
            tvWorkoutName.text = if (workout.name.isNotEmpty()) workout.name else "训练"
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            tvWorkoutDate.text = dateFormat.format(Date(workout.date))
            
            // 格式化时长
            val durationMinutes = workout.duration / 60000
            tvDuration.text = "${durationMinutes}分钟"
            
            tvExerciseCount.text = item.exerciseCount.toString()
            
            // 格式化容量
            tvVolume.text = if (item.totalVolume >= 1000) {
                String.format("%.0fkg", item.totalVolume)
            } else if (item.totalVolume == item.totalVolume.toLong().toDouble()) {
                String.format("%.0fkg", item.totalVolume)
            } else {
                String.format("%.1fkg", item.totalVolume)
            }
            
            tvExercisesSummary.text = if (item.exerciseNames.isNotEmpty()) {
                item.exerciseNames.joinToString("、")
            } else {
                "暂无动作记录"
            }
            
            itemView.setOnClickListener { onItemClick(workout) }
            btnDelete.setOnClickListener { onDeleteClick(workout) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<HistoryItem>() {
        override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
            return oldItem.workout.id == newItem.workout.id
        }

        override fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
            return oldItem == newItem
        }
    }
}

data class HistoryItem(
    val workout: Workout,
    val exerciseCount: Int,
    val totalVolume: Double,
    val exerciseNames: List<String>
)
