package com.ecommerce.subastas.infraestructura;

import com.ecommerce.subastas.application.port.in.Subastas;
import com.ecommerce.subastas.application.port.out.ClimaDespachoPort;
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
    private final ClimaDespachoPort clima;

    ReferenciasController(Subastas subastas, ReferenciasSubastas referencias, ClimaDespachoPort clima) {
        this.subastas = subastas;
        this.referencias = referencias;
        this.clima = clima;
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

    @GetMapping("/clima")
    @Operation(summary="Condiciones de referencia de la sede simulada de despacho, Bogotá")
    Clima clima() {
        var observacion = clima.bogota().orElse(null);
        return new Clima(observacion != null,"Bogotá","Open-Meteo",observacion);
    }

    record Clima(boolean disponible,String ciudad,String fuente,ClimaDespachoPort.Observacion observacion) {}
}
