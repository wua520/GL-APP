package com.fitness.training.ui.plans

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class PlansPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    
    override fun getItemCount(): Int = 2
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> RecommendedPlansFragment()
            1 -> PersonalPlansFragment()
            else -> RecommendedPlansFragment()
        }
    }
}
