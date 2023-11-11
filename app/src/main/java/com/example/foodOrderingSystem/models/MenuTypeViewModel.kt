package com.example.foodOrderingSystem.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
//import com.example.foodOrderingSystem.database.AppDatabase

class MenuTypeViewModel : ViewModel() {

    private val _menuTypeList = MutableLiveData<MutableList<MenuType>>()

    // Expose the LiveData as an immutable public property
    val menuTypeList: MutableLiveData<MutableList<MenuType>> get() = _menuTypeList

    private val _menuTypeId = MutableLiveData<String?>()
    val menuTypeId: LiveData<String?> get() = _menuTypeId

    private val _menuType = MutableLiveData<String?>()
    val menuType: LiveData<String?> get() = _menuType

    init {
        _menuTypeList.value = ArrayList()
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

//class MenuViewModel(application: Application) : AndroidViewModel(application) {
//
//    private val menuTypeDao: MenuTypeDao
//
//    init {
//        val database = AppDatabase.getDatabase(application)
//        menuTypeDao = database.menuTypeDao()
//    }
//
//    private val _menuTypeList = MutableLiveData<List<MenuType>>()
//    val menuTypeList: LiveData<List<MenuType>> = _menuTypeList
//
//    fun addMenuType(menuType: MenuType) {
//        viewModelScope.launch {
//            menuTypeDao.insertMenuType(menuType)
//            // Update the MutableLiveData with the new list
//            _menuTypeList.value = menuTypeDao.getAllMenuTypes().value?.toMutableList()
//        }
//    }
//
//    fun updateMenuType(menuType: MenuType) {
//        viewModelScope.launch {
//            menuTypeDao.updateMenuType(menuType)
//            // Update the MutableLiveData with the new list
//            _menuTypeList.value = menuTypeDao.getAllMenuTypes().value?.toMutableList()
//        }
//    }
//
//    fun deleteMenuType(menuType: MenuType) {
//        viewModelScope.launch {
//            menuTypeDao.deleteMenuType(menuType)
//            // Update the MutableLiveData with the new list
//            _menuTypeList.value = menuTypeDao.getAllMenuTypes().value?.toMutableList()
//        }
//    }
//}
