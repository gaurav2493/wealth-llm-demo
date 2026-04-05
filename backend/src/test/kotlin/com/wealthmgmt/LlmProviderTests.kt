package com.wealthmgmt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.wealthmgmt.llm.AnthropicLlmProvider
import com.wealthmgmt.llm.GeminiLlmProvider
import com.wealthmgmt.llm.OpenAiLlmProvider
import com.wealthmgmt.model.Intent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

class LlmProviderTests : FunSpec({

    val objectMapper = jacksonObjectMapper()

    // ========== OpenAI Provider Tests ==========

    context("OpenAiLlmProvider") {

        test("resolveIntent calls correct URL") {
            val restTemplate = mockk<RestTemplate>()
            val provider = OpenAiLlmProvider("test-key", "gpt-4o", "https://api.openai.com/v1", restTemplate)

            val response = """{"choices":[{"message":{"content":"{\"intent\":\"CLIENTS\",\"params\":{}}"}}]}"""
            val urlSlot = slot<String>()
            every {
                restTemplate.postForEntity(capture(urlSlot), any<HttpEntity<String>>(), eq(String::class.java))
            } returns ResponseEntity(response, HttpStatus.OK)

            provider.resolveIntent("show clients")

            urlSlot.captured shouldBe "https://api.openai.com/v1/chat/completions"
        }

        test("resolveIntent sets Bearer auth header") {
            val restTemplate = mockk<RestTemplate>()
            val provider = OpenAiLlmProvider("my-secret-key", "gpt-4o", "https://api.openai.com/v1", restTemplate)

            val response = """{"choices":[{"message":{"content":"{\"intent\":\"CLIENTS\",\"params\":{}}"}}]}"""
            val entitySlot = slot<HttpEntity<String>>()
            every {
                restTemplate.postForEntity(any<String>(), capture(entitySlot), eq(String::class.java))
            } returns ResponseEntity(response, HttpStatus.OK)

            provider.resolveIntent("show clients")

            val authHeader = entitySlot.captured.headers["Authorization"]?.first()
            authHeader shouldBe "Bearer my-secret-key"
        }

        test("resolveIntent parses valid response into IntentResult") {
            val restTemplate = mockk<RestTemplate>()
            val provider = OpenAiLlmProvider("test-key", "gpt-4o", "https://api.openai.com/v1", restTemplate)

            val response = """{"choices":[{"message":{"content":"{\"intent\":\"CLIENTS\",\"params\":{\"clientName\":\"Alice\"}}"}}]}"""
            every {
                restTemplate.postForEntity(any<String>(), any<HttpEntity<String>>(), eq(String::class.java))
            } returns ResponseEntity(response, HttpStatus.OK)

            val result = provider.resolveIntent("find Alice")

            result.intent shouldBe Intent.CLIENTS
            result.params["clientName"] shouldBe "Alice"
        }

        test("resolveIntent returns UNKNOWN for malformed LLM response") {
            val restTemplate = mockk<RestTemplate>()
            val provider = OpenAiLlmProvider("test-key", "gpt-4o", "https://api.openai.com/v1", restTemplate)

            val response = """{"choices":[{"message":{"content":"this is not json"}}]}"""
            every {
                restTemplate.postForEntity(any<String>(), any<HttpEntity<String>>(), eq(String::class.java))
            } returns ResponseEntity(response, HttpStatus.OK)

            val result = provider.resolveIntent("gibberish")

            result.intent shouldBe Intent.UNKNOWN
            result.params shouldBe emptyMap()
        }

        test("resolveIntent returns UNKNOWN when RestTemplate throws exception") {
            val restTemplate = mockk<RestTemplate>()
            val provider = OpenAiLlmProvider("test-key", "gpt-4o", "https://api.openai.com/v1", restTemplate)

            every {
                restTemplate.postForEntity(any<String>(), any<HttpEntity<String>>(), eq(String::class.java))
            } throws RestClientException("Connection refused")

            val result = provider.resolveIntent("show clients")

            result.intent shouldBe Intent.UNKNOWN
            result.params shouldBe emptyMap()
        }

        test("resolveIntent sends correct request body with model and messages") {
            val restTemplate = mockk<RestTemplate>()
            val provider = OpenAiLlmProvider("test-key", "gpt-4o-mini", "https://api.openai.com/v1", restTemplate)

            val response = """{"choices":[{"message":{"content":"{\"intent\":\"UNKNOWN\",\"params\":{}}"}}]}"""
            val entitySlot = slot<HttpEntity<String>>()
            every {
                restTemplate.postForEntity(any<String>(), capture(entitySlot), eq(String::class.java))
            } returns ResponseEntity(response, HttpStatus.OK)

            provider.resolveIntent("hello")

            val body = objectMapper.readTree(entitySlot.captured.body)
            body.get("model").asText() shouldBe "gpt-4o-mini"
            body.get("messages").size() shouldBe 2
            body.get("messages").get(0).get("role").asText() shouldBe "system"
            body.get("messages").get(1).get("role").asText() shouldBe "user"
            body.get("messages").get(1).get("content").asText() shouldBe "hello"
        }
    }


    // ========== Anthropic Provider Tests ==========

    context("AnthropicLlmProvider") {

        test("resolveIntent calls correct URL") {
            val restTemplate = mockk<RestTemplate>()
            val provider = AnthropicLlmProvider("test-key", "claude-sonnet-4-20250514", "https://api.anthropic.com/v1", restTemplate)

            val response = """{"content":[{"text":"{\"intent\":\"SCHEMES\",\"params\":{}}"}]}"""
            val urlSlot = slot<String>()
            every {
                restTemplate.postForEntity(capture(urlSlot), any<HttpEntity<String>>(), eq(String::class.java))
            } returns ResponseEntity(response, HttpStatus.OK)

            provider.resolveIntent("show schemes")

            urlSlot.captured shouldBe "https://api.anthropic.com/v1/messages"
        }

        test("resolveIntent sets x-api-key and anthropic-version headers") {
            val restTemplate = mockk<RestTemplate>()
            val provider = AnthropicLlmProvider("anthropic-secret", "claude-sonnet-4-20250514", "https://api.anthropic.com/v1", restTemplate)

            val response = """{"content":[{"text":"{\"intent\":\"SCHEMES\",\"params\":{}}"}]}"""
            val entitySlot = slot<HttpEntity<String>>()
            every {
                restTemplate.postForEntity(any<String>(), capture(entitySlot), eq(String::class.java))
            } returns ResponseEntity(response, HttpStatus.OK)

            provider.resolveIntent("show schemes")

            entitySlot.captured.headers["x-api-key"]?.first() shouldBe "anthropic-secret"
            entitySlot.captured.headers["anthropic-version"]?.first() shouldBe "2023-06-01"
        }

        test("resolveIntent parses valid response into IntentResult") {
            val restTemplate = mockk<RestTemplate>()
            val provider = AnthropicLlmProvider("test-key", "claude-sonnet-4-20250514", "https://api.anthropic.com/v1", restTemplate)

            val response = """{"content":[{"text":"{\"intent\":\"SCHEMES\",\"params\":{\"schemeName\":\"Growth Fund\"}}"}]}"""
            every {
                restTemplate.postForEntity(any<String>(), any<HttpEntity<String>>(), eq(String::class.java))
            } returns ResponseEntity(response, HttpStatus.OK)

            val result = provider.resolveIntent("find Growth Fund")

            result.intent shouldBe Intent.SCHEMES
            result.params["schemeName"] shouldBe "Growth Fund"
        }

        test("resolveIntent returns UNKNOWN for malformed LLM response") {
            val restTemplate = mockk<RestTemplate>()
            val provider = AnthropicLlmProvider("test-key", "claude-sonnet-4-20250514", "https://api.anthropic.com/v1", restTemplate)

            val response = """{"content":[{"text":"not valid json at all"}]}"""
            every {
                restTemplate.postForEntity(any<String>(), any<HttpEntity<String>>(), eq(String::class.java))
            } returns ResponseEntity(response, HttpStatus.OK)

            val result = provider.resolveIntent("gibberish")

            result.intent shouldBe Intent.UNKNOWN
            result.params shouldBe emptyMap()
        }

        test("resolveIntent returns UNKNOWN when RestTemplate throws exception") {
            val restTemplate = mockk<RestTemplate>()
            val provider = AnthropicLlmProvider("test-key", "claude-sonnet-4-20250514", "https://api.anthropic.com/v1", restTemplate)

            every {
                restTemplate.postForEntity(any<String>(), any<HttpEntity<String>>(), eq(String::class.java))
            } throws RestClientException("Timeout")

            val result = provider.resolveIntent("show schemes")

            result.intent shouldBe Intent.UNKNOWN
            result.params shouldBe emptyMap()
        }

        test("resolveIntent sends correct request body with model and system prompt") {
            val restTemplate = mockk<RestTemplate>()
            val provider = AnthropicLlmProvider("test-key", "claude-sonnet-4-20250514", "https://api.anthropic.com/v1", restTemplate)

            val response = """{"content":[{"text":"{\"intent\":\"UNKNOWN\",\"params\":{}}"}]}"""
            val entitySlot = slot<HttpEntity<String>>()
            every {
                restTemplate.postForEntity(any<String>(), capture(entitySlot), eq(String::class.java))
            } returns ResponseEntity(response, HttpStatus.OK)

            provider.resolveIntent("hello")

            val body = objectMapper.readTree(entitySlot.captured.body)
            body.get("model").asText() shouldBe "claude-sonnet-4-20250514"
            body.get("max_tokens").asInt() shouldBe 256
            body.get("system").asText() shouldContain "intent classifier"
            body.get("messages").size() shouldBe 1
            body.get("messages").get(0).get("role").asText() shouldBe "user"
            body.get("messages").get(0).get("content").asText() shouldBe "hello"
        }
    }


    // ========== Gemini Provider Tests ==========

    context("GeminiLlmProvider") {

        test("resolveIntent calls correct URL with API key as query param") {
            val restTemplate = mockk<RestTemplate>()
            val provider = GeminiLlmProvider("gemini-key-123", "gemini-2.0-flash", "https://generativelanguage.googleapis.com", restTemplate)

            val response = """{"candidates":[{"content":{"parts":[{"text":"{\"intent\":\"HOLDINGS\",\"params\":{}}"}]}}]}"""
            val urlSlot = slot<String>()
            every {
                restTemplate.postForEntity(capture(urlSlot), any<HttpEntity<String>>(), eq(String::class.java))
            } returns ResponseEntity(response, HttpStatus.OK)

            provider.resolveIntent("show holdings")

            urlSlot.captured shouldBe "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=gemini-key-123"
        }

        test("resolveIntent does NOT set Authorization header") {
            val restTemplate = mockk<RestTemplate>()
            val provider = GeminiLlmProvider("gemini-key", "gemini-2.0-flash", "https://generativelanguage.googleapis.com", restTemplate)

            val response = """{"candidates":[{"content":{"parts":[{"text":"{\"intent\":\"HOLDINGS\",\"params\":{}}"}]}}]}"""
            val entitySlot = slot<HttpEntity<String>>()
            every {
                restTemplate.postForEntity(any<String>(), capture(entitySlot), eq(String::class.java))
            } returns ResponseEntity(response, HttpStatus.OK)

            provider.resolveIntent("show holdings")

            entitySlot.captured.headers["Authorization"] shouldBe null
        }

        test("resolveIntent parses valid response into IntentResult") {
            val restTemplate = mockk<RestTemplate>()
            val provider = GeminiLlmProvider("gemini-key", "gemini-2.0-flash", "https://generativelanguage.googleapis.com", restTemplate)

            val response = """{"candidates":[{"content":{"parts":[{"text":"{\"intent\":\"HOLDINGS\",\"params\":{\"clientName\":\"Bob\",\"sortBy\":\"value\",\"sortOrder\":\"desc\"}}"}]}}]}"""
            every {
                restTemplate.postForEntity(any<String>(), any<HttpEntity<String>>(), eq(String::class.java))
            } returns ResponseEntity(response, HttpStatus.OK)

            val result = provider.resolveIntent("show Bob's holdings sorted by value")

            result.intent shouldBe Intent.HOLDINGS
            result.params["clientName"] shouldBe "Bob"
            result.params["sortBy"] shouldBe "value"
            result.params["sortOrder"] shouldBe "desc"
        }

        test("resolveIntent returns UNKNOWN for malformed LLM response") {
            val restTemplate = mockk<RestTemplate>()
            val provider = GeminiLlmProvider("gemini-key", "gemini-2.0-flash", "https://generativelanguage.googleapis.com", restTemplate)

            val response = """{"candidates":[{"content":{"parts":[{"text":"totally not json"}]}}]}"""
            every {
                restTemplate.postForEntity(any<String>(), any<HttpEntity<String>>(), eq(String::class.java))
            } returns ResponseEntity(response, HttpStatus.OK)

            val result = provider.resolveIntent("gibberish")

            result.intent shouldBe Intent.UNKNOWN
            result.params shouldBe emptyMap()
        }

        test("resolveIntent returns UNKNOWN when RestTemplate throws exception") {
            val restTemplate = mockk<RestTemplate>()
            val provider = GeminiLlmProvider("gemini-key", "gemini-2.0-flash", "https://generativelanguage.googleapis.com", restTemplate)

            every {
                restTemplate.postForEntity(any<String>(), any<HttpEntity<String>>(), eq(String::class.java))
            } throws RestClientException("Service unavailable")

            val result = provider.resolveIntent("show holdings")

            result.intent shouldBe Intent.UNKNOWN
            result.params shouldBe emptyMap()
        }

        test("resolveIntent sends correct request body with system instruction and user content") {
            val restTemplate = mockk<RestTemplate>()
            val provider = GeminiLlmProvider("gemini-key", "gemini-2.0-flash", "https://generativelanguage.googleapis.com", restTemplate)

            val response = """{"candidates":[{"content":{"parts":[{"text":"{\"intent\":\"UNKNOWN\",\"params\":{}}"}]}}]}"""
            val entitySlot = slot<HttpEntity<String>>()
            every {
                restTemplate.postForEntity(any<String>(), capture(entitySlot), eq(String::class.java))
            } returns ResponseEntity(response, HttpStatus.OK)

            provider.resolveIntent("hello")

            val body = objectMapper.readTree(entitySlot.captured.body)
            body.get("system_instruction").get("parts").get(0).get("text").asText() shouldContain "intent classifier"
            body.get("contents").size() shouldBe 1
            body.get("contents").get(0).get("role").asText() shouldBe "user"
            body.get("contents").get(0).get("parts").get(0).get("text").asText() shouldBe "hello"
        }
    }
})
