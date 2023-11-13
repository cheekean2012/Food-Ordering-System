package com.example.foodOrderingSystem.models

data class MenuType(
    val id: String? = null,
    val menuType: String = ""
)

data class MenuItem (
    val id: String? = null,
    var image: String = "",
    val itemName: String = "",
    val type: String = "",
    val price: String = "",
    val ingredient: String = "",
    val available: Boolean? = null
)