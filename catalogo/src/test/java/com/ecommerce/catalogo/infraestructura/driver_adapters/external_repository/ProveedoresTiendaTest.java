package com.ecommerce.catalogo.infraestructura.driver_adapters.external_repository;

import com.ecommerce.catalogo.application.usecase.InformacionTienda;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ProveedoresTiendaTest {
    private static final String TASA = "https://tasas.test/cop/usd";
    private static final String FESTIVOS = "https://festivos.test/{year}/CO";
    private final InformacionTienda tienda = new InformacionTienda();
    private final RestClient.Builder cliente = RestClient.builder();
    private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(cliente).build();

    @Test
    void reintentaTimeoutYErrorHttpYConservaCacheVigente() {
        int ano = tienda.hoy().getYear();
        String actual = FESTIVOS.replace("{year}", String.valueOf(ano));
        String siguiente = FESTIVOS.replace("{year}", String.valueOf(ano + 1));
        servidor.expect(requestTo(TASA)).andRespond(withException(new SocketTimeoutException()));
        servidor.expect(requestTo(TASA)).andRespond(withSuccess(
                "{\"rate\":0.0003,\"date\":\"2026-11-20\"}", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(actual)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        servidor.expect(requestTo(actual)).andRespond(withSuccess(
                "[{\"date\":\"" + ano + "-01-01\"}]", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(siguiente)).andRespond(withSuccess(
                "[{\"date\":\"" + (ano + 1) + "-01-01\"}]", MediaType.APPLICATION_JSON));

        var proveedores = new ProveedoresTienda(tienda, TASA, FESTIVOS, cliente.build());
        proveedores.actualizar();
        proveedores.actualizar();

        servidor.verify();
        assertEquals(new BigDecimal("3.00"), tienda.cotizar(new BigDecimal("10000")).precioUsd());
        assertTrue(tienda.estimarEntrega(2).incluyeFestivos());
    }
}
