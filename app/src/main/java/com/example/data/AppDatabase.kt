package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Category::class,
        Product::class,
        Customer::class,
        Sale::class,
        SaleItem::class,
        StockAdjustment::class,
        ReceiptConfig::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun saleDao(): SaleDao
    abstract fun customerDao(): CustomerDao
    abstract fun stockAdjustmentDao(): StockAdjustmentDao
    abstract fun receiptConfigDao(): ReceiptConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nexapos_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedInitialData(database)
                    }
                }
            }
        }

        suspend fun seedInitialData(database: AppDatabase) {
            // Seed Categories
            val categories = listOf(
                Category(id = 1, name = "Beverages", colorHex = "#0284C7", iconName = "local_cafe"),
                Category(id = 2, name = "Bakery & Snacks", colorHex = "#D97706", iconName = "bakery_dining"),
                Category(id = 3, name = "Dairy & Eggs", colorHex = "#059669", iconName = "egg"),
                Category(id = 4, name = "Fresh Produce", colorHex = "#16A34A", iconName = "eco"),
                Category(id = 5, name = "Personal Care", colorHex = "#9333EA", iconName = "health_and_safety"),
                Category(id = 6, name = "Pantry & Spices", colorHex = "#DC2626", iconName = "restaurant")
            )
            database.categoryDao().insertCategories(categories)

            // Seed Products with realistic barcodes and retail prices
            val products = listOf(
                Product(
                    name = "Espresso Roast Coffee 250g",
                    sku = "BEV-001",
                    barcode = "8901234567890",
                    categoryId = 1,
                    costPrice = 4.50,
                    sellingPrice = 8.99,
                    stockQuantity = 45.0,
                    minStockThreshold = 10.0,
                    unit = "bag"
                ),
                Product(
                    name = "Organic Green Tea 50 Bags",
                    sku = "BEV-002",
                    barcode = "8901234567891",
                    categoryId = 1,
                    costPrice = 2.80,
                    sellingPrice = 5.49,
                    stockQuantity = 30.0,
                    minStockThreshold = 8.0,
                    unit = "box"
                ),
                Product(
                    name = "Artisan Sourdough Loaf",
                    sku = "BAK-001",
                    barcode = "8901234567892",
                    categoryId = 2,
                    costPrice = 1.90,
                    sellingPrice = 4.25,
                    stockQuantity = 18.0,
                    minStockThreshold = 5.0,
                    unit = "loaf"
                ),
                Product(
                    name = "Dark Chocolate Almond Cookies",
                    sku = "BAK-002",
                    barcode = "8901234567893",
                    categoryId = 2,
                    costPrice = 1.40,
                    sellingPrice = 3.50,
                    stockQuantity = 52.0,
                    minStockThreshold = 12.0,
                    unit = "pack"
                ),
                Product(
                    name = "Whole Fresh Milk 1 Gallon",
                    sku = "DAI-001",
                    barcode = "8901234567894",
                    categoryId = 3,
                    costPrice = 2.10,
                    sellingPrice = 3.89,
                    stockQuantity = 24.0,
                    minStockThreshold = 6.0,
                    unit = "gal"
                ),
                Product(
                    name = "Farm Fresh Brown Eggs (Dozen)",
                    sku = "DAI-002",
                    barcode = "8901234567895",
                    categoryId = 3,
                    costPrice = 2.30,
                    sellingPrice = 4.49,
                    stockQuantity = 35.0,
                    minStockThreshold = 10.0,
                    unit = "dozen"
                ),
                Product(
                    name = "Crisp Gala Apples (1kg)",
                    sku = "PRO-001",
                    barcode = "8901234567896",
                    categoryId = 4,
                    costPrice = 1.20,
                    sellingPrice = 2.99,
                    stockQuantity = 60.0,
                    minStockThreshold = 15.0,
                    unit = "kg"
                ),
                Product(
                    name = "Ripe Hass Avocados (Pack of 3)",
                    sku = "PRO-002",
                    barcode = "8901234567897",
                    categoryId = 4,
                    costPrice = 2.00,
                    sellingPrice = 4.99,
                    stockQuantity = 4.0, // Low stock on purpose
                    minStockThreshold = 10.0,
                    unit = "pack"
                ),
                Product(
                    name = "Natural Herbal Hand Wash 300ml",
                    sku = "PER-001",
                    barcode = "8901234567898",
                    categoryId = 5,
                    costPrice = 1.80,
                    sellingPrice = 4.75,
                    stockQuantity = 25.0,
                    minStockThreshold = 5.0,
                    unit = "bottle"
                ),
                Product(
                    name = "Extra Virgin Olive Oil 750ml",
                    sku = "PAN-001",
                    barcode = "8901234567899",
                    categoryId = 6,
                    costPrice = 7.50,
                    sellingPrice = 13.99,
                    stockQuantity = 20.0,
                    minStockThreshold = 5.0,
                    unit = "bottle"
                )
            )
            database.productDao().insertProducts(products)

            // Seed Customers
            val customers = listOf(
                Customer(id = 1, name = "Walk-in Customer", phone = "", totalSpend = 0.0),
                Customer(id = 2, name = "Sarah Connor", phone = "+1-555-0192", email = "sarah@example.com", totalSpend = 142.50),
                Customer(id = 3, name = "Ahmad Khan", phone = "+92-300-1234567", email = "ahmad@example.com", totalSpend = 310.00)
            )
            database.customerDao().insertCustomers(customers)

            // Seed default receipt config
            val defaultReceiptConfig = ReceiptConfig(
                id = 1,
                shopName = "Nexa Superstore & Cafe",
                tagline = "Modern Quality & Fast Checkout",
                address = "104 Main Boulevard, Commercial Zone",
                phone = "+1 (800) 555-0199",
                taxId = "GST-99214-PK",
                paperWidthMm = 58,
                currencyCode = "USD",
                currencySymbol = "$",
                headerMessage = "Welcome to NexaPOS",
                footerMessage = "Thank you for visiting! Have a great day.",
                returnPolicy = "Exchange within 7 days with original receipt.",
                showLogo = true,
                showTaxBreakdown = true,
                showBarcode = true,
                showQrCode = true,
                autoPrintOnCheckout = false
            )
            database.receiptConfigDao().insertOrUpdateConfig(defaultReceiptConfig)
        }
    }
}
