package com.ecommerce.catalogo.infraestructura.driver_adapters.jpa_repository.producto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static com.ecommerce.catalogo.infraestructura.driver_adapters.jpa_repository.producto.CatalogoVisible.NOMBRE_PUBLICO;

public interface ProductoDataJpaRepository extends JpaRepository<ProductoData, Long>, JpaSpecificationExecutor<ProductoData> {
    java.util.List<ProductoData> findAllByEstadoPublicacion(String estado);
    java.util.Optional<ProductoData> findByIdAndEstadoPublicacion(Long id, String estado);
    @Query("select p from ProductoData p where p.estadoPublicacion = 'ACTIVA'" + NOMBRE_PUBLICO + "order by p.id desc")
    java.util.List<ProductoData> recientesPublicos(Pageable pagina);

    static Specification<ProductoData> nombrePublico() {
        return CatalogoVisible.nombrePublico();
    }

    @Query(value = """
            SELECT candidatos.* FROM (
                SELECT p.*, ROW_NUMBER() OVER(PARTITION BY categoria ORDER BY id DESC) AS lugar
                FROM products p WHERE estado_publicacion='ACTIVA' AND stock>0
            """ + NOMBRE_PUBLICO + """
            ) candidatos WHERE lugar<=2 ORDER BY id DESC
            """,nativeQuery=true)
    java.util.List<ProductoData> candidatosInicio();

    @Query("select p.categoria as categoria, count(p) as total from ProductoData p "
            + "where p.estadoPublicacion = 'ACTIVA' and p.categoria is not null" + NOMBRE_PUBLICO
            + "group by p.categoria order by p.categoria")
    java.util.List<ConteoCategoria> contarPorCategoria();

    interface ConteoCategoria {
        String getCategoria();
        long getTotal();
    }

    @Query("select p.id as id, coalesce(p.stock, 0) as stock from ProductoData p "
            + "where p.id in :ids and p.estadoPublicacion = 'ACTIVA'")
    java.util.List<Existencia> consultarStock(@Param("ids") java.util.List<Long> ids);

    interface Existencia {
        Long getId();
        Integer getStock();
    }

    @Query("select distinct p.marca from ProductoData p where p.estadoPublicacion = 'ACTIVA' and p.marca is not null"
            + NOMBRE_PUBLICO + "order by p.marca")
    java.util.List<String> marcasActivas();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ProductoData p set p.stock = p.stock - :cantidad "
            + "where p.id = :id and p.stock >= :cantidad and p.estadoPublicacion = 'ACTIVA'")
    int descontarStock(@Param("id") Long id, @Param("cantidad") int cantidad);
}
