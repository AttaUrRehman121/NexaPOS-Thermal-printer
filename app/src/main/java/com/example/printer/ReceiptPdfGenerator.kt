package com.example.printer

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.dao.SaleWithItems
import com.example.data.entity.ReceiptConfig
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ReceiptPdfGenerator {

    fun generateReceiptPdf(
        context: Context,
        saleWithItems: SaleWithItems,
        config: ReceiptConfig
    ): File? {
        val pageWidth = if (config.paperWidthMm == 80) 576 else 384 // points / thermal width
        // Calculate dynamic height based on item count
        val baseHeight = 480
        val itemsHeight = saleWithItems.items.size * 32
        val pageHeight = baseHeight + itemsHeight

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Background
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        val boldPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val headerPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#1E3A8A")
            textSize = 20f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val centerPaint = Paint().apply {
            isAntiAlias = true
            color = Color.DKGRAY
            textSize = 12f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val rightPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 13f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        var y = 40f
        val centerX = pageWidth / 2f
        val leftX = 20f
        val rightX = pageWidth - 20f

        // Store Header
        if (config.showHeader) {
            canvas.drawText(config.shopName, centerX, y, headerPaint)
            y += 20f
            if (config.tagline.isNotBlank()) {
                canvas.drawText(config.tagline, centerX, y, centerPaint)
                y += 18f
            }
        }
        if (config.showAddress && config.address.isNotBlank()) {
            canvas.drawText(config.address, centerX, y, centerPaint)
            y += 18f
        }
        if (config.showPhone && config.phone.isNotBlank()) {
            canvas.drawText("Tel: ${config.phone}", centerX, y, centerPaint)
            y += 18f
        }
        if (config.taxId.isNotBlank()) {
            canvas.drawText("Tax/NTN: ${config.taxId}", centerX, y, centerPaint)
            y += 20f
        }

        canvas.drawLine(leftX, y, rightX, y, linePaint)
        y += 18f

        // Invoice info
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val dateStr = dateFormat.format(Date(saleWithItems.sale.timestamp))

        canvas.drawText("Invoice: ${saleWithItems.sale.invoiceNumber}", leftX, y, boldPaint.apply { textSize = 13f })
        y += 18f
        canvas.drawText("Date: $dateStr", leftX, y, paint)
        y += 18f
        if (config.showCustomerInfo) {
            canvas.drawText("Customer: ${saleWithItems.sale.customerName}", leftX, y, paint)
            y += 18f
        }
        if (config.showPaymentMethod) {
            canvas.drawText("Payment: ${saleWithItems.sale.paymentMethod}", leftX, y, paint)
            y += 22f
        } else {
            y += 4f
        }

        canvas.drawLine(leftX, y, rightX, y, linePaint)
        y += 16f

        // Table Header
        canvas.drawText("ITEM", leftX, y, boldPaint.apply { textSize = 12f })
        canvas.drawText("QTY", leftX + 160f, y, boldPaint)
        canvas.drawText("TOTAL", rightX, y, rightPaint.apply { typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) })
        y += 12f
        canvas.drawLine(leftX, y, rightX, y, linePaint)
        y += 18f

        // Items
        val sym = config.currencySymbol
        for (item in saleWithItems.items) {
            val itemName = if (item.productName.length > 20) item.productName.take(19) + "…" else item.productName
            val qtyStr = if (item.quantity % 1.0 == 0.0) "${item.quantity.toInt()}x" else "%.2fx".format(item.quantity)
            val totalStr = "$sym%.2f".format(item.lineTotal)

            canvas.drawText(itemName, leftX, y, paint)
            canvas.drawText(qtyStr, leftX + 160f, y, paint)
            canvas.drawText(totalStr, rightX, y, rightPaint.apply { typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL) })
            y += 22f
        }

        canvas.drawLine(leftX, y, rightX, y, linePaint)
        y += 20f

        // Financials
        canvas.drawText("Subtotal", leftX, y, paint)
        canvas.drawText("$sym%.2f".format(saleWithItems.sale.subtotal), rightX, y, rightPaint)
        y += 18f

        if (saleWithItems.sale.discountAmount > 0) {
            canvas.drawText("Discount", leftX, y, paint)
            canvas.drawText("-$sym%.2f".format(saleWithItems.sale.discountAmount), rightX, y, rightPaint)
            y += 18f
        }

        if (config.showTaxBreakdown && saleWithItems.sale.taxAmount > 0) {
            canvas.drawText("Tax / VAT", leftX, y, paint)
            canvas.drawText("+$sym%.2f".format(saleWithItems.sale.taxAmount), rightX, y, rightPaint)
            y += 18f
        }

        canvas.drawLine(leftX, y, rightX, y, linePaint)
        y += 22f

        boldPaint.textSize = 18f
        canvas.drawText("GRAND TOTAL", leftX, y, boldPaint)
        canvas.drawText("$sym%.2f".format(saleWithItems.sale.grandTotal), rightX, y, rightPaint.apply {
            textSize = 18f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        })
        y += 24f

        canvas.drawText("Amount Paid", leftX, y, paint)
        canvas.drawText("$sym%.2f".format(saleWithItems.sale.amountPaid), rightX, y, rightPaint.apply { textSize = 13f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL) })
        y += 18f

        if (saleWithItems.sale.changeDue > 0) {
            canvas.drawText("Change Due", leftX, y, paint)
            canvas.drawText("$sym%.2f".format(saleWithItems.sale.changeDue), rightX, y, rightPaint)
            y += 22f
        }

        canvas.drawLine(leftX, y, rightX, y, linePaint)
        y += 24f

        // Footer
        if (config.footerMessage.isNotBlank()) {
            canvas.drawText(config.footerMessage, centerX, y, centerPaint)
            y += 18f
        }
        if (config.returnPolicy.isNotBlank()) {
            canvas.drawText(config.returnPolicy, centerX, y, centerPaint)
            y += 18f
        }

        document.finishPage(page)

        return try {
            val file = File(context.cacheDir, "${saleWithItems.sale.invoiceNumber}.pdf")
            val outputStream = FileOutputStream(file)
            document.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            document.close()
            file
        } catch (e: Exception) {
            document.close()
            null
        }
    }

    fun sharePdf(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share Receipt PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            // Fallback generic view
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(android.net.Uri.fromFile(file), "application/pdf")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try { context.startActivity(intent) } catch (ignored: Exception) {}
        }
    }
}
