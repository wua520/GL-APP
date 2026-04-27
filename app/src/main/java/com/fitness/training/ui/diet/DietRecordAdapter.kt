package com.fitness.training.ui.diet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.fitness.training.data.entity.DietRecord

class DietRecordAdapter(
    private val onDeleteClick: (DietRecord) -> Unit
) : ListAdapter<DietRecord, DietRecordAdapter.RecordViewHolder>(RecordDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diet_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFoodName: TextView = itemView.findViewById(R.id.tv_food_name)
        private val tvMealType: TextView = itemView.findViewById(R.id.tv_meal_type)
        private val tvCalories: TextView = itemView.findViewById(R.id.tv_calories)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)

        fun bind(record: DietRecord) {
            tvFoodName.text = record.foodName
            tvMealType.text = record.amount
            tvCalories.text = "${record.calories}千卡"
            
            btnDelete.setOnClickListener {
                onDeleteClick(record)
            }
        }
    }

    class RecordDiffCallback : DiffUtil.ItemCallback<DietRecord>() {
        override fun areItemsTheSame(oldItem: DietRecord, newItem: DietRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DietRecord, newItem: DietRecord): Boolean {
            return oldItem == newItem
        }
    }
}
