package com.ecommerce.catalogo.infraestructura.driver_adapters.jpa_repository.compra;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface CertificadoDataRepository extends JpaRepository<CertificadoData, String> {
    List<CertificadoData> findAllByCompraIdOrderByProductoId(Long compraId);
}
