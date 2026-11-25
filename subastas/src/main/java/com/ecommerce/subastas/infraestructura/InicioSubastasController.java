package com.ecommerce.subastas.infraestructura;

import com.ecommerce.subastas.application.port.in.Subastas;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
class InicioSubastasController {
    private final Subastas subastas;
    private Inicio resumen;
    private long actualizado;

    InicioSubastasController(Subastas subastas) { this.subastas = subastas; }

    @GetMapping("/api/subastas/inicio")
    @Operation(summary = "Lotes activos que cierran pronto y conteos por categoría")
    synchronized Inicio inicio() {
        long ahora = System.nanoTime();
        if (resumen == null || ahora - actualizado >= Duration.ofSeconds(15).toNanos()) {
            var datos = subastas.inicio();
            resumen = new Inicio(datos.lotes().stream().map(l -> SubastasController.Publico.de(l,null)).toList(),
                    datos.categorias(), datos.total());
            actualizado = ahora;
        }
        return resumen;
    }

    record Inicio(List<SubastasController.Publico> items, List<Subastas.CategoriaActiva> categorias, long total) {}
}