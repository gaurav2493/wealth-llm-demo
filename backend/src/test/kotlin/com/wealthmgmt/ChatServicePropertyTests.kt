package com.wealthmgmt

import com.wealthmgmt.llm.LlmProvider
import com.wealthmgmt.model.*
import com.wealthmgmt.repository.ClientRepository
import com.wealthmgmt.repository.HoldingRepository
import com.wealthmgmt.repository.SchemeRepository
import com.wealthmgmt.repository.TransactionRepository
import com.wealthmgmt.service.ChatService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeMonotonicallyDecreasing
import io.kotest.matchers.collections.shouldBeMonotonicallyIncreasing
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.string
import io.kotest.property.forAll
import io.mockk.*
import java.math.BigDecimal
import java.time.LocalDate

// Feature: admin-chat-data-query, ChatService Property Tests
class ChatServicePropertyTests : FunSpec({

    // **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**
    test("Property 1: Intent routing correctness - correct repository is invoked for each intent") {
        forAll(PropTestConfig(iterations = 100), Arb.enum<Intent>()) { intent ->
            val llmProvider = mockk<LlmProvider>()
            val clientRepo = mockk<ClientRepository>()
            val schemeRepo = mockk<SchemeRepository>()
            val transactionRepo = mockk<TransactionRepository>()
            val holdingRepo = mockk<HoldingRepository>()

            every { llmProvider.resolveIntent(any()) } returns IntentResult(intent)
            every { clientRepo.findAll() } returns emptyList()
            every { schemeRepo.findAll() } returns emptyList()
            every { transactionRepo.findAll() } returns emptyList()
            every { holdingRepo.getHoldings(any(), any(), any(), any(), any()) } returns emptyList()

            val chatService = ChatService(llmProvider, clientRepo, schemeRepo, transactionRepo, holdingRepo)
            val response = chatService.processMessage("test message")

            when (intent) {
                Intent.CLIENTS -> {
                    verify(exactly = 1) { clientRepo.findAll() }
                    verify(exactly = 0) { schemeRepo.findAll() }
                    verify(exactly = 0) { transactionRepo.findAll() }
                    verify(exactly = 0) { holdingRepo.getHoldings(any(), any(), any(), any(), any()) }
                }
                Intent.SCHEMES -> {
                    verify(exactly = 0) { clientRepo.findAll() }
                    verify(exactly = 1) { schemeRepo.findAll() }
                    verify(exactly = 0) { transactionRepo.findAll() }
                    verify(exactly = 0) { holdingRepo.getHoldings(any(), any(), any(), any(), any()) }
                }
                Intent.TRANSACTIONS -> {
                    verify(exactly = 0) { clientRepo.findAll() }
                    verify(exactly = 0) { schemeRepo.findAll() }
                    verify(exactly = 1) { transactionRepo.findAll() }
                    verify(exactly = 0) { holdingRepo.getHoldings(any(), any(), any(), any(), any()) }
                }
                Intent.HOLDINGS -> {
                    verify(exactly = 0) { clientRepo.findAll() }
                    verify(exactly = 0) { schemeRepo.findAll() }
                    verify(exactly = 0) { transactionRepo.findAll() }
                    verify(exactly = 1) { holdingRepo.getHoldings(any(), any(), any(), any(), any()) }
                }
                Intent.REPORT -> {
                    verify(exactly = 0) { clientRepo.findAll() }
                    verify(exactly = 0) { schemeRepo.findAll() }
                    verify(exactly = 0) { transactionRepo.findAll() }
                    verify(exactly = 0) { holdingRepo.getHoldings(any(), any(), any(), any(), any()) }
                    response.reply.contains("specify a client", ignoreCase = true) shouldBe true
                }
                Intent.UNKNOWN -> {
                    verify(exactly = 0) { clientRepo.findAll() }
                    verify(exactly = 0) { schemeRepo.findAll() }
                    verify(exactly = 0) { transactionRepo.findAll() }
                    verify(exactly = 0) { holdingRepo.getHoldings(any(), any(), any(), any(), any()) }
                    // Help text should mention supported query types
                    response.reply.contains("clients", ignoreCase = true) shouldBe true
                }
            }

            response shouldNotBe null
            true
        }
    }

    // **Validates: Requirements 9.2**
    test("Property 3: Response structure invariant - processMessage always returns non-blank reply") {
        forAll(PropTestConfig(iterations = 100), Arb.string(1..50), Arb.enum<Intent>()) { message, intent ->
            val llmProvider = mockk<LlmProvider>()
            val clientRepo = mockk<ClientRepository>()
            val schemeRepo = mockk<SchemeRepository>()
            val transactionRepo = mockk<TransactionRepository>()
            val holdingRepo = mockk<HoldingRepository>()

            every { llmProvider.resolveIntent(any()) } returns IntentResult(intent)
            every { clientRepo.findAll() } returns emptyList()
            every { schemeRepo.findAll() } returns emptyList()
            every { transactionRepo.findAll() } returns emptyList()
            every { holdingRepo.getHoldings(any(), any(), any(), any(), any()) } returns emptyList()

            val chatService = ChatService(llmProvider, clientRepo, schemeRepo, transactionRepo, holdingRepo)
            val response = chatService.processMessage(message)

            response.reply.shouldNotBeBlank()
            true
        }
    }

    // **Validates: Requirements 11.3**
    test("Property 9: Chat service error containment - LLM provider exceptions are caught") {
        val exceptionArb = Arb.of(
            RuntimeException("LLM timeout"),
            IllegalStateException("Bad state"),
            IllegalArgumentException("Invalid arg"),
            NullPointerException("Null ref"),
            UnsupportedOperationException("Not supported")
        )

        forAll(PropTestConfig(iterations = 100), exceptionArb) { exception ->
            val llmProvider = mockk<LlmProvider>()
            val clientRepo = mockk<ClientRepository>()
            val schemeRepo = mockk<SchemeRepository>()
            val transactionRepo = mockk<TransactionRepository>()
            val holdingRepo = mockk<HoldingRepository>()

            every { llmProvider.resolveIntent(any()) } throws exception

            val chatService = ChatService(llmProvider, clientRepo, schemeRepo, transactionRepo, holdingRepo)
            val response = chatService.processMessage("test message")

            response.reply.shouldNotBeBlank()
            true
        }
    }

    // **Validates: Requirements 11.3**
    test("Property 9: Chat service error containment - repository exceptions are caught") {
        val intentAndExceptionArb = Arb.of(
            Pair(Intent.CLIENTS, RuntimeException("DB connection lost")),
            Pair(Intent.SCHEMES, IllegalStateException("Schema error")),
            Pair(Intent.TRANSACTIONS, RuntimeException("Query timeout")),
            Pair(Intent.HOLDINGS, IllegalArgumentException("Invalid param"))
        )

        forAll(PropTestConfig(iterations = 100), intentAndExceptionArb) { (intent, exception) ->
            val llmProvider = mockk<LlmProvider>()
            val clientRepo = mockk<ClientRepository>()
            val schemeRepo = mockk<SchemeRepository>()
            val transactionRepo = mockk<TransactionRepository>()
            val holdingRepo = mockk<HoldingRepository>()

            every { llmProvider.resolveIntent(any()) } returns IntentResult(intent)
            every { clientRepo.findAll() } throws exception
            every { schemeRepo.findAll() } throws exception
            every { transactionRepo.findAll() } throws exception
            every { holdingRepo.getHoldings(any(), any(), any(), any(), any()) } throws exception

            val chatService = ChatService(llmProvider, clientRepo, schemeRepo, transactionRepo, holdingRepo)
            val response = chatService.processMessage("test message")

            response.reply.shouldNotBeBlank()
            true
        }
    }

    // **Validates: Requirements 4.2, 5.2, 6.2, 7.2**
    test("Property 4: Filtered query results contain only matching records") {
        val testClients = listOf(
            Client(1, "Alice Smith", "alice@test.com", "111-0001"),
            Client(2, "Bob Jones", "bob@test.com", "222-0002"),
            Client(3, "Alice Johnson", "alicej@test.com", "333-0003"),
            Client(4, "Charlie Brown", "charlie@test.com", "444-0004")
        )
        val testSchemes = listOf(
            Scheme(1, "Growth Fund", "equity", BigDecimal("150.00")),
            Scheme(2, "Bond Fund", "debt", BigDecimal("100.00")),
            Scheme(3, "Growth Plus", "equity", BigDecimal("200.00")),
            Scheme(4, "Liquid Fund", "debt", BigDecimal("50.00"))
        )
        val testTransactions = listOf(
            Transaction(1, 1, 1, "buy", BigDecimal("10"), BigDecimal("1500"), LocalDate.of(2024, 1, 1)),
            Transaction(2, 2, 2, "sell", BigDecimal("5"), BigDecimal("500"), LocalDate.of(2024, 2, 1)),
            Transaction(3, 1, 3, "buy", BigDecimal("20"), BigDecimal("4000"), LocalDate.of(2024, 3, 1)),
            Transaction(4, 3, 1, "sell", BigDecimal("3"), BigDecimal("450"), LocalDate.of(2024, 4, 1))
        )

        // Arb that picks a filter scenario: (intent, params, assertion on data)
        data class FilterScenario(val name: String, val intent: Intent, val params: Map<String, String?>, val check: (List<Map<String, Any?>>) -> Boolean)

        val scenarioArb = Arb.of(
            FilterScenario("clients filtered by Alice", Intent.CLIENTS, mapOf("clientName" to "Alice")) { data ->
                data.all { (it["name"] as String).contains("Alice", ignoreCase = true) }
            },
            FilterScenario("clients filtered by Bob", Intent.CLIENTS, mapOf("clientName" to "Bob")) { data ->
                data.all { (it["name"] as String).contains("Bob", ignoreCase = true) }
            },
            FilterScenario("clients filtered by Charlie", Intent.CLIENTS, mapOf("clientName" to "Charlie")) { data ->
                data.all { (it["name"] as String).contains("Charlie", ignoreCase = true) }
            },
            FilterScenario("schemes filtered by name Growth", Intent.SCHEMES, mapOf("schemeName" to "Growth")) { data ->
                data.all { (it["name"] as String).contains("Growth", ignoreCase = true) }
            },
            FilterScenario("schemes filtered by type equity", Intent.SCHEMES, mapOf("schemeType" to "equity")) { data ->
                data.all { (it["type"] as String).contains("equity", ignoreCase = true) }
            },
            FilterScenario("schemes filtered by type debt", Intent.SCHEMES, mapOf("schemeType" to "debt")) { data ->
                data.all { (it["type"] as String).contains("debt", ignoreCase = true) }
            },
            FilterScenario("transactions filtered by buy", Intent.TRANSACTIONS, mapOf("transactionType" to "buy")) { data ->
                data.all { (it["type"] as String).equals("buy", ignoreCase = true) }
            },
            FilterScenario("transactions filtered by sell", Intent.TRANSACTIONS, mapOf("transactionType" to "sell")) { data ->
                data.all { (it["type"] as String).equals("sell", ignoreCase = true) }
            }
        )

        forAll(PropTestConfig(iterations = 100), scenarioArb) { scenario ->
            val llmProvider = mockk<LlmProvider>()
            val clientRepo = mockk<ClientRepository>()
            val schemeRepo = mockk<SchemeRepository>()
            val transactionRepo = mockk<TransactionRepository>()
            val holdingRepo = mockk<HoldingRepository>()

            every { llmProvider.resolveIntent(any()) } returns IntentResult(scenario.intent, scenario.params)
            every { clientRepo.findAll() } returns testClients
            every { schemeRepo.findAll() } returns testSchemes
            every { transactionRepo.findAll() } returns testTransactions
            every { holdingRepo.getHoldings(any(), any(), any(), any(), any()) } returns emptyList()

            val chatService = ChatService(llmProvider, clientRepo, schemeRepo, transactionRepo, holdingRepo)
            val response = chatService.processMessage("test query")

            response.data != null && scenario.check(response.data!!)
        }
    }

    // **Validates: Requirements 7.3**
    test("Property 5: Holdings sort order") {
        val testHoldingsAsc = listOf(
            Holding(1, "Alice", 1, "Growth Fund", BigDecimal("10"), BigDecimal("1000.00")),
            Holding(2, "Bob", 2, "Bond Fund", BigDecimal("20"), BigDecimal("2000.00")),
            Holding(3, "Charlie", 3, "Equity Fund", BigDecimal("30"), BigDecimal("3000.00"))
        )
        val testHoldingsDesc = testHoldingsAsc.reversed()

        val sortOrderArb = Arb.of("asc", "desc")

        forAll(PropTestConfig(iterations = 100), sortOrderArb) { sortOrder ->
            val llmProvider = mockk<LlmProvider>()
            val clientRepo = mockk<ClientRepository>()
            val schemeRepo = mockk<SchemeRepository>()
            val transactionRepo = mockk<TransactionRepository>()
            val holdingRepo = mockk<HoldingRepository>()

            every { llmProvider.resolveIntent(any()) } returns IntentResult(
                Intent.HOLDINGS,
                mapOf("sortBy" to "value", "sortOrder" to sortOrder)
            )
            // Simulate the repository returning data sorted based on sortOrder
            every { holdingRepo.getHoldings(any(), any(), eq("value"), eq(sortOrder), any()) } answers {
                if (sortOrder == "asc") testHoldingsAsc else testHoldingsDesc
            }

            val chatService = ChatService(llmProvider, clientRepo, schemeRepo, transactionRepo, holdingRepo)
            val response = chatService.processMessage("show holdings sorted by value $sortOrder")

            val values = response.data?.map {
                when (val v = it["holdingValue"]) {
                    is BigDecimal -> v
                    is Number -> BigDecimal(v.toString())
                    else -> BigDecimal.ZERO
                }
            } ?: emptyList()

            if (sortOrder == "asc") {
                values.shouldBeMonotonicallyIncreasing()
            } else {
                values.shouldBeMonotonicallyDecreasing()
            }
            true
        }
    }

    // **Validates: Requirements 8.1, 8.2**
    test("Property 6: Report URL correctness") {
        val testClients = listOf(
            Client(1, "Alice Smith", "alice@test.com", "111-0001"),
            Client(2, "Bob Jones", "bob@test.com", "222-0002"),
            Client(3, "Charlie Brown", "charlie@test.com", "333-0003")
        )

        data class ReportScenario(val clientName: String?, val expectedClientId: Long?)

        val scenarioArb = Arb.of(
            ReportScenario("Alice", 1),
            ReportScenario("Bob", 2),
            ReportScenario("Charlie", 3),
            ReportScenario(null, null)
        )

        forAll(PropTestConfig(iterations = 100), scenarioArb) { scenario ->
            val llmProvider = mockk<LlmProvider>()
            val clientRepo = mockk<ClientRepository>()
            val schemeRepo = mockk<SchemeRepository>()
            val transactionRepo = mockk<TransactionRepository>()
            val holdingRepo = mockk<HoldingRepository>()

            every { llmProvider.resolveIntent(any()) } returns IntentResult(
                Intent.REPORT,
                mapOf("clientName" to scenario.clientName)
            )
            every { clientRepo.findAll() } returns testClients

            val chatService = ChatService(llmProvider, clientRepo, schemeRepo, transactionRepo, holdingRepo)
            val response = chatService.processMessage("generate report")

            if (scenario.clientName != null && scenario.expectedClientId != null) {
                // Should have 3 report URLs with correct client ID
                response.data shouldNotBe null
                response.data!!.size shouldBe 3

                val urls = response.data!!.map { it["url"] as String }
                urls.all { it.contains("/${scenario.expectedClientId}") } shouldBe true

                // Verify all 3 report types are present
                val reportTypes = response.data!!.map { it["reportType"] as String }.toSet()
                reportTypes shouldBe setOf("Holdings", "Transactions", "Capital Gains")
            } else {
                // Should ask for client name, no data
                response.data shouldBe null
                response.reply.lowercase().shouldContain("specify a client")
            }
            true
        }
    }

    // **Validates: Requirements 4.1, 5.1, 6.1, 7.1**
    test("Property 7: Data query results include required fields") {
        val testClients = listOf(
            Client(1, "Alice Smith", "alice@test.com", "111-0001"),
            Client(2, "Bob Jones", "bob@test.com", "222-0002")
        )
        val testSchemes = listOf(
            Scheme(1, "Growth Fund", "equity", BigDecimal("150.00")),
            Scheme(2, "Bond Fund", "debt", BigDecimal("100.00"))
        )
        val testTransactions = listOf(
            Transaction(1, 1, 1, "buy", BigDecimal("10"), BigDecimal("1500"), LocalDate.of(2024, 1, 1)),
            Transaction(2, 2, 2, "sell", BigDecimal("5"), BigDecimal("500"), LocalDate.of(2024, 2, 1))
        )
        val testHoldings = listOf(
            Holding(1, "Alice Smith", 1, "Growth Fund", BigDecimal("10"), BigDecimal("1500.00")),
            Holding(2, "Bob Jones", 2, "Bond Fund", BigDecimal("5"), BigDecimal("500.00"))
        )

        data class FieldScenario(val intent: Intent, val requiredFields: Set<String>)

        val scenarioArb = Arb.of(
            FieldScenario(Intent.CLIENTS, setOf("id", "name", "email", "phone")),
            FieldScenario(Intent.SCHEMES, setOf("id", "name", "type", "nav")),
            FieldScenario(Intent.TRANSACTIONS, setOf("id", "clientId", "schemeId", "type", "units", "amount", "date")),
            FieldScenario(Intent.HOLDINGS, setOf("clientName", "schemeName", "units", "holdingValue"))
        )

        forAll(PropTestConfig(iterations = 100), scenarioArb) { scenario ->
            val llmProvider = mockk<LlmProvider>()
            val clientRepo = mockk<ClientRepository>()
            val schemeRepo = mockk<SchemeRepository>()
            val transactionRepo = mockk<TransactionRepository>()
            val holdingRepo = mockk<HoldingRepository>()

            every { llmProvider.resolveIntent(any()) } returns IntentResult(scenario.intent)
            every { clientRepo.findAll() } returns testClients
            every { schemeRepo.findAll() } returns testSchemes
            every { transactionRepo.findAll() } returns testTransactions
            every { holdingRepo.getHoldings(any(), any(), any(), any(), any()) } returns testHoldings

            val chatService = ChatService(llmProvider, clientRepo, schemeRepo, transactionRepo, holdingRepo)
            val response = chatService.processMessage("test query")

            response.data shouldNotBe null
            response.data!!.isNotEmpty() shouldBe true
            response.data!!.all { record ->
                scenario.requiredFields.all { field -> record.containsKey(field) }
            }
        }
    }
})
