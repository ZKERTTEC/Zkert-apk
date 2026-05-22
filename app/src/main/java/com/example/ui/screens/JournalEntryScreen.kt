package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Account
import com.example.data.model.Currency
import com.example.data.repository.ValidationResult
import com.example.ui.AccountingViewModel
import com.example.ui.TransactionLineDraft
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun JournalEntryScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val draftLines by viewModel.draftLines.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Entry General State
    var description by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(Currency.LIRA) }
    var entryDate by remember { mutableStateOf(System.currentTimeMillis()) }

    // Dialog & Feedback States
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    // Line Drafting Fields
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var isDebitLine by remember { mutableStateOf(true) }
    var lineAmountText by remember { mutableStateOf("") }
    var lineDescText by remember { mutableStateOf("") }

    var accountExpanded by remember { mutableStateOf(false) }

    // Sum calculations
    val totalDebits = draftLines.filter { it.isDebit }.sumOf { it.amount }
    val totalCredits = draftLines.filter { !it.isDebit }.sumOf { it.amount }
    val difference = Math.abs(totalDebits - totalCredits)
    val isBalanced = difference < 0.001 && draftLines.size >= 2 && draftLines.any { it.isDebit } && draftLines.any { !it.isDebit }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("journal_entry_screen")
            .padding(16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen title
        item {
            Text(
                text = "تسجيل قيد يومية محاسبي ✍️",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Section 1: General Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "بيانات العملية العامة",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("بيان القيد العام (شرح العملية)") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextAlign.Right.let { LocalTextStyle.current.copy(textAlign = TextAlign.Right) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Currency Selector Toggle
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(2.dp)
                        ) {
                            Currency.values().forEach { cur ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (currency == cur) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { currency = cur }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = cur.arabicName,
                                        color = if (currency == cur) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        Text(
                            text = "عملة القيد:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Section 2: Line Builder
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "تفاصيل أسطر القيد المحاسبي",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Account SELECT dropdown list
                    ExposedDropdownMenuBox(
                        expanded = accountExpanded,
                        onExpandedChange = { accountExpanded = !accountExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val displayAccName = selectedAccount?.let { "(${it.code}) ${it.name}" } ?: "اختر الحساب المحاسبي"
                        OutlinedTextField(
                            value = displayAccName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("الحساب المتأثر") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = TextAlign.Right.let { LocalTextStyle.current.copy(textAlign = TextAlign.Right) }
                        )

                        ExposedDropdownMenu(
                            expanded = accountExpanded,
                            onDismissRequest = { accountExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("(${acc.code}) ${acc.name}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
                                    onClick = {
                                        selectedAccount = acc
                                        accountExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Nature Toggle (مدين / دائن)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (!isDebitLine) EmeraldGreen else Color.Transparent)
                                    .clickable { isDebitLine = false }
                                    .padding(horizontal = 20.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "دائن",
                                    color = if (!isDebitLine) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDebitLine) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { isDebitLine = true }
                                    .padding(horizontal = 20.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "مدين",
                                    color = if (isDebitLine) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Text(
                            text = "طبيعة السطر:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Amount Input
                    OutlinedTextField(
                        value = lineAmountText,
                        onValueChange = { lineAmountText = it },
                        label = { Text("المبلغ (${currency.symbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextAlign.Right.let { LocalTextStyle.current.copy(textAlign = TextAlign.Right) }
                    )

                    // Description Input per line (Optional)
                    OutlinedTextField(
                        value = lineDescText,
                        onValueChange = { lineDescText = it },
                        label = { Text("شرح تفصيلي للسطر (اختياري)") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextAlign.Right.let { LocalTextStyle.current.copy(textAlign = TextAlign.Right) }
                    )

                    // Add Line trigger
                    Button(
                        onClick = {
                            val acc = selectedAccount
                            val amount = lineAmountText.toDoubleOrNull()
                            if (acc == null) {
                                errorMsg = "الرجاء تحديد الحساب المحاسبي المتأثر أولاً"
                                return@Button
                            }
                            if (amount == null || amount <= 0.0) {
                                errorMsg = "الرجاء إدخال مبلغ صحيح أكبر من الصفر"
                                return@Button
                            }

                            viewModel.addDraftLine(
                                TransactionLineDraft(
                                    accountId = acc.id,
                                    isDebit = isDebitLine,
                                    amount = amount,
                                    description = lineDescText
                                )
                            )

                            // Clear line states
                            lineAmountText = ""
                            lineDescText = ""
                            selectedAccount = null
                            focusManager.clearFocus()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.AddCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "إضافة السطر إلى القيد", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 3: Current Draft list Lines Title
        item {
            Text(
                text = "أسطر القيد المكتوبة حالياً (${draftLines.size}):",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (draftLines.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لم يتم إضافة أي أسطر لهذا القيد بعد. استخدم نموذج تفاصيل الأسطر أعلاه لإضافة المدين والدائن.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.secondary)
                    )
                }
            }
        } else {
            itemsIndexed(draftLines) { idx, item ->
                val accObj = accounts.find { it.id == item.accountId }
                DraftLineRowItem(
                    index = idx,
                    draft = item,
                    account = accObj,
                    symbol = currency.symbol,
                    onDelete = { viewModel.removeDraftLine(idx) }
                )
            }
        }

        // Section 4: Double Entry Balancing Metrics & Submission Action
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "ميزان اتزان القيد الحالي:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${String.format("%,.2f", totalDebits)} ${currency.symbol}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(text = "إجمالي المدين:")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${String.format("%,.2f", totalCredits)} ${currency.symbol}",
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                        Text(text = "إجمالي الدائن:")
                    }

                    Divider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val stateColor = if (isBalanced) EmeraldGreen else RoseRed
                        val feedbackMsg = if (isBalanced) {
                            "قيد متزن ومكتمل ومستعد للمطابقة! ✅"
                        } else {
                            "الفرق: ${String.format("%,.2f", difference)} ${currency.symbol} ❌"
                        }

                        Text(
                            text = feedbackMsg,
                            fontWeight = FontWeight.ExtraBold,
                            color = stateColor
                        )
                        Text(text = "حالة الاتزان المحاسبي:")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Final Submit Button
                    Button(
                        onClick = {
                            if (description.trim().isBlank()) {
                                errorMsg = "الرجاء كتابة بيان أو حجة صحيحة لشرح القيد بالكامل"
                                return@Button
                            }

                            scope.launch {
                                val result = viewModel.saveJournalEntry(
                                    description = description,
                                    date = entryDate,
                                    currency = currency
                                )
                                if (result is ValidationResult.Success) {
                                    successMsg = "تم تسجيل وحفظ القيد المزدوج ترحيلياً إلى الدفتر المحاسبي بنجاح."
                                    description = "" // Clear layout
                                } else if (result is ValidationResult.Error) {
                                    errorMsg = result.message
                                }
                            }
                        },
                        enabled = isBalanced && description.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "حفظ القيد وترجيله بالكامل", fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                }
            }
        }
    }

    // Modal alerts
    errorMsg?.let { error ->
        AlertDialog(
            onDismissRequest = { errorMsg = null },
            confirmButton = {
                TextButton(onClick = { errorMsg = null }) {
                    Text("فهمت")
                }
            },
            title = { Text("خطأ في القيد المحاسبي", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
            text = { Text(error, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) }
        )
    }

    successMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { successMsg = null },
            confirmButton = {
                TextButton(onClick = { successMsg = null }) {
                    Text("حسناً")
                }
            },
            title = { Text("تم ترحيل القيد", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
            text = { Text(msg, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) }
        )
    }
}

@Composable
fun DraftLineRowItem(
    index: Int,
    draft: TransactionLineDraft,
    account: Account?,
    symbol: String,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onDelete) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = RoseRed.copy(alpha = 0.8f))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Amount and designation nature
            val natStr = if (draft.isDebit) "مدين" else "دائن"
            val natColor = if (draft.isDebit) MaterialTheme.colorScheme.primary else EmeraldGreen

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${String.format("%,.2f", draft.amount)} $symbol",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(natColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = natStr,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = natColor
                        )
                    }
                }

                if (draft.description.isNotBlank()) {
                    Text(
                        text = "ملاحظة: ${draft.description}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.secondary),
                        textAlign = TextAlign.Right
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Account code and name
            Text(
                text = account?.let { "(${it.code}) ${it.name}" } ?: "حساب مجهول",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Right
            )
        }
    }
}
