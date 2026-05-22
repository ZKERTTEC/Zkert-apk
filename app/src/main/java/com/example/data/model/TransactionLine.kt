package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_lines",
    foreignKeys = [
        ForeignKey(
            entity = Transaction::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("transactionId"),
        Index("accountId")
    ]
)
data class TransactionLine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val accountId: Long,
    val isDebit: Boolean, // true = مدين (Debit), false = دائن (Credit)
    val amount: Double,
    val lineDescription: String? = null
)

data class HydratedLine(
    val id: Long,
    val transactionId: Long,
    val accountId: Long,
    val isDebit: Boolean,
    val amount: Double,
    val lineDescription: String?,
    val accountCode: String,
    val accountName: String,
    val accountType: AccountType,
    val transactionCurrency: Currency,
    val transactionDate: Long
)

data class TransactionWithLines(
    val transaction: Transaction,
    val lines: List<HydratedLine>
)
