package com.example.printer

import com.example.data.dao.SaleWithItems
import com.example.data.entity.ReceiptConfig
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

class EscPosBuilder(private val charset: Charset = Charsets.UTF_8) {
    private val buffer = ByteArrayOutputStream()

    fun reset(): EscPosBuilder {
        buffer.reset()
        // ESC @: Initialize printer
        buffer.write(byteArrayOf(0x1B, 0x40))
        return this
    }

    fun alignLeft(): EscPosBuilder {
        buffer.write(byteArrayOf(0x1B, 0x61, 0x00))
        return this
    }

    fun alignCenter(): EscPosBuilder {
        buffer.write(byteArrayOf(0x1B, 0x61, 0x01))
        return this
    }

    fun alignRight(): EscPosBuilder {
        buffer.write(byteArrayOf(0x1B, 0x61, 0x02))
        return this
    }

    fun bold(enable: Boolean): EscPosBuilder {
        buffer.write(byteArrayOf(0x1B, 0x45, if (enable) 0x01 else 0x00))
        return this
    }

    fun textSize(widthMultiplier: Int, heightMultiplier: Int): EscPosBuilder {
        // GS ! n : n = (width - 1) * 16 + (height - 1)
        val w = (widthMultiplier.coerceIn(1, 4) - 1) shl 4
        val h = (heightMultiplier.coerceIn(1, 4) - 1)
        val n = (w or h).toByte()
        buffer.write(byteArrayOf(0x1D, 0x21, n))
        return this
    }

    fun text(str: String): EscPosBuilder {
        buffer.write(str.toByteArray(charset))
        return this
    }

    fun line(str: String = ""): EscPosBuilder {
        if (str.isNotEmpty()) {
            buffer.write(str.toByteArray(charset))
        }
        buffer.write(0x0A) // LF
        return this
    }

    fun feed(lines: Int = 1): EscPosBuilder {
        // ESC d n
        val n = lines.coerceIn(1, 10).toByte()
        buffer.write(byteArrayOf(0x1B, 0x64, n))
        return this
    }

    fun divider(columns: Int, char: Char = '-'): EscPosBuilder {
        line(char.toString().repeat(columns))
        return this
    }

    fun doubleDivider(columns: Int): EscPosBuilder {
        line("=".repeat(columns))
        return this
    }

    fun cutPaper(): EscPosBuilder {
        // GS V 66 0 : partial cut with feed
        feed(3)
        buffer.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
        return this
    }

    fun qrCode(content: String, moduleSize: Int = 5): EscPosBuilder {
        val data = content.toByteArray(charset)
        val pL = ((data.size + 3) and 0xFF).toByte()
        val pH = (((data.size + 3) shr 8) and 0xFF).toByte()

        // 1. Model: GS ( k 4 0 49 65 50 0
        buffer.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00))

        // 2. Size: GS ( k 3 0 49 67 size
        buffer.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, moduleSize.coerceIn(3, 8).toByte()))

        // 3. Error correction: GS ( k 3 0 49 69 48 (Level L/M)
        buffer.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x30))

        // 4. Store data: GS ( k pL pH 49 80 48 <data>
        buffer.write(byteArrayOf(0x1D, 0x28, 0x6B, pL, pH, 0x31, 0x50, 0x30))
        buffer.write(data)

        // 5. Print QR: GS ( k 3 0 49 81 48
        buffer.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30))
        feed(1)
        return this
    }

    fun twoColumn(left: String, right: String, totalCols: Int): EscPosBuilder {
        val maxLeft = (totalCols - right.length - 1).coerceAtLeast(1)
        val trimmedLeft = if (left.length > maxLeft) left.take(maxLeft) else left
        val spaces = (totalCols - trimmedLeft.length - right.length).coerceAtLeast(1)
        line(trimmedLeft + " ".repeat(spaces) + right)
        return this
    }

    fun threeColumn(col1: String, col2: String, col3: String, totalCols: Int): EscPosBuilder {
        // e.g. for 32 cols: 16 cols for col1, 6 cols for col2, 10 cols for col3
        val w3 = 9.coerceAtMost(totalCols / 3)
        val w2 = 6.coerceAtMost(totalCols / 4)
        val w1 = totalCols - w2 - w3

        val c1 = if (col1.length > w1) col1.take(w1) else col1.padEnd(w1)
        val c2 = if (col2.length > w2) col2.take(w2) else col2.padStart(w2)
        val c3 = if (col3.length > w3) col3.take(w3) else col3.padStart(w3)
        line(c1 + c2 + c3)
        return this
    }

    fun build(): ByteArray {
        return buffer.toByteArray()
    }

    companion object {
        fun formatReceipt(saleWithItems: SaleWithItems, config: ReceiptConfig): ByteArray {
            val builder = EscPosBuilder()
            builder.reset()

            val cols = if (config.paperWidthMm == 80) 48 else 32
            val sym = config.currencySymbol

            // Header Section
            builder.alignCenter()
            if (config.showLogo) {
                builder.bold(true)
                builder.line("================================")
                builder.line("       *** [ STORE LOGO ] ***       ")
                builder.line("================================")
            }

            if (config.showHeader) {
                builder.bold(true).textSize(2, 2)
                builder.line(config.shopName)
                builder.bold(false).textSize(1, 1)

                if (config.tagline.isNotBlank()) {
                    builder.line(config.tagline)
                }
            }

            if (config.showAddress && config.address.isNotBlank()) {
                builder.line(config.address)
            }
            if (config.showPhone && config.phone.isNotBlank()) {
                builder.line("Tel: ${config.phone}")
            }
            if (config.taxId.isNotBlank()) {
                builder.line("Tax/NTN: ${config.taxId}")
            }

            builder.feed(1)
            builder.doubleDivider(cols)

            // Invoice Metadata
            builder.alignLeft()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val dateStr = dateFormat.format(Date(saleWithItems.sale.timestamp))
            builder.twoColumn("Invoice:", saleWithItems.sale.invoiceNumber, cols)
            builder.twoColumn("Date:", dateStr, cols)
            if (config.showCustomerInfo) {
                builder.twoColumn("Customer:", saleWithItems.sale.customerName, cols)
            }
            if (config.showPaymentMethod) {
                builder.twoColumn("Payment:", saleWithItems.sale.paymentMethod, cols)
            }

            builder.divider(cols)

            // Item Headers
            builder.bold(true)
            if (cols >= 48) {
                // 80mm format: Item (22) Qty (6) Price (10) Total (10)
                val c1 = "Item Description".padEnd(20)
                val c2 = "Qty".padStart(6)
                val c3 = "Price".padStart(10)
                val c4 = "Total".padStart(12)
                builder.line(c1 + c2 + c3 + c4)
            } else {
                // 58mm format (32 cols): Item (15) Qty (7) Total (10)
                builder.threeColumn("Item", "Qty", "Total", cols)
            }
            builder.bold(false)
            builder.divider(cols)

            // Items List
            for (item in saleWithItems.items) {
                val qtyStr = if (item.quantity % 1.0 == 0.0) "${item.quantity.toInt()}x" else "%.2fx".format(item.quantity)
                val totalStr = "$sym%.2f".format(item.lineTotal)

                if (cols >= 48) {
                    val itemName = if (item.productName.length > 20) item.productName.take(19) + "." else item.productName.padEnd(20)
                    val priceStr = "$sym%.2f".format(item.unitPrice).padStart(10)
                    builder.line(itemName + qtyStr.padStart(6) + priceStr + totalStr.padStart(12))
                } else {
                    builder.threeColumn(item.productName, qtyStr, totalStr, cols)
                }

                if (item.discountAmount > 0) {
                    builder.alignRight()
                    builder.line("  (Disc -$sym%.2f)".format(item.discountAmount))
                    builder.alignLeft()
                }
            }

            builder.divider(cols)

            // Financial Summary
            builder.alignLeft()
            builder.twoColumn("Subtotal:", "$sym%.2f".format(saleWithItems.sale.subtotal), cols)
            if (saleWithItems.sale.discountAmount > 0) {
                builder.twoColumn("Discount:", "-$sym%.2f".format(saleWithItems.sale.discountAmount), cols)
            }
            if (config.showTaxBreakdown && saleWithItems.sale.taxAmount > 0) {
                builder.twoColumn("Tax / VAT:", "+$sym%.2f".format(saleWithItems.sale.taxAmount), cols)
            }

            builder.doubleDivider(cols)
            builder.bold(true).textSize(1, 2)
            builder.twoColumn("TOTAL:", "$sym%.2f".format(saleWithItems.sale.grandTotal), cols)
            builder.bold(false).textSize(1, 1)

            builder.twoColumn("Paid (${saleWithItems.sale.paymentMethod}):", "$sym%.2f".format(saleWithItems.sale.amountPaid), cols)
            if (saleWithItems.sale.changeDue > 0) {
                builder.twoColumn("Change Due:", "$sym%.2f".format(saleWithItems.sale.changeDue), cols)
            }

            builder.divider(cols)

            // Footer Message & QR / Barcode
            builder.alignCenter()
            if (config.showFooter && config.footerMessage.isNotBlank()) {
                builder.line(config.footerMessage)
            }
            if (config.showReturnPolicy && config.returnPolicy.isNotBlank()) {
                builder.line(config.returnPolicy)
            }

            if (config.showQrCode) {
                builder.feed(1)
                builder.qrCode("INV:${saleWithItems.sale.invoiceNumber}|TOTAL:${saleWithItems.sale.grandTotal}|DATE:$dateStr", moduleSize = if (cols >= 48) 5 else 4)
                builder.line("Scan to verify receipt")
            }

            builder.cutPaper()
            return builder.build()
        }

        fun generateTestReceipt(config: ReceiptConfig): ByteArray {
            val builder = EscPosBuilder()
            val cols = if (config.paperWidthMm == 80) 48 else 32
            val sym = config.currencySymbol

            builder.reset()
            builder.alignCenter()
            builder.bold(true).textSize(2, 2)
            builder.line(config.shopName)
            builder.bold(false).textSize(1, 1)
            builder.line("ESC/POS Thermal Test Print")
            builder.line("Paper Width: ${config.paperWidthMm}mm ($cols columns)")
            builder.doubleDivider(cols)

            builder.alignLeft()
            builder.line("Left Aligned Text")
            builder.alignCenter()
            builder.line("Center Aligned Text")
            builder.alignRight()
            builder.line("Right Aligned Text")
            builder.alignLeft()

            builder.divider(cols)
            builder.bold(true)
            builder.twoColumn("BOLD TEST", "SUCCESS", cols)
            builder.bold(false)
            builder.twoColumn("Sample Item 1", "${sym}12.50", cols)
            builder.twoColumn("Sample Item 2", "${sym}8.00", cols)
            builder.doubleDivider(cols)
            builder.twoColumn("TOTAL", "${sym}20.50", cols)
            builder.divider(cols)

            builder.alignCenter()
            builder.line("Thermal Printer Connected OK")
            builder.qrCode("NexaPOS-TEST-${System.currentTimeMillis()}", moduleSize = 4)
            builder.cutPaper()

            return builder.build()
        }
    }
}
