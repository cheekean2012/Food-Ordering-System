package com.example.foodOrderingSystem.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
//import com.example.foodOrderingSystem.database.AppDatabase

class MenuTypeViewModel : ViewModel() {

    private val _menuTypeList = MutableLiveData<MutableList<MenuType>>()
    val menuTypeList: MutableLiveData<MutableList<MenuType>> get() = _menuTypeList

    private val _menuTypeId = MutableLiveData<String?>()
    val menuTypeId: LiveData<String?> get() = _menuTypeId

    private val _menuType = MutableLiveData<String?>()
    val menuType: LiveData<String?> get() = _menuType

    init {
        _menuTypeList.value = ArrayList()
    }

    fun setMenuTypes(menuTypes: MutableList<MenuType>) {
        _menuTypeList.value = menuTypes
    }

    fun addMenuType(menuType: MenuType) {
        _menuTypeList.value!!.add(menuType)
    }

    fun updateMenuType(menuType: MenuType) {
        val currentList = _menuTypeList.value
        val index = currentList?.indexOfFirst { it.id == menuType.id }

        if (index != null && index != -1) {
            currentList[index] = menuType
            _menuTypeList.value = currentList!!
        }
    }

    fun deleteMenuType(menuType: MenuType) {
        val currentList = _menuTypeList.value
        currentList?.remove(menuType)
        _menuTypeList.value = currentList!!
    }

    fun setMenuTypeId(id: String) {
        _menuTypeId.value = id
    }

    fun setMenuType(type: String) {
        _menuType.value = type
    }
}
