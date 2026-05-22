package com.example.data.database

import android.content.Context
import androidx.room.*
import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.data.model.Currency
import com.example.data.model.Transaction
import com.example.data.model.TransactionLine

class Converters {
    @TypeConverter
    fun fromAccountType(value: AccountType): String = value.name

    @TypeConverter
    fun toAccountType(value: String): AccountType = AccountType.valueOf(value)

    @TypeConverter
    fun fromCurrency(value: Currency): String = value.name

    @TypeConverter
    fun toCurrency(value: String): Currency = Currency.valueOf(value)
}

@Database(
    entities = [Account::class, Transaction::class, TransactionLine::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AccountingDatabase : RoomDatabase() {
    abstract fun dao(): AccountingDao

    companion object {
        @Volatile
        private var INSTANCE: AccountingDatabase? = null

        fun getDatabase(context: Context): AccountingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AccountingDatabase::class.java,
                    "zkert_accounting_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
