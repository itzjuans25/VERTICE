package com.ecommerce.catalogo.infraestructura.driver_adapters.jpa_repository.producto;

import org.springframework.data.jpa.domain.Specification;

final class CatalogoVisible {
    private static final String CARGA = "k6-%";
    private static final String QA = "qa-%";
    private static final String ENSAYO = "ensayo-%";
    private static final String EQUIPO_PRUEBA = "equipo de prueba %";

    static final String NOMBRE_PUBLICO = " AND lower(p.nombre) NOT LIKE '" + CARGA
            + "' AND lower(p.nombre) NOT LIKE '" + QA
            + "' AND lower(p.nombre) NOT LIKE '" + ENSAYO
            + "' AND lower(p.nombre) NOT LIKE '" + EQUIPO_PRUEBA + "' ";

    private CatalogoVisible() {}

    static Specification<ProductoData> nombrePublico() {
        return (root, query, cb) -> {
            var nombre = cb.lower(root.get("nombre"));
            return cb.and(cb.notLike(nombre, CARGA), cb.notLike(nombre, QA),
                    cb.notLike(nombre, ENSAYO), cb.notLike(nombre, EQUIPO_PRUEBA));
        };
    }
}
