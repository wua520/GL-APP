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
import kotlinx.coroutines.flow.first
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
        // 账号安全
        view.findViewById<LinearLayout>(R.id.item_change_password).setOnClickListener {
            showChangePasswordDialog()
        }
        
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
            
            // 清除收藏状态
            val allExercises = database.exerciseDao().getAllExercises().first()
            allExercises.filter { it.isFavorite }.forEach { exercise ->
                database.exerciseDao().updateFavorite(exercise.id, false)
            }
            
            withContext(Dispatchers.Main) {
                // 清除身体档案（SharedPreferences）
                com.fitness.training.util.BodyProfile.clearProfile(requireContext())
                
                Toast.makeText(context, "所有数据已清除", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showChangePasswordDialog() {
        // 检查是否登录云端账号
        val syncManager = com.fitness.training.network.SyncManager(requireContext())
        if (syncManager.getCloudUsername() == null) {
            Toast.makeText(context, "请先登录云端账号", Toast.LENGTH_SHORT).show()
            return
        }
        
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(android.R.layout.simple_list_item_1, null)
        
        // 创建自定义布局
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
        }
        
        val etOldPassword = com.google.android.material.textfield.TextInputEditText(requireContext()).apply {
            hint = "旧密码"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val tilOldPassword = com.google.android.material.textfield.TextInputLayout(requireContext()).apply {
            addView(etOldPassword)
        }
        
        val etNewPassword = com.google.android.material.textfield.TextInputEditText(requireContext()).apply {
            hint = "新密码（至少6位）"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val tilNewPassword = com.google.android.material.textfield.TextInputLayout(requireContext()).apply {
            addView(etNewPassword)
        }
        
        val etConfirmPassword = com.google.android.material.textfield.TextInputEditText(requireContext()).apply {
            hint = "确认新密码"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val tilConfirmPassword = com.google.android.material.textfield.TextInputLayout(requireContext()).apply {
            addView(etConfirmPassword)
        }
        
        layout.addView(tilOldPassword)
        layout.addView(tilNewPassword)
        layout.addView(tilConfirmPassword)
        
        AlertDialog.Builder(requireContext())
            .setTitle("修改密码")
            .setView(layout)
            .setPositiveButton("确定") { _, _ ->
                val oldPassword = etOldPassword.text?.toString() ?: ""
                val newPassword = etNewPassword.text?.toString() ?: ""
                val confirmPassword = etConfirmPassword.text?.toString() ?: ""
                
                when {
                    oldPassword.isEmpty() -> {
                        Toast.makeText(context, "请输入旧密码", Toast.LENGTH_SHORT).show()
                    }
                    newPassword.isEmpty() -> {
                        Toast.makeText(context, "请输入新密码", Toast.LENGTH_SHORT).show()
                    }
                    newPassword.length < 6 -> {
                        Toast.makeText(context, "新密码长度不能少于6位", Toast.LENGTH_SHORT).show()
                    }
                    newPassword != confirmPassword -> {
                        Toast.makeText(context, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        changePassword(oldPassword, newPassword)
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun changePassword(oldPassword: String, newPassword: String) {
        val syncManager = com.fitness.training.network.SyncManager(requireContext())
        
        lifecycleScope.launch {
            try {
                val result = syncManager.changePassword(oldPassword, newPassword)
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        Toast.makeText(context, "密码修改成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, result.exceptionOrNull()?.message ?: "密码修改失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "密码修改失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
