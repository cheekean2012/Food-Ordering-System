package com.example.foodOrderingSystem.ui.reports

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodOrderingSystem.adapters.OrderItemListAdapter
import com.example.foodOrderingSystem.adapters.ReportDetailItemListAdapter
import com.example.foodOrderingSystem.databinding.FragmentReportItemBinding
import com.example.foodOrderingSystem.firestore.Firestore
import com.example.foodOrderingSystem.models.OrderItem
import com.example.foodOrderingSystem.models.ReportDetailItemViewModel
import com.example.foodOrderingSystem.models.ReportItemViewModel

class ReportItemFragment : Fragment() {

    private lateinit var binding: FragmentReportItemBinding
    private lateinit var reportItemList: MutableLiveData<MutableList<OrderItem>>
    private lateinit var recyclerView: RecyclerView
    private val reportDetailItemViewModel: ReportDetailItemViewModel by activityViewModels()
    private val reportItemViewModel: ReportItemViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentReportItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        reportItemList = reportDetailItemViewModel.reportItemList
        Log.d("report Item List", reportItemList.toString())

        recyclerView = binding.reportItemListRecycleView

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Observe changes in the tableList LiveData
        reportItemList.observe(viewLifecycleOwner) {
            // Update your RecyclerView adapter when the LiveData changes
            recyclerView.adapter = ReportDetailItemListAdapter(this, requireContext(), reportItemList)
        }

        val reportId = reportItemViewModel.reportId.value.toString()

        Firestore().getCustomerReportItemDetail(this, recyclerView, reportId)
    }

}