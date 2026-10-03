package com.tienda.pedidos.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Consulta de precios del catálogo: saca el SQL del cálculo del subtotal. */
@Repository
public class ProductoRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProductoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public double precioDe(Long productoId) {
        return jdbcTemplate.queryForObject("SELECT precio FROM productos WHERE id = ?", Double.class, productoId);
    }
}
