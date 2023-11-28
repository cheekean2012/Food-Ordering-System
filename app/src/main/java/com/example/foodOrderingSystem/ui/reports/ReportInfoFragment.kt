package com.example.foodOrderingSystem.ui.reports

import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.foodOrderingSystem.R
import com.example.foodOrderingSystem.databinding.FragmentReportInfoBinding
import com.example.foodOrderingSystem.models.ReportItemViewModel

class ReportInfoFragment : Fragment() {

    private lateinit var binding: FragmentReportInfoBinding
    private val reportItemViewModel: ReportItemViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentReportInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val report = reportItemViewModel
        binding.apply {
            tableNumberTextview.text = getString(R.string.table_number, report.tableNumber.value.toString())
            dateTextview.text = getString(R.string.date, report.date.value.toString())
            statusTextview.text = getString(R.string.status, report.status.value.toString())
            subTotalTextview.text = getString(R.string.sub_total, formatCurrency(report.subTotalPrice.value))
            serviceChargeTextview.text = getString(R.string.service_charge, formatCurrency(report.serviceCharge.value))
            roundUpTextview.text = getString(R.string.round_up, formatCurrency(report.roundup.value))
            totalPriceTextview.text = getString(R.string.total_price, formatCurrency(report.finalTotal.value))

            totalQuantityTextview.text = getString(R.string.total_quantity, report.totalQuantity.value.toString())
            cancelReasonTextview.text = "Cancel Reason: " + report.cancelReason.value.toString()

            cancelReasonTextview.visibility = if (report.cancelReason.value.toString().isEmpty()) View.INVISIBLE else View.VISIBLE
        }
    }


    private fun formatCurrency(value: String?): String {
        return if (value != null && value.toDoubleOrNull() != null) {
            "RM%.2f".format(value.toDouble())
        } else {
            "Invalid Value"
        }
    }

}