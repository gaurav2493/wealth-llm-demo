package com.wealthmgmt

import com.fasterxml.jackson.databind.ObjectMapper
import com.wealthmgmt.model.Client
import com.wealthmgmt.model.LoginRequest
import com.wealthmgmt.model.Scheme
import com.wealthmgmt.model.Transaction
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ApiTests {

    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var mapper: ObjectMapper

    companion object {
        var session: MockHttpSession? = null
        var clientId: Long = 0
        var schemeId: Long = 0
        var transactionId: Long = 0
    }

    // --- Auth Tests ---

    @Test @Order(1)
    fun `login with invalid credentials returns 401`() {
        mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(LoginRequest("admin", "wrong")))
        ).andExpect(status().isUnauthorized)
    }

    @Test @Order(2)
    fun `unauthenticated request to protected endpoint returns 401`() {
        mvc.perform(get("/api/clients")).andExpect(status().isUnauthorized)
    }

    @Test @Order(3)
    fun `login with valid credentials returns 200 and session`() {
        val result = mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(LoginRequest("admin", "admin123")))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andReturn()
        session = result.request.getSession(false) as MockHttpSession
        assertNotNull(session)
    }

    // --- Client Tests ---

    @Test @Order(10)
    fun `create client`() {
        val result = mvc.perform(
            post("/api/clients").session(session!!)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Client(name = "John Doe", email = "john@example.com", phone = "1234567890")))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("John Doe"))
            .andReturn()
        val body = mapper.readTree(result.response.contentAsString)
        clientId = body["id"].asLong()
        assertTrue(clientId > 0)
    }

    @Test @Order(11)
    fun `list clients`() {
        mvc.perform(get("/api/clients").session(session!!))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("John Doe"))
    }

    // --- Scheme Tests ---

    @Test @Order(20)
    fun `create scheme`() {
        val result = mvc.perform(
            post("/api/schemes").session(session!!)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Scheme(name = "Growth Fund", type = "equity", nav = BigDecimal("150.50"))))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Growth Fund"))
            .andReturn()
        val body = mapper.readTree(result.response.contentAsString)
        schemeId = body["id"].asLong()
        assertTrue(schemeId > 0)
    }

    @Test @Order(21)
    fun `list schemes`() {
        mvc.perform(get("/api/schemes").session(session!!))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("Growth Fund"))
    }

    // --- Transaction Tests ---

    @Test @Order(30)
    fun `create transaction`() {
        val tx = Transaction(
            clientId = clientId, schemeId = schemeId,
            type = "buy", units = BigDecimal("10.0000"), amount = BigDecimal("1505.00"),
            date = LocalDate.of(2026, 1, 15)
        )
        val result = mvc.perform(
            post("/api/transactions").session(session!!)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(tx))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.type").value("buy"))
            .andReturn()
        val body = mapper.readTree(result.response.contentAsString)
        transactionId = body["id"].asLong()
        assertTrue(transactionId > 0)
    }

    @Test @Order(31)
    fun `list transactions`() {
        mvc.perform(get("/api/transactions").session(session!!))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].type").value("buy"))
    }

    @Test @Order(32)
    fun `create transaction with invalid type returns 400`() {
        val tx = Transaction(
            clientId = clientId, schemeId = schemeId,
            type = "invalid", units = BigDecimal("5"), amount = BigDecimal("500"),
            date = LocalDate.now()
        )
        mvc.perform(
            post("/api/transactions").session(session!!)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(tx))
        ).andExpect(status().isBadRequest)
    }

    // --- Holdings Tests ---

    @Test @Order(40)
    fun `get holdings`() {
        mvc.perform(get("/api/holdings").session(session!!))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].clientName").value("John Doe"))
            .andExpect(jsonPath("$[0].schemeName").value("Growth Fund"))
    }

    @Test @Order(41)
    fun `get holdings filtered by client`() {
        mvc.perform(get("/api/holdings?clientId=$clientId").session(session!!))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test @Order(42)
    fun `get holdings filtered by scheme`() {
        mvc.perform(get("/api/holdings?schemeId=$schemeId").session(session!!))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test @Order(43)
    fun `get holdings sorted by value ascending`() {
        mvc.perform(get("/api/holdings?sortBy=value&sortOrder=asc").session(session!!))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].holdingValue").isNumber)
    }

    @Test @Order(44)
    fun `get holdings as of date before transaction returns empty`() {
        mvc.perform(get("/api/holdings?asOfDate=2025-12-31").session(session!!))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test @Order(45)
    fun `get holdings as of date on transaction date returns results`() {
        mvc.perform(get("/api/holdings?asOfDate=2026-01-15").session(session!!))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].clientName").value("John Doe"))
    }

    // --- Report Tests ---

    @Test @Order(46)
    fun `download holdings report PDF`() {
        val result = mvc.perform(get("/api/reports/holdings/$clientId").session(session!!))
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/pdf"))
            .andReturn()
        assertTrue(result.response.contentAsByteArray.size > 100)
        assertTrue(result.response.getHeader("Content-Disposition")!!.contains("holdings_"))
    }

    @Test @Order(47)
    fun `download transaction report PDF`() {
        val result = mvc.perform(get("/api/reports/transactions/$clientId").session(session!!))
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/pdf"))
            .andReturn()
        assertTrue(result.response.contentAsByteArray.size > 100)
        assertTrue(result.response.getHeader("Content-Disposition")!!.contains("transactions_"))
    }

    @Test @Order(48)
    fun `download capital gains report PDF`() {
        val result = mvc.perform(get("/api/reports/capital-gains/$clientId").session(session!!))
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/pdf"))
            .andReturn()
        assertTrue(result.response.contentAsByteArray.size > 100)
        assertTrue(result.response.getHeader("Content-Disposition")!!.contains("capital_gains_"))
    }

    // --- Delete Tests ---

    @Test @Order(50)
    fun `delete transaction`() {
        mvc.perform(delete("/api/transactions/$transactionId").session(session!!))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test @Order(51)
    fun `delete nonexistent transaction returns 404`() {
        mvc.perform(delete("/api/transactions/99999").session(session!!))
            .andExpect(status().isNotFound)
    }

    @Test @Order(52)
    fun `delete client`() {
        mvc.perform(delete("/api/clients/$clientId").session(session!!))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test @Order(53)
    fun `delete scheme`() {
        mvc.perform(delete("/api/schemes/$schemeId").session(session!!))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    // --- Logout ---

    @Test @Order(60)
    fun `logout`() {
        mvc.perform(post("/api/auth/logout").session(session!!))
            .andExpect(status().isOk)
    }
}
