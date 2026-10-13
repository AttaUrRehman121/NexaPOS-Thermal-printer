package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.dao.SaleWithItems
import com.example.data.entity.*
import com.example.data.repository.CartItem
import com.example.data.repository.PosRepository
import com.example.printer.*
import com.example.scanner.AudioHapticFeedback
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

enum class PosTab(val title: String) {
    DASHBOARD("Dashboard"),
    POS("Checkout"),
    ORDERS("Orders"),
    INVENTORY("Inventory"),
    SETTINGS("Settings")
}

data class CurrencyOption(
    val code: String,
    val symbol: String,
    val name: String
)

val SUPPORTED_CURRENCIES = listOf(
    CurrencyOption("USD", "$", "US Dollar ($)"),
    CurrencyOption("EUR", "€", "Euro (€)"),
    CurrencyOption("GBP", "£", "British Pound (£)"),
    CurrencyOption("PKR", "Rs", "Pakistani Rupee (Rs)"),
    CurrencyOption("INR", "₹", "Indian Rupee (₹)"),
    CurrencyOption("AED", "AED", "UAE Dirham (AED)"),
    CurrencyOption("CAD", "C$", "Canadian Dollar (C$)"),
    CurrencyOption("AUD", "A$", "Australian Dollar (A$)")
)

class PosViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    val repository = PosRepository(database)
    val printerManager = ThermalPrinterManager(application)
    private val feedback = AudioHapticFeedback(application)

    // Current Tab
    private val _currentTab = MutableStateFlow(PosTab.DASHBOARD)
    val currentTab: StateFlow<PosTab> = _currentTab.asStateFlow()

    // Products & Categories
    val products = repository.allActiveProducts.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allProducts = repository.allProducts.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val categories = repository.allCategories.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val customers = repository.allCustomers.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val salesHistory = repository.allSales.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val lowStockProducts = repository.lowStockProducts.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val stockAdjustments = repository.recentAdjustments.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val receiptConfig = repository.receiptConfig.stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Filter & Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    // Cart State
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer: StateFlow<Customer?> = _selectedCustomer.asStateFlow()

    private val _orderDiscount = MutableStateFlow(0.0)
    val orderDiscount: StateFlow<Double> = _orderDiscount.asStateFlow()

    private val _taxRatePercent = MutableStateFlow(0.0) // 0%, 5%, 10%, etc.
    val taxRatePercent: StateFlow<Double> = _taxRatePercent.asStateFlow()

    // Active Completed Sale for Receipt Dialog
    private val _activeReceiptSale = MutableStateFlow<SaleWithItems?>(null)
    val activeReceiptSale: StateFlow<SaleWithItems?> = _activeReceiptSale.asStateFlow()

    // Barcode Scanner Sheet
    private val _isScannerOpen = MutableStateFlow(false)
    val isScannerOpen: StateFlow<Boolean> = _isScannerOpen.asStateFlow()

    // Payment Sheet
    private val _isPaymentSheetOpen = MutableStateFlow(false)
    val isPaymentSheetOpen: StateFlow<Boolean> = _isPaymentSheetOpen.asStateFlow()

    // Notification Banner / Snackbar
    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    // Available Printers & Hardware Scanning
    val availablePrinters: StateFlow<List<DiscoveredPrinter>> = printerManager.printersList
    val isScanningPrinters: StateFlow<Boolean> = printerManager.isScanning
    val virtualPrintJobPreview: StateFlow<String?> = printerManager.lastVirtualPrintJob

    private val _selectedPrinter = MutableStateFlow<DiscoveredPrinter?>(null)
    val selectedPrinter: StateFlow<DiscoveredPrinter?> = _selectedPrinter.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureInitialDataSeeded()
            refreshPrinters()

            // Observe available printers to auto-select
            printerManager.printersList.collect { list ->
                val savedAddress = receiptConfig.value?.preferredPrinterAddress
                if (_selectedPrinter.value == null && list.isNotEmpty()) {
                    val match = if (!savedAddress.isNullOrBlank()) {
                        list.find { it.address == savedAddress } ?: list.first()
                    } else {
                        list.first()
                    }
                    _selectedPrinter.value = match
                }
            }
        }
    }

    fun setTab(tab: PosTab) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun setCustomer(customer: Customer?) {
        _selectedCustomer.value = customer
    }

    fun setTaxRate(percent: Double) {
        _taxRatePercent.value = percent
    }

    fun setOrderDiscount(amount: Double) {
        _orderDiscount.value = amount
    }

    fun addToCart(product: Product, quantity: Double = 1.0) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }

        if (index >= 0) {
            val existing = current[index]
            val newQty = existing.quantity + quantity
            if (newQty <= product.stockQuantity || product.stockQuantity <= 0) {
                current[index] = existing.copy(quantity = newQty)
                feedback.playSuccessBeep()
            } else {
                current[index] = existing.copy(quantity = product.stockQuantity)
                showBanner("Maximum available stock reached (${product.stockQuantity})")
                feedback.playErrorBeep()
            }
        } else {
            if (product.stockQuantity >= quantity || product.stockQuantity <= 0) {
                current.add(CartItem(product = product, quantity = quantity))
                feedback.playSuccessBeep()
            } else {
                showBanner("Item is out of stock")
                feedback.playErrorBeep()
            }
        }
        _cartItems.value = current
    }

    fun updateCartItemQuantity(productId: Long, newQuantity: Double) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            if (newQuantity <= 0) {
                current.removeAt(index)
            } else {
                current[index] = current[index].copy(quantity = newQuantity)
            }
            _cartItems.value = current
        }
    }

    fun updateCartItemDiscount(productId: Long, discountPercent: Double) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            current[index] = current[index].copy(discountPercent = discountPercent.coerceIn(0.0, 100.0))
            _cartItems.value = current
        }
    }

    fun removeFromCart(productId: Long) {
        _cartItems.value = _cartItems.value.filterNot { it.product.id == productId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _orderDiscount.value = 0.0
        _selectedCustomer.value = null
    }

    fun scanBarcode(barcode: String) {
        viewModelScope.launch {
            val product = repository.getProductByBarcode(barcode)
            if (product != null) {
                addToCart(product, 1.0)
                showBanner("Added: ${product.name}")
            } else {
                feedback.playErrorBeep()
                showBanner("Barcode not found: $barcode")
            }
        }
    }

    fun openScanner(isOpen: Boolean) {
        _isScannerOpen.value = isOpen
    }

    fun openPaymentSheet(isOpen: Boolean) {
        _isPaymentSheetOpen.value = isOpen
    }

    fun completeCheckout(
        paymentMethod: String,
        amountPaid: Double,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val result = repository.completeSale(
                cartItems = _cartItems.value,
                paymentMethod = paymentMethod,
                amountPaid = amountPaid,
                discountAmount = _orderDiscount.value,
                taxPercent = _taxRatePercent.value,
                customer = _selectedCustomer.value,
                notes = notes
            )

            result.onSuccess { saleWithItems ->
                _activeReceiptSale.value = saleWithItems
                _isPaymentSheetOpen.value = false
                clearCart()
                feedback.playSuccessBeep()

                // Check auto-print setting
                val config = receiptConfig.value
                if (config?.autoPrintOnCheckout == true && _selectedPrinter.value != null) {
                    printCurrentReceipt()
                }
            }.onFailure { err ->
                feedback.playErrorBeep()
                showBanner("Checkout failed: ${err.localizedMessage}")
            }
        }
    }

    fun closeReceiptDialog() {
        _activeReceiptSale.value = null
    }

    fun showReceiptForOrder(saleWithItems: SaleWithItems) {
        _activeReceiptSale.value = saleWithItems
    }

    fun processRefund(saleId: Long, reason: String) {
        viewModelScope.launch {
            val result = repository.processRefund(saleId, reason)
            result.onSuccess {
                showBanner("Sale refunded and stock restored")
                feedback.playSuccessBeep()
                _activeReceiptSale.value = it
            }.onFailure {
                feedback.playErrorBeep()
                showBanner("Refund failed: ${it.localizedMessage}")
            }
        }
    }

    fun refreshPrinters() {
        printerManager.refreshPrinters()
        val list = printerManager.printersList.value
        val savedAddress = receiptConfig.value?.preferredPrinterAddress
        if (_selectedPrinter.value == null && list.isNotEmpty()) {
            _selectedPrinter.value = if (!savedAddress.isNullOrBlank()) {
                list.find { it.address == savedAddress } ?: list.first()
            } else {
                list.first()
            }
        }
    }

    fun startBluetoothScan() {
        printerManager.startDiscovery()
    }

    fun stopBluetoothScan() {
        printerManager.stopDiscovery()
    }

    fun pairPrinter(printer: DiscoveredPrinter) {
        viewModelScope.launch {
            val success = printerManager.pairDevice(printer)
            if (success) {
                showBanner("Pairing requested for ${printer.name}")
            }
        }
    }

    fun addManualPrinter(name: String, address: String, type: String) {
        printerManager.addManualPrinter(name, address, type)
        val added = printerManager.printersList.value.find { it.address == address.trim() }
        if (added != null) {
            selectPrinter(added)
        }
        showBanner("Added $type printer: $name")
    }

    fun removeManualPrinter(address: String) {
        printerManager.removeManualPrinter(address)
        if (_selectedPrinter.value?.address == address) {
            _selectedPrinter.value = printerManager.printersList.value.firstOrNull()
        }
        showBanner("Printer removed")
    }

    fun selectPrinter(printer: DiscoveredPrinter) {
        _selectedPrinter.value = printer
        // Persist preferred printer in config
        viewModelScope.launch {
            val current = receiptConfig.value ?: ReceiptConfig()
            repository.saveReceiptConfig(
                current.copy(
                    preferredPrinterAddress = printer.address,
                    preferredPrinterName = printer.name,
                    preferredPrinterType = printer.type
                )
            )
        }
    }

    fun printCurrentReceipt() {
        val sale = _activeReceiptSale.value ?: return
        val printer = _selectedPrinter.value
        val config = receiptConfig.value ?: ReceiptConfig()

        if (printer == null) {
            showBanner("No thermal printer selected! Configure in Settings.")
            return
        }

        viewModelScope.launch {
            val bytes = EscPosBuilder.formatReceipt(sale, config)
            val success = printerManager.printBytes(printer, bytes)
            if (success) {
                showBanner("Receipt printed on ${printer.name}")
            } else {
                showBanner(printerManager.statusMessage.value)
            }
        }
    }

    fun printTestReceipt() {
        val printer = _selectedPrinter.value
        val config = receiptConfig.value ?: ReceiptConfig()

        if (printer == null) {
            showBanner("Please select a printer first")
            return
        }

        viewModelScope.launch {
            val bytes = EscPosBuilder.generateTestReceipt(config)
            val success = printerManager.printBytes(printer, bytes)
            if (success) {
                showBanner("Test receipt printed successfully!")
            } else {
                showBanner(printerManager.statusMessage.value)
            }
        }
    }

    fun exportCurrentReceiptPdf(): File? {
        val sale = _activeReceiptSale.value ?: return null
        val config = receiptConfig.value ?: ReceiptConfig()
        return ReceiptPdfGenerator.generateReceiptPdf(getApplication(), sale, config)
    }

    fun shareCurrentReceiptPdf() {
        val file = exportCurrentReceiptPdf()
        if (file != null) {
            ReceiptPdfGenerator.sharePdf(getApplication(), file)
        } else {
            showBanner("Could not generate PDF receipt")
        }
    }

    fun updateReceiptConfig(newConfig: ReceiptConfig) {
        viewModelScope.launch {
            repository.saveReceiptConfig(newConfig)
            showBanner("Receipt settings saved")
        }
    }

    fun setCurrency(currency: CurrencyOption) {
        viewModelScope.launch {
            val current = receiptConfig.value ?: ReceiptConfig()
            repository.saveReceiptConfig(
                current.copy(
                    currencyCode = currency.code,
                    currencySymbol = currency.symbol
                )
            )
            showBanner("Currency changed to ${currency.name}")
        }
    }

    fun saveProduct(product: Product) {
        viewModelScope.launch {
            repository.saveProduct(product)
            showBanner("Product saved: ${product.name}")
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            showBanner("Product deleted")
        }
    }

    fun adjustStock(productId: Long, qtyChange: Double, reason: String) {
        viewModelScope.launch {
            repository.adjustProductStock(productId, qtyChange, reason)
            showBanner("Stock updated")
        }
    }

    fun saveCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.saveCustomer(customer)
            showBanner("Customer saved: ${customer.name}")
        }
    }

    fun showBanner(msg: String) {
        _bannerMessage.value = msg
    }

    fun dismissBanner() {
        _bannerMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        feedback.release()
        printerManager.cleanup()
    }
}
