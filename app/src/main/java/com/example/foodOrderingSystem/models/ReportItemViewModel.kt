package com.example.foodOrderingSystem.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ReportItemViewModel: ViewModel() {

    private val _reportItemList = MutableLiveData<MutableList<Report>>()

    // Expose the LiveData as an immutable public property
    val reportItemList: MutableLiveData<MutableList<Report>> get() = _reportItemList

    private val _reportId = MutableLiveData<String>()
    val reportId: LiveData<String> get() = _reportId

    private val _date = MutableLiveData<String>()
    val date: LiveData<String> get() = _date

    private val _status = MutableLiveData<String>()
    val status: LiveData<String> get() = _status

    private val _tableNumber = MutableLiveData<String>()
    val tableNumber: LiveData<String> get() = _tableNumber

    private val _totalQuantity = MutableLiveData<String>()
    val totalQuantity: LiveData<String> get() = _totalQuantity

    private val _subTotalPrice = MutableLiveData<String>()
    val subTotalPrice: LiveData<String> get() = _subTotalPrice

    private val _serviceCharge = MutableLiveData<String>()
    val serviceCharge: LiveData<String> get() = _serviceCharge

    private val _roundup = MutableLiveData<String>()
    val roundup: LiveData<String> get() = _roundup

    private val _finalTotal = MutableLiveData<String>()
    val finalTotal: LiveData<String> get() = _finalTotal

    init {
        _reportItemList.value = ArrayList()
    }

    fun setDate(date: String) {
        _date.value = date
    }

    fun setStatus(status: String) {
        _status.value = status
    }

    fun setTableNumber(number: String) {
        _tableNumber.value = number
    }

    fun setReportItem(reportItem: MutableList<Report>) {
        _reportItemList.value = reportItem
    }

    fun setReportId(id: String) {
        _reportId.value = id
    }

    fun setTotalQuantity(quantity: String) {
        _totalQuantity.value = quantity
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

    fun resetValue() {
        _reportItemList.value!!.clear()
        _roundup.value = "0.00"
        _totalQuantity.value = "0"
        _serviceCharge.value = "0.00"
        _subTotalPrice.value = "0.00"
        _finalTotal.value = "0.00"

    }

}