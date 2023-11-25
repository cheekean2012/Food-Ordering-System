package com.example.foodOrderingSystem.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.foodOrderingSystem.ui.reports.ReportInfoFragment
import com.example.foodOrderingSystem.ui.reports.ReportItemFragment

class ReportTabePageAdapter(fragment: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragment, lifecycle) {

    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                ReportInfoFragment()
            }
            1 -> {
                ReportItemFragment()
            }
            else -> ReportInfoFragment()
        }
    }
}