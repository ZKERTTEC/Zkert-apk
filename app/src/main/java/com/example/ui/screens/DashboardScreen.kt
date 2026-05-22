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
import com.example.data.model.Currency
import com.example.data.model.Transaction
import com.example.data.model.HydratedLine
import com.example.ui.AccountingViewModel
import com.example.ui.DashboardSummary
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.GoldAccent
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier,
    onNavigateToQuickVoucher: () -> Unit,
    onNavigateToJournal: () -> Unit
) {
    val summary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val lines by viewModel.lines.collectAsStateWithLifecycle()

    var selectedCurrency by remember { mutableStateOf(Currency.LIRA) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen")
            .padding(16.dp),
        horizontalAlignment = Alignment.End, // Standard Arabic Right alignment
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFEFF6FF)) // light sleek blue background
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "نظام زكرت للمحاسبة 🖋️",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A), // deep corporate blue
                            fontSize = 20.sp
                        ),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "نظام القيد المزدوج الآمن والاحترافي والمستقل تماماً دون الحاجة للإنترنت",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF1E293B)
                        ),
                        textAlign = TextAlign.Right
                    )
                }
            }
        }

        // Currency Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    text = "فلترة ملخص الأرصدة:",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // Dashboard Balance Summary Cards
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 2
            ) {
                val assets = if (selectedCurrency == Currency.LIRA) summary.liraAssets else summary.usdAssets
                val liabilities = if (selectedCurrency == Currency.LIRA) summary.liraLiabilities else summary.usdLiabilities
                val profit = if (selectedCurrency == Currency.LIRA) summary.liraProfit else summary.usdProfit
                val equity = if (selectedCurrency == Currency.LIRA) summary.liraEquity else summary.usdEquity

                SummaryCard(
                    title = "إجمالي الأصول",
                    amount = assets,
                    currencySymbol = selectedCurrency.symbol,
                    icon = Icons.Default.Home,
                    cardColor = MaterialTheme.colorScheme.surface,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.weight(1f)
                )

                SummaryCard(
                    title = "إجمالي الخصوم",
                    amount = liabilities,
                    currencySymbol = selectedCurrency.symbol,
                    icon = Icons.Default.Warning,
                    cardColor = MaterialTheme.colorScheme.surface,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 2
            ) {
                val profit = if (selectedCurrency == Currency.LIRA) summary.liraProfit else summary.usdProfit
                val equity = if (selectedCurrency == Currency.LIRA) summary.liraEquity else summary.usdEquity

                SummaryCard(
                    title = "حقوق الملكية",
                    amount = equity,
                    currencySymbol = selectedCurrency.symbol,
                    icon = Icons.Default.Done,
                    cardColor = MaterialTheme.colorScheme.surface,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.weight(1f)
                )

                SummaryCard(
                    title = "صافي الربح / الهامش",
                    amount = profit,
                    currencySymbol = selectedCurrency.symbol,
                    icon = Icons.Default.Check,
                    cardColor = if (profit >= 0) EmeraldGreen.copy(alpha = 0.08f) else RoseRed.copy(alpha = 0.08f),
                    borderColor = if (profit >= 0) EmeraldGreen.copy(alpha = 0.4f) else RoseRed.copy(alpha = 0.4f),
                    textColor = if (profit >= 0) EmeraldGreen else RoseRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onNavigateToJournal,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(54.dp)
                        .testTag("action_new_journal"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text(
                        text = "➕ سند قيد جديد",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }

                Button(
                    onClick = onNavigateToQuickVoucher,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(54.dp)
                        .testTag("action_new_voucher"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Text(
                        text = "📊 قبض وصرف سريع",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF334155)
                    )
                }
            }
        }

        // Recent Entries List Title
        item {
            Text(
                text = "آخر القيود اليومية المسجلة",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (transactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا توجد قيود مسجلة بعد. ابدأ بإدخال قيد جديد أو سند سريع لتجربة الدفتر المحاسبي!",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.secondary)
                        )
                    }
                }
            }
        } else {
            items(transactions.take(8)) { transaction ->
                val transLines = lines.filter { it.transactionId == transaction.id }
                TransactionRowItem(
                    transaction = transaction,
                    lines = transLines,
                    onDelete = { viewModel.deleteTransaction(transaction.id) }
                )
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    amount: Double,
    currencySymbol: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    cardColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    val actualCardColor = if (cardColor == MaterialTheme.colorScheme.surface) Color.White else cardColor
    val actualBorderColor = if (borderColor == MaterialTheme.colorScheme.outlineVariant) Color(0xFFE2E8F0) else borderColor
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = actualCardColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, actualBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (textColor == MaterialTheme.colorScheme.onSurface) MaterialTheme.colorScheme.primary else textColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.secondary)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = currencySymbol,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    ),
                    modifier = Modifier.padding(end = 4.dp, bottom = 2.dp)
                )

                val formattedAmount = String.format("%,.2f", amount)
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = textColor,
                        fontSize = 20.sp
                    ),
                    textAlign = TextAlign.Right
                )
            }
        }
    }
}

@Composable
fun TransactionRowItem(
    transaction: Transaction,
    lines: List<HydratedLine>,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .padding(vertical = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Main Line info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Delete Button & Date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف القيد",
                            tint = RoseRed.copy(alpha = 0.8f)
                        )
                    }

                    Text(
                        text = formatDateString(transaction.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // Description and amount
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (transaction.currency == Currency.USD) AccentBlue.copy(alpha = 0.15f)
                                    else GoldAccent.copy(alpha = 0.15f)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = transaction.currency.arabicName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (transaction.currency == Currency.USD) AccentBlue else GoldAccent
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = transaction.description,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Right
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val totalDebit = lines.filter { it.isDebit }.sumOf { it.amount }
                    Text(
                        text = "القيمة الإجمالية: ${String.format("%,.2f", totalDebit)} ${transaction.currency.symbol}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "تفاصيل القيد المزدوج:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    lines.forEach { line ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val lineAmount = String.format("%,.2f", line.amount)
                            val natureStr = if (line.isDebit) "مدين" else "دائن"
                            val natureColor = if (line.isDebit) AccentBlue else EmeraldGreen

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$lineAmount ${transaction.currency.symbol}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(natureColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = natureStr,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = natureColor
                                    )
                                }
                            }

                            Text(
                                text = "(${line.accountCode}) ${line.accountName}",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Right
                            )
                        }

                        if (!line.lineDescription.isNullOrBlank()) {
                            Text(
                                text = "📝 بيان التفصيل: ${line.lineDescription}",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp, start = 8.dp),
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatDateString(millis: Long): String {
    val date = Date(millis)
    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar"))
    return sdf.format(date)
}
