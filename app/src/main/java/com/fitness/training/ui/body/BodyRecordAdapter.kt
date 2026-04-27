package com.fitness.training.ui.body

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.fitness.training.data.entity.BodyRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BodyRecordAdapter(
    private val onEditClick: (BodyRecord) -> Unit,
    private val onDeleteClick: (BodyRecord) -> Unit
) : ListAdapter<BodyRecord, BodyRecordAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_body_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        private val tvWeight: TextView = itemView.findViewById(R.id.tv_weight)
        private val tvBodyFat: TextView = itemView.findViewById(R.id.tv_body_fat)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)

        fun bind(record: BodyRecord) {
            tvDate.text = dateFormat.format(Date(record.date))
            tvWeight.text = record.weight?.let { String.format("%.1f kg", it) } ?: "-"
            tvBodyFat.text = record.bodyFat?.let { String.format("%.1f%%", it) } ?: "-"
            
            btnEdit.setOnClickListener { onEditClick(record) }
            btnDelete.setOnClickListener { onDeleteClick(record) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BodyRecord>() {
        override fun areItemsTheSame(oldItem: BodyRecord, newItem: BodyRecord) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: BodyRecord, newItem: BodyRecord) = oldItem == newItem
    }
}
