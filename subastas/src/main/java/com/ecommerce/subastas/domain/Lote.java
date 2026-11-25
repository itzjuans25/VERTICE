package com.ecommerce.subastas.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record Lote(long id, String titulo, String descripcion, Categoria categoria, Condicion condicion,
                   Map<String,String> atributos, List<String> imagenes, BigDecimal precioInicial,
                   BigDecimal incremento, BigDecimal precioActual, Long ganadorId, String ganadorEmail,
                   String tiendaEmail, Estado estado, Instant inicio, Instant fin, int pujas, long version) {
    public enum Categoria { RELOJES, JOYERIA, CARROS, MOTOS, ELECTRONICA, COLECCIONABLES, HOGAR, OTROS }
    public enum Condicion { NUEVO, USADO }

    public BigDecimal pujaMinima() { return precioActual.add(incremento); }

    public Lote cambiar(Estado destino) {
        estado.exigirTransicion(destino);
        if (destino == Estado.ADJUDICADA && ganadorId == null) {
            throw new ReglaSubasta("Una subasta sin pujas no se puede adjudicar.");
        }
        return copia(precioActual, ganadorId, ganadorEmail, destino, fin, pujas);
    }

    public Lote ofertar(long usuario, String correo, BigDecimal monto, Instant ahora) {
        if (estado != Estado.ACTIVA || ahora.isBefore(inicio) || !ahora.isBefore(fin)) {
            throw new ReglaSubasta("La subasta no está activa.");
        }
        if (Objects.equals(ganadorId, usuario)) {
            throw new ReglaSubasta("Tu puja ya es la más alta.");
        }
        if (monto == null || monto.compareTo(pujaMinima()) < 0) {
            throw new ReglaSubasta("La puja mínima es " + pujaMinima().toPlainString() + " COP.");
        }
        Instant nuevoFin = Duration.between(ahora, fin).compareTo(Duration.ofMinutes(2)) <= 0
                ? fin.plus(Duration.ofMinutes(2)) : fin;
        return copia(monto, usuario, correo, estado, nuevoFin, pujas + 1);
    }

    private Lote copia(BigDecimal monto, Long ganador, String correo, Estado nuevo, Instant cierre, int cantidad) {
        return new Lote(id, titulo, descripcion, categoria, condicion, atributos, imagenes, precioInicial,
                incremento, monto, ganador, correo, tiendaEmail, nuevo, inicio, cierre, cantidad, version);
    }
}
