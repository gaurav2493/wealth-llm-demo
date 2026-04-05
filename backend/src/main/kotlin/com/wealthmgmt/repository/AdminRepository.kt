package com.wealthmgmt.repository

import com.wealthmgmt.model.Admin
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class AdminRepository(private val jdbc: JdbcTemplate) {

    fun findByUsername(username: String): Admin? {
        return jdbc.query(
            "SELECT id, username, password_hash FROM admin WHERE username = ?",
            { rs, _ -> Admin(rs.getLong("id"), rs.getString("username"), rs.getString("password_hash")) },
            username
        ).firstOrNull()
    }
}
