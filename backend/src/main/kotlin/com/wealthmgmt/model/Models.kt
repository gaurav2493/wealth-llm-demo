package com.wealthmgmt.model

import java.math.BigDecimal
import java.time.LocalDate

data class Admin(val id: Long = 0, val username: String, val passwordHash: String)

data class Client(val id: Long = 0, val name: String, val email: String? = null, val phone: String? = null)

data class Scheme(val id: Long = 0, val name: String, val type: String? = null, val nav: BigDecimal = BigDecimal.ZERO)

data class Transaction(
    val id: Long = 0,
    val clientId: Long,
    val schemeId: Long,
    val type: String,
    val units: BigDecimal,
    val amount: BigDecimal,
    val date: LocalDate = LocalDate.now()
)

data class Holding(
    val clientId: Long,
    val clientName: String,
    val schemeId: Long,
    val schemeName: String,
    val units: BigDecimal,
    val holdingValue: BigDecimal
)

data class LoginRequest(val username: String, val password: String)
data class ApiResponse(val message: String, val success: Boolean = true)

data class CapitalGain(
    val schemeName: String,
    val buyDate: LocalDate,
    val sellDate: LocalDate,
    val units: BigDecimal,
    val buyAmount: BigDecimal,
    val sellAmount: BigDecimal,
    val gain: BigDecimal
)

data class ChatRequest(val message: String)

data class ChatResponse(
    val reply: String,
    val data: List<Map<String, Any?>>? = null
)

enum class Intent {
    CLIENTS, SCHEMES, TRANSACTIONS, HOLDINGS, REPORT, UNKNOWN
}

data class IntentResult(
    val intent: Intent,
    val params: Map<String, String?> = emptyMap()
)
