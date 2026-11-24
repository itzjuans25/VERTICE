package com.ecommerce.catalogo.application.port.out;

@FunctionalInterface
public interface DespuesDeConfirmar {
    void ejecutar(Runnable accion);
}
