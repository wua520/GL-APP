package com.fitness.training.ui.profile

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R

data class RmResult(
    val rmNumber: Int,
    val percentage: Int,
    val weight: Double,
    val isHighlight: Boolean = false
)

class RmResultAdapter : RecyclerView.Adapter<RmResultAdapter.ViewHolder>() {

    private var items: List<RmResult> = emptyList()

    fun submitList(newItems: List<RmResult>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rm_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val layoutItem: LinearLayout = itemView.findViewById(R.id.layout_item)
        private val tvRmNumber: TextView = itemView.findViewById(R.id.tv_rm_number)
        private val tvPercentage: TextView = itemView.findViewById(R.id.tv_percentage)
        private val tvWeight: TextView = itemView.findViewById(R.id.tv_weight)

        fun bind(data: RmResult) {
            tvRmNumber.text = data.rmNumber.toString()
            tvPercentage.text = "${data.percentage}%"
            tvWeight.text = formatWeight(data.weight)

            if (data.isHighlight) {
                layoutItem.setBackgroundResource(R.drawable.bg_rm_highlight)
                tvRmNumber.setTextColor(Color.WHITE)
                tvPercentage.setTextColor(Color.WHITE)
                tvWeight.setTextColor(Color.WHITE)
            } else {
                layoutItem.background = null
                tvRmNumber.setTextColor(itemView.context.getColor(R.color.text_primary))
                tvPercentage.setTextColor(itemView.context.getColor(R.color.text_secondary))
                tvWeight.setTextColor(itemView.context.getColor(R.color.text_primary))
            }
        }

        private fun formatWeight(weight: Double): String {
            return if (weight == weight.toLong().toDouble()) {
                weight.toLong().toString()
            } else {
                String.format("%.1f", weight)
            }
        }
    }
}
