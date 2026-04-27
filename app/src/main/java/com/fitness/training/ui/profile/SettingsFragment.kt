package com.fitness.training.ui.profile

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.lifecycleScope
import com.fitness.training.R
import com.fitness.training.data.database.FitnessDatabase
import com.fitness.training.util.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment() {

    private lateinit var database: FitnessDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        database = FitnessDatabase.getDatabase(requireContext())
        
        view.findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            findNavController().navigateUp()
        }
        
        initListeners(view)
        return view
    }

    private fun initListeners(view: View) {
        // 数据管理
        view.findViewById<LinearLayout>(R.id.item_clear_workout).setOnClickListener {
            showConfirmDialog("清除训练数据", "确定要删除所有训练记录吗？") { clearWorkoutData() }
        }
        view.findViewById<LinearLayout>(R.id.item_clear_diet).setOnClickListener {
            showConfirmDialog("清除饮食记录", "确定要删除所有饮食数据吗？") { clearDietData() }
        }
        view.findViewById<LinearLayout>(R.id.item_clear_body).setOnClickListener {
            showConfirmDialog("清除体重记录", "确定要删除所有体重数据吗？") { clearBodyData() }
        }
        view.findViewById<LinearLayout>(R.id.item_clear_template).setOnClickListener {
            showConfirmDialog("清除训练模板", "确定要删除所有模板吗？") { clearTemplateData() }
        }
        view.findViewById<LinearLayout>(R.id.item_clear_all).setOnClickListener { showDangerDialog() }
    }

    private fun showConfirmDialog(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("确定") { _, _ -> onConfirm() }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDangerDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("危险操作")
            .setMessage("确定要删除所有数据吗？此操作无法恢复！")
            .setPositiveButton("删除全部") { _, _ ->
                AlertDialog.Builder(requireContext())
                    .setTitle("最后确认")
                    .setMessage("真的要删除吗？")
                    .setPositiveButton("确定") { _, _ -> clearAllData() }
                    .setNegativeButton("取消", null)
                    .show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun clearWorkoutData() {
        val userId = UserSession.getCurrentUserId(requireContext())
        lifecycleScope.launch(Dispatchers.IO) {
            database.workoutSetDao().deleteByUser(userId)
            database.workoutExerciseDao().deleteByUser(userId)
            database.workoutDao().deleteByUser(userId)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "训练数据已清除", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearDietData() {
        val userId = UserSession.getCurrentUserId(requireContext())
        lifecycleScope.launch(Dispatchers.IO) {
            database.dietRecordDao().deleteByUser(userId)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "饮食记录已清除", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearBodyData() {
        val userId = UserSession.getCurrentUserId(requireContext())
        lifecycleScope.launch(Dispatchers.IO) {
            database.bodyRecordDao().deleteByUser(userId)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "体重记录已清除", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearTemplateData() {
        val userId = UserSession.getCurrentUserId(requireContext())
        lifecycleScope.launch(Dispatchers.IO) {
            database.templateExerciseDao().deleteByUser(userId)
            database.workoutTemplateDao().deleteByUser(userId)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "训练模板已清除", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearAllData() {
        val userId = UserSession.getCurrentUserId(requireContext())
        lifecycleScope.launch(Dispatchers.IO) {
            // 训练数据
            database.workoutSetDao().deleteByUser(userId)
            database.workoutExerciseDao().deleteByUser(userId)
            database.workoutDao().deleteByUser(userId)
            // 饮食记录
            database.dietRecordDao().deleteByUser(userId)
            // 体重记录
            database.bodyRecordDao().deleteByUser(userId)
            // 训练模板
            database.templateExerciseDao().deleteByUser(userId)
            database.workoutTemplateDao().deleteByUser(userId)
            // 训练计划
            database.trainingPlanDao().deleteByUser(userId)
            // 自定义动作
            database.exerciseDao().deleteCustomExercises()
            // 自定义食物
            database.foodDao().deleteCustomFoods(userId)
            // 成就进度
            database.achievementDao().resetAll()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "所有数据已清除", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
