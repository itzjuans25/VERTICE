package com.ecommerce.catalogo.domain.port.out;

import com.ecommerce.catalogo.domain.model.Carrito;

public interface CarritoGateway {

    Carrito guardar(Carrito carrito);

    Carrito buscarPorUsuarioId(Long usuarioId);

    Carrito buscarPorUsuarioIdParaCompra(Long usuarioId);

    void eliminarCarrito(Long carritoId);

    void eliminarPorUsuarioId(Long usuarioId);
}
