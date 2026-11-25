package com.ecommerce.catalogo.infraestructura.driver_adapters.jpa_repository.edicion;

import com.ecommerce.catalogo.domain.model.Edicion;
import com.ecommerce.catalogo.domain.port.out.EdicionGateway;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class EdicionDataGateway implements EdicionGateway {
    private final EdicionDataRepository repository;

    EdicionDataGateway(EdicionDataRepository repository) {
        this.repository = repository;
    }

    @Override public List<Edicion> listar() {
        return repository.findAllByOrderByAperturaAsc().stream().map(EdicionData::toDomain).toList();
    }
    @Override public Optional<Edicion> porCodigo(String codigo) {
        return repository.findByCodigo(codigo).map(EdicionData::toDomain);
    }
    @Override public Optional<Edicion> porProducto(Long id) {
        return repository.findById(id).map(EdicionData::toDomain);
    }
    @Override public Optional<Edicion> bloquear(Long productoId) {
        return repository.bloquear(productoId).map(EdicionData::toDomain);
    }
}
