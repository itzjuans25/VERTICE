package com.ecommerce.subastas.infraestructura.driver_adapters.external_repository;

import com.ecommerce.subastas.application.port.out.CalendarioFestivosPort;
import com.ecommerce.subastas.application.port.out.TasaCambioPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
class ProveedoresSubastas implements TasaCambioPort, CalendarioFestivosPort {
    private final Clock reloj = Clock.system(ZoneId.of("America/Bogota"));
    private final ProveedorEnCache<Tasa> divisas = new ProveedorEnCache<>(reloj,Duration.ofHours(6));
    private final Map<Integer,ProveedorEnCache<Set<LocalDate>>> calendario = new ConcurrentHashMap<>();
    private final RestClient http;
    private final String frankfurter;
    private final String nager;

    ProveedoresSubastas(@Value("${FRANKFURTER_URL:https://api.frankfurter.dev/v2/rate/cop/usd}") String frankfurter,
                        @Value("${NAGER_URL:https://date.nager.at/api/v3/PublicHolidays/{year}/CO}") String nager) {
        this.frankfurter = frankfurter;
        this.nager = nager;
        var tiempos = new SimpleClientHttpRequestFactory();
        tiempos.setConnectTimeout(Duration.ofSeconds(1));
        tiempos.setReadTimeout(Duration.ofSeconds(2));
        http = RestClient.builder().requestFactory(tiempos).build();
    }

    @Scheduled(fixedDelay=60000, scheduler="proveedoresScheduler")
    void renovar() {
        divisas.actualizar(() -> {
            var tasa = http.get().uri(frankfurter).retrieve().body(RespuestaTasa.class);
            if (tasa == null || tasa.rate() == null || tasa.rate().signum() <= 0 || tasa.date() == null) throw new IllegalStateException("Tasa inválida");
            return new Tasa(tasa.rate(),tasa.date());
        });
        int anno = LocalDate.now(reloj).getYear();
        cargarFestivos(anno);
        cargarFestivos(anno + 1);
        calendario.keySet().removeIf(anterior -> anterior < anno);
    }

    private void cargarFestivos(int anno) {
        calendario.computeIfAbsent(anno,a -> new ProveedorEnCache<>(reloj,Duration.ofHours(24))).actualizar(() -> {
            var fechas = http.get().uri(nager,anno).retrieve().body(Festivo[].class);
            if (fechas == null || fechas.length == 0) throw new IllegalStateException("Calendario vacío");
            return Arrays.stream(fechas).map(Festivo::date).collect(Collectors.toUnmodifiableSet());
        });
    }

    @Override public Optional<Tasa> tasa() { return divisas.leer(); }
    @Override public Optional<Set<LocalDate>> festivos(int anno) {
        return Optional.ofNullable(calendario.get(anno)).flatMap(ProveedorEnCache::leer);
    }

    private record RespuestaTasa(BigDecimal rate, LocalDate date) {}
    private record Festivo(LocalDate date) {}
}