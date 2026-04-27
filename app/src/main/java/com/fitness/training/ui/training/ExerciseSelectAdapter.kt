package com.fitness.training.ui.training

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
    private val onSelectionChanged: (List<Exercise>) -> Unit
) : ListAdapter<Exercise, ExerciseSelectAdapter.ExerciseViewHolder>(ExerciseDiffCallback()) {

    private val selectedExercises = mutableListOf<Exercise>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise_select, parent, false)
        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getSelectedExercises(): List<Exercise> = selectedExercises.toList()

    fun clearSelection() {
        selectedExercises.clear()
        notifyDataSetChanged()
        onSelectionChanged(selectedExercises)
    }

    inner class ExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cbSelect: CheckBox = itemView.findViewById(R.id.cb_select)
        private val tvExerciseName: TextView = itemView.findViewById(R.id.tv_exercise_name)
        private val tvMuscleGroup: TextView = itemView.findViewById(R.id.tv_muscle_group)

        fun bind(exercise: Exercise) {
            tvExerciseName.text = exercise.name
            tvMuscleGroup.text = "${exercise.muscleGroup} · ${exercise.equipment}"
            cbSelect.isChecked = selectedExercises.contains(exercise)

            itemView.setOnClickListener {
                if (selectedExercises.contains(exercise)) {
                    selectedExercises.remove(exercise)
                } else {
                    selectedExercises.add(exercise)
                }
                cbSelect.isChecked = selectedExercises.contains(exercise)
                onSelectionChanged(selectedExercises)
            }
        }
    }

    class ExerciseDiffCallback : DiffUtil.ItemCallback<Exercise>() {
        override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
            return oldItem == newItem
        }
    }
}
