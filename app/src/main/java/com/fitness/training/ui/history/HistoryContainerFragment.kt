package com.fitness.training.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.fitness.training.R
import com.fitness.training.ui.statistics.StatisticsFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.viewpager2.widget.ViewPager2

class HistoryContainerFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_history_container, container, false)
        
        tabLayout = view.findViewById(R.id.tab_layout)
        viewPager = view.findViewById(R.id.view_pager)
        
        setupViewPager()
        
        return view
    }

    private fun setupViewPager() {
        viewPager.adapter = HistoryPagerAdapter(this)
        
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "训练日历"
                1 -> "训练记录"
                2 -> "数据统计"
                else -> ""
            }
        }.attach()
    }

    inner class HistoryPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> CalendarFragment()
                1 -> HistoryFragment()
                2 -> StatisticsFragment()
                else -> HistoryFragment()
            }
        }
    }
}
