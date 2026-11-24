package com.ecommerce.catalogo.application.usecase;

import org.junit.jupiter.api.Test;
import com.ecommerce.catalogo.domain.exception.TasaNoDisponibleException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InformacionTiendaTest {
    private final InformacionTienda tienda = new InformacionTienda(Clock.fixed(
            Instant.parse("2026-11-20T16:00:00Z"), ZoneId.of("America/Bogota")));

    @Test
    void aplicaFestivoDespuesDelFinDeSemana() {
        LocalDate lunes = LocalDate.of(2026, 11, 23);
        assertEquals(lunes, tienda.estimarEntrega(1).fechaEstimada());
        assertFalse(tienda.estimarEntrega(1).incluyeFestivos());
        tienda.actualizarFestivos(2026, Set.of(lunes));
        assertEquals(lunes.plusDays(1), tienda.estimarEntrega(1).fechaEstimada());
        assertTrue(tienda.estimarEntrega(1).incluyeFestivos());
    }

    @Test
    void sinTasaNoDevuelveUnPrecioInventado() {
        tienda.actualizarTasa(BigDecimal.ZERO, tienda.hoy());
        assertThrows(TasaNoDisponibleException.class, () -> tienda.cotizar(BigDecimal.TEN));
        tienda.actualizarTasa(new BigDecimal("0.0003"), tienda.hoy());
        assertEquals(new BigDecimal("3.00"), tienda.cotizar(new BigDecimal("10000")).precioUsd());
    }
}
