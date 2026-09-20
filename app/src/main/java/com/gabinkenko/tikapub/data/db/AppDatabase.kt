package com.gabinkenko.tikapub.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gabinkenko.tikapub.data.db.seed.SeedQuotes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [QuoteEntity::class, PublishLogEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun quoteDao(): QuoteDao
    abstract fun publishLogDao(): PublishLogDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context, appScope: CoroutineScope): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tikapub.db",
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        appScope.launch(Dispatchers.IO) {
                            instance?.quoteDao()?.insertAll(SeedQuotes.all())
                        }
                    }
                }).build().also { instance = it }
            }
        }
    }
}
