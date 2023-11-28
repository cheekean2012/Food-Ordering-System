package com.example.foodOrderingSystem.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.foodOrderingSystem.R
import com.example.foodOrderingSystem.databinding.ListReportItemBinding
import com.example.foodOrderingSystem.databinding.ListTableItemBinding
import com.example.foodOrderingSystem.models.Report
import com.example.foodOrderingSystem.models.ReportItemViewModel
import com.example.foodOrderingSystem.utils.Utils
import com.google.android.material.card.MaterialCardView
import kotlin.math.ceil

class ReportListAdapter (
    private val activity: Fragment,
    private val context: Context,
    private val reportList: LiveData<MutableList<Report>>
): RecyclerView.Adapter<ReportListAdapter.ItemViewHolder>() {

    private val reportItemViewModel: ReportItemViewModel by activity.activityViewModels()
    private val serviceChargePercentage = 0.10 // 10%

    inner class ItemViewHolder(private val view: ListReportItemBinding): RecyclerView.ViewHolder(view.root) {
        val timeStamp: TextView = view.timeStampTextView
        val tableNumber: TextView = view.tableNumberTextview
        val totalPrice: TextView = view.totalPriceTextview
        val status: TextView = view.statusTextview

        private var cardContainer: ConstraintLayout = view.reportItemContainer

        init {
            cardContainer.setOnClickListener { openReportItemDetail() }
        }

        private fun openReportItemDetail() {
            val getPosition = reportList.value!![adapterPosition]
            Log.d("get position: ", getPosition.toString())

            reportItemViewModel.setReportId(getPosition.id.toString())
            reportItemViewModel.setTableNumber(getPosition.tableNumber.toString())
            reportItemViewModel.setDate(getPosition.date.toString())
            reportItemViewModel.setStatus(getPosition.status.toString())
            reportItemViewModel.setSubTotalPrice(getPosition.subTotal.toString())
            reportItemViewModel.setTotalQuantity(getPosition.totalQuantity.toString())
            reportItemViewModel.setServiceCharge(getPosition.serviceCharge.toString())
            reportItemViewModel.setFinalTotal(getPosition.totalPrice.toString())
            reportItemViewModel.setCancelReason(getPosition.cancelReason.toString())

            val subTotal = getPosition.subTotal!!.toDouble()
            val serviceCharge = getPosition.serviceCharge!!.toDouble()

            val beforeRoundup = subTotal + serviceCharge

            val roundup =  roundup(subTotal, serviceChargePercentage) - beforeRoundup
            reportItemViewModel.setRoundup(roundup.toString())


            Utils().goToNextNavigate(activity, R.id.action_navigation_reportFragment_to_navigation_report_detail)
        }

    }


    private fun roundup(subTotal: Double, serviceChargePercentage: Double): Double {
        val serviceCharge = subTotal * serviceChargePercentage
        val beforeRoundup = subTotal + serviceCharge

        val decimalValue = (beforeRoundup * 100).toInt() % 10

        val roundedValue = if (decimalValue > 5) {
            ceil(beforeRoundup)
        } else {
            beforeRoundup
        }

        return roundedValue
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ReportListAdapter.ItemViewHolder {
        val itemView: ListReportItemBinding = ListReportItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ReportListAdapter.ItemViewHolder, position: Int) {
        val item = reportList.value!![position]

        Log.d("get item data", item.toString())

        holder.timeStamp.text = item.timestamp.toString()
        holder.tableNumber.text = item.tableNumber.toString()

        val totalPrice = item.totalPrice!!.toDoubleOrNull() ?: 0.0
        holder.totalPrice.text = String.format("RM%.2f", totalPrice)
        holder.status.text = item.status.toString()
    }

    override fun getItemCount(): Int {
        return reportList.value!!.size;
    }

}