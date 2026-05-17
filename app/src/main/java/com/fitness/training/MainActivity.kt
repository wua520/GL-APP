package com.fitness.training

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.fitness.training.databinding.ActivityMainBinding
import com.fitness.training.data.database.FitnessDatabase
import com.fitness.training.util.ThemeHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // 在setContentView之前应用主题
        ThemeHelper.applySavedTheme(this)
        
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 隐藏 ActionBar
        supportActionBar?.hide()
        
        // 一次性清理：修复被错误标记为自定义的动作
        fixFakeCustomExercises()
        
        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        
        navView.setupWithNavController(navController)
    }
    
    private fun fixFakeCustomExercises() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val hasFixed = prefs.getBoolean("fake_custom_exercises_fixed", false)
        
        if (!hasFixed) {
            // 只执行一次
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = FitnessDatabase.getDatabase(this@MainActivity)
                    // 将所有自定义动作改为非自定义，除了"111"
                    database.exerciseDao().fixFakeCustomExercises("111")
                    
                    // 标记已修复
                    prefs.edit().putBoolean("fake_custom_exercises_fixed", true).apply()
                    Log.d("MainActivity", "已修复伪自定义动作")
                } catch (e: Exception) {
                    Log.e("MainActivity", "修复伪自定义动作失败: ${e.message}")
                }
            }
        }
    }
}


