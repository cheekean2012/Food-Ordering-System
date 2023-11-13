package com.example.foodOrderingSystem.menu

import android.Manifest
import android.app.Dialog

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodOrderingSystem.R
import com.example.foodOrderingSystem.adapters.MenuItemListAdapter
import com.example.foodOrderingSystem.databinding.FragmentMenuItemListBinding
import com.example.foodOrderingSystem.firestore.Firestore
import com.example.foodOrderingSystem.models.MenuItem
import com.example.foodOrderingSystem.models.MenuItemViewModel
import com.example.foodOrderingSystem.models.MenuTypeViewModel
import com.example.foodOrderingSystem.utils.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.util.UUID

class MenuItemListFragment : Fragment() {

    private var _binding: FragmentMenuItemListBinding? = null
    private val binding get() = _binding!!
    private var mImageUri: Uri? = null
    private lateinit var dialog: Dialog
    private lateinit var recyclerView: RecyclerView
    private lateinit var menuItemList: LiveData<MutableList<MenuItem>>
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
        _binding = FragmentMenuItemListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        menuItemList = menuItemViewModel.menuItemList

        recyclerView = binding.menuItemListRecycleView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Observe changes in the tableList LiveData
        menuItemList.observe(viewLifecycleOwner) {
            // Update your RecyclerView adapter when the LiveData changes
            recyclerView.adapter = MenuItemListAdapter(this, requireContext(), menuItemList)
        }

        // Get data from firebase
        Firestore().loadSpinnerMenuType(menuTypeViewModel)
        Firestore().getMenuItem(this, recyclerView, menuItemViewModel,)

        binding.apply {
            addMenuItem.setOnClickListener{ addMenuItem() }
            topAppBar.setNavigationOnClickListener { backToPrevious() }
        }

    }

    private fun addMenuItem() {

        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_menu_item, null)

        dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .show()

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val image: ImageView = dialogView.findViewById(R.id.detail_menu_image)
        val menuItemName: EditText = dialogView.findViewById(R.id.menu_item_name_editText)
        val spinner: Spinner = dialogView.findViewById(R.id.menu_type_spinner)
        val menuItemPrice: EditText = dialogView.findViewById(R.id.menu_item_price_editText)
        val menuItemIngredient: EditText = dialogView.findViewById(R.id.menu_item_ingredient_editText)
        val menuItemAvailable: SwitchMaterial  = dialogView.findViewById(R.id.menu_item_available)
        val menuItemButton: Button = dialogView.findViewById(R.id.menu_item_button)

        menuTypeViewModel.menuTypeList.observe(viewLifecycleOwner) { menuTypes ->
            // Create an ArrayAdapter using the menuTypes
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                menuTypes.map { it.menuType })

            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

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

        image.setOnClickListener { uploadPhoto() }

        menuItemButton.setOnClickListener {
            val regex = Regex("^\\d+([.]\\d{1,2})?\$")

            val uniqueID = UUID.randomUUID().toString()
            val displayImage = menuItemViewModel.menuImage.value.toString()
            val itemName = menuItemName.text.toString().trim { it <= ' ' }
            val itemType = menuTypeViewModel.menuType.value.toString()
            val itemPrice = menuItemPrice.text.toString().trim { it <= ' ' }
            val itemIngredient = menuItemIngredient.text.toString().trim { it <= ' ' }
            val itemAvailable = menuItemAvailable.isChecked

            val valid = regex.matches(itemPrice)

            if (valid) {
                val menuItem = MenuItem(
                    uniqueID,
                    displayImage,
                    itemName,
                    itemType,
                    itemPrice,
                    itemIngredient,
                    itemAvailable
                )
                menuItemViewModel.addMenuItem(menuItem)
                Log.d("get menu item value: ", menuItem.toString())
                recyclerView.adapter = MenuItemListAdapter(this, requireContext(), menuItemList)
                Firestore().uploadMenuItem(this, requireContext(), menuItem, recyclerView, menuItemList)
                dialog.dismiss()

            } else {
                Toast.makeText(requireContext(), "Please enter correct information", Toast.LENGTH_SHORT).show()
            }


        }

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
        Utils().backToPrevious(this, R.id.navigation_settings)
    }

}