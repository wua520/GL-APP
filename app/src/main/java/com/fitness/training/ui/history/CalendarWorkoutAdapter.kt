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

data class CalendarWorkoutItem(
    val workout: Workout,
    val exerciseCount: Int,
    val totalVolume: Double
)

class CalendarWorkoutAdapter(
    private val onViewClick: (Workout) -> Unit,
    private val onEditClick: (Workout) -> Unit,
    private val onDeleteClick: (Workout) -> Unit
) : ListAdapter<CalendarWorkoutItem, CalendarWorkoutAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_workout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvWorkoutName: TextView = itemView.findViewById(R.id.tv_workout_name)
        private val tvWorkoutTime: TextView = itemView.findViewById(R.id.tv_workout_time)
        private val tvDuration: TextView = itemView.findViewById(R.id.tv_duration)
        private val tvExercises: TextView = itemView.findViewById(R.id.tv_exercises)
        private val tvVolume: TextView = itemView.findViewById(R.id.tv_volume)
        private val btnView: ImageButton = itemView.findViewById(R.id.btn_view)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)

        fun bind(item: CalendarWorkoutItem) {
            val workout = item.workout
            
            tvWorkoutName.text = if (workout.name.isNotEmpty()) workout.name else "训练"
            
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            tvWorkoutTime.text = timeFormat.format(Date(workout.date))
            
            val durationMinutes = workout.duration / 60000
            tvDuration.text = "${durationMinutes}分钟"
            
            tvExercises.text = "${item.exerciseCount}个动作"
            
            tvVolume.text = if (item.totalVolume >= 1000) {
                String.format("%.0fkg", item.totalVolume)
            } else if (item.totalVolume == item.totalVolume.toLong().toDouble()) {
                String.format("%.0fkg", item.totalVolume)
            } else {
                String.format("%.1fkg", item.totalVolume)
            }
            
            btnView.setOnClickListener { onViewClick(workout) }
            btnEdit.setOnClickListener { onEditClick(workout) }
            btnDelete.setOnClickListener { onDeleteClick(workout) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CalendarWorkoutItem>() {
        override fun areItemsTheSame(oldItem: CalendarWorkoutItem, newItem: CalendarWorkoutItem): Boolean {
            return oldItem.workout.id == newItem.workout.id
        }

        override fun areContentsTheSame(oldItem: CalendarWorkoutItem, newItem: CalendarWorkoutItem): Boolean {
            return oldItem == newItem
        }
    }
}
