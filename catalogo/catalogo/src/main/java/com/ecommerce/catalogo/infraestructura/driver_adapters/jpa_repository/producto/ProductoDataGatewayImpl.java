package com.ecommerce.catalogo.infraestructura.driver_adapters.jpa_repository.producto;

import com.ecommerce.catalogo.domain.model.Producto;
import com.ecommerce.catalogo.domain.port.out.ProductoGateway;
import com.ecommerce.catalogo.infraestructura.mapper.MapperProducto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ProductoDataGatewayImpl implements ProductoGateway {

    private final ProductoDataJpaRepository repository;
    private final MapperProducto mapper;

    @Override
    public Producto guardar(Producto producto) {
        return mapper.toProducto(repository.save(mapper.toData(producto)));
    }

    @Override
    public Producto buscarPorId(Long id) {
        return repository.findByIdAndEstadoPublicacion(id, "ACTIVA")
                .map(mapper::toProducto)
                .orElse(null);
    }

    @Override
    public List<Producto> obtenerTodos() {
        return repository.findAllByEstadoPublicacion("ACTIVA").stream()
                .map(mapper::toProducto)
                .toList();
    }

    @Override
    public Map<Long, Integer> consultarStock(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return repository.consultarStock(ids).stream().collect(Collectors.toMap(
                ProductoDataJpaRepository.Existencia::getId, ProductoDataJpaRepository.Existencia::getStock));
    }

    @Override
    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    @Override
    public boolean descontarStock(Long productoId, int cantidad) {
        return repository.descontarStock(productoId, cantidad) == 1;
    }
}
