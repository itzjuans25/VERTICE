package com.ecommerce.catalogo.infraestructura.entry_points;

import com.ecommerce.catalogo.infraestructura.driver_adapters.jpa_repository.producto.ProductoDataJpaRepository;
import com.ecommerce.catalogo.infraestructura.entry_points.dto.ProductoResponse;
import com.ecommerce.catalogo.infraestructura.entry_points.mapper.TiendaDtoMapper;
import com.ecommerce.catalogo.infraestructura.mapper.MapperProducto;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@RequiredArgsConstructor
class InicioTiendaController {
    private final ProductoDataJpaRepository productos;
    private final MapperProducto mapper;
    private final TiendaDtoMapper dto;
    private Inicio resumen;
    private long actualizado;

    @GetMapping("/api/ecommerce/inicio")
    @Operation(summary = "Ocho productos recientes y conteos por categoría")
    synchronized Inicio inicio() {
        long ahora = System.nanoTime();
        if (resumen == null || ahora - actualizado >= Duration.ofSeconds(30).toNanos()) {
            var recientes = productos.recientesPublicos(PageRequest.of(0, 8)).stream()
                    .map(mapper::toProducto).map(dto::toResponse).toList();
            var categorias = productos.contarPorCategoria().stream()
                    .map(c -> new Categoria(c.getCategoria(), c.getTotal())).toList();
            var disponibles = productos.candidatosInicio().stream().map(mapper::toProducto).map(dto::toResponse).toList();
            resumen = new Inicio(recientes, categorias, disponibles);
            actualizado = ahora;
        }
        return resumen;
    }

    record Inicio(List<ProductoResponse> items, List<Categoria> categorias, List<ProductoResponse> destacables) {}
    record Categoria(String categoria, long total) {}
}
