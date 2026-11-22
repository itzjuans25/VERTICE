package com.ecommerce.catalogo.infraestructura.entry_points;

import com.ecommerce.catalogo.application.port.in.CarritoService;
import com.ecommerce.catalogo.domain.model.Carrito;
import com.ecommerce.catalogo.domain.model.ItemCarrito;
import java.math.BigDecimal;
import com.ecommerce.catalogo.infraestructura.entry_points.dto.AgregarItemRequest;
import com.ecommerce.catalogo.infraestructura.entry_points.dto.CarritoResponse;
import com.ecommerce.catalogo.infraestructura.entry_points.mapper.TiendaDtoMapper;
import com.ecommerce.catalogo.infraestructura.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ecommerce/carrito")
@RequiredArgsConstructor
public class CarritoController {

    private final CarritoService carritoService;
    private final TiendaDtoMapper mapper;

    @PostMapping("/agregar")
    public CarritoResponse agregarItem(@AuthenticationPrincipal AuthenticatedUser usuario,
                                       @Valid @RequestBody AgregarItemRequest request) {
        Long usuarioId = usuarioId(usuario);
        return respuesta(carritoService.agregarProductoAlCarrito(
                usuarioId, request.productoId(), request.cantidad()));
    }

    @GetMapping
    public CarritoResponse verCarrito(@AuthenticationPrincipal AuthenticatedUser usuario) {
        return respuesta(carritoService.obtenerCarrito(usuarioId(usuario)));
    }

    @GetMapping("/resumen")
    public ResumenCarrito resumen(@AuthenticationPrincipal AuthenticatedUser usuario) {
        var carrito = carritoService.obtenerCarrito(usuarioId(usuario));
        int unidades = carrito.getItems().stream().mapToInt(ItemCarrito::getCantidad).sum();
        var stock = carritoService.consultarStock(carrito);
        boolean habilitaCompra = unidades > 0 && carrito.getItems().stream()
                .allMatch(item -> stock.getOrDefault(item.getProductoId(), 0) >= item.getCantidad());
        return new ResumenCarrito(unidades, carrito.getItems().size(), carrito.getPrecioTotal(), habilitaCompra);
    }

    public record ResumenCarrito(int unidades, int productosDistintos, BigDecimal totalCop, boolean habilitaCompra) {}

    @DeleteMapping("/vaciar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void vaciar(@AuthenticationPrincipal AuthenticatedUser usuario) {
        carritoService.vaciarCarrito(usuarioId(usuario));
    }

    @DeleteMapping("/eliminar/{productoId}")
    public CarritoResponse eliminarItemDelCarrito(
            @AuthenticationPrincipal AuthenticatedUser usuario,
            @PathVariable Long productoId) {
        return respuesta(
                carritoService.eliminarProducto(usuarioId(usuario), productoId));
    }

    @PutMapping("/actualizar/{productoId}")
    public CarritoResponse actualizarCantidad(
            @AuthenticationPrincipal AuthenticatedUser usuario,
            @PathVariable Long productoId,
            @RequestParam Integer cantidad) {
        return respuesta(carritoService.actualizarCantidadProducto(
                usuarioId(usuario), productoId, cantidad));
    }

    private CarritoResponse respuesta(Carrito carrito) {
        return mapper.toResponse(carrito, carritoService.consultarStock(carrito));
    }

    private static Long usuarioId(AuthenticatedUser usuario) {
        if (usuario == null) {
            throw new AccessDeniedException("No autenticado");
        }
        return usuario.id();
    }
}
