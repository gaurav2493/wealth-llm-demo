package com.wealthmgmt.llm

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.wealthmgmt.model.Intent
import com.wealthmgmt.model.IntentResult
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate

class AnthropicLlmProvider(
    private val apiKey: String,
    private val model: String,
    private val endpoint: String,
    private val restTemplate: RestTemplate
) : LlmProvider {

    private val logger = LoggerFactory.getLogger(AnthropicLlmProvider::class.java)
    private val objectMapper = jacksonObjectMapper()

    override fun resolveIntent(message: String): IntentResult {
        return try {
            val url = "${endpoint.trimEnd('/')}/messages"

            val requestBody = mapOf(
                "model" to model,
                "max_tokens" to 256,
                "system" to OpenAiLlmProvider.SYSTEM_PROMPT,
                "messages" to listOf(
                    mapOf("role" to "user", "content" to message)
                )
            )

            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("x-api-key", apiKey)
                set("anthropic-version", "2023-06-01")
            }

            val entity = HttpEntity(objectMapper.writeValueAsString(requestBody), headers)
            val response = restTemplate.postForEntity(url, entity, String::class.java)

            val root = objectMapper.readTree(response.body)
            val content = root["content"][0]["text"].asText()
            parseIntentResult(content)
        } catch (e: Exception) {
            logger.warn("Failed to parse Anthropic response: ${e.message}")
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
