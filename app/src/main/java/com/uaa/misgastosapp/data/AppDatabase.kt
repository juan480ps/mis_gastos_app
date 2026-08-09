package com.uaa.misgastosapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromRecurrenceType(value: RecurrenceType): String = value.name
    @TypeConverter
    fun toRecurrenceType(value: String): RecurrenceType = RecurrenceType.valueOf(value)
}

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        RecurringTransactionEntity::class,
        UserEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gastos_db"
                )
                    .addMigrations(*Migrations.ALL_MIGRATIONS)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
