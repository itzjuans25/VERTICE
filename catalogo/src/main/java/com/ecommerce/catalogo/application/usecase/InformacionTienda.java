package com.ecommerce.catalogo.application.usecase;


import com.ecommerce.catalogo.domain.exception.TasaNoDisponibleException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class InformacionTienda {
    private final Clock reloj;
    private volatile Tasa tasa;
    private final Map<Integer, Festivos> festivos = new ConcurrentHashMap<>();

    public InformacionTienda() {
        this(Clock.system(ZoneId.of("America/Bogota")));
    }

    InformacionTienda(Clock reloj) {
        this.reloj = reloj;
    }

    public void actualizarTasa(BigDecimal valor, LocalDate fecha) {
        if (valor != null && valor.signum() > 0 && fecha != null) {
            tasa = new Tasa(valor, fecha, reloj.instant());
        }
    }

    public void actualizarFestivos(int ano, Set<LocalDate> fechas) {
        festivos.put(ano, new Festivos(Set.copyOf(fechas),reloj.instant()));
        festivos.keySet().removeIf(anterior -> anterior < hoy().getYear());
    }

    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    public Cotizacion cotizar(BigDecimal precioCop) {
        return cotizarSiDisponible(precioCop).orElseThrow(TasaNoDisponibleException::new);
    }

    public Optional<Cotizacion> cotizarSiDisponible(BigDecimal precioCop) {
        Tasa actual = tasa;
        if (actual == null || !reloj.instant().isBefore(actual.consultada().plusSeconds(21600))) {
            return Optional.empty();
        }
        return Optional.of(new Cotizacion(precioCop, precioCop.multiply(actual.valor()).setScale(2, RoundingMode.HALF_UP),
                actual.valor(), actual.fecha(), "Frankfurter", actual.consultada()));
    }

    public Entrega estimarEntrega(int diasHabiles) {
        LocalDate fecha = hoy();
        boolean calendarioCompleto = true;
        int pendientes = diasHabiles;
        while (pendientes > 0) {
            fecha = fecha.plusDays(1);
            if (fecha.getDayOfWeek() == DayOfWeek.SATURDAY || fecha.getDayOfWeek() == DayOfWeek.SUNDAY) {
                continue;
            }
            Festivos datos = festivos.get(fecha.getYear());
            Set<LocalDate> fechas = datos != null && reloj.instant().isBefore(datos.consulta().plusSeconds(86400))
                    ? datos.fechas() : null;
            if (fechas == null) {
                calendarioCompleto = false;
            }
            if (fechas == null || !fechas.contains(fecha)) {
                pendientes--;
            }
        }
        return new Entrega(fecha, diasHabiles, calendarioCompleto,
                calendarioCompleto ? "Nager.Date" : "Calendario parcial, pueden faltar festivos");
    }

    private record Tasa(BigDecimal valor, LocalDate fecha, Instant consultada) {}
    private record Festivos(Set<LocalDate> fechas, Instant consulta) {}

    public record Cotizacion(BigDecimal precioCop, BigDecimal precioUsd, BigDecimal tasa,
                             LocalDate fechaTasa, String fuente, Instant ultimaConsulta) {}

    public record Entrega(LocalDate fechaEstimada, int diasHabiles, boolean incluyeFestivos, String fuente) {}
}
