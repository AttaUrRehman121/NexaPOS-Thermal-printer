package com.example.ui.screens

import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.dao.SaleWithItems
import com.example.data.entity.ReceiptConfig

@Composable
fun ReceiptDesignerScreen(
    currentConfig: ReceiptConfig,
    salesHistory: List<SaleWithItems> = emptyList(),
    onSaveConfig: (ReceiptConfig) -> Unit,
    onTestPrint: () -> Unit
) {
    var shopName by remember(currentConfig) { mutableStateOf(currentConfig.shopName) }
    var tagline by remember(currentConfig) { mutableStateOf(currentConfig.tagline) }
    var address by remember(currentConfig) { mutableStateOf(currentConfig.address) }
    var phone by remember(currentConfig) { mutableStateOf(currentConfig.phone) }
    var taxId by remember(currentConfig) { mutableStateOf(currentConfig.taxId) }
    var paperWidthMm by remember(currentConfig) { mutableStateOf(currentConfig.paperWidthMm) }

    var showLogo by remember(currentConfig) { mutableStateOf(currentConfig.showLogo) }
    var showHeader by remember(currentConfig) { mutableStateOf(currentConfig.showHeader) }
    var showAddress by remember(currentConfig) { mutableStateOf(currentConfig.showAddress) }
    var showPhone by remember(currentConfig) { mutableStateOf(currentConfig.showPhone) }
    var showCustomerInfo by remember(currentConfig) { mutableStateOf(currentConfig.showCustomerInfo) }
    var showPaymentMethod by remember(currentConfig) { mutableStateOf(currentConfig.showPaymentMethod) }
    var showTaxBreakdown by remember(currentConfig) { mutableStateOf(currentConfig.showTaxBreakdown) }
    var showFooter by remember(currentConfig) { mutableStateOf(currentConfig.showFooter) }
    var showReturnPolicy by remember(currentConfig) { mutableStateOf(currentConfig.showReturnPolicy) }
    var showQrCode by remember(currentConfig) { mutableStateOf(currentConfig.showQrCode) }
    var autoPrintOnCheckout by remember(currentConfig) { mutableStateOf(currentConfig.autoPrintOnCheckout) }

    var footerMessage by remember(currentConfig) { mutableStateOf(currentConfig.footerMessage) }
    var returnPolicy by remember(currentConfig) { mutableStateOf(currentConfig.returnPolicy) }

    var useRealOrder by remember { mutableStateOf(salesHistory.isNotEmpty()) }
    val latestOrder = remember(salesHistory) { salesHistory.firstOrNull() }

    val sym = currentConfig.currencySymbol

    val liveConfig = currentConfig.copy(
        shopName = shopName,
        tagline = tagline,
        address = address,
        phone = phone,
        taxId = taxId,
        paperWidthMm = paperWidthMm,
        showLogo = showLogo,
        showHeader = showHeader,
        showAddress = showAddress,
        showPhone = showPhone,
        showCustomerInfo = showCustomerInfo,
        showPaymentMethod = showPaymentMethod,
        showTaxBreakdown = showTaxBreakdown,
        showFooter = showFooter,
        showReturnPolicy = showReturnPolicy,
        showQrCode = showQrCode,
        footerMessage = footerMessage,
        returnPolicy = returnPolicy,
        autoPrintOnCheckout = autoPrintOnCheckout
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize().testTag("receipt_designer_screen")) {
        val isWide = maxWidth >= 760.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Receipt Template Designer",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Customize thermal receipt layout, paper size, and branding",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onTestPrint,
                        modifier = Modifier.testTag("test_print_btn")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print")
                    }
                    Button(
                        onClick = { onSaveConfig(liveConfig) },
                        modifier = Modifier.testTag("save_receipt_config_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isWide) {
                // Adaptive Split-Screen: Controls on Left, Sticky Live Thermal Preview on Right
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Column: Scrollable Controls
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    ) {
                        DesignerControls(
                            salesHistory = salesHistory,
                            latestOrder = latestOrder,
                            useRealOrder = useRealOrder,
                            paperWidthMm = paperWidthMm,
                            showLogo = showLogo,
                            showHeader = showHeader,
                            showAddress = showAddress,
                            showPhone = showPhone,
                            showCustomerInfo = showCustomerInfo,
                            showPaymentMethod = showPaymentMethod,
                            showTaxBreakdown = showTaxBreakdown,
                            showFooter = showFooter,
                            showReturnPolicy = showReturnPolicy,
                            showQrCode = showQrCode,
                            shopName = shopName,
                            tagline = tagline,
                            address = address,
                            phone = phone,
                            taxId = taxId,
                            footerMessage = footerMessage,
                            returnPolicy = returnPolicy,
                            onUseRealOrderChange = { useRealOrder = it },
                            onPaperWidthChange = { paperWidthMm = it },
                            onToggleLogo = { showLogo = it },
                            onToggleHeader = { showHeader = it },
                            onToggleAddress = { showAddress = it },
                            onTogglePhone = { showPhone = it },
                            onToggleCustomer = { showCustomerInfo = it },
                            onTogglePayment = { showPaymentMethod = it },
                            onToggleTax = { showTaxBreakdown = it },
                            onToggleFooter = { showFooter = it },
                            onTogglePolicy = { showReturnPolicy = it },
                            onToggleQr = { showQrCode = it },
                            onShopNameChange = { shopName = it },
                            onTaglineChange = { tagline = it },
                            onAddressChange = { address = it },
                            onPhoneChange = { phone = it },
                            onTaxIdChange = { taxId = it },
                            onFooterMessageChange = { footerMessage = it },
                            onReturnPolicyChange = { returnPolicy = it },
                            onSave = { onSaveConfig(liveConfig) }
                        )
                    }

                    VerticalDivider()

                    // Right Column: Live Formatted Thermal Paper Preview
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "LIVE FORMATTED THERMAL PREVIEW",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        ThermalPaperCard(
                            liveConfig = liveConfig,
                            useRealOrder = useRealOrder,
                            latestOrder = latestOrder,
                            sym = sym,
                            shopName = shopName,
                            tagline = tagline,
                            address = address,
                            phone = phone,
                            taxId = taxId,
                            showLogo = showLogo,
                            showHeader = showHeader,
                            showAddress = showAddress,
                            showPhone = showPhone,
                            showCustomerInfo = showCustomerInfo,
                            showPaymentMethod = showPaymentMethod,
                            showTaxBreakdown = showTaxBreakdown,
                            showFooter = showFooter,
                            showReturnPolicy = showReturnPolicy,
                            showQrCode = showQrCode,
                            footerMessage = footerMessage,
                            returnPolicy = returnPolicy
                        )
                    }
                }
            } else {
                // Compact Single-column Mobile Layout
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "LIVE FORMATTED THERMAL PREVIEW",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ThermalPaperCard(
                        liveConfig = liveConfig,
                        useRealOrder = useRealOrder,
                        latestOrder = latestOrder,
                        sym = sym,
                        shopName = shopName,
                        tagline = tagline,
                        address = address,
                        phone = phone,
                        taxId = taxId,
                        showLogo = showLogo,
                        showHeader = showHeader,
                        showAddress = showAddress,
                        showPhone = showPhone,
                        showCustomerInfo = showCustomerInfo,
                        showPaymentMethod = showPaymentMethod,
                        showTaxBreakdown = showTaxBreakdown,
                        showFooter = showFooter,
                        showReturnPolicy = showReturnPolicy,
                        showQrCode = showQrCode,
                        footerMessage = footerMessage,
                        returnPolicy = returnPolicy
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    DesignerControls(
                        salesHistory = salesHistory,
                        latestOrder = latestOrder,
                        useRealOrder = useRealOrder,
                        paperWidthMm = paperWidthMm,
                        showLogo = showLogo,
                        showHeader = showHeader,
                        showAddress = showAddress,
                        showPhone = showPhone,
                        showCustomerInfo = showCustomerInfo,
                        showPaymentMethod = showPaymentMethod,
                        showTaxBreakdown = showTaxBreakdown,
                        showFooter = showFooter,
                        showReturnPolicy = showReturnPolicy,
                        showQrCode = showQrCode,
                        shopName = shopName,
                        tagline = tagline,
                        address = address,
                        phone = phone,
                        taxId = taxId,
                        footerMessage = footerMessage,
                        returnPolicy = returnPolicy,
                        onUseRealOrderChange = { useRealOrder = it },
                        onPaperWidthChange = { paperWidthMm = it },
                        onToggleLogo = { showLogo = it },
                        onToggleHeader = { showHeader = it },
                        onToggleAddress = { showAddress = it },
                        onTogglePhone = { showPhone = it },
                        onToggleCustomer = { showCustomerInfo = it },
                        onTogglePayment = { showPaymentMethod = it },
                        onToggleTax = { showTaxBreakdown = it },
                        onToggleFooter = { showFooter = it },
                        onTogglePolicy = { showReturnPolicy = it },
                        onToggleQr = { showQrCode = it },
                        onShopNameChange = { shopName = it },
                        onTaglineChange = { tagline = it },
                        onAddressChange = { address = it },
                        onPhoneChange = { phone = it },
                        onTaxIdChange = { taxId = it },
                        onFooterMessageChange = { footerMessage = it },
                        onReturnPolicyChange = { returnPolicy = it },
                        onSave = { onSaveConfig(liveConfig) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DesignerControls(
    salesHistory: List<SaleWithItems>,
    latestOrder: SaleWithItems?,
    useRealOrder: Boolean,
    paperWidthMm: Int,
    showLogo: Boolean,
    showHeader: Boolean,
    showAddress: Boolean,
    showPhone: Boolean,
    showCustomerInfo: Boolean,
    showPaymentMethod: Boolean,
    showTaxBreakdown: Boolean,
    showFooter: Boolean,
    showReturnPolicy: Boolean,
    showQrCode: Boolean,
    shopName: String,
    tagline: String,
    address: String,
    phone: String,
    taxId: String,
    footerMessage: String,
    returnPolicy: String,
    onUseRealOrderChange: (Boolean) -> Unit,
    onPaperWidthChange: (Int) -> Unit,
    onToggleLogo: (Boolean) -> Unit,
    onToggleHeader: (Boolean) -> Unit,
    onToggleAddress: (Boolean) -> Unit,
    onTogglePhone: (Boolean) -> Unit,
    onToggleCustomer: (Boolean) -> Unit,
    onTogglePayment: (Boolean) -> Unit,
    onToggleTax: (Boolean) -> Unit,
    onToggleFooter: (Boolean) -> Unit,
    onTogglePolicy: (Boolean) -> Unit,
    onToggleQr: (Boolean) -> Unit,
    onShopNameChange: (String) -> Unit,
    onTaglineChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onTaxIdChange: (String) -> Unit,
    onFooterMessageChange: (String) -> Unit,
    onReturnPolicyChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Column {
        if (salesHistory.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !useRealOrder,
                    onClick = { onUseRealOrderChange(false) },
                    label = { Text("Sample Order") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = useRealOrder,
                    onClick = { onUseRealOrderChange(true) },
                    label = { Text("Real Order (${latestOrder?.sale?.invoiceNumber?.takeLast(8) ?: "Recent"})") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Paper Width Selector
        Text("Paper Width", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChip(
                selected = paperWidthMm == 58,
                onClick = { onPaperWidthChange(58) },
                label = { Text("58 mm (Standard)") },
                leadingIcon = { if (paperWidthMm == 58) Icon(Icons.Default.Check, contentDescription = null) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = paperWidthMm == 80,
                onClick = { onPaperWidthChange(80) },
                label = { Text("80 mm (Wide)") },
                leadingIcon = { if (paperWidthMm == 80) Icon(Icons.Default.Check, contentDescription = null) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Visibility Toggles
        Text("Field Visibility Controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                TemplateToggleItem("Store Logo", "Show top emblem", showLogo, onToggleLogo)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                TemplateToggleItem("Store Name & Slogan", "Print store header", showHeader, onToggleHeader)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                TemplateToggleItem("Location & Address", "Print street address", showAddress, onToggleAddress)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                TemplateToggleItem("Phone & WhatsApp", "Print contact numbers", showPhone, onTogglePhone)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                TemplateToggleItem("Customer Details", "Print customer name", showCustomerInfo, onToggleCustomer)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                TemplateToggleItem("Payment Method", "Print Cash/Card details", showPaymentMethod, onTogglePayment)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                TemplateToggleItem("Tax Breakdown", "Print separate tax line", showTaxBreakdown, onToggleTax)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                TemplateToggleItem("Thank-You Footer", "Print gratitude note", showFooter, onToggleFooter)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                TemplateToggleItem("Return & Warranty Policy", "Print exchange rules", showReturnPolicy, onTogglePolicy)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                TemplateToggleItem("Verification QR Code", "Print scannable QR code", showQrCode, onToggleQr)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Editable Business Details
        Text("Editable Store Profile Text", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = shopName,
            onValueChange = onShopNameChange,
            label = { Text("Shop / Business Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = tagline,
            onValueChange = onTaglineChange,
            label = { Text("Slogan / Tagline") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            label = { Text("Location Address") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                label = { Text("Phone") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )
            OutlinedTextField(
                value = taxId,
                onValueChange = onTaxIdChange,
                label = { Text("Tax / NTN") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = footerMessage,
            onValueChange = onFooterMessageChange,
            label = { Text("Footer Note") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = returnPolicy,
            onValueChange = onReturnPolicyChange,
            label = { Text("Return Policy") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("bottom_save_template_btn"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Template & Update Bills", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ThermalPaperCard(
    liveConfig: ReceiptConfig,
    useRealOrder: Boolean,
    latestOrder: SaleWithItems?,
    sym: String,
    shopName: String,
    tagline: String,
    address: String,
    phone: String,
    taxId: String,
    showLogo: Boolean,
    showHeader: Boolean,
    showAddress: Boolean,
    showPhone: Boolean,
    showCustomerInfo: Boolean,
    showPaymentMethod: Boolean,
    showTaxBreakdown: Boolean,
    showFooter: Boolean,
    showReturnPolicy: Boolean,
    showQrCode: Boolean,
    footerMessage: String,
    returnPolicy: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = ThermalPaperColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides ReceiptInkColor) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo
                if (showLogo) {
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .size(42.dp)
                            .padding(bottom = 6.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Header
                if (showHeader) {
                    Text(
                        text = shopName.ifBlank { "NEXA STORE" },
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = ReceiptInkColor,
                        textAlign = TextAlign.Center
                    )
                    if (tagline.isNotBlank()) {
                        Text(
                            text = tagline,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = ReceiptSecondaryInk,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                if (showAddress && address.isNotBlank()) {
                    Text(
                        text = address,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = ReceiptSecondaryInk,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (showPhone && phone.isNotBlank()) {
                    Text(
                        text = "Tel: $phone",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = ReceiptSecondaryInk,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (taxId.isNotBlank()) {
                    Text(
                        text = "Tax/NTN: $taxId",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = ReceiptSecondaryInk,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                ReceiptDivider(isDouble = true)

                val orderInvoice = if (useRealOrder && latestOrder != null) latestOrder.sale.invoiceNumber else "INV-20261013-4821"
                val orderCustomer = if (useRealOrder && latestOrder != null) latestOrder.sale.customerName else "Sarah Connor"
                val orderPayment = if (useRealOrder && latestOrder != null) latestOrder.sale.paymentMethod else "CASH"

                ReceiptRow(left = "Invoice #:", right = orderInvoice, isBold = true)
                ReceiptRow(left = "Date & Time:", right = "2026-10-13 14:30")

                if (showCustomerInfo) {
                    ReceiptRow(left = "Customer:", right = orderCustomer)
                }

                if (showPaymentMethod) {
                    ReceiptRow(left = "Payment:", right = orderPayment)
                }

                ReceiptDivider()

                // Items List Header
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("ITEM", modifier = Modifier.weight(1.8f), fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = ReceiptInkColor)
                    Text("QTY", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = ReceiptInkColor, textAlign = TextAlign.Center)
                    Text("TOTAL", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = ReceiptInkColor, textAlign = TextAlign.End)
                }

                ReceiptDivider()

                if (useRealOrder && latestOrder != null && latestOrder.items.isNotEmpty()) {
                    latestOrder.items.forEach { item ->
                        val qtyStr = if (item.quantity % 1.0 == 0.0) "${item.quantity.toInt()}x" else "%.2fx".format(item.quantity)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(item.productName, modifier = Modifier.weight(1.8f), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = ReceiptInkColor)
                            Text(qtyStr, modifier = Modifier.weight(0.8f), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = ReceiptInkColor, textAlign = TextAlign.Center)
                            Text("$sym%.2f".format(item.lineTotal), modifier = Modifier.weight(1.2f), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = ReceiptInkColor, textAlign = TextAlign.End)
                        }
                    }
                } else {
                    ReceiptRow(left = "Espresso Roast (2x)", right = "$sym 17.98")
                    ReceiptRow(left = "Artisan Bread (1x)", right = "$sym 4.25")
                }

                ReceiptDivider()

                val previewSubtotal = if (useRealOrder && latestOrder != null) latestOrder.sale.subtotal else 22.23
                val previewTax = if (useRealOrder && latestOrder != null) latestOrder.sale.taxAmount else 1.11
                val previewGrandTotal = if (useRealOrder && latestOrder != null) latestOrder.sale.grandTotal else 23.34
                val previewPaid = if (useRealOrder && latestOrder != null) latestOrder.sale.amountPaid else 30.00
                val previewChange = if (useRealOrder && latestOrder != null) latestOrder.sale.changeDue else 6.66

                ReceiptRow(left = "Subtotal:", right = "$sym%.2f".format(previewSubtotal))

                if (showTaxBreakdown && previewTax > 0) {
                    ReceiptRow(left = "Tax (5%):", right = "$sym%.2f".format(previewTax))
                }

                ReceiptDivider(isDouble = true)
                ReceiptRow(left = "GRAND TOTAL:", right = "$sym%.2f".format(previewGrandTotal), isBold = true, fontSize = 15)
                ReceiptDivider()

                ReceiptRow(left = "Paid ($orderPayment):", right = "$sym%.2f".format(previewPaid))
                if (previewChange > 0) {
                    ReceiptRow(left = "Change Due:", right = "$sym%.2f".format(previewChange), isBold = true)
                }

                ReceiptDivider()

                if (showFooter && footerMessage.isNotBlank()) {
                    Text(
                        text = footerMessage,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = ReceiptInkColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (showReturnPolicy && returnPolicy.isNotBlank()) {
                    Text(
                        text = returnPolicy,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = ReceiptSecondaryInk,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (showQrCode) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.size(64.dp),
                        color = Color.Black,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = "QR Code",
                                tint = Color.White,
                                modifier = Modifier.size(52.dp)
                            )
                        }
                    }
                    Text(
                        text = "Scan to verify invoice",
                        fontSize = 9.sp,
                        color = ReceiptSecondaryInk,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "=== End of Receipt ===",
                    fontSize = 9.sp,
                    color = ReceiptDividerColor,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun TemplateToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
