package com.tienda.pedidos.service;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.validacion.ContextoPedido;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/** Persistencia del pedido: cabecera, detalle y descuento del inventario. */
@Repository
public class PedidoRepository {

    private final JdbcTemplate jdbcTemplate;

    public PedidoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Transaccional, a diferencia del original: las inserciones y el descuento
     * de stock se confirman todos o ninguno. Además garantiza que "CALL IDENTITY()"
     * se ejecute en la misma conexión que el INSERT, que es de donde lee el id.
     */
    @Transactional
    public Long guardar(ContextoPedido contexto, double descuento, double impuesto, double total) {
        jdbcTemplate.update(
                "INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                contexto.getRequest().getClienteId(), contexto.getSubtotal(), descuento, impuesto, total,
                Timestamp.valueOf(LocalDateTime.now()), "CONFIRMADO");
        Long pedidoId = jdbcTemplate.queryForObject("CALL IDENTITY()", Long.class);
        for (ItemPedido item : contexto.getRequest().getItems()) {
            jdbcTemplate.update(
                    "INSERT INTO detalle_pedido (pedido_id, producto_id, cantidad) VALUES (?, ?, ?)",
                    pedidoId, item.getProductoId(), item.getCantidad());
            jdbcTemplate.update(
                    "UPDATE inventario SET stock = stock - ? WHERE producto_id = ?",
                    item.getCantidad(), item.getProductoId());
        }
        return pedidoId;
    }
}
