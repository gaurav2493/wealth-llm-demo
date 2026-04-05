package com.wealthmgmt

import com.fasterxml.jackson.databind.ObjectMapper
import com.wealthmgmt.llm.LlmProvider
import com.wealthmgmt.model.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

// Feature: admin-chat-data-query, ChatController Tests
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ChatControllerTests {

    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var mapper: ObjectMapper

    @MockitoBean
    lateinit var llmProvider: LlmProvider

    companion object {
        var session: MockHttpSession? = null
    }

    @BeforeAll
    fun setup() {
        val result = mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(LoginRequest("admin", "admin123")))
        ).andExpect(status().isOk).andReturn()
        session = result.request.getSession(false) as MockHttpSession
    }

    // ---- Property 2: Empty/blank message rejection ----
    // **Validates: Requirements 2.3, 9.4**

    @ParameterizedTest(name = "blank message \"{0}\" returns 400")
    @ValueSource(strings = ["", " ", "  ", "\t", "\n", " \t\n ", "   \n\t  "])
    @Order(1)
    fun `Property 2 - blank or whitespace-only messages return 400`(blankMessage: String) {
        val body = mapper.writeValueAsString(ChatRequest(blankMessage))
        mvc.perform(
            post("/api/chat").session(session!!)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.reply").value("Message cannot be empty."))
    }

    // ---- Unit tests for ChatController (Task 6.3) ----

    @Test
    @Order(10)
    fun `POST api-chat with valid message returns 200 with reply`() {
        `when`(llmProvider.resolveIntent("show me all clients"))
            .thenReturn(IntentResult(Intent.CLIENTS))

        val body = mapper.writeValueAsString(ChatRequest("show me all clients"))
        mvc.perform(
            post("/api/chat").session(session!!)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.reply").isNotEmpty)
    }

    @Test
    @Order(11)
    fun `POST api-chat with empty body returns 400`() {
        val body = mapper.writeValueAsString(ChatRequest(""))
        mvc.perform(
            post("/api/chat").session(session!!)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.reply").value("Message cannot be empty."))
    }

    @Test
    @Order(12)
    fun `POST api-chat without authentication returns 401`() {
        val body = mapper.writeValueAsString(ChatRequest("hello"))
        mvc.perform(
            post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isUnauthorized)
    }
}
