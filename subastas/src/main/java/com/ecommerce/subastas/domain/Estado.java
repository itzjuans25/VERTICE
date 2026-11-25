package com.ecommerce.subastas.domain;

public enum Estado {
    PROGRAMADA, ACTIVA, CERRADA, ADJUDICADA, CANCELADA;

    public void exigirTransicion(Estado siguiente) {
        boolean valida = switch (this) {
            case PROGRAMADA -> siguiente == ACTIVA || siguiente == CANCELADA;
            case ACTIVA -> siguiente == CERRADA || siguiente == CANCELADA;
            case CERRADA -> siguiente == ADJUDICADA;
            default -> false;
        };
        if (!valida) throw new ReglaSubasta("La transición de " + this + " a " + siguiente + " no está permitida.");
    }
}
