package com.fitness.training.ui.history

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

data class EditExerciseItem(
    val exerciseId: Long,
    val exerciseName: String,
    val sets: MutableList<EditSetItem>
)

data class EditSetItem(
    var setNumber: Int,
    var weight: Double,
    var reps: Int
)

class EditWorkoutAdapter(
    private val onDeleteExercise: (Int) -> Unit,
    private val onDataChanged: () -> Unit
) : ListAdapter<EditExerciseItem, EditWorkoutAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_edit_exercise, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvExerciseName: TextView = itemView.findViewById(R.id.tv_exercise_name)
        private val btnDeleteExercise: ImageButton = itemView.findViewById(R.id.btn_delete_exercise)
        private val layoutSets: LinearLayout = itemView.findViewById(R.id.layout_sets)
        private val btnAddSet: MaterialButton = itemView.findViewById(R.id.btn_add_set)

        fun bind(item: EditExerciseItem, position: Int) {
            tvExerciseName.text = item.exerciseName
            
            btnDeleteExercise.setOnClickListener {
                onDeleteExercise(position)
            }
            
            // 清空并重新添加组
            layoutSets.removeAllViews()
            item.sets.forEachIndexed { setIndex, set ->
                addSetView(item, setIndex, set)
            }
            
            btnAddSet.setOnClickListener {
                val newSetNumber = item.sets.size + 1
                val lastSet = item.sets.lastOrNull()
                val newSet = EditSetItem(
                    setNumber = newSetNumber,
                    weight = lastSet?.weight ?: 0.0,
                    reps = lastSet?.reps ?: 10
                )
                item.sets.add(newSet)
                addSetView(item, item.sets.size - 1, newSet)
                onDataChanged()
            }
        }
        
        private fun addSetView(item: EditExerciseItem, setIndex: Int, set: EditSetItem) {
            val setView = LayoutInflater.from(itemView.context)
                .inflate(R.layout.item_edit_set, layoutSets, false)
            
            val tvSetNumber = setView.findViewById<TextView>(R.id.tv_set_number)
            val etWeight = setView.findViewById<TextInputEditText>(R.id.et_weight)
            val etReps = setView.findViewById<TextInputEditText>(R.id.et_reps)
            val btnDeleteSet = setView.findViewById<ImageButton>(R.id.btn_delete_set)
            
            tvSetNumber.text = "第${set.setNumber}组"
            etWeight.setText(if (set.weight > 0) set.weight.toString() else "")
            etReps.setText(if (set.reps > 0) set.reps.toString() else "")
            
            etWeight.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    set.weight = s.toString().toDoubleOrNull() ?: 0.0
                    onDataChanged()
                }
            })
            
            etReps.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    set.reps = s.toString().toIntOrNull() ?: 0
                    onDataChanged()
                }
            })
            
            btnDeleteSet.setOnClickListener {
                if (item.sets.size > 1) {
                    item.sets.removeAt(setIndex)
                    // 重新编号
                    item.sets.forEachIndexed { i, s -> s.setNumber = i + 1 }
                    // 刷新视图
                    layoutSets.removeAllViews()
                    item.sets.forEachIndexed { i, s -> addSetView(item, i, s) }
                    onDataChanged()
                }
            }
            
            layoutSets.addView(setView)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<EditExerciseItem>() {
        override fun areItemsTheSame(oldItem: EditExerciseItem, newItem: EditExerciseItem): Boolean {
            return oldItem.exerciseId == newItem.exerciseId
        }

        override fun areContentsTheSame(oldItem: EditExerciseItem, newItem: EditExerciseItem): Boolean {
            return oldItem == newItem
        }
    }
}
