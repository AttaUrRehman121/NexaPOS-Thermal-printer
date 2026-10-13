package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Category
import com.example.data.entity.Customer
import com.example.data.entity.Product
import com.example.data.entity.ReceiptConfig
import com.example.data.repository.CartItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosCheckoutScreen(
    products: List<Product>,
    categories: List<Category>,
    customers: List<Customer>,
    cartItems: List<CartItem>,
    selectedCustomer: Customer?,
    searchQuery: String,
    selectedCategoryId: Long?,
    orderDiscount: Double,
    taxRatePercent: Double,
    receiptConfig: ReceiptConfig?,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (Long?) -> Unit,
    onProductClick: (Product) -> Unit,
    onOpenScanner: () -> Unit,
    onUpdateQuantity: (productId: Long, qty: Double) -> Unit,
    onUpdateDiscount: (productId: Long, discountPercent: Double) -> Unit,
    onRemoveCartItem: (productId: Long) -> Unit,
    onClearCart: () -> Unit,
    onCustomerSelect: (Customer?) -> Unit,
    onTaxRateSelect: (Double) -> Unit,
    onOrderDiscountChange: (Double) -> Unit,
    onProceedToPayment: () -> Unit
) {
    var isCartSheetOpen by remember { mutableStateOf(false) }
    val sym = receiptConfig?.currencySymbol ?: "$"

    // Filter products
    val filteredProducts = remember(products, searchQuery, selectedCategoryId) {
        products.filter { p ->
            val matchSearch = searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.sku.contains(searchQuery, ignoreCase = true) ||
                    p.barcode.contains(searchQuery, ignoreCase = true)
            val matchCategory = selectedCategoryId == null || p.categoryId == selectedCategoryId
            matchSearch && matchCategory
        }
    }

    val cartTotalCount = remember(cartItems) { cartItems.sumOf { it.quantity } }
    val cartSubtotal = remember(cartItems) { cartItems.sumOf { it.lineTotal } }
    val cartTaxAmount = remember(cartSubtotal, orderDiscount, taxRatePercent) {
        (cartSubtotal - orderDiscount).coerceAtLeast(0.0) * (taxRatePercent / 100.0)
    }
    val cartGrandTotal = remember(cartSubtotal, orderDiscount, cartTaxAmount) {
        (cartSubtotal - orderDiscount + cartTaxAmount).coerceAtLeast(0.0)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().testTag("pos_checkout_screen")) {
        val isWide = maxWidth >= 720.dp

        if (isWide) {
            // TABLET / LANDSCAPE ADAPTIVE SPLIT SCREEN
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Column: Product Search, Category Chips, and Product Grid
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    // Search & Scan Bar
                    ProductSearchBar(
                        searchQuery = searchQuery,
                        onSearchChange = onSearchChange,
                        onOpenScanner = onOpenScanner
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Categories Row
                    CategoryFilterRow(
                        categories = categories,
                        products = products,
                        selectedCategoryId = selectedCategoryId,
                        onCategorySelect = onCategorySelect
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Products Grid
                    ProductsGridView(
                        filteredProducts = filteredProducts,
                        cartItems = cartItems,
                        sym = sym,
                        searchQuery = searchQuery,
                        onProductClick = onProductClick,
                        modifier = Modifier.weight(1f)
                    )
                }

                VerticalDivider()

                // Right Column: Permanent Live Register Cart & Fast Checkout Panel
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier
                        .widthIn(min = 340.dp, max = 430.dp)
                        .fillMaxHeight()
                ) {
                    CartPaneContent(
                        cartItems = cartItems,
                        customers = customers,
                        selectedCustomer = selectedCustomer,
                        orderDiscount = orderDiscount,
                        taxRatePercent = taxRatePercent,
                        currencySymbol = sym,
                        subtotal = cartSubtotal,
                        taxAmount = cartTaxAmount,
                        grandTotal = cartGrandTotal,
                        onUpdateQuantity = onUpdateQuantity,
                        onUpdateDiscount = onUpdateDiscount,
                        onRemoveItem = onRemoveCartItem,
                        onClearCart = onClearCart,
                        onCustomerSelect = onCustomerSelect,
                        onTaxRateSelect = onTaxRateSelect,
                        onOrderDiscountChange = onOrderDiscountChange,
                        onCharge = onProceedToPayment,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            // COMPACT MOBILE PHONE LAYOUT
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = if (cartItems.isNotEmpty()) 84.dp else 16.dp)
                ) {
                    ProductSearchBar(
                        searchQuery = searchQuery,
                        onSearchChange = onSearchChange,
                        onOpenScanner = onOpenScanner,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )

                    CategoryFilterRow(
                        categories = categories,
                        products = products,
                        selectedCategoryId = selectedCategoryId,
                        onCategorySelect = onCategorySelect
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ProductsGridView(
                        filteredProducts = filteredProducts,
                        cartItems = cartItems,
                        sym = sym,
                        searchQuery = searchQuery,
                        onProductClick = onProductClick,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Sticky Bottom Cart Bar
                if (cartItems.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { isCartSheetOpen = true }
                            .testTag("bottom_cart_bar"),
                        color = MaterialTheme.colorScheme.primary,
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (cartTotalCount % 1.0 == 0.0) "${cartTotalCount.toInt()}" else "%.1f".format(cartTotalCount),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Current Order",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "$sym%.2f".format(cartGrandTotal),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "View Cart",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Mobile Modal Sheet
                if (isCartSheetOpen) {
                    ModalBottomSheet(
                        onDismissRequest = { isCartSheetOpen = false },
                        modifier = Modifier.testTag("cart_bottom_sheet")
                    ) {
                        CartPaneContent(
                            cartItems = cartItems,
                            customers = customers,
                            selectedCustomer = selectedCustomer,
                            orderDiscount = orderDiscount,
                            taxRatePercent = taxRatePercent,
                            currencySymbol = sym,
                            subtotal = cartSubtotal,
                            taxAmount = cartTaxAmount,
                            grandTotal = cartGrandTotal,
                            onUpdateQuantity = onUpdateQuantity,
                            onUpdateDiscount = onUpdateDiscount,
                            onRemoveItem = onRemoveCartItem,
                            onClearCart = {
                                onClearCart()
                                isCartSheetOpen = false
                            },
                            onCustomerSelect = onCustomerSelect,
                            onTaxRateSelect = onTaxRateSelect,
                            onOrderDiscountChange = onOrderDiscountChange,
                            onCharge = {
                                isCartSheetOpen = false
                                onProceedToPayment()
                            },
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductSearchBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onOpenScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search product name, SKU or barcode…") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier
                .weight(1f)
                .testTag("product_search_input"),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        FilledTonalIconButton(
            onClick = onOpenScanner,
            modifier = Modifier
                .size(52.dp)
                .testTag("open_scanner_btn"),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "Scan Barcode",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun CategoryFilterRow(
    categories: List<Category>,
    products: List<Product>,
    selectedCategoryId: Long?,
    onCategorySelect: (Long?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onCategorySelect(null) },
                label = { Text("All Items (${products.size})") },
                leadingIcon = {
                    if (selectedCategoryId == null) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            )
        }
        items(categories) { category ->
            val isSelected = selectedCategoryId == category.id
            val count = products.count { it.categoryId == category.id }
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelect(category.id) },
                label = { Text("${category.name} ($count)") },
                leadingIcon = {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            )
        }
    }
}

@Composable
private fun ProductsGridView(
    filteredProducts: List<Product>,
    cartItems: List<CartItem>,
    sym: String,
    searchQuery: String,
    onProductClick: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    if (filteredProducts.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (searchQuery.isNotEmpty()) "No products matching '$searchQuery'" else "No products in this category",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 145.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = modifier
        ) {
            items(filteredProducts, key = { it.id }) { product ->
                val inCartQty = cartItems.find { it.product.id == product.id }?.quantity ?: 0.0
                ProductPosCard(
                    product = product,
                    inCartQty = inCartQty,
                    currencySymbol = sym,
                    onClick = { onProductClick(product) }
                )
            }
        }
    }
}

@Composable
fun ProductPosCard(
    product: Product,
    inCartQty: Double,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val isLowStock = product.stockQuantity <= product.minStockThreshold
    val isOutOfStock = product.stockQuantity <= 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !isOutOfStock, onClick = onClick)
            .testTag("product_card_${product.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (inCartQty > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = when {
                        isOutOfStock -> MaterialTheme.colorScheme.errorContainer
                        isLowStock -> Color(0xFFFEF3C7)
                        else -> Color(0xFFDCFCE7)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = when {
                            isOutOfStock -> "Out of Stock"
                            isLowStock -> "Low: ${product.stockQuantity.toInt()}"
                            else -> "Stock: ${product.stockQuantity.toInt()}"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isOutOfStock -> MaterialTheme.colorScheme.error
                            isLowStock -> Color(0xFFB45309)
                            else -> Color(0xFF15803D)
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (inCartQty > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (inCartQty % 1.0 == 0.0) "${inCartQty.toInt()}" else "%.1f".format(inCartQty),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${product.sku} • ${product.unit}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$currencySymbol%.2f".format(product.sellingPrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add to Cart",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CartPaneContent(
    cartItems: List<CartItem>,
    customers: List<Customer>,
    selectedCustomer: Customer?,
    orderDiscount: Double,
    taxRatePercent: Double,
    currencySymbol: String,
    subtotal: Double,
    taxAmount: Double,
    grandTotal: Double,
    onUpdateQuantity: (Long, Double) -> Unit,
    onUpdateDiscount: (Long, Double) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onClearCart: () -> Unit,
    onCustomerSelect: (Customer?) -> Unit,
    onTaxRateSelect: (Double) -> Unit,
    onOrderDiscountChange: (Double) -> Unit,
    onCharge: () -> Unit,
    modifier: Modifier = Modifier
) {
    var discountInput by remember(orderDiscount) {
        mutableStateOf(if (orderDiscount > 0) "%.2f".format(orderDiscount) else "")
    }

    Column(modifier = modifier.fillMaxHeight()) {
        // Cart Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Register Cart",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${cartItems.size} item(s) • Total qty: ${cartItems.sumOf { it.quantity }.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (cartItems.isNotEmpty()) {
                TextButton(
                    onClick = onClearCart,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear")
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Customer Selection Chip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedCustomer?.name ?: "Walk-in Customer",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
            TextButton(
                onClick = {
                    val next = if (selectedCustomer == null) customers.getOrNull(1) else null
                    onCustomerSelect(next)
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("Switch", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AddShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Cart is empty",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap any product to add to register",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Scrollable Cart Items List
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cartItems.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text(
                                    text = item.product.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$currencySymbol%.2f each".format(item.unitPrice),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$currencySymbol%.2f".format(item.lineTotal),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp
                                )
                            }

                            // Stepper
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onUpdateQuantity(item.product.id, item.quantity - 1.0) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease", modifier = Modifier.size(20.dp))
                                }

                                Text(
                                    text = if (item.quantity % 1.0 == 0.0) "${item.quantity.toInt()}" else "%.1f".format(item.quantity),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )

                                IconButton(
                                    onClick = { onUpdateQuantity(item.product.id, item.quantity + 1.0) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase", modifier = Modifier.size(20.dp))
                                }

                                IconButton(
                                    onClick = { onRemoveItem(item.product.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Tax & Discount Controls
                Text("Tax Rate", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.0, 5.0, 10.0, 15.0).forEach { rate ->
                        FilterChip(
                            selected = taxRatePercent == rate,
                            onClick = { onTaxRateSelect(rate) },
                            label = { Text("${rate.toInt()}%", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = discountInput,
                    onValueChange = {
                        discountInput = it
                        val disc = it.toDoubleOrNull() ?: 0.0
                        onOrderDiscountChange(disc)
                    },
                    label = { Text("Order Discount ($currencySymbol)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Financial Breakdown Banner
        HorizontalDivider()
        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Subtotal", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Text("$currencySymbol%.2f".format(subtotal), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        if (orderDiscount > 0) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Discount", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Text("-$currencySymbol%.2f".format(orderDiscount), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
        if (taxAmount > 0) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tax (${taxRatePercent.toInt()}%)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Text("+$currencySymbol%.2f".format(taxAmount), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GRAND TOTAL",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$currencySymbol%.2f".format(grandTotal),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Charge Button
        Button(
            onClick = onCharge,
            enabled = cartItems.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("charge_total_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Payment, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (cartItems.isEmpty()) "Cart Empty" else "Proceed to Pay $currencySymbol%.2f".format(grandTotal),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
