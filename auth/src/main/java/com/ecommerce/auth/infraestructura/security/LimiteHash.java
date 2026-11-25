package com.ecommerce.auth.infraestructura.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
public class LimiteHash {
    private final Semaphore hashes;
    private final Semaphore admisiones;
    private final PerfilAuth perfil;
    private final long esperaMs;

    public LimiteHash(PerfilAuth perfil, @Value("${AUTH_HASH_CONCURRENCY:0}") int concurrencia,
                      @Value("${AUTH_HASH_WAIT_MS:350}") long esperaMs) {
        int disponibles = concurrencia > 0 ? concurrencia : Runtime.getRuntime().availableProcessors();
        this.hashes = new Semaphore(disponibles, true);
        this.admisiones = new Semaphore(2 * disponibles);
        this.perfil = perfil;
        this.esperaMs = esperaMs;
    }

    public <T> T ejecutar(Supplier<T> accion) {
        if (!admisiones.tryAcquire()) {
            throw new CapacidadHashAgotadaException();
        }
        try {
            perfil.medir("espera_hash", this::adquirir);
            try {
                return perfil.medir("hash", accion);
            } finally {
                hashes.release();
            }
        } finally {
            admisiones.release();
        }
    }

    private boolean adquirir() {
        try {
            if (hashes.tryAcquire(esperaMs, TimeUnit.MILLISECONDS)) {
                return true;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        throw new CapacidadHashAgotadaException();
    }
}
