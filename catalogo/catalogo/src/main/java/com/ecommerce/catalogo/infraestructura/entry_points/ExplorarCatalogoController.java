package com.ecommerce.catalogo.infraestructura.entry_points;

import com.ecommerce.catalogo.infraestructura.driver_adapters.jpa_repository.producto.ProductoData;
import com.ecommerce.catalogo.infraestructura.driver_adapters.jpa_repository.producto.ProductoDataJpaRepository;
import com.ecommerce.catalogo.infraestructura.entry_points.dto.ProductoResponse;
import com.ecommerce.catalogo.infraestructura.entry_points.mapper.TiendaDtoMapper;
import com.ecommerce.catalogo.infraestructura.mapper.MapperProducto;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/ecommerce/producto")
@RequiredArgsConstructor
public class ExplorarCatalogoController {
    private final ProductoDataJpaRepository productos;
    private final MapperProducto mapper;
    private final TiendaDtoMapper dto;

    public static final List<String> CATEGORIAS = List.of(
            "Computadores y clones", "Procesadores", "Memoria RAM", "Almacenamiento", "Tarjetas gráficas",
            "Placas base", "Fuentes y cajas", "Periféricos", "Celulares y tablets", "Consolas", "Redes y accesorios");

    @GetMapping("/marcas")
    @Operation(summary = "Marcas de los productos disponibles en el catálogo")
    public List<String> marcas() { return productos.marcasActivas(); }

    @GetMapping("/categorias")
    @Operation(summary = "Consultar las categorías disponibles")
    public List<String> categorias() { return CATEGORIAS; }

    @GetMapping("/explorar")
    @Operation(summary = "Buscar productos de la tienda con filtros, orden y paginación")
    public Pagina explorar(@RequestParam(defaultValue = "0") int pagina,
                           @RequestParam(defaultValue = "12") int tamano,
                           @RequestParam(defaultValue = "") String q,
                           @RequestParam(defaultValue = "") String categoria,
                           @RequestParam(defaultValue = "") String condicion,
                           @RequestParam(defaultValue = "") String marca,
                           @RequestParam(required = false) BigDecimal minimo,
                           @RequestParam(required = false) BigDecimal maximo,
                           @RequestParam(defaultValue = "recientes") String orden) {
        if (pagina < 0 || pagina > 10000 || tamano < 1 || tamano > 48 || q.length() > 120
                || minimo != null && minimo.signum() < 0 || maximo != null && maximo.signum() < 0
                || minimo != null && maximo != null && minimo.compareTo(maximo) > 0
                || !categoria.isEmpty() && !CATEGORIAS.contains(categoria)
                || !condicion.isEmpty() && !Set.of("NUEVO", "USADO").contains(condicion)
                || marca.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Revisa los parámetros de búsqueda");
        }
        Sort recientes = Sort.by(Sort.Direction.DESC, "id");
        Sort sort = switch (orden) {
            case "recientes" -> recientes;
            case "precio-asc" -> Sort.by("precio").and(recientes);
            case "precio-desc" -> Sort.by(Sort.Direction.DESC, "precio").and(recientes);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Orden no disponible");
        };
        Specification<ProductoData> filtro = (root, query, cb) -> {
            var criterios = new ArrayList<Predicate>();
            criterios.add(cb.equal(root.get("estadoPublicacion"), "ACTIVA"));
            if (!q.isBlank()) {
                var patron = "%" + q.strip().toLowerCase(Locale.ROOT)
                        .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
                criterios.add(cb.or(cb.like(cb.lower(root.get("nombre")), patron, '!'),
                        cb.like(cb.lower(root.get("descripcion")), patron, '!'),
                        cb.like(cb.lower(root.get("modelo")), patron, '!')));
            }
            if (!categoria.isEmpty()) criterios.add(cb.equal(root.get("categoria"), categoria));
            if (!condicion.isEmpty()) criterios.add(cb.equal(root.get("condicion"), condicion));
            if (!marca.isBlank()) criterios.add(cb.equal(root.get("marca"), marca));
            if (minimo != null) criterios.add(cb.greaterThanOrEqualTo(root.get("precio"), minimo));
            if (maximo != null) criterios.add(cb.lessThanOrEqualTo(root.get("precio"), maximo));
            return cb.and(criterios.toArray(Predicate[]::new));
        };
        var resultado = productos.findAll(filtro, PageRequest.of(pagina, tamano, sort));
        return new Pagina(resultado.stream().map(mapper::toProducto).map(dto::toResponse).toList(),
                resultado.getTotalElements(), resultado.getTotalPages(), pagina);
    }

    public record Pagina(List<ProductoResponse> items, long total, int paginas, int pagina) {}
}
