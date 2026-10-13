package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.data.entity.Category
import com.example.data.entity.Product
import com.example.data.entity.ReceiptConfig
import com.example.data.entity.StockAdjustment

@Composable
fun InventoryScreen(
    products: List<Product>,
    categories: List<Category>,
    lowStockProducts: List<Product>,
    stockAdjustments: List<StockAdjustment>,
    receiptConfig: ReceiptConfig?,
    onSaveProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onAdjustStock: (productId: Long, qtyChange: Double, reason: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showOnlyLowStock by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var isNewProductDialog by remember { mutableStateOf(false) }
    var adjustingProduct by remember { mutableStateOf<Product?>(null) }
    var showAuditHistory by remember { mutableStateOf(false) }

    val sym = receiptConfig?.currencySymbol ?: "$"

    val displayedProducts = remember(products, searchQuery, showOnlyLowStock) {
        val base = if (showOnlyLowStock) products.filter { it.stockQuantity <= it.minStockThreshold } else products
        if (searchQuery.isBlank()) base
        else base.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.sku.contains(searchQuery, ignoreCase = true) ||
                    it.barcode.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isNewProductDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .testTag("inventory_screen")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Inventory & Products",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${products.size} products cataloged",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(onClick = { showAuditHistory = true }) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stock History")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search & Filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search products, SKU or barcode…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Low Stock Toggle Chip
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showOnlyLowStock,
                    onClick = { showOnlyLowStock = false },
                    label = { Text("All (${products.size})") }
                )
                FilterChip(
                    selected = showOnlyLowStock,
                    onClick = { showOnlyLowStock = true },
                    label = { Text("Low Stock Alert (${lowStockProducts.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Product Adaptive Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayedProducts, key = { it.id }) { product ->
                    val isLowStock = product.stockQuantity <= product.minStockThreshold
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { editingProduct = product },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isLowStock) Color(0xFFFEF2F2) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${product.sku} • Barcode: ${product.barcode}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Cost: $sym%.2f • Price: $sym%.2f".format(product.costPrice, product.sellingPrice),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    color = if (isLowStock) MaterialTheme.colorScheme.errorContainer else Color(0xFFDCFCE7),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${product.stockQuantity.toInt()} ${product.unit}",
                                        color = if (isLowStock) MaterialTheme.colorScheme.error else Color(0xFF15803D),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 13.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedButton(
                                    onClick = { adjustingProduct = product },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Adjust Stock", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Product Dialog
    if (isNewProductDialog || editingProduct != null) {
        val prod = editingProduct
        ProductEditorDialog(
            initialProduct = prod,
            categories = categories,
            currencySymbol = sym,
            onDismiss = {
                isNewProductDialog = false
                editingProduct = null
            },
            onSave = { saved ->
                onSaveProduct(saved)
                isNewProductDialog = false
                editingProduct = null
            },
            onDelete = { toDelete ->
                onDeleteProduct(toDelete)
                editingProduct = null
            }
        )
    }

    // Stock Adjustment Dialog
    adjustingProduct?.let { product ->
        StockAdjustModal(
            product = product,
            onDismiss = { adjustingProduct = null },
            onConfirm = { qtyChange, reason ->
                onAdjustStock(product.id, qtyChange, reason)
                adjustingProduct = null
            }
        )
    }

    // Stock Adjustment History Dialog
    if (showAuditHistory) {
        Dialog(onDismissRequest = { showAuditHistory = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Stock Movement Log", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showAuditHistory = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(stockAdjustments) { adj ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(adj.productName, fontWeight = FontWeight.Bold)
                                        Text(adj.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (adj.adjustmentQty >= 0) "+${adj.adjustmentQty}" else "${adj.adjustmentQty}",
                                            fontWeight = FontWeight.Bold,
                                            color = if (adj.adjustmentQty >= 0) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
                                        )
                                        Text("Now: ${adj.newQty}", style = MaterialTheme.typography.bodySmall)
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

@Composable
fun ProductEditorDialog(
    initialProduct: Product?,
    categories: List<Category>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit,
    onDelete: (Product) -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var sku by remember { mutableStateOf(initialProduct?.sku ?: "SKU-${System.currentTimeMillis() % 10000}") }
    var barcode by remember { mutableStateOf(initialProduct?.barcode ?: "${(8900000000000L..8999999999999L).random()}") }
    var costPriceStr by remember { mutableStateOf(initialProduct?.costPrice?.toString() ?: "0.0") }
    var sellingPriceStr by remember { mutableStateOf(initialProduct?.sellingPrice?.toString() ?: "0.0") }
    var stockQuantityStr by remember { mutableStateOf(initialProduct?.stockQuantity?.toString() ?: "10.0") }
    var minThresholdStr by remember { mutableStateOf(initialProduct?.minStockThreshold?.toString() ?: "5.0") }
    var unit by remember { mutableStateOf(initialProduct?.unit ?: "pcs") }
    var categoryId by remember { mutableStateOf(initialProduct?.categoryId ?: categories.firstOrNull()?.id ?: 1L) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .clip(RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialProduct == null) "New Product" else "Edit Product",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("SKU") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Barcode") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costPriceStr,
                        onValueChange = { costPriceStr = it },
                        label = { Text("Cost Price ($currencySymbol)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = sellingPriceStr,
                        onValueChange = { sellingPriceStr = it },
                        label = { Text("Selling Price ($currencySymbol)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockQuantityStr,
                        onValueChange = { stockQuantityStr = it },
                        label = { Text("Stock Qty") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = minThresholdStr,
                        onValueChange = { minThresholdStr = it },
                        label = { Text("Min Alert Qty") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (initialProduct != null) {
                        OutlinedButton(
                            onClick = { onDelete(initialProduct) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete")
                        }
                    }

                    Button(
                        onClick = {
                            val product = Product(
                                id = initialProduct?.id ?: 0L,
                                name = name.ifBlank { "New Product" },
                                sku = sku,
                                barcode = barcode,
                                categoryId = categoryId,
                                costPrice = costPriceStr.toDoubleOrNull() ?: 0.0,
                                sellingPrice = sellingPriceStr.toDoubleOrNull() ?: 0.0,
                                stockQuantity = stockQuantityStr.toDoubleOrNull() ?: 0.0,
                                minStockThreshold = minThresholdStr.toDoubleOrNull() ?: 5.0,
                                unit = unit
                            )
                            onSave(product)
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Product")
                    }
                }
            }
        }
    }
}

@Composable
fun StockAdjustModal(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (qtyChange: Double, reason: String) -> Unit
) {
    var qtyChangeStr by remember { mutableStateOf("10") }
    var selectedReason by remember { mutableStateOf("Restock Purchase") }

    val reasons = listOf("Restock Purchase", "Damage / Broken", "Stock Correction", "Customer Return")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust Stock: ${product.name}") },
        text = {
            Column {
                Text("Current Stock: ${product.stockQuantity.toInt()} ${product.unit}")
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = qtyChangeStr,
                    onValueChange = { qtyChangeStr = it },
                    label = { Text("Quantity Change (+ or -)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Reason for adjustment", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                reasons.forEach { r ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = r }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(selected = selectedReason == r, onClick = { selectedReason = r })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(r)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val qty = qtyChangeStr.toDoubleOrNull() ?: 0.0
                onConfirm(qty, selectedReason)
            }) {
                Text("Update Stock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
