package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.ReceiptConfig
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDialog(
    grandTotal: Double,
    receiptConfig: ReceiptConfig?,
    onDismiss: () -> Unit,
    onComplete: (paymentMethod: String, amountPaid: Double, notes: String) -> Unit
) {
    val sym = receiptConfig?.currencySymbol ?: "$"
    var selectedMethod by remember { mutableStateOf("CASH") }
    var amountPaidStr by remember { mutableStateOf("%.2f".format(grandTotal)) }
    var notes by remember { mutableStateOf("") }

    val amountPaid = amountPaidStr.toDoubleOrNull() ?: 0.0
    val changeDue = (amountPaid - grandTotal).coerceAtLeast(0.0)
    val isShort = amountPaid < grandTotal && selectedMethod == "CASH"

    // Quick cash suggestions based on total
    val quickAmounts = remember(grandTotal) {
        val list = mutableListOf<Double>()
        list.add(grandTotal) // Exact
        val ceil10 = ceil(grandTotal / 10.0) * 10.0
        if (ceil10 > grandTotal) list.add(ceil10)
        val ceil50 = ceil(grandTotal / 50.0) * 50.0
        if (ceil50 > ceil10 && ceil50 > grandTotal) list.add(ceil50)
        val ceil100 = ceil(grandTotal / 100.0) * 100.0
        if (ceil100 > ceil50 && ceil100 > grandTotal) list.add(ceil100)
        if (list.size < 4) {
            list.add(grandTotal + 10.0)
            list.add(grandTotal + 20.0)
        }
        list.distinct().take(4)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .testTag("payment_dialog"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Checkout & Payment",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select payment method and finalize sale",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Grand Total Display Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TOTAL DUE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$sym%.2f".format(grandTotal),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Payment Method Selector
                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val methods = listOf(
                    "CASH" to Icons.Default.Payments,
                    "CARD" to Icons.Default.CreditCard,
                    "SPLIT" to Icons.Default.CallSplit,
                    "CREDIT" to Icons.Default.AccountBalanceWallet
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    methods.forEach { (method, icon) ->
                        val isSelected = selectedMethod == method
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedMethod = method
                                    if (method != "CASH") {
                                        amountPaidStr = "%.2f".format(grandTotal)
                                    }
                                }
                                .testTag("method_$method"),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = method,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = method,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Cash Tendered Inputs
                if (selectedMethod == "CASH" || selectedMethod == "SPLIT") {
                    Text(
                        text = "Amount Tendered ($sym)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = amountPaidStr,
                        onValueChange = { amountPaidStr = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("amount_tendered_input"),
                        leadingIcon = { Text(sym, fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Cash Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickAmounts.forEach { amt ->
                            SuggestionChip(
                                onClick = { amountPaidStr = "%.2f".format(amt) },
                                label = {
                                    Text(
                                        text = if (amt == grandTotal) "Exact" else "$sym%.0f".format(amt),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Change Due Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isShort) MaterialTheme.colorScheme.errorContainer else Color(0xFFDCFCE7)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isShort) "Short Balance" else "Change Due",
                                fontWeight = FontWeight.Bold,
                                color = if (isShort) MaterialTheme.colorScheme.error else Color(0xFF166534)
                            )
                            Text(
                                text = if (isShort) "$sym%.2f".format(grandTotal - amountPaid) else "$sym%.2f".format(changeDue),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isShort) MaterialTheme.colorScheme.error else Color(0xFF166534)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cashier Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Order Notes / Reference (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Complete Sale Button
                Button(
                    onClick = {
                        val paid = if (selectedMethod == "CARD") grandTotal else amountPaid
                        onComplete(selectedMethod, paid, notes)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("complete_sale_button"),
                    enabled = !isShort || selectedMethod != "CASH",
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Complete Sale & Generate Receipt",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
