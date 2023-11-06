package com.example.foodOrderingSystem.ui.table

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.foodOrderingSystem.R
import com.example.foodOrderingSystem.databinding.FragmentTableBinding
import com.example.foodOrderingSystem.databinding.FragmentTableCustomerOrderBinding
import com.example.foodOrderingSystem.utils.Utils

class TableCustomerOrderFragment: Fragment() {

    private var _binding: FragmentTableCustomerOrderBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTableCustomerOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topAppBar.setNavigationOnClickListener { backToPrevious() }
    }

    private fun backToPrevious() {
        Utils().backToPrevious(this, R.id.navigation_table)
    }
}