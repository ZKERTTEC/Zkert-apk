package com.example.data.database

import androidx.room.*
import androidx.room.Transaction as RoomTransaction
import com.example.data.model.Account
import com.example.data.model.HydratedLine
import com.example.data.model.Transaction
import com.example.data.model.TransactionLine
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountingDao {

    // --- Accounts ---
    @Query("SELECT * FROM accounts ORDER BY code ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): Account?

    @Query("SELECT * FROM accounts WHERE code = :code")
    suspend fun getAccountByCode(code: String): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)

    // --- Transactions ---
    @Query("SELECT * FROM transactions ORDER BY date DESC, timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    // --- Transaction Lines ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionLines(lines: List<TransactionLine>)

    @Query("SELECT tl.*, a.code as accountCode, a.name as accountName, a.type as accountType, t.currency as transactionCurrency, t.date as transactionDate " +
           "FROM transaction_lines tl " +
           "JOIN accounts a ON tl.accountId = a.id " +
           "JOIN transactions t ON tl.transactionId = t.id " +
           "WHERE tl.transactionId = :transactionId")
    suspend fun getLinesForTransaction(transactionId: Long): List<HydratedLine>

    @Query("SELECT tl.*, a.code as accountCode, a.name as accountName, a.type as accountType, t.currency as transactionCurrency, t.date as transactionDate " +
           "FROM transaction_lines tl " +
           "JOIN accounts a ON tl.accountId = a.id " +
           "JOIN transactions t ON tl.transactionId = t.id " +
           "ORDER BY tl.id ASC")
    fun getAllHydratedLinesFlow(): Flow<List<HydratedLine>>

    // --- High level atomic transaction entry ---
    @RoomTransaction
    suspend fun insertFullTransaction(transaction: Transaction, lines: List<TransactionLine>) {
        val transId = insertTransaction(transaction)
        val linesWithTransId = lines.map { it.copy(transactionId = transId) }
        insertTransactionLines(linesWithTransId)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long
}
