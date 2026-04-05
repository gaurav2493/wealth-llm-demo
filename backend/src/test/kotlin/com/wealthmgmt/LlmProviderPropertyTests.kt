package com.wealthmgmt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.wealthmgmt.llm.AnthropicLlmProvider
import com.wealthmgmt.llm.GeminiLlmProvider
import com.wealthmgmt.llm.OpenAiLlmProvider
import com.wealthmgmt.model.Intent
import com.wealthmgmt.model.IntentResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.forAll
import io.mockk.mockk
import org.springframework.web.client.RestTemplate

// Feature: admin-chat-data-query, Property 11: LLM response parsing round trip
class LlmProviderPropertyTests : FunSpec({

    val objectMapper = jacksonObjectMapper()
    val restTemplate = mockk<RestTemplate>()

    val paramKeys = listOf("clientName", "schemeName", "schemeType", "transactionType", "sortBy", "sortOrder")

    // Generator for random IntentResult objects
    val intentResultArb: Arb<IntentResult> = Arb.enum<Intent>().map { intent ->
        intent
    }.let { intentArb ->
        // Combine intent with random nullable string params
        io.kotest.property.arbitrary.arbitrary {
            val intent = intentArb.bind()
            val params = mutableMapOf<String, String?>()
            for (key in paramKeys) {
                params[key] = Arb.string(1..20).orNull(0.4).bind()
            }
            IntentResult(intent, params)
        }
    }

    fun serializeToJson(intentResult: IntentResult): String {
        val paramsNode = objectMapper.createObjectNode()
        for ((key, value) in intentResult.params) {
            if (value == null) {
                paramsNode.putNull(key)
            } else {
                paramsNode.put(key, value)
            }
        }
        val root = objectMapper.createObjectNode()
        root.put("intent", intentResult.intent.name)
        root.set<com.fasterxml.jackson.databind.node.ObjectNode>("params", paramsNode)
        return objectMapper.writeValueAsString(root)
    }

    // **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5**

    test("Property 11: OpenAiLlmProvider parseIntentResult round trip") {
        val provider = OpenAiLlmProvider("key", "model", "http://localhost", restTemplate)

        forAll(PropTestConfig(iterations = 100), intentResultArb) { original ->
            val json = serializeToJson(original)
            val parsed = provider.parseIntentResult(json)
            parsed.intent == original.intent && parsed.params == original.params
        }
    }

    test("Property 11: AnthropicLlmProvider parseIntentResult round trip") {
        val provider = AnthropicLlmProvider("key", "model", "http://localhost", restTemplate)

        forAll(PropTestConfig(iterations = 100), intentResultArb) { original ->
            val json = serializeToJson(original)
            val parsed = provider.parseIntentResult(json)
            parsed.intent == original.intent && parsed.params == original.params
        }
    }

    test("Property 11: GeminiLlmProvider parseIntentResult round trip") {
        val provider = GeminiLlmProvider("key", "model", "http://localhost", restTemplate)

        forAll(PropTestConfig(iterations = 100), intentResultArb) { original ->
            val json = serializeToJson(original)
            val parsed = provider.parseIntentResult(json)
            parsed.intent == original.intent && parsed.params == original.params
        }
    }
})
