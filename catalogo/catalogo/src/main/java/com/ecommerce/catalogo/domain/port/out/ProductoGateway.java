package com.ecommerce.catalogo.domain.port.out;

import com.ecommerce.catalogo.domain.model.Producto;

import java.util.List;
import java.util.Map;

public interface ProductoGateway {

    Producto guardar(Producto producto);

    /** Retorna {@code null} cuando el producto no existe. */
    Producto buscarPorId(Long id);

    List<Producto> obtenerTodos();

    Map<Long, Integer> consultarStock(List<Long> ids);

    void eliminar(Long id);

    /**
     * Descuenta stock de forma atomica.
     *
     * @return {@code true} si habia stock suficiente y se desconto;
     *         {@code false} en caso contrario.
     */
    boolean descontarStock(Long productoId, int cantidad);
}
