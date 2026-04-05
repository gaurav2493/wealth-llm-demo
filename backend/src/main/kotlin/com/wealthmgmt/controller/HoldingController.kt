package com.wealthmgmt.controller

import com.wealthmgmt.model.Holding
import com.wealthmgmt.repository.HoldingRepository
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/holdings")
class HoldingController(private val repo: HoldingRepository) {

    @GetMapping
    fun list(
        @RequestParam(required = false) clientId: Long?,
        @RequestParam(required = false) schemeId: Long?,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(required = false) sortOrder: String?
    ): List<Holding> = repo.getHoldings(clientId, schemeId, sortBy, sortOrder)
}
