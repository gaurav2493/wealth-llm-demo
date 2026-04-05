package com.wealthmgmt.controller

import com.wealthmgmt.model.ApiResponse
import com.wealthmgmt.model.Transaction
import com.wealthmgmt.repository.TransactionRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/transactions")
class TransactionController(private val repo: TransactionRepository) {

    @GetMapping
    fun list(): List<Transaction> = repo.findAll()

    @PostMapping
    fun create(@RequestBody tx: Transaction): ResponseEntity<Transaction> {
        if (tx.type !in listOf("buy", "sell")) return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(repo.create(tx))
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<ApiResponse> {
        return if (repo.delete(id)) ResponseEntity.ok(ApiResponse("Deleted"))
        else ResponseEntity.status(404).body(ApiResponse("Not found", false))
    }
}
