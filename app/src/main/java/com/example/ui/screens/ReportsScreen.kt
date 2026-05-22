package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Account
import com.example.data.model.Currency
import com.example.data.model.HydratedLine
import com.example.ui.AccountingViewModel
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import java.text.SimpleDateFormat
import java.util.Locale

enum class ReportTab(val titleAr: String) {
    TRIAL_BALANCE("ميزان المراجعة والأرصدة"),
    LEDGER("كشف حساب تفصيلي")
}

@Composable
fun ReportsScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val balances by viewModel.accountBalances.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val lines by viewModel.lines.collectAsStateWithLifecycle()
    val transactionsList by viewModel.transactions.collectAsStateWithLifecycle()

    var activeReportTab by remember { mutableStateOf(ReportTab.TRIAL_BALANCE) }
    var selectedCurrency by remember { mutableStateOf(Currency.LIRA) }

    // State for Trial Balance
    var searchCodeOrName by remember { mutableStateOf("") }

    // State for Account Ledger
    var selectedAccountForLedger by remember { mutableStateOf<Account?>(null) }
    var accountSearchQuery by remember { mutableStateOf("") }
    var isAccountDropdownExpanded by remember { mutableStateOf(false) }

    val sdf = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar")) }
    val transactionsMap = remember(transactionsList) { transactionsList.associateBy { it.id } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen")
            .padding(16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Segment Switcher (Trial Balance / Ledger)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFF1F5F9))
                .padding(4.dp)
        ) {
            ReportTab.values().forEach { tab ->
                val selected = activeReportTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (selected) Color.White else Color.Transparent)
                        .clickable { activeReportTab = tab }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.titleAr,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (selected) Color(0xFF2563EB) else Color(0xFF64748B)
                        )
                    )
                }
            }
        }

        // Filter / Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Currency filter toggle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedCurrency == Currency.USD) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { selectedCurrency = Currency.USD }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "الدولار ($)",
                        color = if (selectedCurrency == Currency.USD) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedCurrency == Currency.LIRA) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { selectedCurrency = Currency.LIRA }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "ليرة (ل.س)",
                        color = if (selectedCurrency == Currency.LIRA) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "عملة التقرير المحاسبي:",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            )
        }

        if (activeReportTab == ReportTab.TRIAL_BALANCE) {
            // =============== TRIAL BALANCE TAB ===============
            
            // Search text field
            OutlinedTextField(
                value = searchCodeOrName,
                onValueChange = { searchCodeOrName = it },
                placeholder = { Text("ابحث برمز الحساب أو اسمه...") },
                trailingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextAlign.Right.let { LocalTextStyle.current.copy(textAlign = TextAlign.Right) }
            )

            // Filter balances by search term and compute totals
            val filteredBalances = remember(balances, searchCodeOrName) {
                balances.filter { item ->
                    val query = searchCodeOrName.trim()
                    query.isEmpty() ||
                            item.account.name.contains(query, ignoreCase = true) ||
                            item.account.code.contains(query)
                }
            }

            // Compute Totals in real time
            val totalDebitSum = remember(filteredBalances, selectedCurrency) {
                filteredBalances.sumOf {
                    if (selectedCurrency == Currency.LIRA) it.liraDebit else it.usdDebit
                }
            }

            val totalCreditSum = remember(filteredBalances, selectedCurrency) {
                filteredBalances.sumOf {
                    if (selectedCurrency == Currency.LIRA) it.liraCredit else it.usdCredit
                }
            }

            val totalNetDebitBalance = remember(filteredBalances, selectedCurrency) {
                filteredBalances.sumOf { item ->
                    val isLira = selectedCurrency == Currency.LIRA
                    val debit = if (isLira) item.liraDebit else item.usdDebit
                    val credit = if (isLira) item.liraCredit else item.usdCredit
                    if (item.account.type.isDebitNormal) {
                        val net = debit - credit
                        if (net > 0) net else 0.0
                    } else {
                        val net = credit - debit
                        if (net < 0) -net else 0.0
                    }
                }
            }

            val totalNetCreditBalance = remember(filteredBalances, selectedCurrency) {
                filteredBalances.sumOf { item ->
                    val isLira = selectedCurrency == Currency.LIRA
                    val debit = if (isLira) item.liraDebit else item.usdDebit
                    val credit = if (isLira) item.liraCredit else item.usdCredit
                    if (!item.account.type.isDebitNormal) {
                        val net = credit - debit
                        if (net > 0) net else 0.0
                    } else {
                        val net = debit - credit
                        if (net < 0) -net else 0.0
                    }
                }
            }

            // Ledger Audit Card Warning
            val sumDiff = Math.abs(totalDebitSum - totalCreditSum)
            val balanceDiff = Math.abs(totalNetDebitBalance - totalNetCreditBalance)
            val isPerfect = sumDiff < 0.1 && balanceDiff < 0.1

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPerfect) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                ),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isPerfect) Color(0xFFBBF7D0) else Color(0xFFFECACA)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = if (isPerfect) "ميزان المراجعة متزن ومطابق 100%! ✅" else "تحذير: يوجد فارق عدم اتزان في الميزان المحاسبي! ⚠️",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isPerfect) EmeraldGreen else RoseRed,
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "إجمالي مدين المجاميع يساوي إجمالي دائنه، مما يضمن دقة الترحيل وخلوه من العيوب.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isPerfect) MaterialTheme.colorScheme.onSurface else RoseRed
                            ),
                            textAlign = TextAlign.Right
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isPerfect) EmeraldGreen else RoseRed
                    )
                }
            }

            // Spreadsheet Column Headers Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الرصيد",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                    modifier = Modifier.width(75.dp),
                    textAlign = TextAlign.Left
                )
                Text(
                    text = "المجاميع (مدين/دائن)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                    modifier = Modifier.width(130.dp),
                    textAlign = TextAlign.Right
                )
                Text(
                    text = "الحساب",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Right
                )
            }

            // Accounts Table Body list
            if (filteredBalances.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا تتوفر حسابات تطابق معايير تصفيتك.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.secondary)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredBalances) { item ->
                        val isLira = selectedCurrency == Currency.LIRA
                        val debit = if (isLira) item.liraDebit else item.usdDebit
                        val credit = if (isLira) item.liraCredit else item.usdCredit
                        val rawBal = if (item.account.type.isDebitNormal) debit - credit else credit - debit

                        TrialBalanceRow(
                            code = item.account.code,
                            name = item.account.name,
                            debit = debit,
                            credit = credit,
                            netBalance = rawBal,
                            isDebitNormal = item.account.type.isDebitNormal,
                            symbol = selectedCurrency.symbol
                        )
                    }
                }
            }

            // Financial report summaries footer banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "مجموع اتزان الدفتر المحاسبي النهائي:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

                    // Sum match Check
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${String.format("%,.2f", totalDebitSum)} ${selectedCurrency.symbol}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "مجموع الحركات المدينة:",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${String.format("%,.2f", totalCreditSum)} ${selectedCurrency.symbol}",
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                        Text(
                            text = "مجموع الحركات الدائنة:",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${String.format("%,.2f", totalNetDebitBalance)} ${selectedCurrency.symbol}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "مجموع الأرصدة المدينة:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${String.format("%,.2f", totalNetCreditBalance)} ${selectedCurrency.symbol}",
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                        Text(
                            text = "مجموع الأرصدة الدائنة:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // =============== ACCOUNT LEDGER TAB ===============
            
            // Searchable Account Dropdown Selector
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "اختر الحساب المستهدف لطلب كشف تفصيلي:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF475569)),
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                Box(modifier = Modifier.fillMaxWidth().zIndex(10f)) {
                    OutlinedTextField(
                        value = if (isAccountDropdownExpanded) accountSearchQuery else (selectedAccountForLedger?.let { "${it.name} (${it.code})" } ?: ""),
                        onValueChange = {
                            accountSearchQuery = it
                            isAccountDropdownExpanded = true
                        },
                        placeholder = { Text("ابحث عن الحساب باسمه أو رمزه...") },
                        trailingIcon = {
                            IconButton(onClick = { isAccountDropdownExpanded = !isAccountDropdownExpanded }) {
                                Text(if (isAccountDropdownExpanded) "▲" else "▼", fontSize = 14.sp)
                            }
                        },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextAlign.Right.let { LocalTextStyle.current.copy(textAlign = TextAlign.Right) }
                    )

                    if (isAccountDropdownExpanded) {
                        val filteredAccounts = accounts.filter {
                            accountSearchQuery.isEmpty() ||
                                    it.name.contains(accountSearchQuery, ignoreCase = true) ||
                                    it.code.contains(accountSearchQuery)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 60.dp)
                                .heightIn(max = 220.dp),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            if (filteredAccounts.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("لا توجد حسابات مطابقة", color = Color.Gray)
                                }
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                    items(filteredAccounts) { acc ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedAccountForLedger = acc
                                                    accountSearchQuery = ""
                                                    isAccountDropdownExpanded = false
                                                }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "(${acc.type.arabicName})",
                                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                                            )
                                            Text(
                                                text = "${acc.name} - ${acc.code}",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                textAlign = TextAlign.Right
                                            )
                                        }
                                        Divider(color = Color(0xFFF1F5F9))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (selectedAccountForLedger == null) {
                // Initial prompt when no account is selected
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "🔎",
                            fontSize = 48.sp
                        )
                        Text(
                            text = "الرجاء اختيار حساب من القائمة أعلاه لعرض كشف الحساب التفصيلي.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Filtered ledger lines
                val ledgerLines = remember(lines, selectedAccountForLedger, selectedCurrency) {
                    lines.filter { it.accountId == selectedAccountForLedger!!.id && it.transactionCurrency == selectedCurrency }
                        .sortedWith(compareBy<HydratedLine> { it.transactionDate }.thenBy { it.id })
                }

                val totalDebitsForLedger = remember(ledgerLines) {
                    ledgerLines.filter { it.isDebit }.sumOf { it.amount }
                }

                val totalCreditsForLedger = remember(ledgerLines) {
                    ledgerLines.filter { !it.isDebit }.sumOf { it.amount }
                }

                val finalNetBalance = remember(totalDebitsForLedger, totalCreditsForLedger, selectedAccountForLedger) {
                    val diff = totalDebitsForLedger - totalCreditsForLedger
                    if (selectedAccountForLedger!!.type.isDebitNormal) diff else -diff
                }

                // Overview Header Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(24.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("الرصيد النهائي الحالي", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B)))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${String.format("%,.2f", finalNetBalance)} ${selectedCurrency.symbol}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = if (finalNetBalance >= 0) Color(0xFF1E3A8A) else Color(0xFFEF4444)
                            )
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color(0xFFE2E8F0)))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إجمالي الدائن (📤)", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B)))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${String.format("%,.2f", totalCreditsForLedger)} ${selectedCurrency.symbol}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color(0xFFE2E8F0)))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إجمالي المدين (📥)", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B)))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${String.format("%,.2f", totalDebitsForLedger)} ${selectedCurrency.symbol}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
                            )
                        )
                    }
                }

                // Header Table List
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEFF6FF))
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الرصيد",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, color = Color(0xFF1E3A8A)),
                        modifier = Modifier.width(85.dp),
                        textAlign = TextAlign.Left
                    )
                    Text(
                        text = "مدين/دائن",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, color = Color(0xFF1E3A8A)),
                        modifier = Modifier.width(100.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "التاريخ والبيان",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, color = Color(0xFF1E3A8A)),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Right
                    )
                }

                if (ledgerLines.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📄", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "لا توجد قيود أو حركات مسجلة لهذا الحساب بالعملة المحددة حالياً.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF64748B)),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Render Ledger Rows with Running Cumulative Balances
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        var runningBalance = 0.0
                        val isDebitNormal = selectedAccountForLedger!!.type.isDebitNormal

                        items(ledgerLines.size) { index ->
                            val line = ledgerLines[index]
                            val tx = transactionsMap[line.transactionId]
                            val descriptionToShow = line.lineDescription?.ifBlank { null } ?: tx?.description ?: "قيد ترحيل يومية"

                            // Calculate cumulative balance dynamically step-by-step
                            val change = if (line.isDebit) {
                                if (isDebitNormal) line.amount else -line.amount
                            } else {
                                if (!isDebitNormal) line.amount else -line.amount
                            }
                            runningBalance += change

                            LedgerRow(
                                dateStr = sdf.format(line.transactionDate),
                                desc = descriptionToShow,
                                isDebit = line.isDebit,
                                amount = line.amount,
                                runningBal = runningBalance,
                                symbol = selectedCurrency.symbol
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LedgerRow(
    dateStr: String,
    desc: String,
    isDebit: Boolean,
    amount: Double,
    runningBal: Double,
    symbol: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Left: Cumulative Balance after this row
            Column(
                modifier = Modifier.width(85.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "${String.format("%,.2f", runningBal)} $symbol",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = if (runningBal >= 0) Color(0xFF1E3A8A) else Color(0xFFEF4444)
                )
                Text(
                    text = "الرصيد التراكمي",
                    fontSize = 9.sp,
                    color = Color.Gray
                )
            }

            // 2. Middle: Debit / Credit amount
            Column(
                modifier = Modifier.width(100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${if (isDebit) "📥 +" else "📤 -"} ${String.format("%,.2f", amount)} $symbol",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isDebit) Color(0xFF2563EB) else EmeraldGreen
                    )
                )
                Text(
                    text = if (isDebit) "مدين" else "دائن",
                    fontSize = 9.sp,
                    color = if (isDebit) Color(0xFF3B82F6).copy(alpha = 0.8f) else EmeraldGreen.copy(alpha = 0.8f)
                )
            }

            // 3. Right: Date & Description
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = desc,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF1E293B),
                    textAlign = TextAlign.Right
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateStr,
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Right
                    )
                    Text(
                        text = "📅",
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TrialBalanceRow(
    code: String,
    name: String,
    debit: Double,
    credit: Double,
    netBalance: Double,
    isDebitNormal: Boolean,
    symbol: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Balance net block
            Column(
                modifier = Modifier.width(80.dp),
                horizontalAlignment = Alignment.Start
            ) {
                val absBalanced = Math.abs(netBalance)
                val isNegative = netBalance < 0.0
                val color = if (isDebitNormal) {
                    if (isNegative) RoseRed else MaterialTheme.colorScheme.primary
                } else {
                    if (isNegative) RoseRed else EmeraldGreen
                }

                Text(
                    text = "${String.format("%,.2f", absBalanced)} $symbol",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = color
                )

                Text(
                    text = if (isDebitNormal) {
                        if (isNegative) "رصيد دائن شاذ" else "رصيد مدين"
                    } else {
                        if (isNegative) "رصيد مدين شاذ" else "رصيد دائن"
                    },
                    fontSize = 9.sp,
                    color = Color.Gray
                )
            }

            // Sum details
            Column(
                modifier = Modifier.width(130.dp),
                horizontalAlignment = Alignment.End
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "مدين: ", fontSize = 10.sp, color = Color.Gray)
                    Text(text = String.format("%,.1f", debit), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "دائن: ", fontSize = 10.sp, color = Color.Gray)
                    Text(text = String.format("%,.1f", credit), fontSize = 12.sp, color = EmeraldGreen)
                }
            }

            // Account details
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Right,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "كود: $code",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Right
                )
            }
        }
    }
}

