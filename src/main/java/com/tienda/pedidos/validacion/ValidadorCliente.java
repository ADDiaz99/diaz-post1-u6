package com.tienda.pedidos.validacion;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalTime;
import java.util.List;

/** Eslabón 2: el cliente existe y, si es moroso, no tiene deuda dentro del horario de corte. */
@Component
public class ValidadorCliente extends ValidadorPedido {

    private static final LocalTime HORA_DE_CORTE = LocalTime.of(20, 0);

    private final JdbcTemplate jdbcTemplate;
    private final Clock reloj;

    public ValidadorCliente(JdbcTemplate jdbcTemplate, Clock reloj) {
        this.jdbcTemplate = jdbcTemplate;
        this.reloj = reloj;
    }

    @Override
    protected void ejecutar(ContextoPedido contexto) {
        Long clienteId = contexto.getRequest().getClienteId();
        // queryForList devuelve una lista vacía si el cliente no existe. Con
        // queryForObject se lanzaba EmptyResultDataAccessException y la rama
        // "Cliente no registrado" del código original nunca se ejecutaba.
        List<String> tipos = jdbcTemplate.queryForList(
                "SELECT tipo_cliente FROM clientes WHERE id = ?", String.class, clienteId);
        if (tipos.isEmpty() || tipos.get(0) == null) {
            contexto.rechazar("Cliente no registrado");
            return;
        }
        String tipo = tipos.get(0);
        contexto.setTipoCliente(tipo);

        if (tipo.equals("MOROSO")) {
            Double deuda = jdbcTemplate.queryForObject(
                    "SELECT SUM(monto) FROM facturas WHERE cliente_id = ? AND pagada = false",
                    Double.class, clienteId);
            boolean dentroDelHorarioDeCorte = LocalTime.now(reloj).isBefore(HORA_DE_CORTE);
            if (deuda != null && deuda > 0 && dentroDelHorarioDeCorte) {
                contexto.rechazar("Cliente con deuda pendiente: $" + deuda);
            }
        }
    }
}
