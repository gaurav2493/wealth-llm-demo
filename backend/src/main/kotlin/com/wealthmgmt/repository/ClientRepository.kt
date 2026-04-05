package com.wealthmgmt.repository

import com.wealthmgmt.model.Client
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.sql.Statement

@Repository
class ClientRepository(private val jdbc: JdbcTemplate) {

    fun findAll(): List<Client> = jdbc.query("SELECT * FROM client ORDER BY id") { rs, _ ->
        Client(rs.getLong("id"), rs.getString("name"), rs.getString("email"), rs.getString("phone"))
    }

    fun create(client: Client): Client {
        val keyHolder = GeneratedKeyHolder()
        jdbc.update({ conn ->
            val ps = conn.prepareStatement(
                "INSERT INTO client (name, email, phone) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            )
            ps.setString(1, client.name)
            ps.setString(2, client.email)
            ps.setString(3, client.phone)
            ps
        }, keyHolder)
        val id = (keyHolder.keys?.get("id") as Number).toLong()
        return client.copy(id = id)
    }

    fun delete(id: Long): Boolean = jdbc.update("DELETE FROM client WHERE id = ?", id) > 0
}
