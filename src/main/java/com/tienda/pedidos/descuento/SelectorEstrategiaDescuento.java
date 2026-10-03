package com.tienda.pedidos.descuento;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Único punto de decisión sobre qué regla aplica: reemplaza el if/else anidado
 * por tipo de cliente. Un tipo nuevo es una clase nueva más una entrada en el mapa.
 */
@Component
public class SelectorEstrategiaDescuento {

    private final Map<String, EstrategiaDescuento> estrategias;
    private final EstrategiaDescuento porDefecto;

    public SelectorEstrategiaDescuento(DescuentoVip vip, DescuentoFrecuente frecuente,
                                       DescuentoEstandar estandar) {
        this.estrategias = Map.of("VIP", vip, "FRECUENTE", frecuente, "ESTANDAR", estandar);
        this.porDefecto = estandar;
    }

    public EstrategiaDescuento seleccionar(String tipoCliente) {
        return estrategias.getOrDefault(tipoCliente, porDefecto);
    }
}
