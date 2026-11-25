package com.ecommerce.catalogo.infraestructura.driver_adapters.jpa_repository.compra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface CompraDataJpaRepository extends JpaRepository<CompraData, Long> {

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update CompraData c set c.estado = :nuevo where c.id = :id and c.estado = :anterior")
    int cambiarEstado(Long id, String anterior, String nuevo);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CompraData c set c.pagoSimuladoEn = :fecha
            where c.id = :id and c.usuarioId = :propietario
              and c.requierePagoSimulado = true and c.pagoSimuladoEn is null and c.estado <> 'CANCELADA'
            """)
    int registrarPagoSimulado(Long id, Long propietario, Instant fecha);

    @Query("""
            select coalesce(sum(i.cantidad), 0) from CompraData c join c.items i
            where c.usuarioId = :usuarioId and i.productoId = :productoId and c.estado <> 'CANCELADA'
            """)
    long unidadesCompradas(Long usuarioId, Long productoId);

    @EntityGraph(attributePaths = "items")
    List<CompraData> findByUsuarioIdOrderByFechaCompraDesc(Long usuarioId);

    @Override
    @EntityGraph(attributePaths = "items")
    List<CompraData> findAll();
}
