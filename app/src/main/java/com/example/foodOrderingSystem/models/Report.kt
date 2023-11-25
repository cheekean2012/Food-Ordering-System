package com.example.foodOrderingSystem.models

import com.google.firebase.firestore.ServerTimestamp
import java.sql.Timestamp

data class Report (
    var id: String? = null,
    var tableId: String? = null,
    var tableNumber: String? = null,
    var timestamp: String? = null,
    var date: String? = null,
    var totalQuantity: String? = null,
    var totalPrice: String? = null,
    var serviceCharge: String? = null,
    var subTotal: String? = null,
    var orderItem: MutableList<OrderItem>? = null,
    var status: String? = null,
    @ServerTimestamp var dateTime: com.google.firebase.Timestamp? = null,
)