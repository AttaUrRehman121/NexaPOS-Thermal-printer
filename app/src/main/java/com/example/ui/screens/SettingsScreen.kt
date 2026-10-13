package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ReceiptConfig
import com.example.printer.DiscoveredPrinter
import com.example.printer.PrinterConnectionState
import com.example.ui.CurrencyOption
import com.example.ui.SUPPORTED_CURRENCIES

@Composable
fun SettingsScreen(
    receiptConfig: ReceiptConfig?,
    availablePrinters: List<DiscoveredPrinter>,
    selectedPrinter: DiscoveredPrinter?,
    printerState: PrinterConnectionState,
    printerStatusMessage: String,
    isScanningPrinters: Boolean = false,
    virtualPrintJobPreview: String? = null,
    onRefreshPrinters: () -> Unit,
    onStartBluetoothScan: () -> Unit = {},
    onStopBluetoothScan: () -> Unit = {},
    onPairPrinter: (DiscoveredPrinter) -> Unit = {},
    onSelectPrinter: (DiscoveredPrinter) -> Unit,
    onAddManualPrinter: (name: String, address: String, type: String) -> Unit = { _, _, _ -> },
    onRemoveManualPrinter: (address: String) -> Unit = {},
    onTestPrint: () -> Unit,
    onSelectCurrency: (CurrencyOption) -> Unit,
    onUpdateReceiptConfig: (ReceiptConfig) -> Unit,
    onOpenReceiptDesigner: () -> Unit
) {
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showManualPrinterDialog by remember { mutableStateOf(false) }
    var showVirtualTapeDialog by remember { mutableStateOf(false) }

    // Store profile states initialized from receiptConfig
    var shopName by remember(receiptConfig) { mutableStateOf(receiptConfig?.shopName ?: "") }
    var phone by remember(receiptConfig) { mutableStateOf(receiptConfig?.phone ?: "") }
    var address by remember(receiptConfig) { mutableStateOf(receiptConfig?.address ?: "") }
    var tagline by remember(receiptConfig) { mutableStateOf(receiptConfig?.tagline ?: "") }
    var taxId by remember(receiptConfig) { mutableStateOf(receiptConfig?.taxId ?: "") }

    // Runtime Bluetooth Permission Launcher
    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms.values.any { it }
        if (granted) {
            onStartBluetoothScan()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 760.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("settings_screen")
        ) {
            Text(
                text = "Business & Hardware Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Manage shop profile, receipt branding, currency, and thermal printers",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isWide) {
                // Wide tablet layout: Profile on Left, Printer & Currency on Right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        StoreProfileCard(
                            shopName = shopName,
                            phone = phone,
                            address = address,
                            tagline = tagline,
                            taxId = taxId,
                            receiptConfig = receiptConfig,
                            onShopNameChange = { shopName = it },
                            onPhoneChange = { phone = it },
                            onAddressChange = { address = it },
                            onTaglineChange = { tagline = it },
                            onTaxIdChange = { taxId = it },
                            onSave = { onUpdateReceiptConfig(it) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        ReceiptDesignerNavCard(
                            receiptConfig = receiptConfig,
                            onOpen = onOpenReceiptDesigner
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        StoreCurrencyCard(
                            receiptConfig = receiptConfig,
                            onClick = { showCurrencyDialog = true }
                        )
                    }

                    Column(modifier = Modifier.weight(1.1f)) {
                        ThermalPrinterHardwareCard(
                            receiptConfig = receiptConfig,
                            availablePrinters = availablePrinters,
                            selectedPrinter = selectedPrinter,
                            printerState = printerState,
                            printerStatusMessage = printerStatusMessage,
                            isScanningPrinters = isScanningPrinters,
                            virtualPrintJobPreview = virtualPrintJobPreview,
                            onRefreshPrinters = onRefreshPrinters,
                            onStartScan = { permissionLauncher.launch(permissionsToRequest) },
                            onStopScan = onStopBluetoothScan,
                            onPairPrinter = onPairPrinter,
                            onSelectPrinter = onSelectPrinter,
                            onOpenManualDialog = { showManualPrinterDialog = true },
                            onOpenVirtualTape = { showVirtualTapeDialog = true },
                            onTestPrint = onTestPrint
                        )
                    }
                }
            } else {
                // Compact mobile layout
                StoreProfileCard(
                    shopName = shopName,
                    phone = phone,
                    address = address,
                    tagline = tagline,
                    taxId = taxId,
                    receiptConfig = receiptConfig,
                    onShopNameChange = { shopName = it },
                    onPhoneChange = { phone = it },
                    onAddressChange = { address = it },
                    onTaglineChange = { tagline = it },
                    onTaxIdChange = { taxId = it },
                    onSave = { onUpdateReceiptConfig(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                ReceiptDesignerNavCard(
                    receiptConfig = receiptConfig,
                    onOpen = onOpenReceiptDesigner
                )

                Spacer(modifier = Modifier.height(16.dp))

                StoreCurrencyCard(
                    receiptConfig = receiptConfig,
                    onClick = { showCurrencyDialog = true }
                )

                Spacer(modifier = Modifier.height(16.dp))

                ThermalPrinterHardwareCard(
                    receiptConfig = receiptConfig,
                    availablePrinters = availablePrinters,
                    selectedPrinter = selectedPrinter,
                    printerState = printerState,
                    printerStatusMessage = printerStatusMessage,
                    isScanningPrinters = isScanningPrinters,
                    virtualPrintJobPreview = virtualPrintJobPreview,
                    onRefreshPrinters = onRefreshPrinters,
                    onStartScan = { permissionLauncher.launch(permissionsToRequest) },
                    onStopScan = onStopBluetoothScan,
                    onPairPrinter = onPairPrinter,
                    onSelectPrinter = onSelectPrinter,
                    onOpenManualDialog = { showManualPrinterDialog = true },
                    onOpenVirtualTape = { showVirtualTapeDialog = true },
                    onTestPrint = onTestPrint
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Currency Picker Dialog
    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Store Currency") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SUPPORTED_CURRENCIES.forEach { curr ->
                        val isSelected = receiptConfig?.currencyCode == curr.code
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onSelectCurrency(curr)
                                    showCurrencyDialog = false
                                },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onSelectCurrency(curr)
                                        showCurrencyDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(curr.name, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) { Text("Close") }
            }
        )
    }

    // Manual Printer Dialog (IP or Bluetooth MAC)
    if (showManualPrinterDialog) {
        var printerType by remember { mutableStateOf("NETWORK") }
        var printerName by remember { mutableStateOf("Kitchen Receipt Printer") }
        var printerAddress by remember { mutableStateOf("192.168.1.100:9100") }

        AlertDialog(
            onDismissRequest = { showManualPrinterDialog = false },
            title = { Text("Add Printer Manually") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select connection type:", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = printerType == "NETWORK",
                            onClick = {
                                printerType = "NETWORK"
                                if (printerAddress.isBlank() || printerAddress.contains(":")) {
                                    printerAddress = "192.168.1.100:9100"
                                }
                            },
                            label = { Text("LAN / Wi-Fi IP") }
                        )
                        FilterChip(
                            selected = printerType == "BLUETOOTH",
                            onClick = {
                                printerType = "BLUETOOTH"
                                printerAddress = "00:11:22:33:44:55"
                            },
                            label = { Text("Bluetooth MAC") }
                        )
                    }

                    OutlinedTextField(
                        value = printerName,
                        onValueChange = { printerName = it },
                        label = { Text("Printer Friendly Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = printerAddress,
                        onValueChange = { printerAddress = it },
                        label = { Text(if (printerType == "NETWORK") "IP:Port (e.g. 192.168.1.100:9100)" else "MAC (e.g. 00:11:22:33:44:55)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (printerAddress.isNotBlank()) {
                        onAddManualPrinter(printerName, printerAddress, printerType)
                        showManualPrinterDialog = false
                    }
                }) {
                    Text("Add & Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualPrinterDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Virtual Print Tape Dialog
    if (showVirtualTapeDialog && virtualPrintJobPreview != null) {
        AlertDialog(
            onDismissRequest = { showVirtualTapeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Virtual ESC/POS Paper Output")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .background(Color(0xFFFEFCE8), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = virtualPrintJobPreview,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF1E293B)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showVirtualTapeDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun StoreProfileCard(
    shopName: String,
    phone: String,
    address: String,
    tagline: String,
    taxId: String,
    receiptConfig: ReceiptConfig?,
    onShopNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onTaglineChange: (String) -> Unit,
    onTaxIdChange: (String) -> Unit,
    onSave: (ReceiptConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Store Profile & Receipt Info", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Appears at top of printed customer bills", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = shopName,
                onValueChange = onShopNameChange,
                label = { Text("Shop / Business Name *") },
                leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                placeholder = { Text("e.g. Nexa Supermarket") },
                modifier = Modifier.fillMaxWidth().testTag("setting_shop_name_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                label = { Text("Contact Phone / WhatsApp *") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                placeholder = { Text("e.g. +1 555-0199 or +92 300 1234567") },
                modifier = Modifier.fillMaxWidth().testTag("setting_shop_phone_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = address,
                onValueChange = onAddressChange,
                label = { Text("Location / Shop Address *") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                placeholder = { Text("e.g. Shop 14, Commercial Market, Main Blvd") },
                modifier = Modifier.fillMaxWidth().testTag("setting_shop_address_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = tagline,
                    onValueChange = onTaglineChange,
                    label = { Text("Slogan / Tagline") },
                    placeholder = { Text("Quality & Freshness") },
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = taxId,
                    onValueChange = onTaxIdChange,
                    label = { Text("Tax / NTN / GST") },
                    placeholder = { Text("NTN-9988-PK") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val current = receiptConfig ?: ReceiptConfig()
                    onSave(
                        current.copy(
                            shopName = shopName.ifBlank { "My Shop" },
                            phone = phone,
                            address = address,
                            tagline = tagline,
                            taxId = taxId
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_store_details_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Store Profile", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ReceiptDesignerNavCard(
    receiptConfig: ReceiptConfig?,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
            .testTag("open_receipt_designer_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.DesignServices, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Receipt Template & Bill Designer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Live preview, paper width (${receiptConfig?.paperWidthMm ?: 58}mm), header/footer, QR code",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun StoreCurrencyCard(
    receiptConfig: ReceiptConfig?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("currency_selection_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFF059669),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = receiptConfig?.currencySymbol ?: "$",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Store Currency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${receiptConfig?.currencyCode ?: "USD"} (${receiptConfig?.currencySymbol ?: "$"})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun ThermalPrinterHardwareCard(
    receiptConfig: ReceiptConfig?,
    availablePrinters: List<DiscoveredPrinter>,
    selectedPrinter: DiscoveredPrinter?,
    printerState: PrinterConnectionState,
    printerStatusMessage: String,
    isScanningPrinters: Boolean,
    virtualPrintJobPreview: String?,
    onRefreshPrinters: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onPairPrinter: (DiscoveredPrinter) -> Unit,
    onSelectPrinter: (DiscoveredPrinter) -> Unit,
    onOpenManualDialog: () -> Unit,
    onOpenVirtualTape: () -> Unit,
    onTestPrint: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Thermal Receipt Printer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onRefreshPrinters) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh paired printers")
                    }
                    IconButton(onClick = onOpenManualDialog) {
                        Icon(Icons.Default.AddLink, contentDescription = "Add manual IP/MAC printer")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Connection status banner
            Surface(
                color = when (printerState) {
                    PrinterConnectionState.CONNECTED, PrinterConnectionState.SUCCESS -> Color(0xFFDCFCE7)
                    PrinterConnectionState.CONNECTING, PrinterConnectionState.PRINTING -> Color(0xFFE0F2FE)
                    PrinterConnectionState.ERROR -> MaterialTheme.colorScheme.errorContainer
                    PrinterConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.surface
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (printerState) {
                            PrinterConnectionState.CONNECTED, PrinterConnectionState.SUCCESS -> Icons.Default.CheckCircle
                            PrinterConnectionState.CONNECTING, PrinterConnectionState.PRINTING -> Icons.Default.Sync
                            PrinterConnectionState.ERROR -> Icons.Default.Error
                            PrinterConnectionState.DISCONNECTED -> Icons.Default.BluetoothDisabled
                        },
                        contentDescription = null,
                        tint = when (printerState) {
                            PrinterConnectionState.CONNECTED, PrinterConnectionState.SUCCESS -> Color(0xFF15803D)
                            PrinterConnectionState.CONNECTING, PrinterConnectionState.PRINTING -> Color(0xFF0369A1)
                            PrinterConnectionState.ERROR -> MaterialTheme.colorScheme.error
                            PrinterConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedPrinter != null) "Selected: ${selectedPrinter.name}" else "No Printer Selected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = printerStatusMessage,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (virtualPrintJobPreview != null) {
                        TextButton(onClick = onOpenVirtualTape) {
                            Text("View Slip", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bluetooth Discovery Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bluetooth Discovery", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = if (isScanningPrinters) "Searching for nearby devices..." else "Find unpaired 58mm/80mm printers",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isScanningPrinters) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = onStopScan) { Text("Stop") }
                        }
                    } else {
                        Button(
                            onClick = onStartScan,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.BluetoothSearching, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Available & Discovered Printers List
            Text("Select Printer to Connect", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))

            if (availablePrinters.isEmpty()) {
                Text(
                    text = "No printers available. Tap 'Scan' to discover nearby Bluetooth printers or 'Add manual IP' for network printers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    availablePrinters.forEach { printer ->
                        val isSelected = selectedPrinter?.address == printer.address

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectPrinter(printer) },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { onSelectPrinter(printer) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(printer.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            if (printer.isProbablePrinter) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = Color(0xFFDCFCE7),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "RECEIPT PRINTER",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF15803D),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${printer.type} • ${printer.address}" + if (printer.isPaired) " • PAIRED" else " • UNPAIRED",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (!printer.isPaired && printer.type == "BLUETOOTH") {
                                    OutlinedButton(
                                        onClick = { onPairPrinter(printer) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Pair", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Test Print Action Button
            Button(
                onClick = onTestPrint,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("printer_test_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send ESC/POS Test Print (58mm/80mm)", fontWeight = FontWeight.Bold)
            }
        }
    }
}
