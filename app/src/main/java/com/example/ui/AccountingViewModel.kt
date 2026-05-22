package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.data.model.Currency
import com.example.data.model.HydratedLine
import com.example.data.model.Transaction
import com.example.data.model.TransactionLine
import com.example.data.repository.AccountingRepository
import com.example.data.repository.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountBalance(
    val account: Account,
    val liraDebit: Double,
    val liraCredit: Double,
    val liraBalance: Double, // Net balance depending on character
    val usdDebit: Double,
    val usdCredit: Double,
    val usdBalance: Double // Net balance depending on character
)

data class DashboardSummary(
    val liraAssets: Double = 0.0,
    val usdAssets: Double = 0.0,
    val liraLiabilities: Double = 0.0,
    val usdLiabilities: Double = 0.0,
    val liraEquity: Double = 0.0,
    val usdEquity: Double = 0.0,
    val liraRevenue: Double = 0.0,
    val usdRevenue: Double = 0.0,
    val liraExpense: Double = 0.0,
    val usdExpense: Double = 0.0,
    val liraProfit: Double = 0.0,
    val usdProfit: Double = 0.0
)

data class TransactionLineDraft(
    val accountId: Long = 0,
    val isDebit: Boolean = true,
    val amount: Double = 0.0,
    val description: String = ""
)

class AccountingViewModel(private val repository: AccountingRepository) : ViewModel() {

    // Seed standard accounts on init
    init {
        viewModelScope.launch {
            repository.seedAccountsIfEmpty()
        }
    }

    val accounts: StateFlow<List<Account>> = repository.allAccounts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val lines: StateFlow<List<HydratedLine>> = repository.allLines
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Compute account balances in real time (Offline and reactive)
    val accountBalances: StateFlow<List<AccountBalance>> = combine(accounts, lines) { accountsList, linesList ->
        accountsList.map { account ->
            val liraLines = linesList.filter { it.accountId == account.id && it.transactionCurrency == Currency.LIRA }
            val usdLines = linesList.filter { it.accountId == account.id && it.transactionCurrency == Currency.USD }

            val liraDebit = liraLines.filter { it.isDebit }.sumOf { it.amount }
            val liraCredit = liraLines.filter { !it.isDebit }.sumOf { it.amount }
            val liraBalance = if (account.type.isDebitNormal) {
                liraDebit - liraCredit
            } else {
                liraCredit - liraDebit
            }

            val usdDebit = usdLines.filter { it.isDebit }.sumOf { it.amount }
            val usdCredit = usdLines.filter { !it.isDebit }.sumOf { it.amount }
            val usdBalance = if (account.type.isDebitNormal) {
                usdDebit - usdCredit
            } else {
                usdCredit - usdDebit
            }

            AccountBalance(
                account = account,
                liraDebit = liraDebit,
                liraCredit = liraCredit,
                liraBalance = liraBalance,
                usdDebit = usdDebit,
                usdCredit = usdCredit,
                usdBalance = usdBalance
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dashboard summary
    val dashboardSummary: StateFlow<DashboardSummary> = accountBalances.map { balances ->
        var liraAssets = 0.0
        var usdAssets = 0.0
        var liraLiabilities = 0.0
        var usdLiabilities = 0.0
        var liraEquity = 0.0
        var usdEquity = 0.0
        var liraRevenue = 0.0
        var usdRevenue = 0.0
        var liraExpense = 0.0
        var usdExpense = 0.0

        for (b in balances) {
            when (b.account.type) {
                AccountType.ASSET -> {
                    liraAssets += b.liraBalance
                    usdAssets += b.usdBalance
                }
                AccountType.LIABILITY -> {
                    liraLiabilities += b.liraBalance
                    usdLiabilities += b.usdBalance
                }
                AccountType.EQUITY -> {
                    liraEquity += b.liraBalance
                    usdEquity += b.usdBalance
                }
                AccountType.REVENUE -> {
                    liraRevenue += b.liraBalance
                    usdRevenue += b.usdBalance
                }
                AccountType.EXPENSE -> {
                    liraExpense += b.liraBalance
                    usdExpense += b.usdBalance
                }
            }
        }

        DashboardSummary(
            liraAssets = liraAssets,
            usdAssets = usdAssets,
            liraLiabilities = liraLiabilities,
            usdLiabilities = usdLiabilities,
            liraEquity = liraEquity,
            usdEquity = usdEquity,
            liraRevenue = liraRevenue,
            usdRevenue = usdRevenue,
            liraExpense = liraExpense,
            usdExpense = usdExpense,
            liraProfit = liraRevenue - liraExpense,
            usdProfit = usdRevenue - usdExpense
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardSummary()
    )

    // --- Journal Entry Drafting State ---
    private val _draftLines = MutableStateFlow<List<TransactionLineDraft>>(emptyList())
    val draftLines: StateFlow<List<TransactionLineDraft>> = _draftLines.asStateFlow()

    fun addDraftLine(line: TransactionLineDraft) {
        _draftLines.value = _draftLines.value + line
    }

    fun removeDraftLine(index: Int) {
        val list = _draftLines.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _draftLines.value = list
        }
    }

    fun clearDraft() {
        _draftLines.value = emptyList()
    }

    suspend fun saveJournalEntry(
        description: String,
        date: Long,
        currency: Currency
    ): ValidationResult {
        if (description.isBlank()) {
            return ValidationResult.Error("يرجى إدخال بيان/وصف عام للعملية")
        }
        val linesDraftList = _draftLines.value
        if (linesDraftList.isEmpty()) {
            return ValidationResult.Error("لا يمكن حفظ قيد فارغ")
        }

        val dbLines = linesDraftList.map {
            TransactionLine(
                transactionId = 0,
                accountId = it.accountId,
                isDebit = it.isDebit,
                amount = it.amount,
                lineDescription = it.description.ifBlank { null }
            )
        }

        val transaction = Transaction(
            date = date,
            description = description.trim(),
            currency = currency
        )

        val result = repository.saveTransaction(transaction, dbLines)
        if (result is ValidationResult.Success) {
            clearDraft()
        }
        return result
    }

    // --- Fast/Quick Treasury Voucher (Receipt/Payment) ---
    suspend fun saveQuickVoucher(
        isReceipt: Boolean, // true = سند قبض, false = سند صرف
        cashAccountId: Long,
        counterpartAccountId: Long,
        amount: Double,
        currency: Currency,
        description: String,
        date: Long
    ): ValidationResult {
        if (amount <= 0) {
            return ValidationResult.Error("يجب أن يكون المبلغ أكبر من الصفر")
        }
        if (cashAccountId == 0L || counterpartAccountId == 0L) {
            return ValidationResult.Error("يرجى تحديد حساب الصندوق والحساب المقابل")
        }
        if (cashAccountId == counterpartAccountId) {
            return ValidationResult.Error("لا يمكن اختيار نفس الحساب للطرفين")
        }
        if (description.isBlank()) {
            return ValidationResult.Error("يرجى إدخال وصف أو بيان للسند")
        }

        val prefix = if (isReceipt) "سند قبض سريغ" else "سند صرف سريع"
        val fullDescription = "$prefix: $description"

        val transaction = Transaction(
            date = date,
            description = fullDescription,
            currency = currency
        )

        val lines = if (isReceipt) {
            // Receipt: Cash gets debited (isDebit = true), Counterpart gets credited (isDebit = false)
            listOf(
                TransactionLine(transactionId = 0, accountId = cashAccountId, isDebit = true, amount = amount, lineDescription = description),
                TransactionLine(transactionId = 0, accountId = counterpartAccountId, isDebit = false, amount = amount, lineDescription = description)
            )
        } else {
            // Payment: Counterpart gets debited (isDebit = true), Cash gets credited (isDebit = false)
            listOf(
                TransactionLine(transactionId = 0, accountId = counterpartAccountId, isDebit = true, amount = amount, lineDescription = description),
                TransactionLine(transactionId = 0, accountId = cashAccountId, isDebit = false, amount = amount, lineDescription = description)
            )
        }

        return repository.saveTransaction(transaction, lines)
    }

    // --- Account Management ---
    suspend fun createAccount(
        code: String,
        name: String,
        type: AccountType,
        parentId: Long? = null
    ): ValidationResult {
        val trimmedCode = code.trim()
        val trimmedName = name.trim()
        if (trimmedCode.isBlank()) {
            return ValidationResult.Error("الرجاء إدخال رمز الحساب")
        }
        if (trimmedName.isBlank()) {
            return ValidationResult.Error("الرجاء إدخال اسم الحساب")
        }

        // Check if code is unique
        val existing = repository.getAccountByCode(trimmedCode)
        if (existing != null) {
            return ValidationResult.Error("رمز الحساب ($trimmedCode) مستخدم مسبقاً لحساب: '${existing.name}'")
        }

        val account = Account(
            code = trimmedCode,
            name = trimmedName,
            type = type,
            parentId = parentId
        )

        return try {
            repository.insertAccount(account)
            ValidationResult.Success
        } catch (e: Exception) {
            ValidationResult.Error("فشلت عملية الإضافة: ${e.localizedMessage}")
        }
    }

    fun deleteAccount(account: Account, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteAccount(account)
                onSuccess()
            } catch (e: Exception) {
                onError("لا يمكن حذف هذا الحساب لأنه مرتبط بقيود محاسبية مسجلة.")
            }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }
}

class AccountingViewModelFactory(private val repository: AccountingRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AccountingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AccountingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
