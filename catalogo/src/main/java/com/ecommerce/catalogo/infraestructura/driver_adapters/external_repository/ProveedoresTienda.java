package com.ecommerce.catalogo.infraestructura.driver_adapters.external_repository;

import com.ecommerce.catalogo.application.usecase.InformacionTienda;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Component
@EnableScheduling
@ConditionalOnProperty(name = "app.integraciones.enabled", matchIfMissing = true)
public class ProveedoresTienda {
    private final InformacionTienda informacion;
    private final RestClient cliente;
    private final String frankfurter;
    private final String nager;

    @Autowired
    public ProveedoresTienda(InformacionTienda informacion,
                            @Value("${app.integraciones.frankfurter}") String frankfurter,
                            @Value("${app.integraciones.nager}") String nager) {
        this(informacion, frankfurter, nager, clienteConTimeout());
    }

    ProveedoresTienda(InformacionTienda informacion, String frankfurter, String nager, RestClient cliente) {
        this.informacion = informacion;
        this.frankfurter = frankfurter;
        this.nager = nager;
        this.cliente = cliente;
    }

    // La latencia de Internet queda fuera del camino de las peticiones de la tienda.
    @Scheduled(fixedDelayString = "${app.integraciones.actualizacion-ms:300000}")
    public void actualizar() {
        try {
            TasaDto tasa = cliente.get().uri(frankfurter).retrieve().body(TasaDto.class);
            if (tasa != null) {
                informacion.actualizarTasa(tasa.rate(), tasa.date());
            }
        } catch (RestClientException ex) {
            log.warn("Frankfurter no disponible: {}", ex.getClass().getSimpleName());
        }
        int ano = informacion.hoy().getYear();
        cargarFestivos(ano);
        cargarFestivos(ano + 1);
    }

    private void cargarFestivos(int ano) {
        try {
            FestivoDto[] respuesta = cliente.get().uri(nager, ano).retrieve().body(FestivoDto[].class);
            if (respuesta != null && respuesta.length > 0) {
                informacion.actualizarFestivos(ano, Arrays.stream(respuesta)
                        .map(FestivoDto::date)
                        .collect(Collectors.toSet()));
            }
        } catch (RestClientException ex) {
            log.warn("Nager.Date no disponible para {}: {}", ano, ex.getClass().getSimpleName());
        }
    }

    private static RestClient clienteConTimeout() {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(2000);
        fabrica.setReadTimeout(2000);
        return RestClient.builder().requestFactory(fabrica).build();
    }

    private record TasaDto(BigDecimal rate, LocalDate date) {}
    private record FestivoDto(LocalDate date) {}
}
