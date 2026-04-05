package com.wealthmgmt.controller

import com.wealthmgmt.model.ApiResponse
import com.wealthmgmt.model.LoginRequest
import com.wealthmgmt.repository.AdminRepository
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val adminRepo: AdminRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @PostMapping("/login")
    fun login(@RequestBody req: LoginRequest, httpReq: HttpServletRequest): ResponseEntity<ApiResponse> {
        val admin = adminRepo.findByUsername(req.username)
            ?: return ResponseEntity.status(401).body(ApiResponse("Invalid credentials", false))

        if (!passwordEncoder.matches(req.password, admin.passwordHash)) {
            return ResponseEntity.status(401).body(ApiResponse("Invalid credentials", false))
        }

        val auth = UsernamePasswordAuthenticationToken(
            admin.username, null, listOf(SimpleGrantedAuthority("ROLE_ADMIN"))
        )
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = auth
        SecurityContextHolder.setContext(context)
        httpReq.getSession(true).setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context
        )

        return ResponseEntity.ok(ApiResponse("Login successful"))
    }
}
