package com.example.foodOrderingSystem

import android.Manifest
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.example.foodOrderingSystem.databinding.FragmentMenuItemDetailBinding
import com.example.foodOrderingSystem.firestore.Firestore
import com.example.foodOrderingSystem.models.MenuItemViewModel
import com.example.foodOrderingSystem.models.MenuTypeViewModel
import com.example.foodOrderingSystem.utils.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener

class MenuItemDetailFragment : Fragment() {

    private var _binding: FragmentMenuItemDetailBinding? = null
    private val binding get() = _binding!!
    private var mImageUri: Uri? = null
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

            Glide.with(requireContext())
                .load(mImageUri)
                .into(binding.detailMenuImage)
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
            Log.d("get type value", defaultMenuType.toString())
            if (defaultMenuType != null) {
                val defaultIndex = menuTypeViewModel.menuTypeList.value!!.indexOfFirst { it.menuType == defaultMenuType }
                Log.d("get selected index", defaultIndex.toString())
                if (defaultIndex != -1) {
                    val handler = Handler(Looper.getMainLooper())
                    handler.post {
                        // Set the selection in the spinner
                        menuTypeSpinner.setSelection(defaultIndex)
                        Log.d("get selected index", defaultIndex.toString())
                    }
                }
            }

            topAppBar.setNavigationOnClickListener { backToPrevious() }
            detailMenuImage.setOnClickListener { uploadPhoto() }
            deleteMenuItem.setOnClickListener{ deleteItem() }
            menuItemButton.setOnClickListener { updateMenuItemDetail() }
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

    private fun updateMenuItemDetail() {
        binding.apply {
            val id = menuItemViewModel.menuItemId.value.toString()
            val image = menuItemViewModel.menuImage.value.toString()
            val menuItemName = menuItemNameEditText.text.toString().trim() { it <= ' ' }
            val menuType = menuTypeViewModel.menuType.value.toString().trim() { it <= ' ' }
            val menuItemPrice = menuItemPriceEditText.text.toString().trim() { it <= ' ' }
            val ingredient = menuItemIngredientEditText.text.toString().trim() { it <= ' '}
            val available = menuItemAvailable.isChecked

            val regex = Regex("^\\d+([.]\\d{1,2})?\$")
            val valid = regex.matches(menuItemPrice)

            if (valid) {
                val menuItem = com.example.foodOrderingSystem.models.MenuItem(
                    id,
                    image,
                    menuItemName,
                    menuType,
                    menuItemPrice,
                    ingredient,
                    available
                )
                menuItemViewModel.updateMenuItem(menuItem)
                Firestore().updateMenuItem(this@MenuItemDetailFragment, menuItem)
                menuItemViewModel.resetValue()
                backToPrevious()
            } else {
                Toast.makeText(requireContext(), "Please enter correct information", Toast.LENGTH_SHORT).show()
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
                val id = menuItemViewModel.menuItemId.value.toString()
                Firestore().deleteMenuItem(id)
                backToPrevious()
            }
            .show()
    }

    private fun uploadPhoto() {

        Dexter.withContext(requireContext())
            .withPermission(
                Manifest.permission.READ_EXTERNAL_STORAGE
            ).withListener(object: PermissionListener {
                // Permission granted
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    galleryLauncher.launch("image/*")
                }
                // Permission denied
                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(requireContext(), "You have denied the storage permission to select image",
                        Toast.LENGTH_SHORT).show()
                }
                // Permission denied checked if the user or admin denied before
                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {
                    showRationalDialogForPermission()
                }
            }).onSameThread().check()
    }

    // Permission denied dialog
    private fun showRationalDialogForPermission() {
        MaterialAlertDialogBuilder(requireContext()).setMessage("It looks like you have turned off permission" +
                " required for this feature. It can be enabled under application setting.")
            .setPositiveButton("Go to setting") {
                    _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", requireActivity().packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") {
                    dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun backToPrevious() {
        Utils().backToPrevious(this, R.id.navigation_menu_item_list)
    }


}