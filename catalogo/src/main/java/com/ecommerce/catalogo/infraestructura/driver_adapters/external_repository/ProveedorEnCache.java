package com.ecommerce.catalogo.infraestructura.driver_adapters.external_repository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

final class ProveedorEnCache<T> {
    private final Clock reloj;
    private final Duration ttl;
    private volatile Dato<T> dato;
    private Instant reabrir = Instant.MIN;
    private int fallos;

    ProveedorEnCache(Clock reloj, Duration ttl) {
        this.reloj = reloj;
        this.ttl = ttl;
    }

    Optional<T> leer() {
        Dato<T> actual = dato;
        return actual != null && reloj.instant().isBefore(actual.fecha().plus(ttl))
                ? Optional.of(actual.valor()) : Optional.empty();
    }

    synchronized Optional<T> actualizar(Supplier<T> carga) {
        if (leer().isPresent() || reloj.instant().isBefore(reabrir)) return Optional.empty();
        for (int intento = 0; intento <= 1; intento++) {
            try {
                T nuevo = java.util.Objects.requireNonNull(carga.get());
                dato = new Dato<>(nuevo, reloj.instant());
                fallos = 0;
                return Optional.of(nuevo);
            } catch (RuntimeException fallo) {
                // El caché existente conserva su fecha y la API no espera al proveedor.
            }
        }
        if (++fallos >= 3) {
            reabrir = reloj.instant().plusSeconds(60);
            fallos = 0;
        }
        return Optional.empty();
    }

    private record Dato<T>(T valor, Instant fecha) {}
}