package com.tienda.pedidos;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Reemplaza el reloj del sistema por uno ajustable solo en las pruebas que lo importan. */
@TestConfiguration
public class RelojDePruebaConfig {

    @Bean
    @Primary
    public RelojAjustable relojAjustable() {
        return new RelojAjustable();
    }
}
