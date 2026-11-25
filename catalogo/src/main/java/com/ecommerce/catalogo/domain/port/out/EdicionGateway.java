package com.ecommerce.catalogo.domain.port.out;

import com.ecommerce.catalogo.domain.model.Edicion;

import java.util.List;
import java.util.Optional;

public interface EdicionGateway {
    List<Edicion> listar();
    Optional<Edicion> porCodigo(String codigo);
    Optional<Edicion> porProducto(Long id);
    Optional<Edicion> bloquear(Long productoId);
}
