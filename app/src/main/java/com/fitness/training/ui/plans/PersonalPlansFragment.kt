package com.fitness.training.ui.plans

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.fitness.training.data.entity.TrainingPlan
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class PersonalPlansFragment : Fragment() {
    
    private lateinit var viewModel: TrainingPlanViewModel
    private lateinit var adapter: TrainingPlanAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var fabAddPlan: FloatingActionButton
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_personal_plans, container, false)
        
        viewModel = ViewModelProvider(this)[TrainingPlanViewModel::class.java]
        
        recyclerView = view.findViewById(R.id.recycler_view_plans)
        layoutEmptyState = view.findViewById(R.id.layout_empty_state)
        fabAddPlan = view.findViewById(R.id.fab_add_plan)
        
        setupRecyclerView()
        setupListeners()
        observePlans()
        
        return view
    }
    
    private fun setupRecyclerView() {
        adapter = TrainingPlanAdapter(
            onDeleteClick = { plan -> showDeleteConfirmDialog(plan) },
            onEditClick = { plan -> showEditPlanDialog(plan) },
            onExpandClick = { },
            onPinClick = { plan -> viewModel.togglePin(plan) }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }
    
    private fun setupListeners() {
        fabAddPlan.setOnClickListener {
            showCreatePlanDialog()
        }
    }
    
    private fun observePlans() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allPlans.collect { plans ->
                adapter.submitList(plans)
                
                if (plans.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    layoutEmptyState.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    layoutEmptyState.visibility = View.GONE
                }
            }
        }
    }
    
    private fun showCreatePlanDialog() {
        showPlanDialog(null)
    }
    
    private fun showEditPlanDialog(plan: TrainingPlan) {
        showPlanDialog(plan)
    }
    
    private fun showPlanDialog(existingPlan: TrainingPlan?) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_plan, null)
        
        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_title)
        val etTitle = dialogView.findViewById<EditText>(R.id.et_plan_title)
        val etDescription = dialogView.findViewById<EditText>(R.id.et_plan_description)
        val etDetails = dialogView.findViewById<EditText>(R.id.et_plan_details)
        val chipGroupGoal = dialogView.findViewById<ChipGroup>(R.id.chip_group_goal)
        val chipGroupDays = dialogView.findViewById<ChipGroup>(R.id.chip_group_days)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btn_save)
        
        val isEdit = existingPlan != null
        tvDialogTitle.text = if (isEdit) "编辑训练计划" else "新建训练计划"
        
        // 如果是编辑，填充现有数据
        existingPlan?.let { plan ->
            etTitle.setText(plan.title)
            etDescription.setText(plan.description)
            etDetails.setText(plan.details)
            
            // 设置目标选中状态
            when (plan.goal) {
                "增肌" -> dialogView.findViewById<Chip>(R.id.chip_muscle_gain).isChecked = true
                "减脂" -> dialogView.findViewById<Chip>(R.id.chip_fat_loss).isChecked = true
                "维持" -> dialogView.findViewById<Chip>(R.id.chip_maintain).isChecked = true
            }
            
            // 设置天数选中状态
            when (plan.trainingDays) {
                3 -> dialogView.findViewById<Chip>(R.id.chip_3_days).isChecked = true
                4 -> dialogView.findViewById<Chip>(R.id.chip_4_days).isChecked = true
                5 -> dialogView.findViewById<Chip>(R.id.chip_5_days).isChecked = true
                6 -> dialogView.findViewById<Chip>(R.id.chip_6_days).isChecked = true
            }
        }
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val details = etDetails.text.toString().trim()
            
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "请输入计划名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val goal = when (chipGroupGoal.checkedChipId) {
                R.id.chip_muscle_gain -> "增肌"
                R.id.chip_fat_loss -> "减脂"
                R.id.chip_maintain -> "维持"
                else -> "增肌"
            }
            
            val trainingDays = when (chipGroupDays.checkedChipId) {
                R.id.chip_3_days -> 3
                R.id.chip_4_days -> 4
                R.id.chip_5_days -> 5
                R.id.chip_6_days -> 6
                else -> 3
            }
            
            val plan = TrainingPlan(
                id = existingPlan?.id ?: 0,
                userId = existingPlan?.userId ?: 0,
                title = title,
                description = description,
                details = details,
                goal = goal,
                experience = existingPlan?.experience ?: "新手",
                targetMuscles = existingPlan?.targetMuscles ?: "",
                trainingDays = trainingDays,
                trainingDuration = existingPlan?.trainingDuration ?: "标准",
                equipment = existingPlan?.equipment ?: "健身房",
                createdAt = existingPlan?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isPinned = existingPlan?.isPinned ?: false,
                isFromRecommendation = false
            )
            
            if (isEdit) {
                viewModel.updatePlan(plan)
                Toast.makeText(requireContext(), "计划已更新", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.insertPlan(plan)
                Toast.makeText(requireContext(), "计划已创建", Toast.LENGTH_SHORT).show()
            }
            
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun showDeleteConfirmDialog(plan: TrainingPlan) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除计划")
            .setMessage("确定要删除「${plan.title}」吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deletePlan(plan)
            }
            .setNegativeButton("取消", null)
            .show()
            .apply {
                // 使用更亮的颜色确保按钮可见
                getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(android.graphics.Color.parseColor("#FF6B6B"))
                getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(android.graphics.Color.WHITE)
            }
    }
}
