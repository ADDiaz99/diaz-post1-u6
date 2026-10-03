package com.tienda.pedidos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Reloj del sistema como bean. Las reglas que dependen de la hora (el horario de
 * corte de los clientes morosos) lo reciben inyectado, y así las pruebas pueden
 * fijar la hora en vez de depender de cuándo se ejecutan.
 */
@Configuration
public class RelojConfig {

    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }
}
