package com.wealthmgmt.llm

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.wealthmgmt.model.Intent
import com.wealthmgmt.model.IntentResult
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate

class GeminiLlmProvider(
    private val apiKey: String,
    private val model: String,
    private val endpoint: String,
    private val restTemplate: RestTemplate
) : LlmProvider {

    private val logger = LoggerFactory.getLogger(GeminiLlmProvider::class.java)
    private val objectMapper = jacksonObjectMapper()

    override fun resolveIntent(message: String): IntentResult {
        return try {
            val url = "${endpoint.trimEnd('/')}/v1beta/models/$model:generateContent?key=$apiKey"

            val requestBody = mapOf(
                "system_instruction" to mapOf(
                    "parts" to listOf(mapOf("text" to OpenAiLlmProvider.SYSTEM_PROMPT))
                ),
                "contents" to listOf(
                    mapOf("role" to "user", "parts" to listOf(mapOf("text" to message)))
                )
            )

            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

            val entity = HttpEntity(objectMapper.writeValueAsString(requestBody), headers)
            val response = restTemplate.postForEntity(url, entity, String::class.java)

            val root = objectMapper.readTree(response.body)
            val content = root["candidates"][0]["content"]["parts"][0]["text"].asText()
            parseIntentResult(content)
        } catch (e: Exception) {
            logger.warn("Failed to parse Gemini response: ${e.message}")
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
