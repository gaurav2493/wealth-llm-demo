package com.wealthmgmt.controller

import com.wealthmgmt.model.ApiResponse
import com.wealthmgmt.model.Client
import com.wealthmgmt.repository.ClientRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/clients")
class ClientController(private val repo: ClientRepository) {

    @GetMapping
    fun list(): List<Client> = repo.findAll()

    @PostMapping
    fun create(@RequestBody client: Client): ResponseEntity<Client> {
        if (client.name.isBlank()) return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(repo.create(client))
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<ApiResponse> {
        return if (repo.delete(id)) ResponseEntity.ok(ApiResponse("Deleted"))
        else ResponseEntity.status(404).body(ApiResponse("Not found", false))
    }
}
