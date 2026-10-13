package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.dao.SaleWithItems
import com.example.data.entity.ReceiptConfig
import com.example.printer.DiscoveredPrinter
import java.text.SimpleDateFormat
import java.util.*

val ReceiptInkColor = Color(0xFF0F172A) // Dark slate ink
val ReceiptSecondaryInk = Color(0xFF334155) // Slate gray ink
val ReceiptDividerColor = Color(0xFF64748B) // Medium slate divider
val ThermalPaperColor = Color(0xFFFFFFFF) // Crisp white thermal paper

@Composable
fun ReceiptModal(
    saleWithItems: SaleWithItems,
    config: ReceiptConfig,
    activePrinter: DiscoveredPrinter?,
    onPrintThermal: () -> Unit,
    onSharePdf: () -> Unit,
    onDismiss: () -> Unit
) {
    val sym = config.currencySymbol
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    val dateStr = dateFormat.format(Date(saleWithItems.sale.timestamp))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .testTag("receipt_modal"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Modal Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Customer Receipt",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${config.paperWidthMm}mm ESC/POS Thermal Format",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Realistic Thermal Paper View (Scrollable)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(1.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = ThermalPaperColor),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    CompositionLocalProvider(LocalContentColor provides ReceiptInkColor) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 20.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Store Logo & Header
                            if (config.showLogo) {
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

                            if (config.showHeader) {
                                Text(
                                    text = config.shopName.ifBlank { "Nexa Store" },
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ReceiptInkColor,
                                    textAlign = TextAlign.Center
                                )
                                if (config.tagline.isNotBlank()) {
                                    Text(
                                        text = config.tagline,
                                        fontSize = 11.sp,
                                        color = ReceiptSecondaryInk,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            if (config.showAddress && config.address.isNotBlank()) {
                                Text(
                                    text = config.address,
                                    fontSize = 11.sp,
                                    color = ReceiptSecondaryInk,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            if (config.showPhone && config.phone.isNotBlank()) {
                                Text(
                                    text = "Tel: ${config.phone}",
                                    fontSize = 11.sp,
                                    color = ReceiptSecondaryInk,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            if (config.taxId.isNotBlank()) {
                                Text(
                                    text = "Tax/NTN: ${config.taxId}",
                                    fontSize = 11.sp,
                                    color = ReceiptSecondaryInk,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            ReceiptDivider(isDouble = true)

                            // Invoice Metadata
                            ReceiptRow(left = "Invoice #:", right = saleWithItems.sale.invoiceNumber, isBold = true)
                            ReceiptRow(left = "Date & Time:", right = dateStr)
                            if (config.showCustomerInfo) {
                                ReceiptRow(left = "Customer:", right = saleWithItems.sale.customerName)
                            }
                            if (config.showPaymentMethod) {
                                ReceiptRow(left = "Payment:", right = saleWithItems.sale.paymentMethod)
                            }
                            if (saleWithItems.sale.status == "REFUNDED") {
                                ReceiptRow(left = "STATUS:", right = "*** REFUNDED ***", isBold = true, isAlert = true)
                            }

                            ReceiptDivider()

                            // Item Table Headers
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "ITEM",
                                    modifier = Modifier.weight(1.8f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ReceiptInkColor
                                )
                                Text(
                                    text = "QTY",
                                    modifier = Modifier.weight(0.8f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ReceiptInkColor,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "TOTAL",
                                    modifier = Modifier.weight(1.2f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ReceiptInkColor,
                                    textAlign = TextAlign.End
                                )
                            }

                            ReceiptDivider()

                            // Items List
                            saleWithItems.items.forEach { item ->
                                val qtyStr = if (item.quantity % 1.0 == 0.0) "${item.quantity.toInt()}x" else "%.2fx".format(item.quantity)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.productName,
                                        modifier = Modifier.weight(1.8f),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = ReceiptInkColor,
                                        lineHeight = 14.sp
                                    )
                                    Text(
                                        text = qtyStr,
                                        modifier = Modifier.weight(0.8f),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = ReceiptInkColor,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "$sym%.2f".format(item.lineTotal),
                                        modifier = Modifier.weight(1.2f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = ReceiptInkColor,
                                        textAlign = TextAlign.End
                                    )
                                }
                                if (item.discountAmount > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(
                                            text = "(Disc -$sym%.2f)".format(item.discountAmount),
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                }
                            }

                            ReceiptDivider()

                            // Financial Summary
                            ReceiptRow(left = "Subtotal:", right = "$sym%.2f".format(saleWithItems.sale.subtotal))
                            if (saleWithItems.sale.discountAmount > 0) {
                                ReceiptRow(left = "Order Discount:", right = "-$sym%.2f".format(saleWithItems.sale.discountAmount), isAlert = true)
                            }
                            if (config.showTaxBreakdown && saleWithItems.sale.taxAmount > 0) {
                                ReceiptRow(left = "Tax / VAT:", right = "+$sym%.2f".format(saleWithItems.sale.taxAmount))
                            }

                            ReceiptDivider(isDouble = true)

                            ReceiptRow(
                                left = "GRAND TOTAL:",
                                right = "$sym%.2f".format(saleWithItems.sale.grandTotal),
                                isBold = true,
                                fontSize = 15
                            )

                            ReceiptDivider()

                            ReceiptRow(left = "Paid (${saleWithItems.sale.paymentMethod}):", right = "$sym%.2f".format(saleWithItems.sale.amountPaid))
                            if (saleWithItems.sale.changeDue > 0) {
                                ReceiptRow(left = "Change Due:", right = "$sym%.2f".format(saleWithItems.sale.changeDue), isBold = true)
                            }

                            ReceiptDivider()

                            // Footer text & Policies
                            if (config.footerMessage.isNotBlank()) {
                                Text(
                                    text = config.footerMessage,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    fontFamily = FontFamily.Monospace,
                                    color = ReceiptInkColor,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            if (config.returnPolicy.isNotBlank()) {
                                Text(
                                    text = config.returnPolicy,
                                    fontSize = 10.sp,
                                    color = ReceiptSecondaryInk,
                                    textAlign = TextAlign.Center,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            if (config.showQrCode) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    modifier = Modifier.size(72.dp),
                                    color = Color.Black,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.QrCode2,
                                            contentDescription = "QR Code",
                                            tint = Color.White,
                                            modifier = Modifier.size(60.dp)
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

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "=== End of Receipt ===",
                                fontSize = 9.sp,
                                color = ReceiptDividerColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onSharePdf,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("share_pdf_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share PDF")
                    }

                    Button(
                        onClick = onPrintThermal,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp)
                            .testTag("print_thermal_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (activePrinter != null) "Print to ${activePrinter.name.take(12)}" else "Print Receipt",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptRow(
    left: String,
    right: String,
    isBold: Boolean = false,
    isAlert: Boolean = false,
    fontSize: Int = 12
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = left,
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize.sp,
            color = if (isAlert) Color(0xFFDC2626) else ReceiptInkColor,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = right,
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize.sp,
            color = if (isAlert) Color(0xFFDC2626) else ReceiptInkColor,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun ReceiptDivider(isDouble: Boolean = false) {
    Text(
        text = if (isDouble) "================================" else "--------------------------------",
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        color = ReceiptDividerColor,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
