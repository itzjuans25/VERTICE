package com.ecommerce.subastas;

import com.ecommerce.subastas.application.port.out.TasaCambioPort.Tasa;
import com.ecommerce.subastas.application.usecase.ReferenciasSubastas;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ReferenciasSubastasTest {
    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneId.of("America/Bogota"));

    @Test void convierteConLaTasaVigenteYSaltaFinDeSemanaYFestivo() {
        var fecha = LocalDate.of(2026,11,20);
        var referencias = new ReferenciasSubastas(() -> Optional.of(new Tasa(new BigDecimal("0.0003"),fecha)),
                anno -> Optional.of(Set.of(fecha.plusDays(3))),reloj);
        var precio = referencias.cotizar(new BigDecimal("10000"));
        assertTrue(precio.disponible());
        assertEquals(new BigDecimal("3.00"),precio.precioUsd());
        var entrega = referencias.entrega(1);
        assertTrue(entrega.incluyeFestivos());
        assertEquals(LocalDate.of(2026,11,24),entrega.fechaEstimada());
    }

    @Test void informaDisponibilidadYCalendarioParcialSinConsultarFuentes() {
        var referencias = new ReferenciasSubastas(Optional::empty,anno -> Optional.empty(),reloj);
        assertFalse(referencias.cotizar(BigDecimal.TEN).disponible());
        assertFalse(referencias.entrega(1).incluyeFestivos());
        assertEquals(LocalDate.of(2026,11,23),referencias.entrega(1).fechaEstimada());
    }
}