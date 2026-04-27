package com.fitness.training.ui.training

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.fitness.training.data.entity.WorkoutSet
import com.google.android.material.button.MaterialButton

// 显示项：可以是单个动作或超级组
sealed class DisplayItem {
    data class SingleExercise(val exercise: WorkoutExerciseWithDetails) : DisplayItem()
    data class Superset(val groupId: Long, val exercises: List<WorkoutExerciseWithDetails>) : DisplayItem()
}

class WorkoutExerciseAdapter(
    private val onAddSet: (Long, Double, Int) -> Unit,
    private val onSetUpdated: (WorkoutSet) -> Unit,
    private val onSetDeleted: (WorkoutSet) -> Unit,
    private val onExerciseDeleted: (WorkoutExerciseWithDetails) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val expandedItems = mutableSetOf<Long>()
    private var displayItems: List<DisplayItem> = emptyList()

    companion object {
        private const val TYPE_SINGLE = 0
        private const val TYPE_SUPERSET = 1
    }

    fun submitList(list: List<WorkoutExerciseWithDetails>?) {
        val items = list ?: emptyList()
        displayItems = groupExercises(items)
        notifyDataSetChanged()
    }

    private fun groupExercises(exercises: List<WorkoutExerciseWithDetails>): List<DisplayItem> {
        val result = mutableListOf<DisplayItem>()
        val grouped = mutableMapOf<Long, MutableList<WorkoutExerciseWithDetails>>()
        val singles = mutableListOf<WorkoutExerciseWithDetails>()

        exercises.forEach { exercise ->
            val groupId = exercise.workoutExercise.supersetGroupId
            if (groupId != null) {
                grouped.getOrPut(groupId) { mutableListOf() }.add(exercise)
            } else {
                singles.add(exercise)
            }
        }

        // 按顺序添加
        val processed = mutableSetOf<Long>()
        exercises.forEach { exercise ->
            val groupId = exercise.workoutExercise.supersetGroupId
            if (groupId != null) {
                if (!processed.contains(groupId)) {
                    processed.add(groupId)
                    val group = grouped[groupId] ?: emptyList()
                    if (group.size > 1) {
                        result.add(DisplayItem.Superset(groupId, group))
                    } else {
                        result.add(DisplayItem.SingleExercise(group.first()))
                    }
                }
            } else {
                result.add(DisplayItem.SingleExercise(exercise))
            }
        }

        return result
    }

    override fun getItemCount(): Int = displayItems.size

    override fun getItemViewType(position: Int): Int {
        return when (displayItems[position]) {
            is DisplayItem.SingleExercise -> TYPE_SINGLE
            is DisplayItem.Superset -> TYPE_SUPERSET
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_SUPERSET -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_superset_container, parent, false)
                SupersetViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_workout_exercise, parent, false)
                SingleExerciseViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = displayItems[position]) {
            is DisplayItem.SingleExercise -> (holder as SingleExerciseViewHolder).bind(item.exercise)
            is DisplayItem.Superset -> (holder as SupersetViewHolder).bind(item)
        }
    }

    fun expandItem(workoutExerciseId: Long) {
        expandedItems.add(workoutExerciseId)
        notifyDataSetChanged()
    }

    // 单个动作的 ViewHolder
    inner class SingleExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val layoutHeader: LinearLayout = itemView.findViewById(R.id.layout_header)
        private val layoutContent: LinearLayout = itemView.findViewById(R.id.layout_content)
        private val tvExpandIcon: TextView = itemView.findViewById(R.id.tv_expand_icon)
        private val tvExerciseName: TextView = itemView.findViewById(R.id.tv_exercise_name)
        private val tvExerciseSummary: TextView = itemView.findViewById(R.id.tv_exercise_summary)
        private val layoutSets: LinearLayout = itemView.findViewById(R.id.layout_sets)
        private val btnAddSet: MaterialButton = itemView.findViewById(R.id.btn_add_set)
        private val btnDeleteExercise: ImageButton = itemView.findViewById(R.id.btn_delete_exercise)

        fun bind(item: WorkoutExerciseWithDetails) {
            val isExpanded = expandedItems.contains(item.workoutExercise.id)

            tvExerciseName.text = item.exercise.name
            val completedSets = item.sets.count { it.isCompleted }
            val totalSets = item.sets.size
            tvExerciseSummary.text = "${item.exercise.muscleGroup} · $completedSets/$totalSets 组"

            tvExpandIcon.text = if (isExpanded) "▼" else "▶"
            layoutContent.visibility = if (isExpanded) View.VISIBLE else View.GONE

            layoutHeader.setOnClickListener {
                if (expandedItems.contains(item.workoutExercise.id)) {
                    expandedItems.remove(item.workoutExercise.id)
                } else {
                    expandedItems.add(item.workoutExercise.id)
                }
                notifyItemChanged(bindingAdapterPosition)
            }

            // 使用LinearLayout显示组数据
            layoutSets.removeAllViews()
            item.sets.forEach { set ->
                val setView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_workout_set, layoutSets, false)
                bindSetView(setView, set)
                layoutSets.addView(setView)
            }

            btnAddSet.setOnClickListener {
                // 先保存所有现有组的 UI 数据到数据库
                for (i in 0 until layoutSets.childCount) {
                    val setView = layoutSets.getChildAt(i)
                    val weightEdit = setView.findViewById<EditText>(R.id.et_weight)
                    val repsEdit = setView.findViewById<EditText>(R.id.et_reps)
                    val cbCompleted = setView.findViewById<android.widget.CheckBox>(R.id.cb_completed)
                    
                    val weight = weightEdit?.text?.toString()?.toDoubleOrNull() ?: 0.0
                    val reps = repsEdit?.text?.toString()?.toIntOrNull() ?: 0
                    val isCompleted = cbCompleted?.isChecked ?: false
                    
                    val originalSet = item.sets.getOrNull(i)
                    if (originalSet != null && (weight != originalSet.weight || reps != originalSet.reps || isCompleted != originalSet.isCompleted)) {
                        onSetUpdated(originalSet.copy(weight = weight, reps = reps, isCompleted = isCompleted))
                    }
                }
                
                // 从 LinearLayout 最后一个子 View 获取当前输入的重量和次数
                val lastWeight: Double
                val lastReps: Int
                
                if (layoutSets.childCount > 0) {
                    val lastSetView = layoutSets.getChildAt(layoutSets.childCount - 1)
                    val weightEdit = lastSetView.findViewById<EditText>(R.id.et_weight)
                    val repsEdit = lastSetView.findViewById<EditText>(R.id.et_reps)
                    lastWeight = weightEdit?.text?.toString()?.toDoubleOrNull() ?: 0.0
                    lastReps = repsEdit?.text?.toString()?.toIntOrNull() ?: 0
                } else {
                    lastWeight = 0.0
                    lastReps = 0
                }
                
                onAddSet(item.workoutExercise.id, lastWeight, lastReps)
            }

            btnDeleteExercise.setOnClickListener {
                onExerciseDeleted(item)
            }
        }
        
        private fun bindSetView(view: View, set: WorkoutSet) {
            val tvSetNumber = view.findViewById<TextView>(R.id.tv_set_number)
            val etWeight = view.findViewById<EditText>(R.id.et_weight)
            val etReps = view.findViewById<EditText>(R.id.et_reps)
            val cbCompleted = view.findViewById<android.widget.CheckBox>(R.id.cb_completed)
            val btnWeightMinus = view.findViewById<TextView>(R.id.btn_weight_minus)
            val btnWeightPlus = view.findViewById<TextView>(R.id.btn_weight_plus)
            val btnRepsMinus = view.findViewById<TextView>(R.id.btn_reps_minus)
            val btnRepsPlus = view.findViewById<TextView>(R.id.btn_reps_plus)
            val btnDeleteSet = view.findViewById<TextView>(R.id.btn_delete_set)
            
            val weightStep = 2.5
            
            tvSetNumber.text = set.setNumber.toString()
            etWeight.setText(formatWeight(set.weight))
            etReps.setText(if (set.reps == 0) "" else set.reps.toString())
            cbCompleted.isChecked = set.isCompleted
            
            var currentSet = set
            
            cbCompleted.setOnClickListener {
                val weight = etWeight.text.toString().toDoubleOrNull() ?: 0.0
                val reps = etReps.text.toString().toIntOrNull() ?: 0
                currentSet = currentSet.copy(weight = weight, reps = reps, isCompleted = cbCompleted.isChecked)
                onSetUpdated(currentSet)
            }
            
            btnWeightMinus.setOnClickListener {
                val currentWeight = etWeight.text.toString().toDoubleOrNull() ?: 0.0
                val newWeight = maxOf(0.0, currentWeight - weightStep)
                etWeight.setText(formatWeight(newWeight))
            }
            
            btnWeightPlus.setOnClickListener {
                val currentWeight = etWeight.text.toString().toDoubleOrNull() ?: 0.0
                etWeight.setText(formatWeight(currentWeight + weightStep))
            }
            
            btnRepsMinus.setOnClickListener {
                val currentReps = etReps.text.toString().toIntOrNull() ?: 0
                val newReps = maxOf(0, currentReps - 1)
                etReps.setText(if (newReps == 0) "" else newReps.toString())
            }
            
            btnRepsPlus.setOnClickListener {
                val currentReps = etReps.text.toString().toIntOrNull() ?: 0
                etReps.setText((currentReps + 1).toString())
            }
            
            btnDeleteSet.setOnClickListener {
                onSetDeleted(currentSet)
            }
            
            etWeight.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val weight = etWeight.text.toString().toDoubleOrNull() ?: 0.0
                    val reps = etReps.text.toString().toIntOrNull() ?: 0
                    if (weight != currentSet.weight || reps != currentSet.reps) {
                        currentSet = currentSet.copy(weight = weight, reps = reps)
                        onSetUpdated(currentSet)
                    }
                }
            }
            
            etReps.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val weight = etWeight.text.toString().toDoubleOrNull() ?: 0.0
                    val reps = etReps.text.toString().toIntOrNull() ?: 0
                    if (weight != currentSet.weight || reps != currentSet.reps) {
                        currentSet = currentSet.copy(weight = weight, reps = reps)
                        onSetUpdated(currentSet)
                    }
                }
            }
        }
        
        private fun formatWeight(weight: Double): String {
            return if (weight == 0.0) ""
            else if (weight == weight.toLong().toDouble()) weight.toLong().toString()
            else weight.toString()
        }
    }

    // 超级组的 ViewHolder
    inner class SupersetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSupersetCount: TextView = itemView.findViewById(R.id.tv_superset_count)
        private val llExercises: LinearLayout = itemView.findViewById(R.id.ll_exercises)

        fun bind(superset: DisplayItem.Superset) {
            tvSupersetCount.text = "${superset.exercises.size}个动作"
            llExercises.removeAllViews()

            superset.exercises.forEachIndexed { index, exercise ->
                val exerciseView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_superset_exercise, llExercises, false)

                // 显示分割线（非第一个）
                val divider = exerciseView.findViewById<View>(R.id.divider)
                divider.visibility = if (index > 0) View.VISIBLE else View.GONE

                bindExerciseView(exerciseView, exercise)
                llExercises.addView(exerciseView)
            }
        }

        private fun bindExerciseView(view: View, item: WorkoutExerciseWithDetails) {
            val layoutHeader = view.findViewById<LinearLayout>(R.id.layout_header)
            val layoutContent = view.findViewById<LinearLayout>(R.id.layout_content)
            val tvExpandIcon = view.findViewById<TextView>(R.id.tv_expand_icon)
            val tvExerciseName = view.findViewById<TextView>(R.id.tv_exercise_name)
            val tvExerciseSummary = view.findViewById<TextView>(R.id.tv_exercise_summary)
            val layoutSets = view.findViewById<LinearLayout>(R.id.layout_sets)
            val btnAddSet = view.findViewById<MaterialButton>(R.id.btn_add_set)
            val btnDeleteExercise = view.findViewById<ImageButton>(R.id.btn_delete_exercise)

            val isExpanded = expandedItems.contains(item.workoutExercise.id)

            tvExerciseName.text = item.exercise.name
            val completedSets = item.sets.count { it.isCompleted }
            val totalSets = item.sets.size
            tvExerciseSummary.text = "${item.exercise.muscleGroup} · $completedSets/$totalSets 组"

            tvExpandIcon.text = if (isExpanded) "▼" else "▶"
            layoutContent.visibility = if (isExpanded) View.VISIBLE else View.GONE

            layoutHeader.setOnClickListener {
                if (expandedItems.contains(item.workoutExercise.id)) {
                    expandedItems.remove(item.workoutExercise.id)
                } else {
                    expandedItems.add(item.workoutExercise.id)
                }
                notifyItemChanged(bindingAdapterPosition)
            }

            // 使用LinearLayout显示组数据
            layoutSets.removeAllViews()
            item.sets.forEach { set ->
                val setView = LayoutInflater.from(view.context)
                    .inflate(R.layout.item_workout_set, layoutSets, false)
                bindSetView(setView, set)
                layoutSets.addView(setView)
            }

            btnAddSet.setOnClickListener {
                // 先保存所有现有组的 UI 数据到数据库
                for (i in 0 until layoutSets.childCount) {
                    val setView = layoutSets.getChildAt(i)
                    val weightEdit = setView.findViewById<EditText>(R.id.et_weight)
                    val repsEdit = setView.findViewById<EditText>(R.id.et_reps)
                    val cbCompleted = setView.findViewById<android.widget.CheckBox>(R.id.cb_completed)
                    
                    val weight = weightEdit?.text?.toString()?.toDoubleOrNull() ?: 0.0
                    val reps = repsEdit?.text?.toString()?.toIntOrNull() ?: 0
                    val isCompleted = cbCompleted?.isChecked ?: false
                    
                    val originalSet = item.sets.getOrNull(i)
                    if (originalSet != null && (weight != originalSet.weight || reps != originalSet.reps || isCompleted != originalSet.isCompleted)) {
                        onSetUpdated(originalSet.copy(weight = weight, reps = reps, isCompleted = isCompleted))
                    }
                }
                
                // 从 LinearLayout 最后一个子 View 获取当前输入的重量和次数
                val lastWeight: Double
                val lastReps: Int
                
                if (layoutSets.childCount > 0) {
                    val lastSetView = layoutSets.getChildAt(layoutSets.childCount - 1)
                    val weightEdit = lastSetView.findViewById<EditText>(R.id.et_weight)
                    val repsEdit = lastSetView.findViewById<EditText>(R.id.et_reps)
                    lastWeight = weightEdit?.text?.toString()?.toDoubleOrNull() ?: 0.0
                    lastReps = repsEdit?.text?.toString()?.toIntOrNull() ?: 0
                } else {
                    lastWeight = 0.0
                    lastReps = 0
                }
                
                onAddSet(item.workoutExercise.id, lastWeight, lastReps)
            }

            btnDeleteExercise.setOnClickListener {
                onExerciseDeleted(item)
            }
        }
        
        private fun bindSetView(view: View, set: WorkoutSet) {
            val tvSetNumber = view.findViewById<TextView>(R.id.tv_set_number)
            val etWeight = view.findViewById<EditText>(R.id.et_weight)
            val etReps = view.findViewById<EditText>(R.id.et_reps)
            val cbCompleted = view.findViewById<android.widget.CheckBox>(R.id.cb_completed)
            val btnWeightMinus = view.findViewById<TextView>(R.id.btn_weight_minus)
            val btnWeightPlus = view.findViewById<TextView>(R.id.btn_weight_plus)
            val btnRepsMinus = view.findViewById<TextView>(R.id.btn_reps_minus)
            val btnRepsPlus = view.findViewById<TextView>(R.id.btn_reps_plus)
            val btnDeleteSet = view.findViewById<TextView>(R.id.btn_delete_set)
            
            val weightStep = 2.5
            
            tvSetNumber.text = set.setNumber.toString()
            etWeight.setText(formatWeight(set.weight))
            etReps.setText(if (set.reps == 0) "" else set.reps.toString())
            cbCompleted.isChecked = set.isCompleted
            
            var currentSet = set
            
            cbCompleted.setOnClickListener {
                val weight = etWeight.text.toString().toDoubleOrNull() ?: 0.0
                val reps = etReps.text.toString().toIntOrNull() ?: 0
                currentSet = currentSet.copy(weight = weight, reps = reps, isCompleted = cbCompleted.isChecked)
                onSetUpdated(currentSet)
            }
            
            btnWeightMinus.setOnClickListener {
                val currentWeight = etWeight.text.toString().toDoubleOrNull() ?: 0.0
                val newWeight = maxOf(0.0, currentWeight - weightStep)
                etWeight.setText(formatWeight(newWeight))
            }
            
            btnWeightPlus.setOnClickListener {
                val currentWeight = etWeight.text.toString().toDoubleOrNull() ?: 0.0
                etWeight.setText(formatWeight(currentWeight + weightStep))
            }
            
            btnRepsMinus.setOnClickListener {
                val currentReps = etReps.text.toString().toIntOrNull() ?: 0
                val newReps = maxOf(0, currentReps - 1)
                etReps.setText(if (newReps == 0) "" else newReps.toString())
            }
            
            btnRepsPlus.setOnClickListener {
                val currentReps = etReps.text.toString().toIntOrNull() ?: 0
                etReps.setText((currentReps + 1).toString())
            }
            
            btnDeleteSet.setOnClickListener {
                onSetDeleted(currentSet)
            }
            
            etWeight.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val weight = etWeight.text.toString().toDoubleOrNull() ?: 0.0
                    val reps = etReps.text.toString().toIntOrNull() ?: 0
                    if (weight != currentSet.weight || reps != currentSet.reps) {
                        currentSet = currentSet.copy(weight = weight, reps = reps)
                        onSetUpdated(currentSet)
                    }
                }
            }
            
            etReps.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val weight = etWeight.text.toString().toDoubleOrNull() ?: 0.0
                    val reps = etReps.text.toString().toIntOrNull() ?: 0
                    if (weight != currentSet.weight || reps != currentSet.reps) {
                        currentSet = currentSet.copy(weight = weight, reps = reps)
                        onSetUpdated(currentSet)
                    }
                }
            }
        }
        
        private fun formatWeight(weight: Double): String {
            return if (weight == 0.0) ""
            else if (weight == weight.toLong().toDouble()) weight.toLong().toString()
            else weight.toString()
        }
    }
}
