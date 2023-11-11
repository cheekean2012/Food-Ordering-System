package com.example.foodOrderingSystem

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.example.foodOrderingSystem.databinding.FragmentMenuItemDetailBinding
import com.example.foodOrderingSystem.models.MenuItemViewModel
import com.example.foodOrderingSystem.models.MenuTypeViewModel
import com.example.foodOrderingSystem.utils.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MenuItemDetailFragment : Fragment() {

    private var _binding: FragmentMenuItemDetailBinding? = null
    private val binding get() = _binding!!
    private var mImageUri: Uri? = null
    private lateinit var dialog: Dialog
    private val menuTypeViewModel: MenuTypeViewModel by activityViewModels()
    private val menuItemViewModel: MenuItemViewModel by activityViewModels()

    // Gallery setting for new API
    private var galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) {

        if (it != null) {
            // Get gallery image uri
            mImageUri = it

            menuItemViewModel.setImage(mImageUri!!)
            Log.d("image Uri", "$mImageUri")
            val image: ImageView = dialog.findViewById(R.id.detail_menu_image)

            Glide.with(requireContext())
                .load(mImageUri)
                .into(image)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMenuItemDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            val imageUri: Uri = menuItemViewModel.menuImage.value.toString().toUri()

            Glide.with(requireContext())
                .load(imageUri)
                .into(detailMenuImage)

            viewModel = menuItemViewModel

            // Specify the fragment as the lifecycle owner
            lifecycleOwner = viewLifecycleOwner

            val defaultMenuType = menuItemViewModel.menuType.value
            if (defaultMenuType != null) {
                val defaultIndex = menuTypeViewModel.menuTypeList.value!!.indexOfFirst { it.menuType == defaultMenuType }
                if (defaultIndex != -1) {
                    menuTypeSpinner.setSelection(defaultIndex)
                }
            }

            topAppBar.setNavigationOnClickListener { backToPrevious() }
            deleteMenuItem.setOnClickListener{ deleteItem() }
        }

        menuTypeViewModel.menuTypeList.observe(viewLifecycleOwner) { menuTypes ->
            // Create an ArrayAdapter using the menuTypes
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                menuTypes.map { it.menuType })

            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            val spinner: Spinner = binding.menuTypeSpinner

            // Apply the adapter to the spinner
            spinner.adapter = adapter

            // Set the OnItemSelectedListener
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parentView: AdapterView<*>?,
                    selectedItemView: View?,
                    position: Int,
                    id: Long
                ) {
                    // Get the selected menu type
                    menuTypeViewModel.setMenuType(menuTypes[position].menuType)
                }

                override fun onNothingSelected(parentView: AdapterView<*>?) {
                    // Do nothing here if nothing is selected
                }
            }
        }
    }

    private fun deleteItem() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(requireContext().getString(R.string.delete_menu_item))
            .setMessage(requireContext().getString(R.string.confirm_delete_menu_item))
            .setNegativeButton(requireActivity().resources.getString(R.string.cancel)) { dialog, _ ->
                // Respond to negative button press
                dialog.dismiss()
            }
            .setPositiveButton(requireActivity().resources.getString(R.string.ok)) { _, _ ->
                // Delete data from menu view model
                menuItemViewModel.deleteMenuItem(menuItemViewModel.menuItemId.value.toString())
                backToPrevious()
            }
            .show()
    }

    private fun backToPrevious() {
        Utils().backToPrevious(this, R.id.navigation_menu_item_list)
    }


}