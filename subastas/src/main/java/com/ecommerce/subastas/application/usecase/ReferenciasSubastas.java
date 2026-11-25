package com.ecommerce.subastas.application.usecase;

import com.ecommerce.subastas.application.port.out.CalendarioFestivosPort;
import com.ecommerce.subastas.application.port.out.TasaCambioPort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;

public class ReferenciasSubastas {
    private final TasaCambioPort divisas;
    private final CalendarioFestivosPort calendario;
    private final Clock reloj;

    public ReferenciasSubastas(TasaCambioPort divisas, CalendarioFestivosPort calendario, Clock reloj) {
        this.divisas = divisas;
        this.calendario = calendario;
        this.reloj = reloj;
    }

    public Cotizacion cotizar(BigDecimal cop) {
        return divisas.tasa().map(t -> new Cotizacion(true,cop,cop.multiply(t.valor()).setScale(2,RoundingMode.HALF_UP),
                t.valor(),t.fecha(),"Frankfurter")).orElseGet(() -> new Cotizacion(false,cop,null,null,null,"Frankfurter"));
    }

    public Entrega entrega(int dias) {
        var fecha = LocalDate.now(reloj);
        boolean completo = true;
        for (int restantes = dias; restantes > 0;) {
            fecha = fecha.plusDays(1);
            var dia = fecha.getDayOfWeek();
            if (dia == DayOfWeek.SUNDAY || dia == DayOfWeek.SATURDAY) continue;
            var festivos = calendario.festivos(fecha.getYear());
            completo &= festivos.isPresent();
            if (festivos.isEmpty() || !festivos.get().contains(fecha)) restantes--;
        }
        return new Entrega(fecha,dias,completo,completo ? "Nager.Date" : "Calendario parcial");
    }

    public record Cotizacion(boolean disponible, BigDecimal precioCop, BigDecimal precioUsd,
                             BigDecimal tasa, LocalDate fechaTasa, String fuente) {}
    public record Entrega(LocalDate fechaEstimada, int diasHabiles, boolean incluyeFestivos, String fuente) {}
}