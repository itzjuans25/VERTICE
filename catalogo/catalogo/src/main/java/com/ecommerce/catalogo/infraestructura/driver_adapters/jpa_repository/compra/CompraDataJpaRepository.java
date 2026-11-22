package com.ecommerce.catalogo.infraestructura.driver_adapters.jpa_repository.compra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface CompraDataJpaRepository extends JpaRepository<CompraData, Long> {

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update CompraData c set c.estado = :nuevo where c.id = :id and c.estado = :anterior")
    int cambiarEstado(Long id, String anterior, String nuevo);

    @EntityGraph(attributePaths = "items")
    List<CompraData> findByUsuarioIdOrderByFechaCompraDesc(Long usuarioId);

    @Override
    @EntityGraph(attributePaths = "items")
    List<CompraData> findAll();
}
