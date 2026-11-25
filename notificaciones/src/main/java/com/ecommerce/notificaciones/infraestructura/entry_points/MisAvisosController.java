package com.ecommerce.notificaciones.infraestructura.entry_points;

import com.ecommerce.notificaciones.application.port.out.HistorialAvisos;
import com.ecommerce.notificaciones.application.port.out.HistorialAvisos.Aviso;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
class MisAvisosController {
    private final HistorialAvisos historial;
    private final JwtParser parser;

    MisAvisosController(HistorialAvisos historial, @Value("${JWT_SECRET:}") String clave) {
        this.historial = historial;
        byte[] bytes = clave.getBytes(StandardCharsets.UTF_8);
        parser = bytes.length < 32 ? null : Jwts.parser().verifyWith(Keys.hmacShaKeyFor(bytes)).build();
    }

    @GetMapping("/api/notificaciones/mis-avisos")
    @Operation(summary = "Consultar por páginas los avisos de la cuenta de la sesión")
    Pagina listar(@RequestHeader(value=HttpHeaders.AUTHORIZATION, required=false) String autorizacion,
                  @RequestParam(defaultValue="0") int pagina, @RequestParam(defaultValue="20") int tamano) {
        long usuario = usuario(autorizacion);
        if (pagina < 0 || pagina > 10000 || tamano < 1 || tamano > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Paginación inválida");
        }
        var items = historial.listar(usuario,pagina,tamano);
        return new Pagina(items.stream().limit(tamano).toList(),pagina,items.size() > tamano);
    }

    private long usuario(String autorizacion) {
        if (parser != null && autorizacion != null && autorizacion.startsWith("Bearer ")) {
            try {
                var claims = parser.parseSignedClaims(autorizacion.substring(7)).getPayload();
                long id = Long.parseLong(claims.getSubject());
                if (claims.getExpiration() != null && id > 0) return id;
            } catch (JwtException | IllegalArgumentException invalido) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sesión inválida");
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Inicia sesión");
    }

    record Pagina(List<Aviso> items, int pagina, boolean siguiente) {}
}