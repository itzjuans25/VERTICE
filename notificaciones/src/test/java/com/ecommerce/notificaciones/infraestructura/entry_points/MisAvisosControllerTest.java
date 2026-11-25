package com.ecommerce.notificaciones.infraestructura.entry_points;

import com.ecommerce.notificaciones.application.port.out.HistorialAvisos;
import com.ecommerce.notificaciones.application.port.out.HistorialAvisos.Aviso;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MisAvisosControllerTest {
    private final String clave = "k".repeat(40);
    private final HistorialAvisos historial = mock(HistorialAvisos.class);
    private final MisAvisosController controller = new MisAvisosController(historial,clave);

    @Test
    void consultaSoloLaCuentaFirmadaYPaginaLaRespuesta() {
        var registro = new Aviso(1,"EMAIL","Compra confirmada","ENVIADO",Instant.EPOCH);
        when(historial.listar(41,2,1)).thenReturn(List.of(registro,registro));
        var pagina = controller.listar(token("41",Instant.now().plusSeconds(100)),2,1);
        assertEquals(List.of(registro),pagina.items());
        assertEquals(2,pagina.pagina());
        assertTrue(pagina.siguiente());
        verify(historial).listar(41,2,1);
    }

    @Test
    void rechazaTokenAusenteVencidoOConUsuarioInvalido() {
        assertEquals(401, assertThrows(ResponseStatusException.class,() -> controller.listar(null,0,20)).getStatusCode().value());
        assertEquals(401, assertThrows(ResponseStatusException.class,() -> controller.listar(token("41",Instant.EPOCH),0,20)).getStatusCode().value());
        assertEquals(401, assertThrows(ResponseStatusException.class,() -> controller.listar(token("sin-id",Instant.now().plusSeconds(100)),0,20)).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class,() -> controller.listar(token("41",Instant.now().plusSeconds(100)),-1,20)).getStatusCode().value());
        verifyNoInteractions(historial);
    }

    private String token(String usuario, Instant vence) {
        return "Bearer " + Jwts.builder().subject(usuario).expiration(Date.from(vence))
                .signWith(Keys.hmacShaKeyFor(clave.getBytes(StandardCharsets.UTF_8))).compact();
    }
}