package com.fitness.training.ui.training

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.fitness.training.data.entity.WorkoutSet

class WorkoutSetAdapter(
    private val onSetChanged: (WorkoutSet) -> Unit,
    private val onSetDeleted: (WorkoutSet) -> Unit
) : ListAdapter<WorkoutSet, WorkoutSetAdapter.SetViewHolder>(SetDiffCallback()) {

    private val weightStep = 2.5

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout_set, parent, false)
        return SetViewHolder(view)
    }

    override fun onBindViewHolder(holder: SetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSetNumber: TextView = itemView.findViewById(R.id.tv_set_number)
        private val etWeight: EditText = itemView.findViewById(R.id.et_weight)
        private val etReps: EditText = itemView.findViewById(R.id.et_reps)
        private val cbCompleted: CheckBox = itemView.findViewById(R.id.cb_completed)
        private val btnWeightMinus: TextView = itemView.findViewById(R.id.btn_weight_minus)
        private val btnWeightPlus: TextView = itemView.findViewById(R.id.btn_weight_plus)
        private val btnRepsMinus: TextView = itemView.findViewById(R.id.btn_reps_minus)
        private val btnRepsPlus: TextView = itemView.findViewById(R.id.btn_reps_plus)
        private val btnDeleteSet: TextView = itemView.findViewById(R.id.btn_delete_set)

        private var currentSet: WorkoutSet? = null

        init {
            // 完成勾选 - 立即保存（包含当前UI上的重量和次数）
            cbCompleted.setOnClickListener {
                currentSet?.let { set ->
                    val weight = etWeight.text.toString().toDoubleOrNull() ?: 0.0
                    val reps = etReps.text.toString().toIntOrNull() ?: 0
                    val updatedSet = set.copy(
                        weight = weight,
                        reps = reps,
                        isCompleted = cbCompleted.isChecked
                    )
                    currentSet = updatedSet
                    onSetChanged(updatedSet)
                }
            }

            // 重量减少 - 只更新UI
            btnWeightMinus.setOnClickListener {
                val currentWeight = etWeight.text.toString().toDoubleOrNull() ?: 0.0
                val newWeight = maxOf(0.0, currentWeight - weightStep)
                etWeight.setText(formatWeight(newWeight))
            }

            // 重量增加 - 只更新UI
            btnWeightPlus.setOnClickListener {
                val currentWeight = etWeight.text.toString().toDoubleOrNull() ?: 0.0
                val newWeight = currentWeight + weightStep
                etWeight.setText(formatWeight(newWeight))
            }

            // 次数减少 - 只更新UI
            btnRepsMinus.setOnClickListener {
                val currentReps = etReps.text.toString().toIntOrNull() ?: 0
                val newReps = maxOf(0, currentReps - 1)
                etReps.setText(if (newReps == 0) "" else newReps.toString())
            }

            // 次数增加 - 只更新UI
            btnRepsPlus.setOnClickListener {
                val currentReps = etReps.text.toString().toIntOrNull() ?: 0
                val newReps = currentReps + 1
                etReps.setText(newReps.toString())
            }

            // 删除按钮
            btnDeleteSet.setOnClickListener {
                currentSet?.let { onSetDeleted(it) }
            }

            // 失去焦点时保存
            etWeight.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) saveCurrentData()
            }

            etReps.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) saveCurrentData()
            }
        }

        private fun saveCurrentData() {
            currentSet?.let { set ->
                val weight = etWeight.text.toString().toDoubleOrNull() ?: 0.0
                val reps = etReps.text.toString().toIntOrNull() ?: 0
                if (weight != set.weight || reps != set.reps) {
                    val updatedSet = set.copy(weight = weight, reps = reps)
                    currentSet = updatedSet
                    onSetChanged(updatedSet)
                }
            }
        }

        // 公开方法，供外部调用保存
        fun saveData() {
            saveCurrentData()
        }

        private fun formatWeight(weight: Double): String {
            return if (weight == 0.0) ""
            else if (weight == weight.toLong().toDouble()) weight.toLong().toString()
            else weight.toString()
        }

        fun bind(set: WorkoutSet) {
            currentSet = set
            tvSetNumber.text = set.setNumber.toString()
            etWeight.setText(formatWeight(set.weight))
            etReps.setText(if (set.reps == 0) "" else set.reps.toString())
            cbCompleted.isChecked = set.isCompleted
        }
    }

    // 保存所有未保存的数据
    fun saveAllData(recyclerView: RecyclerView) {
        for (i in 0 until itemCount) {
            val holder = recyclerView.findViewHolderForAdapterPosition(i) as? SetViewHolder
            holder?.saveData()
        }
    }

    class SetDiffCallback : DiffUtil.ItemCallback<WorkoutSet>() {
        override fun areItemsTheSame(oldItem: WorkoutSet, newItem: WorkoutSet): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WorkoutSet, newItem: WorkoutSet): Boolean {
            return oldItem == newItem
        }
    }
}
