package com.example.foodOrderingSystem.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.foodOrderingSystem.R
import com.example.foodOrderingSystem.databinding.FragmentSettingsBinding
import com.example.foodOrderingSystem.utils.Utils
import com.google.android.material.bottomnavigation.BottomNavigationView

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navView: BottomNavigationView = binding.navViewSettings
        val navController = findNavController()
        navView.setupWithNavController(navController)

        navView.setOnItemSelectedListener  { item ->
            when (item.itemId) {
                R.id.navigation_table -> {
                    if (!item.isChecked) {
                        navController.navigate(R.id.navigation_table)
                    }
                    true
                }
                R.id.navigation_settings -> {
                    if (!item.isChecked) {
                        navController.navigate(R.id.navigation_settings)
                    }
                    true
                }
                else -> false
            }
        }

        binding.apply {
            menuTypeTextview.setOnClickListener { goToNextPage(R.id.action_navigation_settings_to_menuTypeFragment) }
            menuItemTextview.setOnClickListener { goToNextPage(R.id.action_navigation_settings_to_menuItemListFragment) }
        }

    }

    private fun goToNextPage(nextDestination: Int) {
        Utils().goToNextNavigate(this, nextDestination)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}