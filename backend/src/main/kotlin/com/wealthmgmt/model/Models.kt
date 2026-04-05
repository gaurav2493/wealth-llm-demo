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
