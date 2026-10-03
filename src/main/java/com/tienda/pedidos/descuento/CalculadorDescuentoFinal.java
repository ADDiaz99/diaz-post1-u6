package com.tienda.pedidos.descuento;

import com.tienda.pedidos.validacion.ContextoPedido;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Combina el descuento por tipo de cliente con el de las campañas activas y
 * aplica el mayor: la misma regla de negocio que la versión con eslabones, pero
 * sin escribir en un campo mutable compartido desde clases que no validan nada.
 *
 * Si mercadeo decide que las campañas se SUMAN en vez de competir, el cambio
 * queda en este único método (por ejemplo, .sum() en lugar de .max()).
 */
@Component
public class CalculadorDescuentoFinal {

    private final SelectorEstrategiaDescuento selectorPorCliente;
    private final List<EstrategiaDescuento> campanas;

    public CalculadorDescuentoFinal(SelectorEstrategiaDescuento selectorPorCliente,
                                    DescuentoBlackFriday blackFriday, DescuentoCorporativo corporativo,
                                    DescuentoVolumen volumen) {
        this.selectorPorCliente = selectorPorCliente;
        this.campanas = List.of(blackFriday, corporativo, volumen);
    }

    public double calcular(ContextoPedido contexto) {
        double porTipoCliente = selectorPorCliente.seleccionar(contexto.getTipoCliente()).calcular(contexto);
        double porCampana = campanas.stream()
                .mapToDouble(estrategia -> estrategia.calcular(contexto))
                .max().orElse(0.0);
        return Math.max(porTipoCliente, porCampana);
    }
}
