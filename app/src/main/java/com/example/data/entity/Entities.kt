package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#2563EB",
    val iconName: String = "category"
)

@Entity(
    tableName = "products",
    indices = [Index(value = ["barcode"]), Index(value = ["sku"])]
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sku: String,
    val barcode: String,
    val categoryId: Long,
    val costPrice: Double,
    val sellingPrice: Double,
    val stockQuantity: Double,
    val minStockThreshold: Double = 5.0,
    val unit: String = "pcs",
    val isActive: Boolean = true
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val totalSpend: Double = 0.0,
    val outstandingBalance: Double = 0.0
)

@Entity(
    tableName = "sales",
    indices = [Index(value = ["invoiceNumber"], unique = true)]
)
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val subtotal: Double,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val grandTotal: Double,
    val amountPaid: Double,
    val changeDue: Double = 0.0,
    val paymentMethod: String = "CASH", // CASH, CARD, SPLIT, CREDIT
    val status: String = "COMPLETED", // COMPLETED, REFUNDED, HELD
    val customerId: Long? = null,
    val customerName: String = "Walk-in Customer",
    val notes: String = ""
)

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = Sale::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["saleId"]), Index(value = ["productId"])]
)
data class SaleItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val unitPrice: Double,
    val costPrice: Double,
    val quantity: Double,
    val discountAmount: Double = 0.0,
    val lineTotal: Double
)

@Entity(tableName = "stock_adjustments")
data class StockAdjustment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val productName: String,
    val adjustmentQty: Double,
    val previousQty: Double,
    val newQty: Double,
    val reason: String, // RESTOCK, DAMAGE, RETURN, MANUAL_CORRECTION
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "receipt_config")
data class ReceiptConfig(
    @PrimaryKey val id: Int = 1,
    val shopName: String = "Nexa Store & Supermarket",
    val tagline: String = "Quality & Freshness Guaranteed",
    val address: String = "45 Commercial Ave, Tech District",
    val phone: String = "+1 (555) 234-5678",
    val taxId: String = "TAX-9988-7766",
    val paperWidthMm: Int = 58, // 58 or 80
    val currencyCode: String = "USD",
    val currencySymbol: String = "$",
    val headerMessage: String = "Welcome to NexaPOS",
    val footerMessage: String = "Thank you for shopping with us!",
    val returnPolicy: String = "Exchange within 7 days with valid receipt.",
    val showLogo: Boolean = true,
    val showHeader: Boolean = true,
    val showAddress: Boolean = true,
    val showPhone: Boolean = true,
    val showCustomerInfo: Boolean = true,
    val showPaymentMethod: Boolean = true,
    val showTaxBreakdown: Boolean = true,
    val showBarcode: Boolean = true,
    val showQrCode: Boolean = true,
    val showFooter: Boolean = true,
    val showReturnPolicy: Boolean = true,
    val autoPrintOnCheckout: Boolean = false,
    val preferredPrinterAddress: String = "",
    val preferredPrinterName: String = "",
    val preferredPrinterType: String = "BLUETOOTH" // BLUETOOTH, USB, TCP
)
