package com.ecommerce.catalogo.infraestructura.driver_adapters.jpa_repository.carrito;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface CarritoDataJpaRepository extends JpaRepository<CarritoData, Long> {

    Optional<CarritoData> findByUsuarioId(Long usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CarritoData c where c.usuarioId = :usuarioId")
    Optional<CarritoData> buscarParaCompra(@Param("usuarioId") Long usuarioId);
}
