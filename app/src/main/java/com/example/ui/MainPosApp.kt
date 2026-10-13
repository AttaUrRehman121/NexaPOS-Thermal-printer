package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.printer.DiscoveredPrinter
import com.example.printer.PrinterConnectionState
import com.example.ui.screens.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPosApp(viewModel: PosViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val salesHistory by viewModel.salesHistory.collectAsStateWithLifecycle()
    val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val stockAdjustments by viewModel.stockAdjustments.collectAsStateWithLifecycle()
    val receiptConfig by viewModel.receiptConfig.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val selectedCustomer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    val orderDiscount by viewModel.orderDiscount.collectAsStateWithLifecycle()
    val taxRatePercent by viewModel.taxRatePercent.collectAsStateWithLifecycle()

    val activeReceiptSale by viewModel.activeReceiptSale.collectAsStateWithLifecycle()
    val isScannerOpen by viewModel.isScannerOpen.collectAsStateWithLifecycle()
    val isPaymentSheetOpen by viewModel.isPaymentSheetOpen.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()

    val availablePrinters by viewModel.availablePrinters.collectAsStateWithLifecycle()
    val selectedPrinter by viewModel.selectedPrinter.collectAsStateWithLifecycle()
    val printerState by viewModel.printerManager.connectionState.collectAsStateWithLifecycle()
    val printerStatusMessage by viewModel.printerManager.statusMessage.collectAsStateWithLifecycle()
    val isScanningPrinters by viewModel.isScanningPrinters.collectAsStateWithLifecycle()
    val virtualPrintJobPreview by viewModel.virtualPrintJobPreview.collectAsStateWithLifecycle()

    var isViewingReceiptDesigner by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Display banner message as snackbar
    LaunchedEffect(bannerMessage) {
        bannerMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissBanner()
        }
    }

    // Hardware Back Handler
    BackHandler(enabled = isViewingReceiptDesigner || currentTab != PosTab.DASHBOARD) {
        if (isViewingReceiptDesigner) {
            isViewingReceiptDesigner = false
        } else {
            viewModel.setTab(PosTab.DASHBOARD)
        }
    }

    val sym = receiptConfig?.currencySymbol ?: "$"

    val tabs = listOf(
        PosTab.DASHBOARD to Icons.Default.Dashboard,
        PosTab.POS to Icons.Default.ShoppingCart,
        PosTab.ORDERS to Icons.Default.ReceiptLong,
        PosTab.INVENTORY to Icons.Default.Inventory2,
        PosTab.SETTINGS to Icons.Default.Settings
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTabletLandscape = maxWidth >= 720.dp

        if (isTabletLandscape) {
            // Tablet & Landscape Adaptive Layout: NavigationRail on Left + Screen on Right
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PointOfSale,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "NexaPOS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    },
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    tabs.forEach { (tab, icon) ->
                        val isSelected = currentTab == tab && !isViewingReceiptDesigner
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = {
                                isViewingReceiptDesigner = false
                                viewModel.setTab(tab)
                            },
                            icon = {
                                if (tab == PosTab.POS && cartItems.isNotEmpty()) {
                                    BadgedBox(
                                        badge = {
                                            Badge {
                                                Text("${cartItems.sumOf { it.quantity }.toInt()}")
                                            }
                                        }
                                    ) {
                                        Icon(icon, contentDescription = tab.title)
                                    }
                                } else if (tab == PosTab.INVENTORY && lowStockProducts.isNotEmpty()) {
                                    BadgedBox(
                                        badge = {
                                            Badge(containerColor = MaterialTheme.colorScheme.error) {
                                                Text("${lowStockProducts.size}")
                                            }
                                        }
                                    ) {
                                        Icon(icon, contentDescription = tab.title)
                                    }
                                } else {
                                    Icon(icon, contentDescription = tab.title)
                                }
                            },
                            label = { Text(tab.title) },
                            modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}")
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    // Printer Status Indicator on Rail
                    Surface(
                        color = when (printerState) {
                            PrinterConnectionState.CONNECTED, PrinterConnectionState.SUCCESS -> Color(0xFFDCFCE7)
                            PrinterConnectionState.CONNECTING, PrinterConnectionState.PRINTING -> Color(0xFFE0F2FE)
                            PrinterConnectionState.ERROR -> MaterialTheme.colorScheme.errorContainer
                            PrinterConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { viewModel.setTab(PosTab.SETTINGS) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "Printer status",
                                tint = when (printerState) {
                                    PrinterConnectionState.CONNECTED, PrinterConnectionState.SUCCESS -> Color(0xFF15803D)
                                    PrinterConnectionState.CONNECTING, PrinterConnectionState.PRINTING -> Color(0xFF0369A1)
                                    PrinterConnectionState.ERROR -> MaterialTheme.colorScheme.error
                                    PrinterConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                VerticalDivider()

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    modifier = Modifier.weight(1f)
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        MainContentArea(
                            currentTab = currentTab,
                            isViewingReceiptDesigner = isViewingReceiptDesigner,
                            receiptConfig = receiptConfig,
                            salesHistory = salesHistory,
                            allProducts = allProducts,
                            lowStockProducts = lowStockProducts,
                            products = products,
                            categories = categories,
                            customers = customers,
                            cartItems = cartItems,
                            selectedCustomer = selectedCustomer,
                            searchQuery = searchQuery,
                            selectedCategoryId = selectedCategoryId,
                            orderDiscount = orderDiscount,
                            taxRatePercent = taxRatePercent,
                            stockAdjustments = stockAdjustments,
                            availablePrinters = availablePrinters,
                            selectedPrinter = selectedPrinter,
                            printerState = printerState,
                            printerStatusMessage = printerStatusMessage,
                            isScanningPrinters = isScanningPrinters,
                            virtualPrintJobPreview = virtualPrintJobPreview,
                            viewModel = viewModel,
                            onOpenReceiptDesigner = { isViewingReceiptDesigner = true },
                            onCloseReceiptDesigner = { isViewingReceiptDesigner = false }
                        )
                    }
                }
            }
        } else {
            // Compact Mobile Layout: TopAppBar + Screen + Bottom Navigation
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { viewModel.setTab(PosTab.DASHBOARD) }
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PointOfSale,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "NexaPOS",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = receiptConfig?.shopName ?: "Mobile POS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        actions = {
                            // Currency Indicator Chip
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setTab(PosTab.SETTINGS) }
                            ) {
                                Text(
                                    text = "${receiptConfig?.currencyCode ?: "USD"} ($sym)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // Thermal Printer Connection Badge
                            Surface(
                                color = when (printerState) {
                                    PrinterConnectionState.CONNECTED, PrinterConnectionState.SUCCESS -> Color(0xFFDCFCE7)
                                    PrinterConnectionState.CONNECTING, PrinterConnectionState.PRINTING -> Color(0xFFE0F2FE)
                                    PrinterConnectionState.ERROR -> MaterialTheme.colorScheme.errorContainer
                                    PrinterConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setTab(PosTab.SETTINGS) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Print,
                                        contentDescription = null,
                                        tint = when (printerState) {
                                            PrinterConnectionState.CONNECTED, PrinterConnectionState.SUCCESS -> Color(0xFF15803D)
                                            PrinterConnectionState.CONNECTING, PrinterConnectionState.PRINTING -> Color(0xFF0369A1)
                                            PrinterConnectionState.ERROR -> MaterialTheme.colorScheme.error
                                            PrinterConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (selectedPrinter != null) "${receiptConfig?.paperWidthMm ?: 58}mm" else "No Printer",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (printerState) {
                                            PrinterConnectionState.CONNECTED, PrinterConnectionState.SUCCESS -> Color(0xFF15803D)
                                            PrinterConnectionState.CONNECTING, PrinterConnectionState.PRINTING -> Color(0xFF0369A1)
                                            PrinterConnectionState.ERROR -> MaterialTheme.colorScheme.error
                                            PrinterConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    if (!isViewingReceiptDesigner) {
                        NavigationBar(
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            tabs.forEach { (tab, icon) ->
                                val isSelected = currentTab == tab
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        isViewingReceiptDesigner = false
                                        viewModel.setTab(tab)
                                    },
                                    icon = {
                                        if (tab == PosTab.POS && cartItems.isNotEmpty()) {
                                            BadgedBox(
                                                badge = {
                                                    Badge {
                                                        Text("${cartItems.sumOf { it.quantity }.toInt()}")
                                                    }
                                                }
                                            ) {
                                                Icon(icon, contentDescription = tab.title)
                                            }
                                        } else if (tab == PosTab.INVENTORY && lowStockProducts.isNotEmpty()) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                                        Text("${lowStockProducts.size}")
                                                    }
                                                }
                                            ) {
                                                Icon(icon, contentDescription = tab.title)
                                            }
                                        } else {
                                            Icon(icon, contentDescription = tab.title)
                                        }
                                    },
                                    label = { Text(tab.title) },
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    MainContentArea(
                        currentTab = currentTab,
                        isViewingReceiptDesigner = isViewingReceiptDesigner,
                        receiptConfig = receiptConfig,
                        salesHistory = salesHistory,
                        allProducts = allProducts,
                        lowStockProducts = lowStockProducts,
                        products = products,
                        categories = categories,
                        customers = customers,
                        cartItems = cartItems,
                        selectedCustomer = selectedCustomer,
                        searchQuery = searchQuery,
                        selectedCategoryId = selectedCategoryId,
                        orderDiscount = orderDiscount,
                        taxRatePercent = taxRatePercent,
                        stockAdjustments = stockAdjustments,
                        availablePrinters = availablePrinters,
                        selectedPrinter = selectedPrinter,
                        printerState = printerState,
                        printerStatusMessage = printerStatusMessage,
                        isScanningPrinters = isScanningPrinters,
                        virtualPrintJobPreview = virtualPrintJobPreview,
                        viewModel = viewModel,
                        onOpenReceiptDesigner = { isViewingReceiptDesigner = true },
                        onCloseReceiptDesigner = { isViewingReceiptDesigner = false }
                    )
                }
            }
        }

        // Modals & Overlays
        if (isPaymentSheetOpen) {
            val subtotal = cartItems.sumOf { it.lineTotal }
            val tax = (subtotal - orderDiscount).coerceAtLeast(0.0) * (taxRatePercent / 100.0)
            val total = (subtotal - orderDiscount + tax).coerceAtLeast(0.0)

            PaymentDialog(
                grandTotal = total,
                receiptConfig = receiptConfig,
                onDismiss = { viewModel.openPaymentSheet(false) },
                onComplete = { method, amount, notes ->
                    viewModel.completeCheckout(method, amount, notes)
                }
            )
        }

        activeReceiptSale?.let { saleWithItems ->
            ReceiptModal(
                saleWithItems = saleWithItems,
                config = receiptConfig ?: com.example.data.entity.ReceiptConfig(),
                activePrinter = selectedPrinter,
                onPrintThermal = { viewModel.printCurrentReceipt() },
                onSharePdf = { viewModel.shareCurrentReceiptPdf() },
                onDismiss = { viewModel.closeReceiptDialog() }
            )
        }

        if (isScannerOpen) {
            BarcodeScannerDialog(
                products = products,
                onBarcodeScanned = { barcode ->
                    viewModel.scanBarcode(barcode)
                },
                onDismiss = { viewModel.openScanner(false) }
            )
        }
    }
}

@Composable
private fun MainContentArea(
    currentTab: PosTab,
    isViewingReceiptDesigner: Boolean,
    receiptConfig: com.example.data.entity.ReceiptConfig?,
    salesHistory: List<com.example.data.dao.SaleWithItems>,
    allProducts: List<com.example.data.entity.Product>,
    lowStockProducts: List<com.example.data.entity.Product>,
    products: List<com.example.data.entity.Product>,
    categories: List<com.example.data.entity.Category>,
    customers: List<com.example.data.entity.Customer>,
    cartItems: List<com.example.data.repository.CartItem>,
    selectedCustomer: com.example.data.entity.Customer?,
    searchQuery: String,
    selectedCategoryId: Long?,
    orderDiscount: Double,
    taxRatePercent: Double,
    stockAdjustments: List<com.example.data.entity.StockAdjustment>,
    availablePrinters: List<DiscoveredPrinter>,
    selectedPrinter: DiscoveredPrinter?,
    printerState: PrinterConnectionState,
    printerStatusMessage: String,
    isScanningPrinters: Boolean,
    virtualPrintJobPreview: String?,
    viewModel: PosViewModel,
    onOpenReceiptDesigner: () -> Unit,
    onCloseReceiptDesigner: () -> Unit
) {
    if (isViewingReceiptDesigner) {
        val config = receiptConfig ?: com.example.data.entity.ReceiptConfig()
        ReceiptDesignerScreen(
            currentConfig = config,
            salesHistory = salesHistory,
            onSaveConfig = {
                viewModel.updateReceiptConfig(it)
                onCloseReceiptDesigner()
            },
            onTestPrint = { viewModel.printTestReceipt() }
        )
    } else {
        when (currentTab) {
            PosTab.DASHBOARD -> {
                DashboardScreen(
                    salesHistory = salesHistory,
                    products = allProducts,
                    lowStockProducts = lowStockProducts,
                    receiptConfig = receiptConfig,
                    onNavigateToCheckout = { viewModel.setTab(PosTab.POS) },
                    onOpenScanner = { viewModel.openScanner(true) },
                    onNavigateToInventory = { viewModel.setTab(PosTab.INVENTORY) },
                    onNavigateToReceiptDesigner = onOpenReceiptDesigner,
                    onViewReceipt = { viewModel.showReceiptForOrder(it) }
                )
            }
            PosTab.POS -> {
                PosCheckoutScreen(
                    products = products,
                    categories = categories,
                    customers = customers,
                    cartItems = cartItems,
                    selectedCustomer = selectedCustomer,
                    searchQuery = searchQuery,
                    selectedCategoryId = selectedCategoryId,
                    orderDiscount = orderDiscount,
                    taxRatePercent = taxRatePercent,
                    receiptConfig = receiptConfig,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onCategorySelect = { viewModel.setSelectedCategory(it) },
                    onProductClick = { viewModel.addToCart(it, 1.0) },
                    onOpenScanner = { viewModel.openScanner(true) },
                    onUpdateQuantity = { id, q -> viewModel.updateCartItemQuantity(id, q) },
                    onUpdateDiscount = { id, d -> viewModel.updateCartItemDiscount(id, d) },
                    onRemoveCartItem = { id -> viewModel.removeFromCart(id) },
                    onClearCart = { viewModel.clearCart() },
                    onCustomerSelect = { viewModel.setCustomer(it) },
                    onTaxRateSelect = { viewModel.setTaxRate(it) },
                    onOrderDiscountChange = { viewModel.setOrderDiscount(it) },
                    onProceedToPayment = { viewModel.openPaymentSheet(true) }
                )
            }
            PosTab.ORDERS -> {
                OrdersScreen(
                    salesHistory = salesHistory,
                    receiptConfig = receiptConfig,
                    onShowReceipt = { viewModel.showReceiptForOrder(it) },
                    onProcessRefund = { id, r -> viewModel.processRefund(id, r) }
                )
            }
            PosTab.INVENTORY -> {
                InventoryScreen(
                    products = allProducts,
                    categories = categories,
                    lowStockProducts = lowStockProducts,
                    stockAdjustments = stockAdjustments,
                    receiptConfig = receiptConfig,
                    onSaveProduct = { viewModel.saveProduct(it) },
                    onDeleteProduct = { viewModel.deleteProduct(it) },
                    onAdjustStock = { id, q, r -> viewModel.adjustStock(id, q, r) }
                )
            }
            PosTab.SETTINGS -> {
                SettingsScreen(
                    receiptConfig = receiptConfig,
                    availablePrinters = availablePrinters,
                    selectedPrinter = selectedPrinter,
                    printerState = printerState,
                    printerStatusMessage = printerStatusMessage,
                    isScanningPrinters = isScanningPrinters,
                    virtualPrintJobPreview = virtualPrintJobPreview,
                    onRefreshPrinters = { viewModel.refreshPrinters() },
                    onStartBluetoothScan = { viewModel.startBluetoothScan() },
                    onStopBluetoothScan = { viewModel.stopBluetoothScan() },
                    onPairPrinter = { viewModel.pairPrinter(it) },
                    onSelectPrinter = { viewModel.selectPrinter(it) },
                    onAddManualPrinter = { name, addr, type -> viewModel.addManualPrinter(name, addr, type) },
                    onRemoveManualPrinter = { viewModel.removeManualPrinter(it) },
                    onTestPrint = { viewModel.printTestReceipt() },
                    onSelectCurrency = { viewModel.setCurrency(it) },
                    onUpdateReceiptConfig = { viewModel.updateReceiptConfig(it) },
                    onOpenReceiptDesigner = onOpenReceiptDesigner
                )
            }
        }
    }
}
