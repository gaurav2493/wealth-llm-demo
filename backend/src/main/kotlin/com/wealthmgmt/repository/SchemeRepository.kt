package com.wealthmgmt.repository

import com.wealthmgmt.model.Scheme
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.sql.Statement

@Repository
class SchemeRepository(private val jdbc: JdbcTemplate) {

    fun findAll(): List<Scheme> = jdbc.query("SELECT * FROM scheme ORDER BY id") { rs, _ ->
        Scheme(rs.getLong("id"), rs.getString("name"), rs.getString("type"), rs.getBigDecimal("nav"))
    }

    fun create(scheme: Scheme): Scheme {
        val keyHolder = GeneratedKeyHolder()
        jdbc.update({ conn ->
            val ps = conn.prepareStatement(
                "INSERT INTO scheme (name, type, nav) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            )
            ps.setString(1, scheme.name)
            ps.setString(2, scheme.type)
            ps.setBigDecimal(3, scheme.nav)
            ps
        }, keyHolder)
        val id = (keyHolder.keys?.get("id") as Number).toLong()
        return scheme.copy(id = id)
    }

    fun delete(id: Long): Boolean = jdbc.update("DELETE FROM scheme WHERE id = ?", id) > 0
}
