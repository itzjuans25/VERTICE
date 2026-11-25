package com.ecommerce.catalogo.domain.model;

import com.ecommerce.catalogo.domain.exception.DatosInvalidosException;

import java.time.Instant;

public record Edicion(Long productoId, int total, int maximoPorCliente, Instant apertura,
                     String codigo, String serie) {
    public void validarCompra(int solicitadas, long compradas, Instant ahora) {
        if (ahora.isBefore(apertura)) {
            throw new DatosInvalidosException("El lanzamiento aún no está disponible");
        }
        if (solicitadas < 1 || solicitadas > maximoPorCliente - compradas) {
            throw new DatosInvalidosException("Superas el máximo de unidades por cliente");
        }
    }
}
