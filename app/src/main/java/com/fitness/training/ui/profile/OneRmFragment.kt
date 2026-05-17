package com.fitness.training.ui.profile

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.fitness.training.R
import com.google.android.material.button.MaterialButton

class OneRmFragment : Fragment() {

    private lateinit var btnBack: ImageButton
    private lateinit var etWeight: EditText
    private lateinit var etReps: EditText
    private lateinit var btnCalculate: MaterialButton
    private lateinit var layoutLeft: LinearLayout
    private lateinit var layoutRight: LinearLayout
    private lateinit var tvHint: TextView
    private lateinit var layoutResults: LinearLayout

    // RM 百分比对照表 (1RM-30RM)
    private val rmPercentages = listOf(
        100, 95, 93, 90, 87, 85, 83, 80, 77, 75,
        73, 70, 68, 65, 63, 60, 58, 55, 53, 50,
        48, 45, 43, 42, 40, 39, 38, 37, 36, 35
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_one_rm, container, false)
        
        initViews(view)
        setupListeners()
        
        return view
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btn_back)
        etWeight = view.findViewById(R.id.et_weight)
        etReps = view.findViewById(R.id.et_reps)
        btnCalculate = view.findViewById(R.id.btn_calculate)
        layoutLeft = view.findViewById(R.id.layout_left)
        layoutRight = view.findViewById(R.id.layout_right)
        tvHint = view.findViewById(R.id.tv_hint)
        layoutResults = view.findViewById(R.id.layout_results)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        
        btnCalculate.setOnClickListener {
            calculate()
        }
    }

    private fun calculate() {
        val weight = etWeight.text.toString().toDoubleOrNull()
        val reps = etReps.text.toString().toIntOrNull()
        
        if (weight == null || weight <= 0) {
            etWeight.error = "请输入重量"
            return
        }
        if (reps == null || reps <= 0 || reps > 30) {
            etReps.error = "请输入1-30次"
            return
        }
        
        // 使用 Lander 公式计算 1RM
        // 1RM = (100 × 重量) / (101.3 - 2.67123 × 次数)
        val oneRm = (100 * weight) / (101.3 - 2.67123 * reps)
        
        // 清空之前的结果
        layoutLeft.removeAllViews()
        layoutRight.removeAllViews()
        
        // 生成 1-30RM 的结果
        for (i in 1..30) {
            val percentage = rmPercentages[i - 1]
            val rmWeight = oneRm * percentage / 100.0
            val isHighlight = (i == 1 || i == 5 || i == 8 || i == 10 || i == 12)
            
            val itemView = createRmItemView(i, percentage, rmWeight, isHighlight)
            
            if (i <= 15) {
                layoutLeft.addView(itemView)
            } else {
                layoutRight.addView(itemView)
            }
        }
        
        tvHint.visibility = View.GONE
        layoutResults.visibility = View.VISIBLE
    }
    
    private fun createRmItemView(rmNumber: Int, percentage: Int, weight: Double, isHighlight: Boolean): View {
        val itemView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_rm_result, layoutLeft, false)
        
        val layoutItem = itemView.findViewById<LinearLayout>(R.id.layout_item)
        val tvRmNumber = itemView.findViewById<TextView>(R.id.tv_rm_number)
        val tvPercentage = itemView.findViewById<TextView>(R.id.tv_percentage)
        val tvWeight = itemView.findViewById<TextView>(R.id.tv_weight)
        
        tvRmNumber.text = rmNumber.toString()
        tvPercentage.text = "${percentage}%"
        tvWeight.text = formatWeight(weight)
        
        if (isHighlight) {
            layoutItem.setBackgroundResource(R.drawable.bg_rm_highlight)
            tvRmNumber.setTextColor(Color.WHITE)
            tvPercentage.setTextColor(Color.WHITE)
            tvWeight.setTextColor(Color.WHITE)
        } else {
            layoutItem.background = null
            tvRmNumber.setTextColor(requireContext().getColor(R.color.text_primary))
            tvPercentage.setTextColor(requireContext().getColor(R.color.text_secondary))
            tvWeight.setTextColor(requireContext().getColor(R.color.text_primary))
        }
        
        return itemView
    }
    
    private fun formatWeight(weight: Double): String {
        return if (weight == weight.toLong().toDouble()) {
            weight.toLong().toString()
        } else {
            String.format("%.1f", weight)
        }
    }
}
