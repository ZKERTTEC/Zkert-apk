package com.example.data.repository

import com.example.data.database.AccountingDao
import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.data.model.HydratedLine
import com.example.data.model.Transaction
import com.example.data.model.TransactionLine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

class AccountingRepository(private val dao: AccountingDao) {

    val allAccounts: Flow<List<Account>> = dao.getAllAccounts()
    val allTransactions: Flow<List<Transaction>> = dao.getAllTransactions()
    val allLines: Flow<List<HydratedLine>> = dao.getAllHydratedLinesFlow()

    suspend fun getAccountById(id: Long): Account? = dao.getAccountById(id)
    suspend fun getAccountByCode(code: String): Account? = dao.getAccountByCode(code)

    suspend fun insertAccount(account: Account): Long {
        return dao.insertAccount(account)
    }

    suspend fun updateAccount(account: Account) {
        dao.updateAccount(account)
    }

    suspend fun deleteAccount(account: Account) {
        dao.deleteAccount(account)
    }

    suspend fun deleteTransaction(id: Long) {
        dao.deleteTransactionById(id)
    }

    suspend fun getLinesForTransaction(transactionId: Long): List<HydratedLine> {
        return dao.getLinesForTransaction(transactionId)
    }

    /**
     * Valide and save a full journal entry.
     */
    suspend fun saveTransaction(
        transaction: Transaction,
        lines: List<TransactionLine>
    ): ValidationResult {
        val validation = validateTransaction(lines)
        if (validation is ValidationResult.Error) {
            return validation
        }
        
        try {
            dao.insertFullTransaction(transaction, lines)
            return ValidationResult.Success
        } catch (e: Exception) {
            return ValidationResult.Error("حدث خطأ أثناء حفظ القيد: ${e.localizedMessage}")
        }
    }

    /**
     * Simple Transaction Validation logic
     */
    fun validateTransaction(lines: List<TransactionLine>): ValidationResult {
        if (lines.isEmpty()) {
            return ValidationResult.Error("يجب أن يحتوي القيد على سطرين على الأقل")
        }
        if (lines.size < 2) {
            return ValidationResult.Error("يجب وجود حساب مدين وحساب دائن على الأقل لإتمام القيد المزدوج")
        }
        
        val debitsSum = lines.filter { it.isDebit }.sumOf { it.amount }
        val creditsSum = lines.filter { !it.isDebit }.sumOf { it.amount }

        val diff = Math.abs(debitsSum - creditsSum)
        if (diff > 0.001) {
            val formattedDiff = String.format("%.2f", diff)
            return ValidationResult.Error(
                "القيد غير متزن! إجمالي المدين ($debitsSum) يجب أن يساوي إجمالي الدائن ($creditsSum). الفرق: $formattedDiff"
            )
        }

        val hasDebit = lines.any { it.isDebit }
        val hasCredit = lines.any { !it.isDebit }
        if (!hasDebit || !hasCredit) {
            return ValidationResult.Error("يجب أن يتضمن القيد حساباً مديناً وحساباً دائناً على الأقل")
        }

        return ValidationResult.Success
    }

    /**
     * Seed predefined Arabic standard accounts if database has no accounts.
     */
    suspend fun seedAccountsIfEmpty() {
        val accounts = allAccounts.first()
        if (accounts.isEmpty()) {
            val defaultAccounts = listOf(
                Account(code = "1101", name = "الصندوق (ليرة)", type = AccountType.ASSET),
                Account(code = "1102", name = "الصندوق (دولار)", type = AccountType.ASSET),
                Account(code = "1103", name = "البنك (ليرة)", type = AccountType.ASSET),
                Account(code = "1104", name = "البنك (دولار)", type = AccountType.ASSET),
                Account(code = "1201", name = "الزبائن / المدينون", type = AccountType.ASSET),
                Account(code = "2101", name = "الموردون / الدائنون", type = AccountType.LIABILITY),
                Account(code = "3101", name = "رأس المال", type = AccountType.EQUITY),
                Account(code = "4101", name = "مبيعات بضائع", type = AccountType.REVENUE),
                Account(code = "4102", name = "إيرادات خدمة", type = AccountType.REVENUE),
                Account(code = "5101", name = "المشتريات", type = AccountType.EXPENSE),
                Account(code = "5102", name = "مصروف الإيجار", type = AccountType.EXPENSE),
                Account(code = "5103", name = "مصروف الرواتب", type = AccountType.EXPENSE),
                Account(code = "5104", name = "مصاريف عامة والإدارية", type = AccountType.EXPENSE)
            )
            for (acc in defaultAccounts) {
                dao.insertAccount(acc)
            }
        }
    }
}
