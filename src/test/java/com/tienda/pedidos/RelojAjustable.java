package com.tienda.pedidos;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/** Reloj de pruebas: permite fijar la hora del día para probar el horario de corte de los morosos. */
public class RelojAjustable extends Clock {

    private final ZoneId zona = ZoneId.systemDefault();
    private Instant instante = Instant.now();

    public void fijarHora(LocalTime hora) {
        this.instante = LocalDate.now(zona).atTime(hora).atZone(zona).toInstant();
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(instante, zone);
    }

    @Override
    public Instant instant() {
        return instante;
    }
}
