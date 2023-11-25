package com.example.foodOrderingSystem.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ReportDetailItemViewModel: ViewModel() {

    private val _reportItemList = MutableLiveData<MutableList<OrderItem>>()

    // Expose the LiveData as an immutable public property
    val reportItemList: MutableLiveData<MutableList<OrderItem>> get() = _reportItemList

    init {
        _reportItemList.value = ArrayList()
    }

    fun setOrderItem(orderItem: MutableList<OrderItem>) {
        _reportItemList.value = orderItem
    }

    fun resetValue() {
        _reportItemList.value!!.clear()
    }
}