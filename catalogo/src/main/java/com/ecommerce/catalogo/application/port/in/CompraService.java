package com.ecommerce.catalogo.application.port.in;

import com.ecommerce.catalogo.domain.model.Compra;
import com.ecommerce.catalogo.domain.port.out.CompraGateway.Certificado;

import java.util.List;

public interface CompraService {

    Compra realizarCompra(Long usuarioId);

    List<Compra> obtenerComprasPorUsuario(Long usuarioId);

    Compra obtenerCompraPorId(Long compraId);

    List<Compra> obtenerTodasLasCompras();
    Compra cambiarEstado(Long id, String estado);
    Compra pagarSimulado(Long id, Long propietario);
    List<Certificado> certificados(Long id, Long propietario);
}
