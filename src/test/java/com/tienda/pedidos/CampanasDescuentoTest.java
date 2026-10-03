package com.tienda.pedidos;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.service.GestorPedidos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Campañas CORPORATIVO y VOLUMEN (Black Friday inactiva, valor por defecto).
 * Estas pruebas se escriben contra la versión con las campañas en la cadena
 * y deben seguir pasando, sin cambios, después de moverlas a Strategy.
 */
@SpringBootTest
@Import(RelojDePruebaConfig.class)
@Transactional
class CampanasDescuentoTest {

    @Autowired
    private GestorPedidos gestor;

    private PedidoRequest pedido(long clienteId, long productoId, int cantidad) {
        return new PedidoRequest(clienteId, "cliente" + clienteId + "@tienda.com",
                List.of(new ItemPedido(productoId, cantidad)));
    }

    @Test
    void clienteConNitRecibeDescuentoCorporativo() {
        // Cliente 5 (ESTANDAR con NIT): 1 teclado = 250.000; -10 % = 225.000; IVA = 42.750
        ResultadoPedido r = gestor.procesarPedido(pedido(5, 1, 1));

        assertTrue(r.isConfirmado());
        assertEquals(267_750.0, r.getTotal(), 0.01);
    }

    @Test
    void masDeVeinteUnidadesRecibeDescuentoPorVolumen() {
        // Cliente 4 (ESTANDAR, sin NIT): 25 mouse = 2.000.000; -12 % = 1.760.000; IVA = 334.400
        ResultadoPedido r = gestor.procesarPedido(pedido(4, 3, 25));

        assertTrue(r.isConfirmado());
        assertEquals(2_094_400.0, r.getTotal(), 0.01);
    }

    @Test
    void entreCampanasGanaElMayorDescuento() {
        // Cliente 5 con NIT y 21 mouse: corporativo 10 % vs volumen 12 % -> 12 %
        // 1.680.000; -12 % = 1.478.400; IVA = 280.896
        ResultadoPedido r = gestor.procesarPedido(pedido(5, 3, 21));

        assertTrue(r.isConfirmado());
        assertEquals(1_759_296.0, r.getTotal(), 0.01);
    }

    @Test
    void elDescuentoPorTipoDeClienteGanaSiEsMayorQueLaCampana() {
        // Cliente 1 (VIP) con 21 mouse: VIP 15 % (más de 1.000.000) vs volumen 12 % -> 15 %
        // 1.680.000; -15 % = 1.428.000; IVA = 271.320
        ResultadoPedido r = gestor.procesarPedido(pedido(1, 3, 21));

        assertTrue(r.isConfirmado());
        assertEquals(1_699_320.0, r.getTotal(), 0.01);
    }
}
