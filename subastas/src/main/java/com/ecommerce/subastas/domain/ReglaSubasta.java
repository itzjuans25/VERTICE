package com.ecommerce.subastas.domain;

public class ReglaSubasta extends RuntimeException {
    private final String codigo;

    public ReglaSubasta(String mensaje) { this("CONFLICTO", mensaje); }

    public ReglaSubasta(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String codigo() { return codigo; }
}
