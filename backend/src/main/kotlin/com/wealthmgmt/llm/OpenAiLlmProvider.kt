package com.wealthmgmt.llm

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.wealthmgmt.model.Intent
import com.wealthmgmt.model.IntentResult
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate

class OpenAiLlmProvider(
    private val apiKey: String,
    private val model: String,
    private val endpoint: String,
    private val restTemplate: RestTemplate
) : LlmProvider {

    private val logger = LoggerFactory.getLogger(OpenAiLlmProvider::class.java)
    private val objectMapper = jacksonObjectMapper()

    companion object {
        const val SYSTEM_PROMPT = """You are an intent classifier for a wealth management admin chat. Given the admin's message, determine the intent and extract parameters.

Respond with ONLY a JSON object in this exact format:
{
  "intent": "CLIENTS" | "SCHEMES" | "TRANSACTIONS" | "HOLDINGS" | "REPORT" | "UNKNOWN",
  "params": {
    "clientName": "<extracted client name or null>",
    "schemeName": "<extracted scheme name or null>",
    "schemeType": "<extracted scheme type or null>",
    "transactionType": "<'buy' or 'sell' or null>",
    "sortBy": "<'value' or null>",
    "sortOrder": "<'asc' or 'desc' or null>"
  }
}

Supported intents:
- CLIENTS: queries about clients, client lists, client details
- SCHEMES: queries about schemes, funds, investment options
- TRANSACTIONS: queries about transactions, trades, buys, sells
- HOLDINGS: queries about holdings, portfolio, positions
- REPORT: requests for reports, capital gains reports, download links
- UNKNOWN: anything that doesn't match the above categories"""
    }

    override fun resolveIntent(message: String): IntentResult {
        return try {
            val url = "${endpoint.trimEnd('/')}/chat/completions"

            val requestBody = mapOf(
                "model" to model,
                "messages" to listOf(
                    mapOf("role" to "system", "content" to SYSTEM_PROMPT),
                    mapOf("role" to "user", "content" to message)
                )
            )

            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                setBearerAuth(apiKey)
            }

            val entity = HttpEntity(objectMapper.writeValueAsString(requestBody), headers)
            val response = restTemplate.postForEntity(url, entity, String::class.java)

            val root = objectMapper.readTree(response.body)
            val content = root["choices"][0]["message"]["content"].asText()
            parseIntentResult(content)
        } catch (e: Exception) {
            logger.warn("Failed to parse OpenAI response: ${e.message}")
            IntentResult(Intent.UNKNOWN, emptyMap())
        }
    }

    internal fun parseIntentResult(json: String): IntentResult {
        return try {
            val cleaned = json.trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()
            val tree = objectMapper.readTree(cleaned)
            val intentStr = tree["intent"]?.asText() ?: return IntentResult(Intent.UNKNOWN, emptyMap())
            val intent = try {
                Intent.valueOf(intentStr)
            } catch (e: IllegalArgumentException) {
                logger.warn("Unknown intent value from LLM: $intentStr")
                return IntentResult(Intent.UNKNOWN, emptyMap())
            }

            val params = mutableMapOf<String, String?>()
            val paramsNode = tree["params"]
            if (paramsNode != null && paramsNode.isObject) {
                paramsNode.fields().forEach { (key, value) ->
                    params[key] = if (value.isNull) null else value.asText()
                }
            }

            IntentResult(intent, params)
        } catch (e: Exception) {
            logger.warn("Failed to parse intent result JSON: ${e.message}")
            IntentResult(Intent.UNKNOWN, emptyMap())
        }
    }
}
