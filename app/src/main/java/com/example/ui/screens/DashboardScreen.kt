package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.dao.SaleWithItems
import com.example.data.entity.Product
import com.example.data.entity.ReceiptConfig
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    salesHistory: List<SaleWithItems>,
    products: List<Product>,
    lowStockProducts: List<Product>,
    receiptConfig: ReceiptConfig?,
    onNavigateToCheckout: () -> Unit,
    onOpenScanner: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToReceiptDesigner: () -> Unit,
    onViewReceipt: (SaleWithItems) -> Unit
) {
    val sym = receiptConfig?.currencySymbol ?: "$"
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
    val todayStr = remember { dateFormat.format(Date()) }

    val completedSales = remember(salesHistory) {
        salesHistory.filter { it.sale.status == "COMPLETED" }
    }

    val totalRevenue = remember(completedSales) {
        completedSales.sumOf { it.sale.grandTotal }
    }

    val totalSalesCount = completedSales.size

    val totalCost = remember(completedSales) {
        completedSales.sumOf { saleWithItems ->
            saleWithItems.items.sumOf { it.costPrice * it.quantity }
        }
    }

    val grossProfit = (totalRevenue - totalCost).coerceAtLeast(0.0)
    val grossMargin = if (totalRevenue > 0) (grossProfit / totalRevenue) * 100.0 else 0.0

    val recentSales = remember(salesHistory) {
        salesHistory.take(8)
    }

    val paymentBreakdown = remember(completedSales) {
        completedSales.groupBy { it.sale.paymentMethod }
            .mapValues { (_, sales) -> sales.sumOf { it.sale.grandTotal } }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().testTag("dashboard_screen")) {
        val isWide = maxWidth >= 760.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 1. Welcome & Business Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = receiptConfig?.shopName ?: "NexaPOS Terminal",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (!receiptConfig?.address.isNullOrBlank()) {
                                Text(
                                    text = receiptConfig?.address ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFF10B981),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color.White, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "OFFLINE-READY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today: $todayStr",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )

                        Text(
                            text = "${products.size} Products in Catalog",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Summary Metric Cards
            Text(
                text = "SALES & REVENUE SUMMARY",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isWide) {
                // 4 cards in one row on wide screens
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardSummaryCard(
                        title = "Total Revenue",
                        value = "$sym%.2f".format(totalRevenue),
                        subtitle = "From $totalSalesCount orders",
                        icon = Icons.Default.Payments,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    DashboardSummaryCard(
                        title = "Total Sales",
                        value = "$totalSalesCount",
                        subtitle = "Completed orders",
                        icon = Icons.Default.ShoppingCart,
                        color = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f)
                    )

                    DashboardSummaryCard(
                        title = "Gross Profit",
                        value = "$sym%.2f".format(grossProfit),
                        subtitle = "Margin: %.1f%%".format(grossMargin),
                        icon = Icons.Default.TrendingUp,
                        color = Color(0xFF059669),
                        modifier = Modifier.weight(1f)
                    )

                    DashboardSummaryCard(
                        title = "Low Stock Items",
                        value = "${lowStockProducts.size}",
                        subtitle = if (lowStockProducts.isEmpty()) "Stock healthy" else "Items need reorder",
                        icon = Icons.Default.Warning,
                        color = if (lowStockProducts.isEmpty()) Color(0xFF64748B) else Color(0xFFDC2626),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToInventory() }
                    )
                }
            } else {
                // 2x2 grid on phones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardSummaryCard(
                        title = "Total Revenue",
                        value = "$sym%.2f".format(totalRevenue),
                        subtitle = "From $totalSalesCount orders",
                        icon = Icons.Default.Payments,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    DashboardSummaryCard(
                        title = "Total Sales",
                        value = "$totalSalesCount",
                        subtitle = "Completed orders",
                        icon = Icons.Default.ShoppingCart,
                        color = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardSummaryCard(
                        title = "Gross Profit",
                        value = "$sym%.2f".format(grossProfit),
                        subtitle = "Margin: %.1f%%".format(grossMargin),
                        icon = Icons.Default.TrendingUp,
                        color = Color(0xFF059669),
                        modifier = Modifier.weight(1f)
                    )

                    DashboardSummaryCard(
                        title = "Low Stock Items",
                        value = "${lowStockProducts.size}",
                        subtitle = if (lowStockProducts.isEmpty()) "Stock healthy" else "Items need reorder",
                        icon = Icons.Default.Warning,
                        color = if (lowStockProducts.isEmpty()) Color(0xFF64748B) else Color(0xFFDC2626),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToInventory() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Split section on wide screens: Left (Quick Actions + Payment Breakdown), Right (Recent Transactions)
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        QuickActionsSection(
                            onNavigateToCheckout = onNavigateToCheckout,
                            onOpenScanner = onOpenScanner,
                            onNavigateToReceiptDesigner = onNavigateToReceiptDesigner
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        if (paymentBreakdown.isNotEmpty()) {
                            PaymentBreakdownCard(
                                paymentBreakdown = paymentBreakdown,
                                totalRevenue = totalRevenue,
                                sym = sym
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1.3f)) {
                        RecentTransactionsSection(
                            recentSales = recentSales,
                            sym = sym,
                            timeFormat = timeFormat,
                            onViewReceipt = onViewReceipt,
                            onNavigateToCheckout = onNavigateToCheckout
                        )
                    }
                }
            } else {
                QuickActionsSection(
                    onNavigateToCheckout = onNavigateToCheckout,
                    onOpenScanner = onOpenScanner,
                    onNavigateToReceiptDesigner = onNavigateToReceiptDesigner
                )

                Spacer(modifier = Modifier.height(24.dp))

                RecentTransactionsSection(
                    recentSales = recentSales,
                    sym = sym,
                    timeFormat = timeFormat,
                    onViewReceipt = onViewReceipt,
                    onNavigateToCheckout = onNavigateToCheckout
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (paymentBreakdown.isNotEmpty()) {
                    PaymentBreakdownCard(
                        paymentBreakdown = paymentBreakdown,
                        totalRevenue = totalRevenue,
                        sym = sym
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickActionsSection(
    onNavigateToCheckout: () -> Unit,
    onOpenScanner: () -> Unit,
    onNavigateToReceiptDesigner: () -> Unit
) {
    Column {
        Text(
            text = "QUICK ACTIONS",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onNavigateToCheckout,
                modifier = Modifier
                    .weight(1.3f)
                    .height(52.dp)
                    .testTag("dashboard_new_sale_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Sale", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onOpenScanner,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("dashboard_scanner_btn"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scan")
            }

            FilledTonalIconButton(
                onClick = onNavigateToReceiptDesigner,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("dashboard_designer_btn"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.DesignServices, contentDescription = "Receipt Designer")
            }
        }
    }
}

@Composable
private fun RecentTransactionsSection(
    recentSales: List<SaleWithItems>,
    sym: String,
    timeFormat: SimpleDateFormat,
    onViewReceipt: (SaleWithItems) -> Unit,
    onNavigateToCheckout: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECENT TRANSACTION LOGS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Last ${recentSales.size} Invoices",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (recentSales.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No sales transactions logged yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onNavigateToCheckout) {
                        Text("Start First Checkout")
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recentSales.forEach { saleWithItems ->
                    val isRefunded = saleWithItems.sale.status == "REFUNDED"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onViewReceipt(saleWithItems) }
                            .testTag("dashboard_tx_${saleWithItems.sale.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isRefunded) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (isRefunded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isRefunded) Icons.Default.Undo else Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = saleWithItems.sale.invoiceNumber,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = if (isRefunded) MaterialTheme.colorScheme.error else Color(0xFF16A34A),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = saleWithItems.sale.status,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${saleWithItems.items.size} item(s) • ${saleWithItems.sale.paymentMethod} • ${timeFormat.format(Date(saleWithItems.sale.timestamp))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$sym%.2f".format(saleWithItems.sale.grandTotal),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isRefunded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "View Bill",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentBreakdownCard(
    paymentBreakdown: Map<String, Double>,
    totalRevenue: Double,
    sym: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Payment Methods Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            paymentBreakdown.forEach { (method, amount) ->
                val share = if (totalRevenue > 0) (amount / totalRevenue).toFloat() else 0f
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(method, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("$sym%.2f (%.0f%%)".format(amount, share * 100), fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { share },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardSummaryCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
