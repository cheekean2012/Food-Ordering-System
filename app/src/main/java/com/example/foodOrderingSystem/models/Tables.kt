package com.example.foodOrderingSystem.models

import androidx.lifecycle.LiveData
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update


data class Tables(
    var id: String? = null,
    var tableNumber: String = ""
)

data class TableOrder (
    var id: String? = null,
    var startTime: String? = null,
    var tableNumber: String? = null,
    var customerOrder:MutableList<OrderItem>? = null,
    var customerOrdering:MutableList<OrderItem>? = null,
    var status: String? = null,
    var paymentType: String? = null,
    var reason: String? = null,
    var token: String? = null
)

data class OrderItem (
    var id: String? = null,
    var itemName: String? = null,
    var unitPrice: String? = null,
    var totalPrice: Double? = null,
    var quantity: Int? = null,
    var remarks: String? = null,
    var takeaway: Boolean? = null
)

data class PrintOrderItem (
    var id: String? = null,
    var customerOrder: MutableList<OrderItem>? = null,
    var customerOrdering: MutableList<OrderItem>? = null,
    var tableNumber: String? = null
)
