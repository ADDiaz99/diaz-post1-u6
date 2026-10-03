package com.tienda.pedidos.validacion;

import com.tienda.pedidos.dto.ItemPedido;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/** Eslabón 1: el pedido tiene ítems, cada producto existe y hay stock suficiente. */
@Component
public class ValidadorStock extends ValidadorPedido {

    private final JdbcTemplate jdbcTemplate;

    public ValidadorStock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void ejecutar(ContextoPedido contexto) {
        // La versión de referencia de la guía omitía esta comprobación: sin ella,
        // un pedido vacío pasaba la cadena y se confirmaba con total 0.
        List<ItemPedido> items = contexto.getRequest().getItems();
        if (items == null || items.isEmpty()) {
            contexto.rechazar("El pedido no contiene items");
            return;
        }
        for (ItemPedido item : items) {
            // queryForList devuelve una lista vacía si el producto no existe;
            // queryForObject lanzaba EmptyResultDataAccessException.
            List<Integer> stocks = jdbcTemplate.queryForList(
                    "SELECT stock FROM inventario WHERE producto_id = ?", Integer.class, item.getProductoId());
            if (stocks.isEmpty()) {
                contexto.rechazar("Producto no registrado: " + item.getProductoId());
                return;
            }
            Integer stock = stocks.get(0);
            if (stock == null || stock < item.getCantidad()) {
                contexto.rechazar("Stock insuficiente: producto " + item.getProductoId());
                return;
            }
        }
    }
}
