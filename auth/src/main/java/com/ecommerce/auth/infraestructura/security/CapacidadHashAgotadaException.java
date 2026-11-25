package com.ecommerce.auth.infraestructura.security;

public class CapacidadHashAgotadaException extends RuntimeException {
    public CapacidadHashAgotadaException() {
        super("El servicio de contraseñas está ocupado. Intenta nuevamente.");
    }
}
