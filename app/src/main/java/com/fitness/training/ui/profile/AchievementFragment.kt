package com.fitness.training.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.fitness.training.data.repository.AchievementRepository
import com.fitness.training.util.UserSession
import kotlinx.coroutines.launch

class AchievementFragment : Fragment() {

    private lateinit var btnBack: ImageButton
    private lateinit var tvUnlockedCount: TextView
    private lateinit var recyclerAchievements: RecyclerView
    
    private lateinit var adapter: AchievementAdapter
    private lateinit var repository: AchievementRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_achievement, container, false)
        
        repository = AchievementRepository(requireContext())
        
        initViews(view)
        setupRecyclerView()
        setupListeners()
        loadData()
        
        return view
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btn_back)
        tvUnlockedCount = view.findViewById(R.id.tv_unlocked_count)
        recyclerAchievements = view.findViewById(R.id.recycler_achievements)
    }

    private fun setupRecyclerView() {
        adapter = AchievementAdapter()
        recyclerAchievements.layoutManager = GridLayoutManager(requireContext(), 3)
        recyclerAchievements.adapter = adapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun loadData() {
        // 使用 UserSession 获取 userId（未登录时为 0）
        val userId = UserSession.getCurrentUserId(requireContext())
        
        lifecycleScope.launch {
            // 初始化成就数据
            repository.initAchievements()
            // 检查并更新成就
            repository.checkAndUpdateAchievements(userId)
        }
        
        // 观察成就数据
        repository.allAchievements.observe(viewLifecycleOwner) { achievements ->
            adapter.submitList(achievements)
            val unlocked = achievements.count { it.isUnlocked }
            tvUnlockedCount.text = "$unlocked/${achievements.size}"
        }
    }
}
