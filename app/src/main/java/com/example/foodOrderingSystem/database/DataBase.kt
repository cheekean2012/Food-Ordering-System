//package com.example.foodOrderingSystem.database
//
//import android.content.Context
//import androidx.room.Database
//import androidx.room.Room
//import androidx.room.RoomDatabase
//import com.example.foodOrderingSystem.models.MenuType
//import com.example.foodOrderingSystem.models.MenuTypeDao
//
//@Database(entities = [MenuType::class], version = 1, exportSchema = false)
//abstract class AppDatabase : RoomDatabase() {
//    abstract fun menuTypeDao(): MenuTypeDao
//
//    companion object {
//        private const val DATABASE_NAME = "app_database"
//
//        @Volatile
//        private var INSTANCE: AppDatabase? = null
//
//        fun getDatabase(context: Context): AppDatabase {
//            return INSTANCE ?: synchronized(this) {
//                val instance = Room.databaseBuilder(
//                    context.applicationContext,
//                    AppDatabase::class.java,
//                    DATABASE_NAME
//                ).build()
//                INSTANCE = instance
//                instance
//            }
//        }
//    }
//}