package com.ecommerce.catalogo.infraestructura.driver_adapters.jpa_repository.producto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoDataJpaRepository extends JpaRepository<ProductoData, Long>, JpaSpecificationExecutor<ProductoData> {
    java.util.List<ProductoData> findAllByEstadoPublicacion(String estado);
    java.util.Optional<ProductoData> findByIdAndEstadoPublicacion(Long id, String estado);

    @Query("select p.id as id, coalesce(p.stock, 0) as stock from ProductoData p "
            + "where p.id in :ids and p.estadoPublicacion = 'ACTIVA'")
    java.util.List<Existencia> consultarStock(@Param("ids") java.util.List<Long> ids);

    interface Existencia {
        Long getId();
        Integer getStock();
    }

    @Query("select distinct p.marca from ProductoData p where p.estadoPublicacion = 'ACTIVA' and p.marca is not null order by p.marca")
    java.util.List<String> marcasActivas();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ProductoData p set p.stock = p.stock - :cantidad "
            + "where p.id = :id and p.stock >= :cantidad and p.estadoPublicacion = 'ACTIVA'")
    int descontarStock(@Param("id") Long id, @Param("cantidad") int cantidad);
}
