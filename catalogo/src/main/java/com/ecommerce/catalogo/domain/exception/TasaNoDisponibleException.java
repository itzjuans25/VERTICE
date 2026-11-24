package com.ecommerce.catalogo.domain.exception;

public class TasaNoDisponibleException extends RuntimeException {
    public TasaNoDisponibleException() {
        super("No hay una tasa COP/USD disponible");
    }
}
