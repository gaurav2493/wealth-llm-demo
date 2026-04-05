package com.wealthmgmt.service

import com.lowagie.text.*
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import com.wealthmgmt.model.CapitalGain
import com.wealthmgmt.model.Holding
import com.wealthmgmt.model.Transaction
import com.wealthmgmt.repository.HoldingRepository
import com.wealthmgmt.repository.TransactionRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
class ReportService(
    private val holdingRepo: HoldingRepository,
    private val txRepo: TransactionRepository,
    private val jdbc: JdbcTemplate
) {

    private val headerFont = Font(Font.HELVETICA, 14f, Font.BOLD)
    private val cellFont = Font(Font.HELVETICA, 10f, Font.NORMAL)
    private val headerCellFont = Font(Font.HELVETICA, 10f, Font.BOLD, Color.WHITE)

    private fun headerCell(text: String): PdfPCell {
        val cell = PdfPCell(Phrase(text, headerCellFont))
        cell.backgroundColor = Color(33, 118, 210)
        cell.horizontalAlignment = Element.ALIGN_CENTER
        cell.setPadding(6f)
        return cell
    }

    private fun cell(text: String): PdfPCell {
        val c = PdfPCell(Phrase(text, cellFont))
        c.setPadding(5f)
        return c
    }

    private fun clientName(clientId: Long): String =
        jdbc.queryForObject("SELECT name FROM client WHERE id = ?", String::class.java, clientId) ?: "Unknown"

    fun generateHoldingsReport(clientId: Long): ByteArray {
        val holdings = holdingRepo.getHoldings(clientId, null, "value", "desc", null)
        val name = clientName(clientId)
        val doc = Document(PageSize.A4)
        val out = ByteArrayOutputStream()
        PdfWriter.getInstance(doc, out)
        doc.open()
        doc.add(Paragraph("Holdings Report — $name", headerFont))
        doc.add(Paragraph("Generated: ${LocalDate.now()}", cellFont))
        doc.add(Paragraph(" "))

        val table = PdfPTable(4)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(3f, 3f, 2f, 2f))
        listOf("Client", "Scheme", "Units", "Value").forEach { table.addCell(headerCell(it)) }
        holdings.forEach { h ->
            table.addCell(cell(h.clientName))
            table.addCell(cell(h.schemeName))
            table.addCell(cell(h.units.setScale(4, RoundingMode.HALF_UP).toString()))
            table.addCell(cell(h.holdingValue.setScale(2, RoundingMode.HALF_UP).toString()))
        }
        doc.add(table)
        doc.close()
        return out.toByteArray()
    }

    fun generateTransactionReport(clientId: Long): ByteArray {
        val transactions = txRepo.findByClientId(clientId)
        val name = clientName(clientId)
        val schemeNames = jdbc.query("SELECT id, name FROM scheme") { rs, _ ->
            rs.getLong("id") to rs.getString("name")
        }.toMap()

        val doc = Document(PageSize.A4)
        val out = ByteArrayOutputStream()
        PdfWriter.getInstance(doc, out)
        doc.open()
        doc.add(Paragraph("Transaction Report — $name", headerFont))
        doc.add(Paragraph("Generated: ${LocalDate.now()}", cellFont))
        doc.add(Paragraph(" "))

        val table = PdfPTable(5)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(2f, 3f, 1.5f, 2f, 2f))
        listOf("Date", "Scheme", "Type", "Units", "Amount").forEach { table.addCell(headerCell(it)) }
        transactions.forEach { t ->
            table.addCell(cell(t.date.toString()))
            table.addCell(cell(schemeNames[t.schemeId] ?: "Unknown"))
            table.addCell(cell(t.type.uppercase()))
            table.addCell(cell(t.units.setScale(4, RoundingMode.HALF_UP).toString()))
            table.addCell(cell(t.amount.setScale(2, RoundingMode.HALF_UP).toString()))
        }
        doc.add(table)
        doc.close()
        return out.toByteArray()
    }

    fun generateCapitalGainsReport(clientId: Long): ByteArray {
        val gains = computeCapitalGains(clientId)
        val name = clientName(clientId)

        val doc = Document(PageSize.A4)
        val out = ByteArrayOutputStream()
        PdfWriter.getInstance(doc, out)
        doc.open()
        doc.add(Paragraph("Capital Gains Report — $name", headerFont))
        doc.add(Paragraph("Generated: ${LocalDate.now()}", cellFont))
        doc.add(Paragraph(" "))

        val table = PdfPTable(7)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(2.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f))
        listOf("Scheme", "Buy Date", "Sell Date", "Units", "Buy Amt", "Sell Amt", "Gain").forEach { table.addCell(headerCell(it)) }
        var totalGain = BigDecimal.ZERO
        gains.forEach { g ->
            table.addCell(cell(g.schemeName))
            table.addCell(cell(g.buyDate.toString()))
            table.addCell(cell(g.sellDate.toString()))
            table.addCell(cell(g.units.setScale(4, RoundingMode.HALF_UP).toString()))
            table.addCell(cell(g.buyAmount.setScale(2, RoundingMode.HALF_UP).toString()))
            table.addCell(cell(g.sellAmount.setScale(2, RoundingMode.HALF_UP).toString()))
            table.addCell(cell(g.gain.setScale(2, RoundingMode.HALF_UP).toString()))
            totalGain = totalGain.add(g.gain)
        }
        doc.add(table)
        doc.add(Paragraph(" "))
        doc.add(Paragraph("Total Capital Gain: ${totalGain.setScale(2, RoundingMode.HALF_UP)}", headerFont))
        doc.close()
        return out.toByteArray()
    }

    private fun computeCapitalGains(clientId: Long): List<CapitalGain> {
        val transactions = txRepo.findByClientId(clientId)
        val schemeNames = jdbc.query("SELECT id, name FROM scheme") { rs, _ ->
            rs.getLong("id") to rs.getString("name")
        }.toMap()

        // FIFO-based capital gains: match sells against buys per scheme
        data class BuyLot(val date: LocalDate, var units: BigDecimal, val pricePerUnit: BigDecimal)

        val buyLots = mutableMapOf<Long, MutableList<BuyLot>>()
        val gains = mutableListOf<CapitalGain>()

        for (tx in transactions) {
            if (tx.type == "buy") {
                buyLots.getOrPut(tx.schemeId) { mutableListOf() }
                    .add(BuyLot(tx.date, tx.units, tx.amount.divide(tx.units, 6, RoundingMode.HALF_UP)))
            } else {
                var remaining = tx.units
                val sellPricePerUnit = tx.amount.divide(tx.units, 6, RoundingMode.HALF_UP)
                val lots = buyLots[tx.schemeId] ?: continue
                while (remaining > BigDecimal.ZERO && lots.isNotEmpty()) {
                    val lot = lots.first()
                    val matched = remaining.min(lot.units)
                    val buyAmt = matched.multiply(lot.pricePerUnit).setScale(2, RoundingMode.HALF_UP)
                    val sellAmt = matched.multiply(sellPricePerUnit).setScale(2, RoundingMode.HALF_UP)
                    gains.add(CapitalGain(
                        schemeName = schemeNames[tx.schemeId] ?: "Unknown",
                        buyDate = lot.date, sellDate = tx.date,
                        units = matched, buyAmount = buyAmt, sellAmount = sellAmt,
                        gain = sellAmt.subtract(buyAmt)
                    ))
                    lot.units = lot.units.subtract(matched)
                    if (lot.units.compareTo(BigDecimal.ZERO) == 0) lots.removeFirst()
                    remaining = remaining.subtract(matched)
                }
            }
        }
        return gains
    }
}
