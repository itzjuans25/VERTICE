package com.ecommerce.catalogo.infraestructura.driver_adapters.external_repository;

import com.ecommerce.catalogo.domain.exception.ServicioUsuariosNoDisponibleException;
import com.ecommerce.catalogo.domain.exception.UsuarioNoAutenticadoException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class UsuarioGatewayImplTest {
    @Test
    void reenviaAutorizacionYTraduceErroresDelServicioAuth() {
        var rest = new RestTemplate();
        var red = MockRestServiceServer.bindTo(rest).build();
        var gateway = new UsuarioGatewayImpl(rest);
        String base = "http://auth.test";
        ReflectionTestUtils.setField(gateway, "authServiceUrl", base);
        var req = new MockHttpServletRequest();
        req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer prueba");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
        try {
            red.expect(requestTo(base + "/api/ecommerce/usuario/1"))
                    .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer prueba"))
                    .andRespond(withSuccess("{\"id\":1,\"nombre\":\"Ana\"}", MediaType.APPLICATION_JSON));
            red.expect(requestTo(base + "/api/ecommerce/usuario/2")).andRespond(withStatus(HttpStatus.NOT_FOUND));
            red.expect(requestTo(base + "/api/ecommerce/usuario/3")).andRespond(withStatus(HttpStatus.FORBIDDEN));
            red.expect(requestTo(base + "/api/ecommerce/usuario/4")).andRespond(withStatus(HttpStatus.BAD_GATEWAY));
            assertNull(gateway.buscarPorId(null));
            assertEquals("Ana", gateway.buscarPorId(1L).getNombre());
            assertNull(gateway.buscarPorId(2L));
            assertThrows(UsuarioNoAutenticadoException.class, () -> gateway.buscarPorId(3L));
            assertThrows(ServicioUsuariosNoDisponibleException.class, () -> gateway.buscarPorId(4L));
            red.verify();
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
