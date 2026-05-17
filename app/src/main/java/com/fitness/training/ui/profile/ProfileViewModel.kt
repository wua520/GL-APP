package com.fitness.training.ui.profile

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.fitness.training.data.database.FitnessDatabase
import com.fitness.training.data.entity.User
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    
    private val userDao = FitnessDatabase.getDatabase(application).userDao()
    private val prefs = application.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    
    private val _currentUser = MutableLiveData<User?>(null)
    val currentUser: LiveData<User?> = _currentUser
    
    private val _isLoggedIn = MutableLiveData(false)
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn
    
    init {
        checkLoginStatus()
    }
    
    private fun checkLoginStatus() {
        val userId = prefs.getLong("user_id", -1)
        if (userId > 0) {
            viewModelScope.launch {
                val user = userDao.getUserById(userId)
                if (user != null) {
                    _currentUser.value = user
                    _isLoggedIn.value = true
                } else {
                    logout()
                }
            }
        }
    }
    
    fun login(username: String, password: String, onResult: (Boolean, String) -> Unit) {
        if (username.isBlank()) {
            onResult(false, "请输入用户名")
            return
        }
        if (password.isBlank()) {
            onResult(false, "请输入密码")
            return
        }
        
        viewModelScope.launch {
            val user = userDao.login(username, password)
            if (user != null) {
                prefs.edit().putLong("user_id", user.id).apply()
                _currentUser.value = user
                _isLoggedIn.value = true
                onResult(true, "登录成功")
            } else {
                onResult(false, "用户名或密码错误")
            }
        }
    }
    
    fun register(
        username: String,
        nickname: String,
        password: String,
        confirmPassword: String,
        onResult: (Boolean, String) -> Unit
    ) {
        if (username.isBlank()) {
            onResult(false, "请输入用户名")
            return
        }
        if (username.length < 3) {
            onResult(false, "用户名至少3个字符")
            return
        }
        if (password.isBlank()) {
            onResult(false, "请输入密码")
            return
        }
        if (password.length < 6) {
            onResult(false, "密码至少6个字符")
            return
        }
        if (password != confirmPassword) {
            onResult(false, "两次密码不一致")
            return
        }
        
        viewModelScope.launch {
            val exists = userDao.isUsernameExists(username)
            if (exists > 0) {
                onResult(false, "用户名已存在")
                return@launch
            }
            
            val user = User(
                username = username,
                password = password,
                nickname = nickname.ifBlank { username }
            )
            val userId = userDao.insert(user)
            
            prefs.edit().putLong("user_id", userId).apply()
            _currentUser.value = user.copy(id = userId)
            _isLoggedIn.value = true
            onResult(true, "注册成功")
        }
    }
    
    fun logout() {
        prefs.edit().remove("user_id").apply()
        _currentUser.value = null
        _isLoggedIn.value = false
    }
    
    fun updateNickname(nickname: String, onResult: (Boolean) -> Unit) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updatedUser = user.copy(nickname = nickname)
            userDao.update(updatedUser)
            _currentUser.value = updatedUser
            onResult(true)
        }
    }
}
