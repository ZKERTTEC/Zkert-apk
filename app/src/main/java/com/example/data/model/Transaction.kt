package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Currency(val symbol: String, val arabicName: String) {
    LIRA("ل.س", "ليرة"),
    USD("$", "دولار")
}

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long, // unix timestamp in ms
    val description: String,
    val currency: Currency,
    val timestamp: Long = System.currentTimeMillis()
)
