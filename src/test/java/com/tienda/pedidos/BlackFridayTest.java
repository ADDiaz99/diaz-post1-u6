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

/** Campaña BLACK_FRIDAY activa (25 % fijo). Usa un contexto propio con la propiedad encendida. */
@SpringBootTest(properties = "promo.black-friday.activa=true")
@Import(RelojDePruebaConfig.class)
@Transactional
class BlackFridayTest {

    @Autowired
    private GestorPedidos gestor;

    private PedidoRequest pedido(long clienteId, long productoId, int cantidad) {
        return new PedidoRequest(clienteId, "cliente" + clienteId + "@tienda.com",
                List.of(new ItemPedido(productoId, cantidad)));
    }

    @Test
    void clienteEstandarRecibe25PorCiento() {
        // 1 teclado = 250.000; -25 % = 187.500; IVA = 35.625
        ResultadoPedido r = gestor.procesarPedido(pedido(4, 1, 1));

        assertTrue(r.isConfirmado());
        assertEquals(223_125.0, r.getTotal(), 0.01);
    }

    @Test
    void blackFridayGanaAlDescuentoVip() {
        // VIP 15 % vs Black Friday 25 % -> 25 %. 2 monitores = 1.800.000; -25 % = 1.350.000; IVA = 256.500
        ResultadoPedido r = gestor.procesarPedido(pedido(1, 2, 2));

        assertTrue(r.isConfirmado());
        assertEquals(1_606_500.0, r.getTotal(), 0.01);
    }
}
