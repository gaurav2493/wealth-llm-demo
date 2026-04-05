package com.wealthmgmt.config

import com.wealthmgmt.llm.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class LlmConfig {

    @Bean
    fun llmProvider(
        @Value("\${app.llm.provider}") provider: String,
        @Value("\${app.llm.api-key}") apiKey: String,
        @Value("\${app.llm.model}") model: String,
        @Value("\${app.llm.endpoint}") endpoint: String,
        restTemplate: RestTemplate
    ): LlmProvider = when (provider.lowercase()) {
        "openai" -> OpenAiLlmProvider(apiKey, model, endpoint, restTemplate)
        "anthropic" -> AnthropicLlmProvider(apiKey, model, endpoint, restTemplate)
        "gemini" -> GeminiLlmProvider(apiKey, model, endpoint, restTemplate)
        else -> throw IllegalArgumentException("Unsupported LLM provider: $provider")
    }

    @Bean
    fun restTemplate(): RestTemplate = RestTemplate()
}
