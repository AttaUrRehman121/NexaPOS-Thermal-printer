package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.dao.SaleWithItems
import com.example.data.entity.Product
import com.example.data.entity.ReceiptConfig

@Composable
fun ReportsScreen(
    salesHistory: List<SaleWithItems>,
    products: List<Product>,
    lowStockProducts: List<Product>,
    receiptConfig: ReceiptConfig?
) {
    val sym = receiptConfig?.currencySymbol ?: "$"

    // Financial KPIs calculation
    val completedSales = remember(salesHistory) {
        salesHistory.filter { it.sale.status == "COMPLETED" }
    }

    val totalRevenue = remember(completedSales) {
        completedSales.sumOf { it.sale.grandTotal }
    }

    val totalTransactions = completedSales.size

    val totalCost = remember(completedSales) {
        completedSales.sumOf { saleWithItems ->
            saleWithItems.items.sumOf { it.costPrice * it.quantity }
        }
    }

    val totalGrossProfit = (totalRevenue - totalCost).coerceAtLeast(0.0)
    val marginPercent = if (totalRevenue > 0) (totalGrossProfit / totalRevenue) * 100.0 else 0.0
    val averageTicket = if (totalTransactions > 0) totalRevenue / totalTransactions else 0.0

    // Payment method breakdown
    val paymentBreakdown = remember(completedSales) {
        completedSales.groupBy { it.sale.paymentMethod }
            .mapValues { (_, sales) -> sales.sumOf { it.sale.grandTotal } }
    }

    // Top selling items
    val topItems = remember(completedSales) {
        completedSales.flatMap { it.items }
            .groupBy { it.productName }
            .mapValues { (_, items) -> items.sumOf { it.quantity } to items.sumOf { it.lineTotal } }
            .toList()
            .sortedByDescending { it.second.first }
            .take(5)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("reports_screen")
    ) {
        Text(
            text = "Business Intelligence & Analytics",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Real-time revenue, margins, and sales velocity",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Key KPI Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiMetricCard(
                title = "Total Revenue",
                value = "$sym%.2f".format(totalRevenue),
                icon = Icons.Default.Payments,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                title = "Gross Profit",
                value = "$sym%.2f".format(totalGrossProfit),
                icon = Icons.Default.TrendingUp,
                color = Color(0xFF16A34A),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiMetricCard(
                title = "Transactions",
                value = "$totalTransactions",
                icon = Icons.Default.ReceiptLong,
                color = Color(0xFF0284C7),
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                title = "Gross Margin",
                value = "%.1f%%".format(marginPercent),
                icon = Icons.Default.PieChart,
                color = Color(0xFFD97706),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Payment Method Breakdown Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Payment Method Volume",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (paymentBreakdown.isEmpty()) {
                    Text("No payment transactions recorded yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                } else {
                    paymentBreakdown.forEach { (method, amount) ->
                        val share = if (totalRevenue > 0) (amount / totalRevenue).toFloat() else 0f
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(method, fontWeight = FontWeight.SemiBold)
                                Text("$sym%.2f (%.0f%%)".format(amount, share * 100))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { share },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Best Selling Products
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Top-Selling Products",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (topItems.isEmpty()) {
                    Text("Complete checkouts to see top-selling velocity", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                } else {
                    topItems.forEachIndexed { index, (itemName, stats) ->
                        val (qty, rev) = stats
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("#${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(itemName, fontWeight = FontWeight.Bold)
                                    Text("${qty.toInt()} units sold", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text("$sym%.2f".format(rev), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Low Stock Attention Alert
        if (lowStockProducts.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Low Stock Reorder Alert", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    lowStockProducts.take(4).forEach { prod ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(prod.name, fontSize = 13.sp)
                            Text(
                                "${prod.stockQuantity.toInt()} ${prod.unit} left",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun KpiMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}
