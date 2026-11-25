package com.ecommerce.catalogo.infraestructura.entry_points;

import com.ecommerce.catalogo.application.usecase.ConsultarEdiciones;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class EdicionesController {
    private final ConsultarEdiciones ediciones;

    EdicionesController(ConsultarEdiciones ediciones) { this.ediciones = ediciones; }

    @GetMapping("/api/ecommerce/lanzamientos")
    List<ConsultarEdiciones.Lanzamiento> lanzamientos() { return ediciones.listar(); }

}
