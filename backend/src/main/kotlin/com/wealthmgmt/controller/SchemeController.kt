package com.wealthmgmt.controller

import com.wealthmgmt.model.ApiResponse
import com.wealthmgmt.model.Scheme
import com.wealthmgmt.repository.SchemeRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/schemes")
class SchemeController(private val repo: SchemeRepository) {

    @GetMapping
    fun list(): List<Scheme> = repo.findAll()

    @PostMapping
    fun create(@RequestBody scheme: Scheme): ResponseEntity<Scheme> {
        if (scheme.name.isBlank()) return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(repo.create(scheme))
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<ApiResponse> {
        return if (repo.delete(id)) ResponseEntity.ok(ApiResponse("Deleted"))
        else ResponseEntity.status(404).body(ApiResponse("Not found", false))
    }
}
