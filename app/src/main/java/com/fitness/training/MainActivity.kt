package com.fitness.training

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.fitness.training.databinding.ActivityMainBinding
import com.fitness.training.util.ThemeHelper
import com.google.android.material.bottomnavigation.BottomNavigationView

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
        
        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        
        navView.setupWithNavController(navController)
    }
}


