package com.wealthmgmt.mcp

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper
import io.modelcontextprotocol.server.McpServer
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider
import io.modelcontextprotocol.spec.McpSchema
import io.modelcontextprotocol.spec.McpSchema.CallToolResult
import io.modelcontextprotocol.spec.McpSchema.JsonSchema
import io.modelcontextprotocol.spec.McpSchema.Tool

val api = ApiClient()

fun main() {
    val objectMapper = ObjectMapper().registerKotlinModule()
    val jsonMapper = JacksonMcpJsonMapper(objectMapper)
    val transport = StdioServerTransportProvider(jsonMapper)

    val server = McpServer.sync(transport)
        .serverInfo("wealth-management-mcp", "1.0.0")
        .capabilities(
            McpSchema.ServerCapabilities.builder()
                .tools(true)
                .build()
        )
        .build()

    server.addTool(listClientsTool())
    server.addTool(listSchemesTool())
    server.addTool(listTransactionsTool())
    server.addTool(getHoldingsTool())
    server.addTool(getHoldingsFilteredTool())

    Thread.currentThread().join()
}

private fun textResult(text: String): CallToolResult =
    CallToolResult.builder()
        .content(listOf(McpSchema.TextContent(text)))
        .build()

private fun emptyJsonSchema(): JsonSchema =
    JsonSchema("object", emptyMap(), emptyList(), null, null, null)

private fun listClientsTool(): SyncToolSpecification =
    SyncToolSpecification.builder()
        .tool(
            Tool.builder()
                .name("list_clients")
                .description("List all clients in the wealth management system. Returns id, name, email, phone for each client.")
                .inputSchema(emptyJsonSchema())
                .build()
        )
        .callHandler { _, _ -> textResult(api.getJson("/api/clients")) }
        .build()

private fun listSchemesTool(): SyncToolSpecification =
    SyncToolSpecification.builder()
        .tool(
            Tool.builder()
                .name("list_schemes")
                .description("List all investment schemes. Returns id, name, type, nav (net asset value) for each scheme.")
                .inputSchema(emptyJsonSchema())
                .build()
        )
        .callHandler { _, _ -> textResult(api.getJson("/api/schemes")) }
        .build()

private fun listTransactionsTool(): SyncToolSpecification =
    SyncToolSpecification.builder()
        .tool(
            Tool.builder()
                .name("list_transactions")
                .description("List all transactions. Returns id, clientId, schemeId, type (buy/sell), units, amount, date.")
                .inputSchema(emptyJsonSchema())
                .build()
        )
        .callHandler { _, _ -> textResult(api.getJson("/api/transactions")) }
        .build()

private fun getHoldingsTool(): SyncToolSpecification =
    SyncToolSpecification.builder()
        .tool(
            Tool.builder()
                .name("get_holdings")
                .description("Get all current holdings aggregated from transactions. Returns clientName, schemeName, units, holdingValue.")
                .inputSchema(emptyJsonSchema())
                .build()
        )
        .callHandler { _, _ -> textResult(api.getJson("/api/holdings")) }
        .build()

private fun getHoldingsFilteredTool(): SyncToolSpecification {
    val schema = JsonSchema(
        "object",
        mapOf(
            "clientId" to mapOf("type" to "integer", "description" to "Filter by client ID"),
            "schemeId" to mapOf("type" to "integer", "description" to "Filter by scheme ID"),
            "sortBy" to mapOf("type" to "string", "enum" to listOf("value"), "description" to "Sort field"),
            "sortOrder" to mapOf("type" to "string", "enum" to listOf("asc", "desc"), "description" to "Sort direction"),
            "asOfDate" to mapOf("type" to "string", "description" to "Holdings as of date (YYYY-MM-DD)")
        ),
        emptyList(), null, null, null
    )

    return SyncToolSpecification.builder()
        .tool(
            Tool.builder()
                .name("get_holdings_filtered")
                .description("Get holdings with optional filters: clientId, schemeId, sortBy, sortOrder, asOfDate.")
                .inputSchema(schema)
                .build()
        )
        .callHandler { _, request ->
            val args = request.arguments() ?: emptyMap()
            val params = mutableListOf<String>()
            args["clientId"]?.let { params.add("clientId=$it") }
            args["schemeId"]?.let { params.add("schemeId=$it") }
            args["sortBy"]?.let { params.add("sortBy=$it") }
            args["sortOrder"]?.let { params.add("sortOrder=$it") }
            args["asOfDate"]?.let { params.add("asOfDate=$it") }
            val query = if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
            textResult(api.getJson("/api/holdings$query"))
        }
        .build()
}
