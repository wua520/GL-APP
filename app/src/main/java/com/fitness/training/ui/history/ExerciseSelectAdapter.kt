package com.fitness.training.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.fitness.training.data.entity.Exercise

class ExerciseSelectAdapter(
    private val onSelectionChanged: (Exercise, Boolean) -> Unit
) : ListAdapter<Exercise, ExerciseSelectAdapter.ViewHolder>(DiffCallback()) {

    private val selectedIds = mutableSetOf<Long>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise_select, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_exercise_name)
        private val tvMuscle: TextView = itemView.findViewById(R.id.tv_muscle_group)
        private val checkbox: CheckBox = itemView.findViewById(R.id.cb_select)

        fun bind(exercise: Exercise) {
            tvName.text = exercise.name
            tvMuscle.text = exercise.muscleGroup
            checkbox.isChecked = selectedIds.contains(exercise.id)
            
            val clickListener = View.OnClickListener {
                val isSelected = !checkbox.isChecked
                checkbox.isChecked = isSelected
                if (isSelected) {
                    selectedIds.add(exercise.id)
                } else {
                    selectedIds.remove(exercise.id)
                }
                onSelectionChanged(exercise, isSelected)
            }
            
            itemView.setOnClickListener(clickListener)
            checkbox.setOnClickListener(clickListener)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Exercise>() {
        override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
            return oldItem == newItem
        }
    }
}
