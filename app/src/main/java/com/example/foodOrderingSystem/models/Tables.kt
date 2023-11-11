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



//
//@Entity(tableName = "menu_types")
//data class MenuType(
//    @PrimaryKey val id: String,
//    @ColumnInfo(name = "menu_type") val menuType: String
//)
//
//@Dao
//interface MenuTypeDao {
//    @Query("SELECT * FROM menu_types")
//    fun getAllMenuTypes(): LiveData<MutableList<MenuType>>
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertMenuType(menuType: MenuType)
//
//    @Update
//    suspend fun updateMenuType(menuType: MenuType)
//
//    @Delete
//    suspend fun deleteMenuType(menuType: MenuType)
//}