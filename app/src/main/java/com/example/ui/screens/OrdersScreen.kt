package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.dao.SaleWithItems
import com.example.data.entity.ReceiptConfig
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OrdersScreen(
    salesHistory: List<SaleWithItems>,
    receiptConfig: ReceiptConfig?,
    onShowReceipt: (SaleWithItems) -> Unit,
    onProcessRefund: (saleId: Long, reason: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedOrder by remember { mutableStateOf<SaleWithItems?>(salesHistory.firstOrNull()) }
    var refundingOrder by remember { mutableStateOf<SaleWithItems?>(null) }
    var refundReason by remember { mutableStateOf("Customer Return") }

    val sym = receiptConfig?.currencySymbol ?: "$"
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)

    val filteredSales = remember(salesHistory, searchQuery) {
        if (searchQuery.isBlank()) salesHistory
        else salesHistory.filter {
            it.sale.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    it.sale.customerName.contains(searchQuery, ignoreCase = true) ||
                    it.sale.paymentMethod.contains(searchQuery, ignoreCase = true)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().testTag("orders_screen")) {
        val isWide = maxWidth >= 760.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Sales & Invoices History",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Search invoices, reprint customer bills, or issue refunds",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by invoice # or customer name…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("orders_search_input"),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredSales.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No orders found matching '$searchQuery'" else "No sales recorded yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (isWide) {
                // Adaptive Split Screen for Tablets / Landscape
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left: Orders list
                    LazyColumn(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredSales, key = { it.sale.id }) { saleWithItems ->
                            val isCurrent = selectedOrder?.sale?.id == saleWithItems.sale.id
                            val isRefunded = saleWithItems.sale.status == "REFUNDED"
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedOrder = saleWithItems }
                                    .testTag("order_item_${saleWithItems.sale.id}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else if (isRefunded) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                OrderListRow(
                                    saleWithItems = saleWithItems,
                                    sym = sym,
                                    dateFormat = dateFormat,
                                    isRefunded = isRefunded,
                                    onShowReceipt = onShowReceipt
                                )
                            }
                        }
                    }

                    VerticalDivider()

                    // Right: Order Details Pane
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        val active = selectedOrder ?: filteredSales.firstOrNull()
                        if (active != null) {
                            OrderDetailsPane(
                                order = active,
                                sym = sym,
                                dateFormat = dateFormat,
                                onReprint = { onShowReceipt(active) },
                                onRefund = { refundingOrder = active }
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text("Select an order to view details")
                            }
                        }
                    }
                }
            } else {
                // Compact Mobile List
                var showMobileDetailDialog by remember { mutableStateOf(false) }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredSales, key = { it.sale.id }) { saleWithItems ->
                        val isRefunded = saleWithItems.sale.status == "REFUNDED"
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    selectedOrder = saleWithItems
                                    showMobileDetailDialog = true
                                }
                                .testTag("order_item_${saleWithItems.sale.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isRefunded) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            OrderListRow(
                                saleWithItems = saleWithItems,
                                sym = sym,
                                dateFormat = dateFormat,
                                isRefunded = isRefunded,
                                onShowReceipt = onShowReceipt
                            )
                        }
                    }
                }

                // Mobile Dialog
                if (showMobileDetailDialog && selectedOrder != null) {
                    Dialog(onDismissRequest = { showMobileDetailDialog = false }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .clip(RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            OrderDetailsPane(
                                order = selectedOrder!!,
                                sym = sym,
                                dateFormat = dateFormat,
                                onReprint = {
                                    showMobileDetailDialog = false
                                    onShowReceipt(selectedOrder!!)
                                },
                                onRefund = {
                                    showMobileDetailDialog = false
                                    refundingOrder = selectedOrder!!
                                },
                                onClose = { showMobileDetailDialog = false }
                            )
                        }
                    }
                }
            }
        }

        // Refund Confirmation Dialog
        refundingOrder?.let { order ->
            AlertDialog(
                onDismissRequest = { refundingOrder = null },
                title = { Text("Issue Refund & Restore Inventory") },
                text = {
                    Column {
                        Text("Are you sure you want to refund ${order.sale.invoiceNumber} for $sym%.2f?".format(order.sale.grandTotal))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This will return all items back into active inventory stock.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = refundReason,
                            onValueChange = { refundReason = it },
                            label = { Text("Refund Reason") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val id = order.sale.id
                            refundingOrder = null
                            onProcessRefund(id, refundReason)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Confirm Refund")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { refundingOrder = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun OrderListRow(
    saleWithItems: SaleWithItems,
    sym: String,
    dateFormat: SimpleDateFormat,
    isRefunded: Boolean,
    onShowReceipt: (SaleWithItems) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.5f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = saleWithItems.sale.invoiceNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = if (isRefunded) MaterialTheme.colorScheme.error else Color(0xFF16A34A),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = saleWithItems.sale.status,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = dateFormat.format(Date(saleWithItems.sale.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${saleWithItems.items.size} items • ${saleWithItems.sale.customerName} • ${saleWithItems.sale.paymentMethod}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$sym%.2f".format(saleWithItems.sale.grandTotal),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isRefunded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = { onShowReceipt(saleWithItems) },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Receipt", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun OrderDetailsPane(
    order: SaleWithItems,
    sym: String,
    dateFormat: SimpleDateFormat,
    onReprint: () -> Unit,
    onRefund: () -> Unit,
    onClose: (() -> Unit)? = null
) {
    val isRefunded = order.sale.status == "REFUNDED"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = order.sale.invoiceNumber,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = dateFormat.format(Date(order.sale.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onClose != null) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text("Purchased Items", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(6.dp))

        order.items.forEach { item ->
            val qtyStr = if (item.quantity % 1.0 == 0.0) "${item.quantity.toInt()}x" else "%.2fx".format(item.quantity)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${item.productName} ($qtyStr)", fontSize = 13.sp)
                Text("$sym%.2f".format(item.lineTotal), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Payment Method:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Text(order.sale.paymentMethod, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Customer:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Text(order.sale.customerName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Subtotal:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Text("$sym%.2f".format(order.sale.subtotal), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        if (order.sale.taxAmount > 0) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tax:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Text("$sym%.2f".format(order.sale.taxAmount), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Grand Total:", fontWeight = FontWeight.Bold)
            Text(
                text = "$sym%.2f".format(order.sale.grandTotal),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = if (isRefunded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onReprint,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reprint")
            }

            if (!isRefunded) {
                Button(
                    onClick = onRefund,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refund")
                }
            }
        }
    }
}
