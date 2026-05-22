package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountType(val arabicName: String, val isDebitNormal: Boolean) {
    ASSET("أصول", true),
    LIABILITY("خصوم", false),
    EQUITY("حقوق ملكية", false),
    REVENUE("إيرادات", false),
    EXPENSE("مصروفات", true)
}

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val type: AccountType,
    val parentId: Long? = null
)
