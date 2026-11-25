package com.ecommerce.subastas.infraestructura;

import com.ecommerce.subastas.application.port.in.Subastas;
import com.ecommerce.subastas.application.usecase.ReferenciasSubastas;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subastas")
class ReferenciasController {
    private final Subastas subastas;
    private final ReferenciasSubastas referencias;

    ReferenciasController(Subastas subastas, ReferenciasSubastas referencias) {
        this.subastas = subastas;
        this.referencias = referencias;
    }

    @GetMapping("/lote/{id}/precio-usd")
    @Operation(summary="Referencia de la última puja en USD usando el caché de Frankfurter")
    ReferenciasSubastas.Cotizacion precio(@PathVariable long id) {
        return referencias.cotizar(subastas.detalle(id).lote().precioActual());
    }

    @GetMapping("/entrega")
    @Operation(summary="Fecha aproximada de entrega en días hábiles colombianos")
    ReferenciasSubastas.Entrega entrega(@RequestParam(defaultValue="3") @Min(1) @Max(30) int diasHabiles) {
        return referencias.entrega(diasHabiles);
    }
}