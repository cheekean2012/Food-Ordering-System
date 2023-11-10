package com.example.foodOrderingSystem.ui.table

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodOrderingSystem.R
import com.example.foodOrderingSystem.adapters.TableListAdapter
import com.example.foodOrderingSystem.databinding.FragmentTableBinding
import com.example.foodOrderingSystem.models.TableViewModel
import com.example.foodOrderingSystem.models.Tables
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.UUID

class TableFragment : Fragment() {

    private var _binding: FragmentTableBinding? = null
    private val tableViewModel: TableViewModel by activityViewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var tableList: LiveData<MutableList<Tables>>
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTableBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navView: BottomNavigationView = binding.navView
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

        tableList = tableViewModel.tableList

        recyclerView = binding.tableRecycleView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Observe changes in the tableList LiveData
        tableList.observe(viewLifecycleOwner) {
            // Update your RecyclerView adapter when the LiveData changes
            recyclerView.adapter = TableListAdapter(this, requireContext(), tableList)
        }

        binding.floatingActionButton.setOnClickListener { addTable() }
    }

    private fun addTable() {
        // Get table dialog layout
        val inflater = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_table, null)

        val tableEditTex: EditText = inflater.findViewById(R.id.newTableEditText)

        MaterialAlertDialogBuilder(requireContext())
            .setView(inflater)
            .setNegativeButton(resources.getString(R.string.cancel)) { dialog, which ->
                // Respond to negative button press
                dialog.dismiss()
            }
            .setPositiveButton(resources.getString(R.string.ok)) { dialog, which ->
                val inputText = tableEditTex.text.toString() // Get the user input from the EditText

                if (inputText.isNotEmpty()) {
                    val uniqueID = UUID.randomUUID().toString()

                    val item = Tables(
                        uniqueID,
                        inputText
                    )
                    // Add data into table view model
                    tableViewModel.addTable(item)
                    recyclerView.adapter = TableListAdapter(this, requireContext(), tableList)

                    // Notify the adapter that the data has changed
//                    recyclerView.adapter?.notifyItemInserted(tableList.size - 1)
                    dialog.dismiss()
                } else {
                    // Handle case where user didn't enter any text
                    Toast.makeText(requireContext(), "Please enter a table name", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}