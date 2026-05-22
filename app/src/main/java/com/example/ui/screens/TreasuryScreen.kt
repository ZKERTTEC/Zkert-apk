package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.data.model.AccountType
import com.example.data.model.Currency
import com.example.data.repository.ValidationResult
import com.example.ui.AccountingViewModel
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreasuryScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Receipts vs Payments tab
    var isReceipt by remember { mutableStateOf(true) } // true = سند قبض, false = سند صرف

    // Form selection states
    var selectedCashAccount by remember { mutableStateOf<Account?>(null) }
    var selectedCounterpartAccount by remember { mutableStateOf<Account?>(null) }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Dropdown toggles
    var cashExpanded by remember { mutableStateOf(false) }
    var counterpartExpanded by remember { mutableStateOf(false) }

    // Dialog sheets
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    // Auto calculate currency from cash account choice:
    // If "الصندوق (دولار)" or code 1102 etc.
    val currency = remember(selectedCashAccount) {
        val name = selectedCashAccount?.name ?: ""
        val code = selectedCashAccount?.code ?: ""
        if (code == "1102" || code == "1103" || name.contains("دولار") || name.contains("USD")) {
            Currency.USD
        } else {
            Currency.LIRA
        }
    }

    // Filter cash accounts: Accounts under ASSETS that contain "الصندوق" or "البنك" or starting with code "11"
    val cashAccounts = remember(accounts) {
        accounts.filter {
            it.type == AccountType.ASSET &&
            (it.code.startsWith("11") || it.name.contains("صندوق") || it.name.contains("الصندوق") || it.name.contains("بنك") || it.name.contains("البنك"))
        }
    }

    // Default cash account selection once accounts are seeded
    LaunchedEffect(cashAccounts) {
        if (selectedCashAccount == null && cashAccounts.isNotEmpty()) {
            selectedCashAccount = cashAccounts.first()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("treasury_screen")
            .padding(16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Title
        Text(
            text = "دفتر الخزينة والمقبوضات 🏦",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
            color = MaterialTheme.colorScheme.onBackground
        )

        // Tab switcher (سند قبض / سند صرف)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFF1F5F9))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (!isReceipt) RoseRed else Color.Transparent)
                    .clickable { isReceipt = false }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = if (!isReceipt) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "سند صرف سريع",
                        color = if (!isReceipt) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isReceipt) EmeraldGreen else Color.Transparent)
                    .clickable { isReceipt = true }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = if (isReceipt) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "سند قبض سريع",
                        color = if (isReceipt) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // Voucher Form Card
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                val accentColor = if (isReceipt) EmeraldGreen else RoseRed
                val voucherName = if (isReceipt) "سند قبض سريغ" else "سند صرف سريع"

                Text(
                    text = "بيانات تفصيل $voucherName",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = accentColor
                )

                Divider()

                // Cash Account Dropdown SELECTOR
                ExposedDropdownMenuBox(
                    expanded = cashExpanded,
                    onExpandedChange = { cashExpanded = !cashExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val labelText = if (isReceipt) "حساب الصندوق المستلم (القبض)" else "حساب الصندوق المصدر (الدفع)"
                    val cashText = selectedCashAccount?.let { "(${it.code}) ${it.name}" } ?: "اختر حساب النقدية"
                    OutlinedTextField(
                        value = cashText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(labelText) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cashExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = TextAlign.Right.let { LocalTextStyle.current.copy(textAlign = TextAlign.Right) }
                    )

                    ExposedDropdownMenu(
                        expanded = cashExpanded,
                        onDismissRequest = { cashExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        cashAccounts.forEach { cashAcc ->
                            DropdownMenuItem(
                                text = { Text("(${cashAcc.code}) ${cashAcc.name}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
                                onClick = {
                                    selectedCashAccount = cashAcc
                                    cashExpanded = false
                                }
                            )
                        }
                    }
                }

                // Counterpart Account Dropdown SELECTOR
                ExposedDropdownMenuBox(
                    expanded = counterpartExpanded,
                    onExpandedChange = { counterpartExpanded = !counterpartExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val labelText = if (isReceipt) "المستلم منه (الحساب المقابل الدائن)" else "المدفوع له (الحساب المقابل المدين)"
                    val counterpartText = selectedCounterpartAccount?.let { "(${it.code}) ${it.name}" } ?: "اختر الحساب الدائن/المدين المقابل"
                    OutlinedTextField(
                        value = counterpartText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(labelText) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = counterpartExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = TextAlign.Right.let { LocalTextStyle.current.copy(textAlign = TextAlign.Right) }
                    )

                    ExposedDropdownMenu(
                        expanded = counterpartExpanded,
                        onDismissRequest = { counterpartExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        accounts.filter { it.id != selectedCashAccount?.id }.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("(${acc.code}) ${acc.name}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
                                onClick = {
                                    selectedCounterpartAccount = acc
                                    counterpartExpanded = false
                                }
                            )
                        }
                    }
                }

                // Amount Textfield
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("مبلغ السند المحاسبي (${currency.symbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextAlign.Right.let { LocalTextStyle.current.copy(textAlign = TextAlign.Right) }
                )

                // Description textfield
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("بيان أو سبب تسجيل السند (مثال: سداد ذمم الزبون أحمد)") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextAlign.Right.let { LocalTextStyle.current.copy(textAlign = TextAlign.Right) }
                )

                // Currency summary indicator line (Readonly auto-tied indicator)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${currency.arabicName} (${currency.symbol})",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "العملة المكتشفة تلقائياً للحساب:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Post Button
                Button(
                    onClick = {
                        val cash = selectedCashAccount
                        val counterpart = selectedCounterpartAccount
                        val amount = amountText.toDoubleOrNull()

                        if (cash == null || counterpart == null) {
                            errorMsg = "الرجاء تحديد كلا الحسابين (الصندوق والحساب المقابل) لإتمام ترحيل السند."
                            return@Button
                        }
                        if (amount == null || amount <= 0) {
                            errorMsg = "الرجاء تدوين مبلغ مالي صحيح أكبر من الصفر."
                            return@Button
                        }
                        if (description.isBlank()) {
                            errorMsg = "الرجاء توضيح شرح أو بيان مبسط للسند لحفظه بالدفتر اليومي."
                            return@Button
                        }

                        scope.launch {
                            val result = viewModel.saveQuickVoucher(
                                isReceipt = isReceipt,
                                cashAccountId = cash.id,
                                counterpartAccountId = counterpart.id,
                                amount = amount,
                                currency = currency,
                                description = description,
                                date = System.currentTimeMillis()
                            )

                            if (result is ValidationResult.Success) {
                                val voucherAction = if (isReceipt) "قبض" else "صرف"
                                successMsg = "تم حفظ وتوليد القيد المزدوج لـ '$voucherName' بقيمة $amountText ${currency.symbol} تلقائياً وترحيله بنجاح."
                                // Clear inputs
                                amountText = ""
                                description = ""
                                focusManager.clearFocus()
                            } else if (result is ValidationResult.Error) {
                                errorMsg = result.message
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ترحيل وحفظ السند المحاسبي",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Modal Notifications
    errorMsg?.let { error ->
        AlertDialog(
            onDismissRequest = { errorMsg = null },
            confirmButton = {
                TextButton(onClick = { errorMsg = null }) {
                    Text("إغلاق")
                }
            },
            title = { Text("فشل التسجيل السريع", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
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
            title = { Text("تم التسجيل والترحيل 📁", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
            text = { Text(msg, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) }
        )
    }
}
