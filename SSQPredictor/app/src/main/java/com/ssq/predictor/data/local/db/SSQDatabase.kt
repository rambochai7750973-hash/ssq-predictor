package com.ssq.predictor.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ssq.predictor.data.local.dao.DrawDao
import com.ssq.predictor.data.local.entity.DrawEntity

@Database(entities = [DrawEntity::class], version = 1, exportSchema = false)
abstract class SSQDatabase : RoomDatabase() {

    abstract fun drawDao(): DrawDao

    companion object {
        @Volatile
        private var INSTANCE: SSQDatabase? = null

        fun create(context: Context): SSQDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SSQDatabase::class.java,
                    "ssq_history.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
