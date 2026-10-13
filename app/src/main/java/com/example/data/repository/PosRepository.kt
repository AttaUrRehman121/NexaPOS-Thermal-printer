package com.example.data.repository

import com.example.data.AppDatabase
import com.example.data.dao.SaleWithItems
import com.example.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

data class CartItem(
    val product: Product,
    var quantity: Double = 1.0,
    var discountPercent: Double = 0.0,
    var customPrice: Double? = null
) {
    val unitPrice: Double
        get() = customPrice ?: product.sellingPrice

    val subtotal: Double
        get() = unitPrice * quantity

    val discountAmount: Double
        get() = subtotal * (discountPercent / 100.0)

    val lineTotal: Double
        get() = subtotal - discountAmount
}

class PosRepository(private val database: AppDatabase) {
    private val productDao = database.productDao()
    private val categoryDao = database.categoryDao()
    private val saleDao = database.saleDao()
    private val customerDao = database.customerDao()
    private val adjustmentDao = database.stockAdjustmentDao()
    private val receiptConfigDao = database.receiptConfigDao()

    val allActiveProducts: Flow<List<Product>> = productDao.getAllActiveProducts()
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val allSales: Flow<List<SaleWithItems>> = saleDao.getAllSalesWithItems()
    val lowStockProducts: Flow<List<Product>> = productDao.getLowStockProducts()
    val recentAdjustments: Flow<List<StockAdjustment>> = adjustmentDao.getRecentAdjustments()
    val receiptConfig: Flow<ReceiptConfig?> = receiptConfigDao.getConfig()

    suspend fun ensureInitialDataSeeded() = withContext(Dispatchers.IO) {
        val existing = productDao.getAllActiveProducts().firstOrNull()
        if (existing.isNullOrEmpty()) {
            AppDatabase.seedInitialData(database)
        }
    }

    suspend fun getProductByBarcode(barcode: String): Product? = withContext(Dispatchers.IO) {
        productDao.getProductByBarcode(barcode.trim())
    }

    suspend fun saveProduct(product: Product): Long = withContext(Dispatchers.IO) {
        if (product.id == 0L) {
            productDao.insertProduct(product)
        } else {
            productDao.updateProduct(product)
            product.id
        }
    }

    suspend fun deleteProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.deleteProduct(product)
    }

    suspend fun adjustProductStock(
        productId: Long,
        adjustmentQty: Double,
        reason: String
    ) = withContext(Dispatchers.IO) {
        val product = productDao.getProductById(productId) ?: return@withContext
        val newQty = (product.stockQuantity + adjustmentQty).coerceAtLeast(0.0)
        productDao.adjustStock(productId, adjustmentQty)
        adjustmentDao.insertAdjustment(
            StockAdjustment(
                productId = productId,
                productName = product.name,
                adjustmentQty = adjustmentQty,
                previousQty = product.stockQuantity,
                newQty = newQty,
                reason = reason
            )
        )
    }

    suspend fun completeSale(
        cartItems: List<CartItem>,
        paymentMethod: String,
        amountPaid: Double,
        discountAmount: Double,
        taxPercent: Double,
        customer: Customer?,
        notes: String = ""
    ): Result<SaleWithItems> = withContext(Dispatchers.IO) {
        if (cartItems.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Cart is empty"))
        }

        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
        val todayStr = dateFormat.format(Date())
        val randomSuffix = Random.nextInt(1000, 9999)
        val invoiceNumber = "INV-$todayStr-$randomSuffix"

        val subtotal = cartItems.sumOf { it.lineTotal }
        val taxAmount = (subtotal - discountAmount).coerceAtLeast(0.0) * (taxPercent / 100.0)
        val grandTotal = (subtotal - discountAmount + taxAmount).coerceAtLeast(0.0)
        val changeDue = (amountPaid - grandTotal).coerceAtLeast(0.0)

        // Execute atomically
        val sale = Sale(
            invoiceNumber = invoiceNumber,
            timestamp = System.currentTimeMillis(),
            subtotal = subtotal,
            discountAmount = discountAmount,
            taxAmount = taxAmount,
            grandTotal = grandTotal,
            amountPaid = amountPaid,
            changeDue = changeDue,
            paymentMethod = paymentMethod,
            status = "COMPLETED",
            customerId = customer?.id,
            customerName = customer?.name ?: "Walk-in Customer",
            notes = notes
        )

        val saleId = saleDao.insertSale(sale)

        val saleItems = cartItems.map { item ->
            // Reduce stock
            productDao.adjustStock(item.product.id, -item.quantity)

            // Log adjustment
            adjustmentDao.insertAdjustment(
                StockAdjustment(
                    productId = item.product.id,
                    productName = item.product.name,
                    adjustmentQty = -item.quantity,
                    previousQty = item.product.stockQuantity,
                    newQty = item.product.stockQuantity - item.quantity,
                    reason = "SALE: $invoiceNumber"
                )
            )

            SaleItem(
                saleId = saleId,
                productId = item.product.id,
                productName = item.product.name,
                unitPrice = item.unitPrice,
                costPrice = item.product.costPrice,
                quantity = item.quantity,
                discountAmount = item.discountAmount,
                lineTotal = item.lineTotal
            )
        }

        saleDao.insertSaleItems(saleItems)

        // Update customer total spend
        if (customer != null && customer.id != 1L) {
            val updatedCustomer = customer.copy(
                totalSpend = customer.totalSpend + grandTotal
            )
            customerDao.updateCustomer(updatedCustomer)
        }

        val createdSale = saleDao.getSaleWithItemsById(saleId)
            ?: SaleWithItems(sale.copy(id = saleId), saleItems)

        Result.success(createdSale)
    }

    suspend fun processRefund(saleId: Long, reason: String = "Customer Return"): Result<SaleWithItems> = withContext(Dispatchers.IO) {
        val saleWithItems = saleDao.getSaleWithItemsById(saleId)
            ?: return@withContext Result.failure(IllegalArgumentException("Sale not found"))

        if (saleWithItems.sale.status == "REFUNDED") {
            return@withContext Result.failure(IllegalStateException("Sale already refunded"))
        }

        // Restore stock
        for (item in saleWithItems.items) {
            val product = productDao.getProductById(item.productId)
            val currentQty = product?.stockQuantity ?: 0.0
            productDao.adjustStock(item.productId, item.quantity)
            adjustmentDao.insertAdjustment(
                StockAdjustment(
                    productId = item.productId,
                    productName = item.productName,
                    adjustmentQty = item.quantity,
                    previousQty = currentQty,
                    newQty = currentQty + item.quantity,
                    reason = "REFUND: ${saleWithItems.sale.invoiceNumber} ($reason)"
                )
            )
        }

        // Update sale status
        val updatedSale = saleWithItems.sale.copy(
            status = "REFUNDED",
            notes = if (saleWithItems.sale.notes.isBlank()) "REFUND: $reason" else "${saleWithItems.sale.notes} | REFUND: $reason"
        )
        saleDao.updateSale(updatedSale)

        val refreshed = saleDao.getSaleWithItemsById(saleId) ?: saleWithItems.copy(sale = updatedSale)
        Result.success(refreshed)
    }

    suspend fun saveCustomer(customer: Customer): Long = withContext(Dispatchers.IO) {
        if (customer.id == 0L) {
            customerDao.insertCustomer(customer)
        } else {
            customerDao.updateCustomer(customer)
            customer.id
        }
    }

    suspend fun saveReceiptConfig(config: ReceiptConfig) = withContext(Dispatchers.IO) {
        receiptConfigDao.insertOrUpdateConfig(config)
    }

    suspend fun getReceiptConfigSync(): ReceiptConfig = withContext(Dispatchers.IO) {
        receiptConfigDao.getConfigSync() ?: ReceiptConfig()
    }
}
