package com.wealthmgmt.mcp

import java.net.CookieManager
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class ApiClient(private val baseUrl: String = "http://localhost:8080") {

    private val http = HttpClient.newBuilder().cookieHandler(CookieManager()).build()
    private var loggedIn = false

    private fun ensureLoggedIn() {
        if (loggedIn) return
        val body = """{"username":"admin","password":"admin123"}"""
        val req = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/auth/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val res = http.send(req, HttpResponse.BodyHandlers.ofString())
        if (res.statusCode() != 200) throw RuntimeException("Login failed: ${res.body()}")
        loggedIn = true
    }

    fun getJson(path: String): String {
        ensureLoggedIn()
        val req = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .GET().build()
        return http.send(req, HttpResponse.BodyHandlers.ofString()).body()
    }
}
