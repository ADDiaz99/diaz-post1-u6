package com.tienda.pedidos.validacion;

/**
 * Patrón Chain of Responsibility: cada validador decide si el pedido continúa o
 * se rechaza. Si un eslabón rechaza, los siguientes no se ejecutan (corte anticipado).
 */
public abstract class ValidadorPedido {

    private ValidadorPedido siguiente;

    /**
     * Enlaza el siguiente eslabón y lo DEVUELVE, para poder escribir
     * a.encadenar(b).encadenar(c). Ojo: el inicio de la cadena sigue siendo "a",
     * no el valor que retorna esta llamada.
     */
    public ValidadorPedido encadenar(ValidadorPedido siguiente) {
        this.siguiente = siguiente;
        return siguiente;
    }

    public final void validar(ContextoPedido contexto) {
        ejecutar(contexto);
        if (!contexto.isRechazado() && siguiente != null) {
            siguiente.validar(contexto);
        }
    }

    protected abstract void ejecutar(ContextoPedido contexto);
}
