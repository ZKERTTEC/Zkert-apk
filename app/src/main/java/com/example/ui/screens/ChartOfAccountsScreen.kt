package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.ui.AccountingViewModel
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.data.repository.ValidationResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChartOfAccountsScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val errorState = remember { mutableStateOf<String?>(null) }
    val successState = remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    var showAddAccountDialog by remember { mutableStateOf(false) }

    // Expanded state for each card type
    val expandedTypes = remember {
        mutableStateMapOf(
            AccountType.ASSET to true,
            AccountType.LIABILITY to true,
            AccountType.EQUITY to true,
            AccountType.REVENUE to true,
            AccountType.EXPENSE to true
        )
    }

    Box(modifier = modifier
        .fillMaxSize()
        .testTag("chart_of_accounts_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Header with add button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showAddAccountDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "إضافة حساب", fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "شجرة دليل الحسابات",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info hint
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    .padding(12.dp)
            ) {
                Text(
                    text = "💡 يتم ترتيب وتوليد دليل الحسابات تلقائياً بناءً على كود الحساب وطبيعته المحاسبية الثنائية القيد.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary),
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main tree structure inside a scrollable LazyColumn
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AccountType.values().forEach { type ->
                    val filteredAccounts = accounts.filter { it.type == type }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(24.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Accordion Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandedTypes[type] = !(expandedTypes[type] ?: false) }
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (expandedTypes[type] == true) "▲" else "▼",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "${filteredAccounts.size}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = type.arabicName,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                // Children List
                                AnimatedVisibility(
                                    visible = expandedTypes[type] == true,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        if (filteredAccounts.isEmpty()) {
                                            Text(
                                                text = "لا توجد حسابات مضافة مسبقاً في هذا التصنيف.",
                                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.secondary),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp)
                                            )
                                        } else {
                                            filteredAccounts.forEach { account ->
                                                AccountListItem(
                                                    account = account,
                                                    onDelete = {
                                                        viewModel.deleteAccount(
                                                            account = account,
                                                            onSuccess = {
                                                                successState.value = "تم حذف حساب '${account.name}' بنجاح"
                                                            },
                                                            onError = { err ->
                                                                errorState.value = err
                                                            }
                                                        )
                                                    }
                                                )
                                                if (account != filteredAccounts.last()) {
                                                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Error message popup
        errorState.value?.let { error ->
            AlertDialog(
                onDismissRequest = { errorState.value = null },
                confirmButton = {
                    TextButton(onClick = { errorState.value = null }) {
                        Text("موافق")
                    }
                },
                title = { Text("تنبيه الحذف المحاسبي", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                text = { Text(error, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) }
            )
        }

        // Success snackbar style alert
        successState.value?.let { successMsg ->
            AlertDialog(
                onDismissRequest = { successState.value = null },
                confirmButton = {
                    TextButton(onClick = { successState.value = null }) {
                        Text("حسناً")
                    }
                },
                title = { Text("تمت العملية", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                text = { Text(successMsg, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) }
            )
        }

        // Add Account Dialog
        if (showAddAccountDialog) {
            AddAccountDialog(
                accountsList = accounts,
                onDismiss = { showAddAccountDialog = false },
                onAddAccount = { code, name, type, parentId ->
                    var isSuccess = false
                    scope.launch {
                        val result = viewModel.createAccount(code, name, type, parentId)
                        if (result is ValidationResult.Success) {
                            successState.value = "تم تأسيس الحساب الجديد المسمى ($name) بنجاح."
                            showAddAccountDialog = false
                        } else if (result is ValidationResult.Error) {
                            errorState.value = result.message
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun AccountListItem(
    account: Account,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Delete button
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = RoseRed.copy(alpha = 0.8f)
            )
        }

        // Details
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Right
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = account.code,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountDialog(
    accountsList: List<Account>,
    onDismiss: () -> Unit,
    onAddAccount: (String, String, AccountType, Long?) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AccountType.ASSET) }
    var selectedParentId by remember { mutableStateOf<Long?>(null) }

    var typeExpanded by remember { mutableStateOf(false) }
    var parentExpanded by remember { mutableStateOf(false) }

    val accountsOfSameType = accountsList.filter { it.type == selectedType }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onAddAccount(code, name, selectedType, selectedParentId) },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("تأسيس وحفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء الأمر")
            }
        },
        title = {
            Text(
                text = "تأسيس حساب محاسبي جديد",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Code Input
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("رمز أو كود الحساب (مثال: 1105)") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextAlign.Right.let { LocalTextStyle.current.copy(textAlign = TextAlign.Right) }
                )

                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الحساب العربي (مثال: صندوق الليرة السوري)") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextAlign.Right.let { LocalTextStyle.current.copy(textAlign = TextAlign.Right) }
                )

                // Type select Dropdown
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedType.arabicName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("نوع وتصنيف الحساب") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = TextAlign.Right.let { LocalTextStyle.current.copy(textAlign = TextAlign.Right) }
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        AccountType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.arabicName, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
                                onClick = {
                                    selectedType = type
                                    selectedParentId = null // Reset parent if type changes
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                // Optional Parent Account drop down
                ExposedDropdownMenuBox(
                    expanded = parentExpanded,
                    onExpandedChange = { parentExpanded = !parentExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val parentName = selectedParentId?.let { pid ->
                        accountsList.find { it.id == pid }?.let { "(${it.code}) ${it.name}" }
                    } ?: "بلا (حساب رئيسي)"

                    OutlinedTextField(
                        value = parentName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الحساب الرئيسي الأب (اختياري)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = parentExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = TextAlign.Right.let { LocalTextStyle.current.copy(textAlign = TextAlign.Right) }
                    )
                    ExposedDropdownMenu(
                        expanded = parentExpanded,
                        onDismissRequest = { parentExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("بلا (حساب رئيسي)", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
                            onClick = {
                                selectedParentId = null
                                parentExpanded = false
                            }
                        )
                        accountsOfSameType.forEach { parentAcc ->
                            DropdownMenuItem(
                                text = { Text("(${parentAcc.code}) ${parentAcc.name}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
                                onClick = {
                                    selectedParentId = parentAcc.id
                                    parentExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    )
}
