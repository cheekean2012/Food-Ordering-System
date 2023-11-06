package com.example.foodOrderingSystem.utils

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController



class Utils: Fragment() {
    // Set to go back to previous page
    fun backToPrevious(fragment: Fragment, navItemId: Int) {
        fragment.findNavController().popBackStack(navItemId, false)
    }

    // Set to go next page
    fun goToNextNavigate(fragment: Fragment, navItemId: Int) {
        fragment.findNavController().navigate(navItemId)
    }
}