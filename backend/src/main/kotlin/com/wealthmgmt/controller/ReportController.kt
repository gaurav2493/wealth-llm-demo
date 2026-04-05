package com.wealthmgmt.controller

import com.wealthmgmt.service.ReportService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/reports")
class ReportController(private val reportService: ReportService) {

    @GetMapping("/holdings/{clientId}")
    fun holdingsReport(@PathVariable clientId: Long): ResponseEntity<ByteArray> =
        pdfResponse(reportService.generateHoldingsReport(clientId), "holdings_$clientId.pdf")

    @GetMapping("/transactions/{clientId}")
    fun transactionReport(@PathVariable clientId: Long): ResponseEntity<ByteArray> =
        pdfResponse(reportService.generateTransactionReport(clientId), "transactions_$clientId.pdf")

    @GetMapping("/capital-gains/{clientId}")
    fun capitalGainsReport(@PathVariable clientId: Long): ResponseEntity<ByteArray> =
        pdfResponse(reportService.generateCapitalGainsReport(clientId), "capital_gains_$clientId.pdf")

    private fun pdfResponse(bytes: ByteArray, filename: String): ResponseEntity<ByteArray> =
        ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$filename")
            .contentType(MediaType.APPLICATION_PDF)
            .body(bytes)
}
