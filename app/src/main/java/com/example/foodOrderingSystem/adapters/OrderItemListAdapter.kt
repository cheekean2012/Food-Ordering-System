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
import com.example.foodOrderingSystem.R
import com.example.foodOrderingSystem.databinding.ListOrderItemBinding
import com.example.foodOrderingSystem.firestore.Firestore
import com.example.foodOrderingSystem.models.OrderItem
import com.example.foodOrderingSystem.models.OrderItemViewModel
import com.example.foodOrderingSystem.models.TableViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.math.ceil

class OrderItemListAdapter(
    private val activity: Fragment,
    private val context: Context,
    private val orderItemList: MutableLiveData<MutableList<OrderItem>>
): RecyclerView.Adapter<OrderItemListAdapter.ItemViewHolder>() {

    private val orderItemViewModel: OrderItemViewModel by activity.activityViewModels()
    private val tableViewModel: TableViewModel by activity.activityViewModels()
    private var subTotal : Double = 0.0
    private var totalQuantity: Int = 0
    private val serviceChargePercentage = 0.10 // 10%
    private var serviceCharge: Double = 0.0
    private var beforeRoundup : Double = 0.0
    private var roundup: Double = 0.0
    private var finalTotal: Double = 0.0


    inner class ItemViewHolder(val view: ListOrderItemBinding) : RecyclerView.ViewHolder(view.root) {
        var itemName = view.orderItemTextview
        var quantity = view.quantityTextview
        var remark = view.remarksTextview
        var takeaway = view.takeawayTextview
        var deleteItem = view.deleteOrderItem

        init {
            deleteItem.setOnClickListener { openDeleteDialog() }
            calculateTotals()
        }

        private fun openDeleteDialog() {
            val getPosition = orderItemList.value!![adapterPosition]
            Log.d("get position: ", getPosition.id.toString())

            MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.delete_order_item))
                .setMessage(context.getString(R.string.confirm_delete_order_item))
                .setNegativeButton(activity.resources.getString(R.string.cancel)) { dialog, _ ->
                    // Respond to negative button press
                    dialog.dismiss()
                }
                .setPositiveButton(activity.resources.getString(R.string.ok)) { _, _ ->
                    // Delete data from menu view model
                    orderItemViewModel.deleteOrderItem(getPosition)
                    val tableId = tableViewModel.tableId.value.toString()

                    // Notify the adapter that an item has been removed
                    notifyItemRemoved(adapterPosition)
                    Firestore().deleteCustomerOrderItem(getPosition.id, tableId)
                }
                .show()
        }

        private fun calculateTotals() {
            subTotal = 0.0
            totalQuantity = 0

            orderItemList.value?.forEach { item ->
                if (item.totalPrice != null) {
                    subTotal += item.totalPrice!!
                    totalQuantity += item.quantity!!
                }
            }

            serviceCharge = subTotal * serviceChargePercentage
            beforeRoundup = subTotal + serviceCharge
            roundup = roundup(subTotal, serviceChargePercentage) - beforeRoundup
            finalTotal = beforeRoundup + roundup

            orderItemViewModel.setSubTotalPrice(subTotal.toString())
            orderItemViewModel.setTotalQuantity(totalQuantity.toString())
            orderItemViewModel.setServiceCharge(serviceCharge.toString())
            orderItemViewModel.setRoundup(roundup.toString())
            orderItemViewModel.setFinalTotal(finalTotal.toString())
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OrderItemListAdapter.ItemViewHolder {
        val itemView: ListOrderItemBinding =
            ListOrderItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: OrderItemListAdapter.ItemViewHolder, position: Int) {
        val item = orderItemList.value!![position]
        holder.itemName.text = item.itemName
        holder.quantity.text = "Quantity: " + item.quantity.toString()
        holder.remark.text = "Remark: "+ item.remarks
        holder.takeaway.text = if (item.takeaway!!) "Takeaway" else ""

//        if (item.totalPrice != null) {
//            subTotal += item.totalPrice!!
//            orderItemViewModel.setSubTotalPrice(subTotal.toString())
//
//            totalQuantity += item.quantity!!
//            orderItemViewModel.setTotalQuantity(totalQuantity.toString())
//
//            serviceCharge = subTotal * serviceChargePercentage
//            orderItemViewModel.setServiceCharge(serviceCharge.toString())
//
//            beforeRoundup = subTotal + serviceCharge
//
//            roundup =  roundup(subTotal, serviceChargePercentage) - beforeRoundup
//            orderItemViewModel.setRoundup(roundup.toString())
//
//            finalTotal = beforeRoundup + roundup
//            orderItemViewModel.setFinalTotal(finalTotal.toString())
//        }

        holder.remark.visibility = if (!TextUtils.isEmpty(holder.remark.text)) View.VISIBLE else View.INVISIBLE
        holder.takeaway.visibility = if (!TextUtils.isEmpty(holder.takeaway.text)) View.VISIBLE else View.INVISIBLE
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

    override fun getItemCount(): Int {
       return orderItemList.value!!.size
    }
}