package com.ecommerce.catalogo.infraestructura.entry_points;

import com.ecommerce.catalogo.application.usecase.ConsultarEdiciones;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class EdicionesController {
    private final ConsultarEdiciones ediciones;

    EdicionesController(ConsultarEdiciones ediciones) { this.ediciones = ediciones; }

    @GetMapping("/api/ecommerce/lanzamientos")
    List<ConsultarEdiciones.Lanzamiento> lanzamientos() { return ediciones.listar(); }

    @GetMapping("/api/ecommerce/certificados/{codigo}")
    ResponseEntity<ConsultarEdiciones.Certificado> certificado(@PathVariable String codigo) {
        if (!codigo.matches("VT-(E|C)-[A-Z0-9]{6,24}")) return ResponseEntity.badRequest().build();
        return ResponseEntity.of(ediciones.verificar(codigo));
    }
}
