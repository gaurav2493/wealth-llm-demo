package com.wealthmgmt.controller

import com.wealthmgmt.model.ChatRequest
import com.wealthmgmt.model.ChatResponse
import com.wealthmgmt.service.ChatService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/chat")
class ChatController(private val chatService: ChatService) {

    @PostMapping
    fun chat(@RequestBody request: ChatRequest): ResponseEntity<ChatResponse> {
        if (request.message.isBlank()) {
            return ResponseEntity.badRequest().body(ChatResponse(reply = "Message cannot be empty."))
        }
        return ResponseEntity.ok(chatService.processMessage(request.message))
    }
}
