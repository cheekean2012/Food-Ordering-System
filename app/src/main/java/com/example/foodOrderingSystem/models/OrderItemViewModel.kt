package com.example.foodOrderingSystem.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class OrderItemViewModel : ViewModel() {

    private val _orderItemList = MutableLiveData<MutableList<OrderItem>?>()

    // Expose the LiveData as an immutable public property
    val orderItemList: MutableLiveData<MutableList<OrderItem>?> get() = _orderItemList

    private val _subTotalPrice = MutableLiveData<String?>()
    val subTotalPrice: LiveData<String?> get() = _subTotalPrice

    private val _serviceCharge = MutableLiveData<String?>()
    val serviceCharge: LiveData<String?> get() = _serviceCharge

    private val _roundup = MutableLiveData<String?>()
    val roundup: LiveData<String?> get() = _roundup

    private val _finalTotal = MutableLiveData<String?>()
    val finalTotal: LiveData<String?> get() = _finalTotal

    init {
        _orderItemList.value = ArrayList()
    }

    fun setOrderItem(orderItem: MutableList<OrderItem>?) {
        _orderItemList.value = orderItem
    }

    fun deleteOrderItem(orderItem: OrderItem) {
        val currentList = _orderItemList.value
        currentList?.remove(orderItem)
        _orderItemList.value = currentList!!
    }

    fun setSubTotalPrice(price: String) {
        _subTotalPrice.value = price
    }

    fun setServiceCharge(price: String) {
        _serviceCharge.value = price
    }

    fun setRoundup(price: String) {
        _roundup.value = price
    }

    fun setFinalTotal(price: String) {
        _finalTotal.value = price
    }
}