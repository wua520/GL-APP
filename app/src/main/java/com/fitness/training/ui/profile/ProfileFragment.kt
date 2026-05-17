package com.fitness.training.ui.profile

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.fitness.training.R
import com.fitness.training.data.database.FitnessDatabase
import com.fitness.training.network.SyncManager
import com.fitness.training.util.BodyProfile
import com.fitness.training.util.ThemeHelper
import com.fitness.training.util.UserSession
import com.fitness.training.utils.AnimationUtils.addCardLiftEffect
import com.fitness.training.utils.AnimationUtils.addClickScaleEffect
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {

    private lateinit var syncManager: SyncManager
    private lateinit var layoutNotLoggedIn: LinearLayout
    private lateinit var layoutLoggedIn: LinearLayout
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnRegister: MaterialButton
    private lateinit var ivAvatar: ImageView
    private lateinit var tvNickname: TextView
    private lateinit var tvUsername: TextView
    private lateinit var btnEditProfile: ImageButton
    private lateinit var btnLogout: MaterialButton
    private lateinit var itemDiet: LinearLayout
    private lateinit var itemBody: LinearLayout
    private lateinit var item1RM: LinearLayout
    private lateinit var itemAchievement: LinearLayout
    private lateinit var itemCloudSync: LinearLayout
    private lateinit var tvCloudSyncStatus: TextView
    private lateinit var progressSync: ProgressBar
    private lateinit var itemSettings: LinearLayout
    private lateinit var itemAbout: LinearLayout
    private lateinit var itemAiAssistant: LinearLayout
    private lateinit var itemDarkMode: LinearLayout
    private lateinit var switchDarkMode: SwitchCompat
    private lateinit var cardBodyProfile: MaterialCardView
    private lateinit var tvBodyGender: TextView
    private lateinit var tvBodyHeight: TextView
    private lateinit var tvBodyAge: TextView
    private lateinit var tvBodyWeight: TextView
    private lateinit var tvBodyFat: TextView
    private lateinit var tvBodyBmi: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        syncManager = SyncManager(requireContext())
        initViews(view)
        setupListeners()
        updateLoginStatus()
        updateBodyProfileDisplay()
        return view
    }
    
    override fun onResume() {
        super.onResume()
        updateLoginStatus()
    }

    private fun initViews(view: View) {
        layoutNotLoggedIn = view.findViewById(R.id.layout_not_logged_in)
        layoutLoggedIn = view.findViewById(R.id.layout_logged_in)
        btnLogin = view.findViewById(R.id.btn_login)
        btnRegister = view.findViewById(R.id.btn_register)
        ivAvatar = view.findViewById(R.id.iv_avatar)
        tvNickname = view.findViewById(R.id.tv_nickname)
        tvUsername = view.findViewById(R.id.tv_username)
        btnEditProfile = view.findViewById(R.id.btn_edit_profile)
        btnLogout = view.findViewById(R.id.btn_logout)
        itemDiet = view.findViewById(R.id.item_diet)
        itemBody = view.findViewById(R.id.item_body)
        item1RM = view.findViewById(R.id.item_1rm)
        itemAchievement = view.findViewById(R.id.item_achievement)
        itemCloudSync = view.findViewById(R.id.item_cloud_sync)
        tvCloudSyncStatus = view.findViewById(R.id.tv_cloud_sync_status)
        progressSync = view.findViewById(R.id.progress_sync)
        itemSettings = view.findViewById(R.id.item_settings)
        itemAbout = view.findViewById(R.id.item_about)
        itemAiAssistant = view.findViewById(R.id.item_ai_assistant)
        itemDarkMode = view.findViewById(R.id.item_dark_mode)
        switchDarkMode = view.findViewById(R.id.switch_dark_mode)
        cardBodyProfile = view.findViewById(R.id.card_body_profile)
        tvBodyGender = view.findViewById(R.id.tv_body_gender)
        tvBodyHeight = view.findViewById(R.id.tv_body_height)
        tvBodyAge = view.findViewById(R.id.tv_body_age)
        tvBodyWeight = view.findViewById(R.id.tv_body_weight)
        tvBodyFat = view.findViewById(R.id.tv_body_fat)
        tvBodyBmi = view.findViewById(R.id.tv_body_bmi)
        
        // 添加按钮点击缩放效果
        btnLogin.addClickScaleEffect()
        btnRegister.addClickScaleEffect()
        btnLogout.addClickScaleEffect()
        
        // 添加卡片抬起效果
        cardBodyProfile.addCardLiftEffect()
        itemDiet.addCardLiftEffect()
        itemBody.addCardLiftEffect()
        item1RM.addCardLiftEffect()
        itemAchievement.addCardLiftEffect()
        itemCloudSync.addCardLiftEffect()
        itemSettings.addCardLiftEffect()
        itemAbout.addCardLiftEffect()
        itemAiAssistant.addCardLiftEffect()
    }

    private fun setupListeners() {
        // 登录/注册按钮 - 现在直接连云端
        btnLogin.setOnClickListener { showLoginDialog() }
        btnRegister.setOnClickListener { showRegisterDialog() }
        btnEditProfile.setOnClickListener { showEditNicknameDialog() }
        btnLogout.setOnClickListener { showLogoutDialog() }
        
        itemDiet.setOnClickListener {
            findNavController().navigate(R.id.navigation_diet)
        }
        
        itemBody.setOnClickListener {
            findNavController().navigate(R.id.navigation_body)
        }
        
        item1RM.setOnClickListener {
            findNavController().navigate(R.id.navigation_one_rm)
        }
        
        itemAchievement.setOnClickListener {
            findNavController().navigate(R.id.navigation_achievement)
        }
        
        // 云端同步
        itemCloudSync.setOnClickListener {
            val token = UserSession.getToken(requireContext())
            if (token != null) {
                performSync()
            } else {
                Toast.makeText(requireContext(), "请先登录账号", Toast.LENGTH_SHORT).show()
            }
        }
        
        itemSettings.setOnClickListener {
            findNavController().navigate(R.id.navigation_settings)
        }
        
        itemAbout.setOnClickListener {
            findNavController().navigate(R.id.navigation_about)
        }
        
        itemAiAssistant.setOnClickListener {
            findNavController().navigate(R.id.navigation_ai_assistant)
        }
        
        cardBodyProfile.setOnClickListener {
            showEditBodyProfileDialog()
        }
        
        // 深色模式切换
        switchDarkMode.isChecked = ThemeHelper.isDarkMode(requireContext())
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            ThemeHelper.setDarkMode(requireContext(), isChecked)
        }
        itemDarkMode.setOnClickListener {
            switchDarkMode.isChecked = !switchDarkMode.isChecked
        }
    }
    
    private fun updateLoginStatus() {
        val token = UserSession.getToken(requireContext())
        val isLoggedIn = token != null
        
        layoutNotLoggedIn.visibility = if (isLoggedIn) View.GONE else View.VISIBLE
        layoutLoggedIn.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
        btnLogout.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
        
        if (isLoggedIn) {
            val username = UserSession.getCloudUsername(requireContext()) ?: "用户"
            val nickname = UserSession.getCloudNickname(requireContext()) ?: username
            tvNickname.text = nickname
            tvUsername.text = "@$username"
            tvCloudSyncStatus.text = "点击同步数据到云端"
        } else {
            tvCloudSyncStatus.text = "登录后可同步数据"
        }
    }
    
    private fun updateAvatarByGender() {
        val gender = BodyProfile.getGender(requireContext())
        val avatarRes = when (gender) {
            1 -> R.drawable.ic_avatar_male    // 男
            2 -> R.drawable.ic_avatar_female  // 女
            else -> R.drawable.ic_avatar_default // 未设置
        }
        ivAvatar.setImageResource(avatarRes)
        ivAvatar.imageTintList = null // 移除tint，显示原始颜色
    }
    
    private fun updateBodyProfileDisplay() {
        tvBodyGender.text = BodyProfile.getGenderText(requireContext())
        tvBodyHeight.text = BodyProfile.getHeightText(requireContext())
        tvBodyAge.text = BodyProfile.getAgeText(requireContext())
        
        // 根据性别自动设置头像
        updateAvatarByGender()
        
        val userId = UserSession.getCurrentUserId(requireContext())
        val database = FitnessDatabase.getDatabase(requireContext())
        
        CoroutineScope(Dispatchers.IO).launch {
            val latestRecord = database.bodyRecordDao().getLatestByUserSync(userId)
            withContext(Dispatchers.Main) {
                if (latestRecord != null) {
                    tvBodyWeight.text = latestRecord.weight?.let { String.format("%.1f", it) } ?: "-"
                    tvBodyFat.text = latestRecord.bodyFat?.let { String.format("%.1f", it) } ?: "-"
                    
                    val height = BodyProfile.getHeight(requireContext())
                    val weight = latestRecord.weight
                    if (height > 0 && weight != null) {
                        val heightM = height / 100f
                        val bmi = weight / (heightM * heightM)
                        tvBodyBmi.text = String.format("%.1f", bmi)
                    } else {
                        tvBodyBmi.text = "-"
                    }
                } else {
                    tvBodyWeight.text = "-"
                    tvBodyFat.text = "-"
                    tvBodyBmi.text = "-"
                }
            }
        }
    }
    
    private fun showEditBodyProfileDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_body_profile, null)
        
        val rbMale = dialogView.findViewById<RadioButton>(R.id.rb_male)
        val rbFemale = dialogView.findViewById<RadioButton>(R.id.rb_female)
        val etHeight = dialogView.findViewById<TextInputEditText>(R.id.et_height)
        val etBirthYear = dialogView.findViewById<TextInputEditText>(R.id.et_birth_year)
        val etWeight = dialogView.findViewById<TextInputEditText>(R.id.et_weight)
        val etBodyFat = dialogView.findViewById<TextInputEditText>(R.id.et_body_fat)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btn_save)
        
        when (BodyProfile.getGender(requireContext())) {
            1 -> rbMale.isChecked = true
            2 -> rbFemale.isChecked = true
        }
        val height = BodyProfile.getHeight(requireContext())
        if (height > 0) etHeight.setText(height.toString())
        val birthYear = BodyProfile.getBirthYear(requireContext())
        if (birthYear > 0) etBirthYear.setText(birthYear.toString())
        
        val userId = UserSession.getCurrentUserId(requireContext())
        val database = FitnessDatabase.getDatabase(requireContext())
        CoroutineScope(Dispatchers.IO).launch {
            val latestRecord = database.bodyRecordDao().getLatestByUserSync(userId)
            withContext(Dispatchers.Main) {
                latestRecord?.weight?.let { etWeight.setText(String.format("%.1f", it)) }
                latestRecord?.bodyFat?.let { etBodyFat.setText(String.format("%.1f", it)) }
            }
        }
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val gender = when {
                rbMale.isChecked -> 1
                rbFemale.isChecked -> 2
                else -> 0
            }
            val heightValue = etHeight.text.toString().toIntOrNull() ?: 0
            val birthYearValue = etBirthYear.text.toString().toIntOrNull() ?: 0
            val weightValue = etWeight.text.toString().toFloatOrNull()
            val bodyFatValue = etBodyFat.text.toString().toFloatOrNull()
            
            BodyProfile.setGender(requireContext(), gender)
            BodyProfile.setHeight(requireContext(), heightValue)
            BodyProfile.setBirthYear(requireContext(), birthYearValue)
            
            if (weightValue != null || bodyFatValue != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    val record = com.fitness.training.data.entity.BodyRecord(
                        userId = userId,
                        weight = weightValue,
                        bodyFat = bodyFatValue
                    )
                    database.bodyRecordDao().insert(record)
                    withContext(Dispatchers.Main) {
                        updateBodyProfileDisplay()
                    }
                }
            }
            
            updateBodyProfileDisplay()
            Toast.makeText(requireContext(), "已保存", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun showLoginDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_login, null)
        val etUsername = dialogView.findViewById<TextInputEditText>(R.id.et_username)
        val etPassword = dialogView.findViewById<TextInputEditText>(R.id.et_password)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnLoginBtn = dialogView.findViewById<MaterialButton>(R.id.btn_login)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progress_bar)
        
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnLoginBtn.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString()
            
            if (username.isBlank() || password.isBlank()) {
                Toast.makeText(requireContext(), "请输入用户名和密码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            btnLoginBtn.isEnabled = false
            progressBar?.visibility = View.VISIBLE
            
            lifecycleScope.launch {
                val result = syncManager.login(username, password)
                withContext(Dispatchers.Main) {
                    btnLoginBtn.isEnabled = true
                    progressBar?.visibility = View.GONE
                    result.onSuccess {
                        Toast.makeText(requireContext(), "登录成功", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        updateLoginStatus()
                    }.onFailure { e ->
                        Toast.makeText(requireContext(), e.message ?: "登录失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun showRegisterDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_register, null)
        val etUsername = dialogView.findViewById<TextInputEditText>(R.id.et_username)
        val etNickname = dialogView.findViewById<TextInputEditText>(R.id.et_nickname)
        val etPassword = dialogView.findViewById<TextInputEditText>(R.id.et_password)
        val etConfirmPwd = dialogView.findViewById<TextInputEditText>(R.id.et_confirm_password)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnRegisterBtn = dialogView.findViewById<MaterialButton>(R.id.btn_register)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progress_bar)
        
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnRegisterBtn.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val nickname = etNickname.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPwd = etConfirmPwd.text.toString()
            
            if (username.length < 3) {
                Toast.makeText(requireContext(), "用户名至少3个字符", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(requireContext(), "密码至少6个字符", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirmPwd) {
                Toast.makeText(requireContext(), "两次密码不一致", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            btnRegisterBtn.isEnabled = false
            progressBar?.visibility = View.VISIBLE
            
            lifecycleScope.launch {
                val result = syncManager.register(username, password, nickname.ifBlank { null })
                withContext(Dispatchers.Main) {
                    btnRegisterBtn.isEnabled = true
                    progressBar?.visibility = View.GONE
                    result.onSuccess {
                        Toast.makeText(requireContext(), "注册成功", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        updateLoginStatus()
                    }.onFailure { e ->
                        Toast.makeText(requireContext(), e.message ?: "注册失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun showEditNicknameDialog() {
        val currentNickname = UserSession.getCloudNickname(requireContext()) ?: ""
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_nickname, null)
        val etNickname = dialogView.findViewById<TextInputEditText>(R.id.et_nickname)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btn_save)
        etNickname.setText(currentNickname)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val nickname = etNickname.text.toString().trim()
            if (nickname.isBlank()) {
                Toast.makeText(requireContext(), "昵称不能为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            UserSession.updateCloudNickname(requireContext(), nickname)
            updateLoginStatus()
            Toast.makeText(requireContext(), "昵称已更新", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("退出登录")
            .setMessage("确定要退出登录吗？退出后数据将不再同步到云端。")
            .setPositiveButton("退出") { _, _ ->
                syncManager.logout()
                updateLoginStatus()
                Toast.makeText(requireContext(), "已退出登录", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun performSync() {
        progressSync.visibility = View.VISIBLE
        tvCloudSyncStatus.text = "正在同步..."
        val userId = UserSession.getCurrentUserId(requireContext())
        
        lifecycleScope.launch {
            val result = syncManager.sync(userId)
            
            withContext(Dispatchers.Main) {
                progressSync.visibility = View.GONE
                
                result.onSuccess {
                    tvCloudSyncStatus.text = "同步成功"
                    Toast.makeText(requireContext(), "同步成功", Toast.LENGTH_SHORT).show()
                }.onFailure { e ->
                    tvCloudSyncStatus.text = "同步失败"
                    Toast.makeText(requireContext(), "同步失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
