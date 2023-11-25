package com.example.foodOrderingSystem.ui.menus

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodOrderingSystem.R
import com.example.foodOrderingSystem.adapters.MenuTypeListAdapter
import com.example.foodOrderingSystem.databinding.FragmentMenuTypeBinding
import com.example.foodOrderingSystem.firestore.Firestore
import com.example.foodOrderingSystem.models.MenuType
import com.example.foodOrderingSystem.models.MenuTypeViewModel
import com.example.foodOrderingSystem.utils.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.UUID

class MenuTypeFragment : Fragment() {

    private var _binding: FragmentMenuTypeBinding? = null
    private val binding get() = _binding!!
    private lateinit var recyclerView: RecyclerView
    private lateinit var menuTypeList: MutableLiveData<MutableList<MenuType>>
    private val menuTypeViewModel: MenuTypeViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuTypeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        menuTypeList = menuTypeViewModel.menuTypeList

        recyclerView = binding.menuTypeRecycleView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Observe changes in the tableList LiveData
        menuTypeList.observe(viewLifecycleOwner) {
            // Update your RecyclerView adapter when the LiveData changes
            recyclerView.adapter = MenuTypeListAdapter(this, requireContext(), menuTypeList)
        }

        // Get data from firebase
        Firestore().getMenuType(this, recyclerView, menuTypeViewModel)

        binding.apply {
            addMenuType.setOnClickListener{ addMenuType() }
            topAppBar.setNavigationOnClickListener { backToPrevious() }
        }

    }

    private fun backToPrevious() {
        Utils().backToPrevious(this, R.id.navigation_settings)
    }

    private fun addMenuType() {
        // Get table dialog layout
        val inflater = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_menu_type, null)

        val menuTypeEditText: EditText = inflater.findViewById(R.id.new_menu_type_editText)

        MaterialAlertDialogBuilder(requireContext())
            .setView(inflater)
            .setNegativeButton(resources.getString(R.string.cancel)) { dialog, which ->
                // Respond to negative button press
                dialog.dismiss()
            }
            .setPositiveButton(resources.getString(R.string.ok)) { dialog, which ->
                val inputText = menuTypeEditText.text.toString().uppercase() // Get the user input from the EditText

                if (inputText.isNotEmpty()) {
                    val uniqueID = UUID.randomUUID().toString()

                    val item = MenuType(
                        uniqueID,
                        inputText
                    )
                    // Add data into table view model
                    menuTypeViewModel.addMenuType(item)
                    Log.d("get Item value", item.toString())
                    recyclerView.adapter = MenuTypeListAdapter(this, requireContext(), menuTypeList)
                    Firestore().uploadMenuType(this, requireContext(), item, recyclerView, menuTypeList)

                    // Notify the adapter that the data has changed
//                    recyclerView.adapter?.notifyItemInserted(tableList.size - 1)
                    dialog.dismiss()
                } else {
                    // Handle case where user didn't enter any text
                    Toast.makeText(requireContext(), "Please enter the menu type", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

}