package com.ecommerce.catalogo.infraestructura.driver_adapters.jpa_repository.edicion;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

interface EdicionDataRepository extends JpaRepository<EdicionData, Long> {
    List<EdicionData> findAllByOrderByAperturaAsc();
    Optional<EdicionData> findByCodigo(String codigo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EdicionData e where e.productoId = :id")
    Optional<EdicionData> bloquear(Long id);
}
