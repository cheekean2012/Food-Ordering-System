package com.example.foodOrderingSystem.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TableViewModel : ViewModel() {

    private val _tableList = MutableLiveData<MutableList<Tables>>()

    // Expose the LiveData as an immutable public property
    val tableList: MutableLiveData<MutableList<Tables>> get() = _tableList

    private val _tableId = MutableLiveData<String?>()
    val tableId: LiveData<String?> get() = _tableId

    private val _tableNumber = MutableLiveData<String?>()
    val tableNumber: LiveData<String?> get() = _tableNumber

    // Initialize the LiveData with an empty list
    init {
        _tableList.value = ArrayList()
    }

    fun setTable(table: MutableList<Tables>) {
        _tableList.value = table
    }

    fun addTable(table: Tables) {
        _tableList.value!!.add(table)
    }

    fun setTableId(id: String) {
        _tableId.value = id
    }

    fun setTableNumber(id: String) {
        _tableNumber.value = id
    }
}