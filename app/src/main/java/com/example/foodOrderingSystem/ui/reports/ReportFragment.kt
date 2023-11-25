package com.example.foodOrderingSystem.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodOrderingSystem.R
import com.example.foodOrderingSystem.adapters.ReportListAdapter
import com.example.foodOrderingSystem.databinding.FragmentReportBinding
import com.example.foodOrderingSystem.firestore.Firestore
import com.example.foodOrderingSystem.models.Report
import com.example.foodOrderingSystem.models.ReportItemViewModel
import com.example.foodOrderingSystem.utils.Utils


class ReportFragment : Fragment() {

    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!
    private lateinit var recyclerView: RecyclerView
    private lateinit var reportItemList: MutableLiveData<MutableList<Report>>
    private val reportItemViewModel: ReportItemViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        reportItemList = reportItemViewModel.reportItemList

        recyclerView = binding.reportItemListRecycleView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Observe changes in the tableList LiveData
        reportItemList.observe(viewLifecycleOwner) {
            // Update your RecyclerView adapter when the LiveData changes
            recyclerView.adapter = ReportListAdapter(this, requireContext(), reportItemList)
        }

        // Get data from firebase
        Firestore().getReport(this, requireContext(), recyclerView, reportItemViewModel)

        binding.apply {
            topAppBar.setNavigationOnClickListener { backToPrevious() }
        }
    }

    private fun backToPrevious() {
        Utils().backToPrevious(this, R.id.navigation_settings)
    }


}