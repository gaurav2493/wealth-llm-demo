package com.wealthmgmt.service

import com.wealthmgmt.llm.LlmProvider
import com.wealthmgmt.model.ChatResponse
import com.wealthmgmt.model.Intent
import com.wealthmgmt.repository.ClientRepository
import com.wealthmgmt.repository.HoldingRepository
import com.wealthmgmt.repository.SchemeRepository
import com.wealthmgmt.repository.TransactionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ChatService(
    private val llmProvider: LlmProvider,
    private val clientRepo: ClientRepository,
    private val schemeRepo: SchemeRepository,
    private val transactionRepo: TransactionRepository,
    private val holdingRepo: HoldingRepository
) {
    private val logger = LoggerFactory.getLogger(ChatService::class.java)

    fun processMessage(message: String): ChatResponse {
        val intentResult = try {
            llmProvider.resolveIntent(message)
        } catch (e: Exception) {
            logger.error("LLM provider error: ${e.message}", e)
            return ChatResponse(reply = "Sorry, I'm having trouble understanding your request right now. Please try again.")
        }

        return try {
            val params = intentResult.params
            when (intentResult.intent) {
                Intent.CLIENTS -> {
                    val clientName = params["clientName"]
                    val clients = clientRepo.findAll().let { all ->
                        if (!clientName.isNullOrBlank()) {
                            all.filter { it.name.contains(clientName, ignoreCase = true) }
                        } else all
                    }
                    val data = clients.map { c ->
                        mapOf<String, Any?>(
                            "id" to c.id,
                            "name" to c.name,
                            "email" to c.email,
                            "phone" to c.phone
                        )
                    }
                    val reply = if (!clientName.isNullOrBlank()) {
                        "Here are clients matching '$clientName':"
                    } else {
                        "Here are all clients:"
                    }
                    ChatResponse(reply = reply, data = data)
                }

                Intent.SCHEMES -> {
                    val schemeName = params["schemeName"]
                    val schemeType = params["schemeType"]
                    val schemes = schemeRepo.findAll().let { all ->
                        var filtered = all
                        if (!schemeName.isNullOrBlank()) {
                            filtered = filtered.filter { it.name.contains(schemeName, ignoreCase = true) }
                        }
                        if (!schemeType.isNullOrBlank()) {
                            filtered = filtered.filter { it.type?.contains(schemeType, ignoreCase = true) == true }
                        }
                        filtered
                    }
                    val data = schemes.map { s ->
                        mapOf<String, Any?>(
                            "id" to s.id,
                            "name" to s.name,
                            "type" to s.type,
                            "nav" to s.nav
                        )
                    }
                    val filterParts = mutableListOf<String>()
                    if (!schemeName.isNullOrBlank()) filterParts.add("name '$schemeName'")
                    if (!schemeType.isNullOrBlank()) filterParts.add("type '$schemeType'")
                    val reply = if (filterParts.isNotEmpty()) {
                        "Here are schemes matching ${filterParts.joinToString(" and ")}:"
                    } else {
                        "Here are all schemes:"
                    }
                    ChatResponse(reply = reply, data = data)
                }

                Intent.TRANSACTIONS -> {
                    val transactionType = params["transactionType"]
                    val clientName = params["clientName"]
                    var transactions = transactionRepo.findAll()

                    if (!transactionType.isNullOrBlank()) {
                        transactions = transactions.filter { it.type.equals(transactionType, ignoreCase = true) }
                    }
                    if (!clientName.isNullOrBlank()) {
                        val clientId = resolveClientId(clientName)
                        if (clientId != null) {
                            transactions = transactions.filter { it.clientId == clientId }
                        }
                    }

                    val data = transactions.map { t ->
                        mapOf<String, Any?>(
                            "id" to t.id,
                            "clientId" to t.clientId,
                            "schemeId" to t.schemeId,
                            "type" to t.type,
                            "units" to t.units,
                            "amount" to t.amount,
                            "date" to t.date
                        )
                    }
                    val filterParts = mutableListOf<String>()
                    if (!transactionType.isNullOrBlank()) filterParts.add("type '$transactionType'")
                    if (!clientName.isNullOrBlank()) filterParts.add("client '$clientName'")
                    val reply = if (filterParts.isNotEmpty()) {
                        "Here are transactions filtered by ${filterParts.joinToString(" and ")}:"
                    } else {
                        "Here are all transactions:"
                    }
                    ChatResponse(reply = reply, data = data)
                }

                Intent.HOLDINGS -> {
                    val clientName = params["clientName"]
                    val schemeName = params["schemeName"]
                    val sortBy = params["sortBy"]
                    val sortOrder = params["sortOrder"]

                    val clientId = if (!clientName.isNullOrBlank()) resolveClientId(clientName) else null
                    val schemeId = if (!schemeName.isNullOrBlank()) resolveSchemeId(schemeName) else null

                    val holdings = holdingRepo.getHoldings(clientId, schemeId, sortBy, sortOrder, null)
                    val data = holdings.map { h ->
                        mapOf<String, Any?>(
                            "clientName" to h.clientName,
                            "schemeName" to h.schemeName,
                            "units" to h.units,
                            "holdingValue" to h.holdingValue
                        )
                    }
                    val filterParts = mutableListOf<String>()
                    if (!clientName.isNullOrBlank()) filterParts.add("client '$clientName'")
                    if (!schemeName.isNullOrBlank()) filterParts.add("scheme '$schemeName'")
                    if (!sortBy.isNullOrBlank()) {
                        val dir = if (sortOrder == "desc") "descending" else "ascending"
                        filterParts.add("sorted by $sortBy $dir")
                    }
                    val reply = if (filterParts.isNotEmpty()) {
                        "Here are holdings for ${filterParts.joinToString(", ")}:"
                    } else {
                        "Here are all holdings:"
                    }
                    ChatResponse(reply = reply, data = data)
                }

                Intent.REPORT -> {
                    val clientName = params["clientName"]
                    if (!clientName.isNullOrBlank()) {
                        val clientId = resolveClientId(clientName)
                        if (clientId != null) {
                            val data = listOf(
                                mapOf("reportType" to "Holdings", "url" to "/api/reports/holdings/$clientId"),
                                mapOf("reportType" to "Transactions", "url" to "/api/reports/transactions/$clientId"),
                                mapOf("reportType" to "Capital Gains", "url" to "/api/reports/capital-gains/$clientId")
                            )
                            ChatResponse(reply = "Here are the report download links for $clientName:", data = data)
                        } else {
                            ChatResponse(reply = "No client found matching '$clientName'. Please check the name and try again.")
                        }
                    } else {
                        ChatResponse(reply = "To generate reports, please specify a client name. For example: 'generate report for Alice'")
                    }
                }

                Intent.UNKNOWN -> {
                    ChatResponse(reply = "I can help you query: clients, schemes, transactions, holdings, and reports. Try asking something like 'show me all clients' or 'what are the holdings for Alice?'")
                }
            }
        } catch (e: Exception) {
            logger.error("Error querying data: ${e.message}", e)
            ChatResponse(reply = "Sorry, I encountered an error while querying data: ${e.message}")
        }
    }

    private fun resolveClientId(clientName: String): Long? {
        val clients = clientRepo.findAll()
        return clients.firstOrNull { it.name.contains(clientName, ignoreCase = true) }?.id
    }

    private fun resolveSchemeId(schemeName: String): Long? {
        val schemes = schemeRepo.findAll()
        return schemes.firstOrNull { it.name.contains(schemeName, ignoreCase = true) }?.id
    }
}
