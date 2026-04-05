package com.wealthmgmt.repository

import com.wealthmgmt.model.Holding
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class HoldingRepository(private val jdbc: JdbcTemplate) {

    fun getHoldings(clientId: Long?, schemeId: Long?, sortBy: String?, sortOrder: String?, asOfDate: LocalDate?): List<Holding> {
        val conditions = mutableListOf<String>()
        val params = mutableListOf<Any>()

        if (clientId != null) {
            conditions.add("t.client_id = ?")
            params.add(clientId)
        }
        if (schemeId != null) {
            conditions.add("t.scheme_id = ?")
            params.add(schemeId)
        }
        if (asOfDate != null) {
            conditions.add("t.date <= ?")
            params.add(asOfDate)
        }

        val where = if (conditions.isNotEmpty()) "WHERE ${conditions.joinToString(" AND ")}" else ""
        val order = if (sortBy == "value") {
            val dir = if (sortOrder == "desc") "DESC" else "ASC"
            "ORDER BY holding_value $dir"
        } else "ORDER BY c.name, s.name"

        val sql = """
            SELECT t.client_id, c.name AS client_name, t.scheme_id, s.name AS scheme_name,
                   SUM(CASE WHEN t.type = 'buy' THEN t.units ELSE -t.units END) AS units,
                   SUM(CASE WHEN t.type = 'buy' THEN t.units ELSE -t.units END) * s.nav AS holding_value
            FROM transaction t
            JOIN client c ON c.id = t.client_id
            JOIN scheme s ON s.id = t.scheme_id
            $where
            GROUP BY t.client_id, c.name, t.scheme_id, s.name, s.nav
            HAVING SUM(CASE WHEN t.type = 'buy' THEN t.units ELSE -t.units END) > 0
            $order
        """.trimIndent()

        return jdbc.query(sql, { rs, _ ->
            Holding(
                rs.getLong("client_id"), rs.getString("client_name"),
                rs.getLong("scheme_id"), rs.getString("scheme_name"),
                rs.getBigDecimal("units"), rs.getBigDecimal("holding_value")
            )
        }, *params.toTypedArray())
    }
}
