package com.wealthmgmt.repository

import com.wealthmgmt.model.Transaction
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.sql.Statement

@Repository
class TransactionRepository(private val jdbc: JdbcTemplate) {

    fun findAll(): List<Transaction> = jdbc.query("SELECT * FROM transaction ORDER BY id") { rs, _ ->
        Transaction(
            rs.getLong("id"), rs.getLong("client_id"), rs.getLong("scheme_id"),
            rs.getString("type"), rs.getBigDecimal("units"), rs.getBigDecimal("amount"),
            rs.getDate("date").toLocalDate()
        )
    }

    fun create(tx: Transaction): Transaction {
        val keyHolder = GeneratedKeyHolder()
        jdbc.update({ conn ->
            val ps = conn.prepareStatement(
                "INSERT INTO transaction (client_id, scheme_id, type, units, amount, date) VALUES (?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            )
            ps.setLong(1, tx.clientId)
            ps.setLong(2, tx.schemeId)
            ps.setString(3, tx.type)
            ps.setBigDecimal(4, tx.units)
            ps.setBigDecimal(5, tx.amount)
            ps.setObject(6, tx.date)
            ps
        }, keyHolder)
        val id = (keyHolder.keys?.get("id") as Number).toLong()
        return tx.copy(id = id)
    }

    fun delete(id: Long): Boolean = jdbc.update("DELETE FROM transaction WHERE id = ?", id) > 0
}
