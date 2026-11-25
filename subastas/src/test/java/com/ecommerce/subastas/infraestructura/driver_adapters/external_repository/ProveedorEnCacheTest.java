package com.ecommerce.subastas.infraestructura.driver_adapters.external_repository;

import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ProveedorEnCacheTest {
    @Test void conservaElDatoHastaElTtlYReintentaSinBloquearLecturas() {
        var reloj = new Reloj();
        var cache = new ProveedorEnCache<String>(reloj,Duration.ofHours(6));
        var consultas = new AtomicInteger();
        assertEquals("tasa",cache.actualizar(() -> {
            if (consultas.incrementAndGet() == 1) throw new IllegalStateException("timeout");
            return "tasa";
        }).orElseThrow());
        assertTrue(cache.actualizar(() -> { fail("El dato vigente evita otra llamada"); return ""; }).isEmpty());
        assertEquals(2,consultas.get());
        assertEquals("tasa",cache.leer().orElseThrow());
        reloj.avanzar(21600);
        assertTrue(cache.leer().isEmpty());
    }

    @Test void abreDespuesDeTresFallosYDejaProbarTrasUnMinuto() {
        var reloj = new Reloj();
        var cache = new ProveedorEnCache<String>(reloj,Duration.ofHours(24));
        var consultas = new AtomicInteger();
        for (int i=0;i<3;i++) cache.actualizar(() -> {
            consultas.incrementAndGet();
            throw new IllegalStateException("proveedor no disponible");
        });
        assertEquals(6,consultas.get());
        assertTrue(cache.actualizar(() -> { fail("Circuito abierto"); return ""; }).isEmpty());
        reloj.avanzar(60);
        assertEquals("calendario",cache.actualizar(() -> "calendario").orElseThrow());
    }

    private static class Reloj extends Clock {
        private Instant instante = Instant.parse("2026-11-24T12:00:00Z");
        void avanzar(long segundos) { instante = instante.plusSeconds(segundos); }
        @Override public Instant instant() { return instante; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zona) { return this; }
    }
}