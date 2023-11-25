package com.example.foodOrderingSystem.ui.reports

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.foodOrderingSystem.R
import com.example.foodOrderingSystem.adapters.ReportTabePageAdapter
import com.example.foodOrderingSystem.databinding.FragmentReportDetailBinding
import com.example.foodOrderingSystem.databinding.FragmentTableCustomerOrderBinding
import com.example.foodOrderingSystem.utils.Utils
import com.google.android.material.tabs.TabLayoutMediator

class ReportDetailFragment : Fragment() {

    private var _binding: FragmentReportDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentReportDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            topAppBar.setOnClickListener { backToPrevious() }
        }
        tabLayoutMediator()
    }

    private fun tabLayoutMediator() {

        val tabLayout = binding.tabLayout
        val viewPager2 = binding.viewPager2

        val adapter = ReportTabePageAdapter(requireActivity().supportFragmentManager, requireActivity().lifecycle)
        viewPager2.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            when (position) {
                0 -> { tab.text = getString(R.string.info) }
                1 -> { tab.text = getString(R.string.item) }
            }
        }.attach()
    }

    private fun backToPrevious() {
        Utils().backToPrevious(this, R.id.navigation_reportFragment)
    }

}