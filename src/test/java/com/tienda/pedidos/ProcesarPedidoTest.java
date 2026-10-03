package com.tienda.pedidos;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.service.GestorPedidos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de caracterización: fijan el comportamiento observable de
 * GestorPedidos.procesarPedido() ANTES de refactorizar. Solo usan su método
 * público, así que siguen siendo válidas con cualquier diseño interno: si
 * pasan antes y después del refactor, el comportamiento se conservó.
 *
 * Cada prueba corre en una transacción que se revierte al final, así que los
 * datos de data.sql (stock, pedidos previos) son los mismos en todas.
 */
@SpringBootTest
@Import(RelojDePruebaConfig.class)
@Transactional
class ProcesarPedidoTest {

    @Autowired
    private GestorPedidos gestor;

    @Autowired
    private RelojAjustable reloj;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void horaHabil() {
        reloj.fijarHora(LocalTime.of(10, 0)); // dentro del horario de corte (antes de las 20:00)
    }

    private PedidoRequest pedido(long clienteId, long productoId, int cantidad) {
        return new PedidoRequest(clienteId, "cliente" + clienteId + "@tienda.com",
                List.of(new ItemPedido(productoId, cantidad)));
    }

    // ---------- Descuentos por tipo de cliente ----------

    @Test
    void vipConMasDeUnMillonRecibe15PorCiento() {
        // 2 monitores x 900.000 = 1.800.000; -15 % = 1.530.000; IVA 19 % = 290.700
        ResultadoPedido r = gestor.procesarPedido(pedido(1, 2, 2));

        assertTrue(r.isConfirmado());
        assertNotNull(r.getPedidoId());
        assertEquals(1_820_700.0, r.getTotal(), 0.01);
        int stockMonitor = jdbc.queryForObject("SELECT stock FROM inventario WHERE producto_id = 2", Integer.class);
        assertEquals(8, stockMonitor);
    }

    @Test
    void frecuenteConMasDeDiezPedidosRecibe8PorCiento() {
        // 4 teclados x 250.000 = 1.000.000; -8 % = 920.000; IVA 19 % = 174.800
        ResultadoPedido r = gestor.procesarPedido(pedido(2, 1, 4));

        assertTrue(r.isConfirmado());
        assertEquals(1_094_800.0, r.getTotal(), 0.01);
    }

    @Test
    void estandarNoRecibeDescuento() {
        // 1 teclado = 250.000; IVA 19 % = 47.500
        ResultadoPedido r = gestor.procesarPedido(pedido(4, 1, 1));

        assertTrue(r.isConfirmado());
        assertEquals(297_500.0, r.getTotal(), 0.01);
    }

    // ---------- Validaciones ----------

    @Test
    void stockInsuficienteSeRechaza() {
        ResultadoPedido r = gestor.procesarPedido(pedido(4, 4, 5)); // hay 2 cables, se piden 5

        assertFalse(r.isConfirmado());
        assertEquals("Stock insuficiente: producto 4", r.getMotivoRechazo());
    }

    @Test
    void pedidoSinItemsSeRechaza() {
        ResultadoPedido r = gestor.procesarPedido(new PedidoRequest(4L, "maria@tienda.com", List.of()));

        assertFalse(r.isConfirmado());
        assertEquals("El pedido no contiene items", r.getMotivoRechazo());
    }

    @Test
    void morosoDentroDelHorarioDeCorteSeRechaza() {
        reloj.fijarHora(LocalTime.of(10, 0));
        ResultadoPedido r = gestor.procesarPedido(pedido(3, 3, 1));

        assertFalse(r.isConfirmado());
        assertEquals("Cliente con deuda pendiente: $300000.0", r.getMotivoRechazo());
    }

    @Test
    void morosoFueraDelHorarioDeCorteSePermite() {
        reloj.fijarHora(LocalTime.of(21, 0));
        // 1 mouse = 80.000; sin descuento (MOROSO no es VIP ni FRECUENTE); IVA 19 % = 15.200
        ResultadoPedido r = gestor.procesarPedido(pedido(3, 3, 1));

        assertTrue(r.isConfirmado());
        assertEquals(95_200.0, r.getTotal(), 0.01);
    }

    @Test
    void clienteInexistenteSeRechaza() {
        // Antes de la corrección esto lanzaba EmptyResultDataAccessException:
        // la rama "Cliente no registrado" del código original nunca se ejecutaba.
        ResultadoPedido r = gestor.procesarPedido(pedido(99, 3, 1));

        assertFalse(r.isConfirmado());
        assertEquals("Cliente no registrado", r.getMotivoRechazo());
    }

    @Test
    void productoInexistenteSeRechaza() {
        ResultadoPedido r = gestor.procesarPedido(pedido(4, 99, 1));

        assertFalse(r.isConfirmado());
        assertEquals("Producto no registrado: 99", r.getMotivoRechazo());
    }
}
