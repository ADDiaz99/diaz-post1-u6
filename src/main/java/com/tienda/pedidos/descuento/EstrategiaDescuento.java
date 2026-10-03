package com.tienda.pedidos.descuento;

import com.tienda.pedidos.validacion.ContextoPedido;

/**
 * Patrón Strategy: cada regla de descuento se encapsula en su propia clase y
 * devuelve un porcentaje (0.15 = 15 %), sin depender de un orden de evaluación.
 */
public interface EstrategiaDescuento {
    double calcular(ContextoPedido contexto);
}
