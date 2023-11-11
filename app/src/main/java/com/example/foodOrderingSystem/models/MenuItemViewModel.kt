package com.example.foodOrderingSystem.models

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MenuItemViewModel: ViewModel() {
    private val _menuItemList = MutableLiveData<MutableList<MenuItem>>()

    // Expose the LiveData as an immutable public property
    val menuItemList: LiveData<MutableList<MenuItem>> get() = _menuItemList

    private val _menuItemId = MutableLiveData<String?>()
    val menuItemId: LiveData<String?> get() = _menuItemId

    private val _menuImage = MutableLiveData<Uri?>()
    val menuImage: LiveData<Uri?> get() = _menuImage

    private val _menuItemName = MutableLiveData<String?>()
    val menuItemName: LiveData<String?> get() = _menuItemName

    private val _menuType = MutableLiveData<String?>()
    val menuType: LiveData<String?> get() = _menuType

    private val _menuPrice = MutableLiveData<String?>()
    val menuPrice: LiveData<String?> get() = _menuPrice

    private val _menuIngredient = MutableLiveData<String?>()
    val menuIngredient: LiveData<String?> get() = _menuIngredient

    private val _menuAvailable = MutableLiveData<Boolean>()
    val menuAvailable: LiveData<Boolean> get() = _menuAvailable

    init {
        _menuItemList.value = ArrayList()
    }

    fun addMenuItem(menuItem: MenuItem) {
        _menuItemList.value!!.add(menuItem)
    }

    fun deleteMenuItem(id: String) {
        val currentList = _menuItemList.value ?: mutableListOf()

        // Find the index of the menu item with the specified ID
        val indexToRemove = currentList.indexOfFirst { it.id == id }

        if (indexToRemove != -1) {
            // Remove the menu item from the list
            currentList.removeAt(indexToRemove)

            // Update the MutableLiveData
            _menuItemList.value = currentList
        }
    }

    fun setId(id: String) {
        _menuItemId.value = id
    }

    fun setImage(image: Uri) {
        _menuImage.value = image
    }

    fun setMenuItemName(title: String) {
        _menuItemName.value = title
    }

    fun setType(type: String) {
        _menuType.value = type
    }

    fun setPrice(price: String) {
        _menuPrice.value = price
    }

    fun setIngredient(description: String) {
        _menuIngredient.value = description
    }

    fun setMenuAvailable(available: Boolean) {
        _menuAvailable.value = available
    }

    fun resetValue() {
        _menuImage.value = null
        _menuType.value = null
        _menuItemName.value = null
        _menuIngredient.value = null
        _menuAvailable.value = true
        _menuPrice.value = null
    }

}