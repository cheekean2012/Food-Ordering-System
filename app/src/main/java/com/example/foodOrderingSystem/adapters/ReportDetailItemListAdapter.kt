package com.example.foodOrderingSystem.adapters

import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.foodOrderingSystem.databinding.ListReportDetailItemBinding
import com.example.foodOrderingSystem.models.OrderItem
import com.example.foodOrderingSystem.models.ReportDetailItemViewModel
import kotlin.math.ceil

class ReportDetailItemListAdapter(
    private val activity: Fragment,
    private val context: Context,
    private val orderItemList: MutableLiveData<MutableList<OrderItem>>
): RecyclerView.Adapter<ReportDetailItemListAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(val view: ListReportDetailItemBinding) : RecyclerView.ViewHolder(view.root) {
        var itemName = view.orderItemTextview
        var quantity = view.quantityTextview
        var remark = view.remarksTextview
        var takeaway = view.takeawayTextview

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ReportDetailItemListAdapter.ItemViewHolder {
        val itemView: ListReportDetailItemBinding =
            ListReportDetailItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ReportDetailItemListAdapter.ItemViewHolder, position: Int) {
        val item = orderItemList.value!![position]
        holder.itemName.text = item.itemName
        holder.quantity.text = "Quantity: " + item.quantity.toString()
        holder.remark.text = "Remark: "+ item.remarks
        holder.takeaway.text = if (item.takeaway!!) "Takeaway" else ""

        holder.remark.visibility = if (!TextUtils.isEmpty(holder.remark.text)) View.VISIBLE else View.INVISIBLE
        holder.takeaway.visibility = if (!TextUtils.isEmpty(holder.takeaway.text)) View.VISIBLE else View.INVISIBLE
    }


    override fun getItemCount(): Int {
        return orderItemList.value!!.size
    }
}