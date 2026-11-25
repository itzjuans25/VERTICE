package com.ecommerce.subastas.application.usecase;

import com.ecommerce.subastas.application.port.in.Subastas;
import com.ecommerce.subastas.application.port.out.RepositorioSubastas;
import com.ecommerce.subastas.domain.Estado;
import com.ecommerce.subastas.domain.Lote;
import com.ecommerce.subastas.domain.ReglaSubasta;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

public class GestionSubastas implements Subastas {
    private final RepositorioSubastas repo;
    private final Clock reloj;

    public GestionSubastas(RepositorioSubastas repo, Clock reloj) {
        this.repo = repo;
        this.reloj = reloj;
    }

    @Override
    public Lote crear(Actor actor, Alta alta) {
        exigirAdmin(actor);
        if (alta.inicio().compareTo(alta.fin()) >= 0 || !alta.fin().isAfter(reloj.instant())) {
            throw new ReglaSubasta("Verifica las fechas de inicio y cierre.");
        }
        Estado estado = alta.inicio().isAfter(reloj.instant()) ? Estado.PROGRAMADA : Estado.ACTIVA;
        return repo.insertar(new Lote(0, alta.titulo(), alta.descripcion(), alta.categoria(), alta.condicion(),
                alta.atributos(), alta.imagenes(), alta.precioInicial(), alta.incremento(), alta.precioInicial(),
                null, null, actor.email(), estado, alta.inicio(), alta.fin(), 0, 0));
    }

    @Override
    public Pagina listar(Filtro filtro) {
        if (filtro.minimo() != null && filtro.maximo() != null && filtro.minimo().compareTo(filtro.maximo()) > 0) {
            throw new ReglaSubasta("El precio mínimo supera al máximo.");
        }
        return repo.listar(filtro);
    }

    @Override
    public Detalle detalle(long id) {
        return new Detalle(buscar(id, false), repo.historial(id), repo.orden(id).orElse(null));
    }

    @Override
    public Lote pujar(Actor actor, long id, BigDecimal monto) {
        return repo.transaccion(() -> {
            var lote = buscar(id, true);
            Instant ahora = reloj.instant();
            if (lote.estado() == Estado.PROGRAMADA && !ahora.isBefore(lote.inicio()) && ahora.isBefore(lote.fin())) {
                lote = lote.cambiar(Estado.ACTIVA);
            }
            var nueva = repo.actualizar(lote.ofertar(actor.id(), actor.email(), monto, ahora));
            repo.registrar(new Puja(id, actor.id(), monto, ahora));
            if (lote.ganadorEmail() != null) {
                repo.avisar("superada:" + id + ":" + nueva.version(), lote.ganadorEmail(), "Puja superada",
                        "Otra persona ofreció más en " + lote.titulo() + ".");
            }
            return nueva;
        });
    }

    @Override
    public MisPujas mias(Actor actor) { return repo.mias(actor.id()); }

    @Override
    public Lote cancelar(Actor actor, long id) {
        exigirAdmin(actor);
        return repo.transaccion(() -> {
            var lote = buscar(id, true);
            if (!reloj.instant().isBefore(lote.fin())) throw new ReglaSubasta("El plazo de la subasta terminó.");
            return repo.actualizar(lote.cambiar(Estado.CANCELADA));
        });
    }

    @Override
    public Orden pagar(Actor actor, long id) {
        return repo.transaccion(() -> {
            buscar(id, true);
            var orden = repo.orden(id).orElseThrow(() -> new ReglaSubasta("La subasta no tiene adjudicación."));
            if (orden.usuarioId() != actor.id()) {
                throw new ReglaSubasta("PROHIBIDO", "La adjudicación pertenece a otra cuenta.");
            }
            return orden.pagada() == null ? repo.pagar(orden.id(), reloj.instant()) : orden;
        });
    }

    @Override
    public void procesarReloj() {
        Instant ahora = reloj.instant();
        for (long id : repo.pendientes(ahora)) {
            repo.transaccion(() -> {
                var lote = buscar(id, true);
                if (lote.estado() == Estado.PROGRAMADA && !ahora.isBefore(lote.inicio())) {
                    lote = repo.actualizar(lote.cambiar(Estado.ACTIVA));
                }
                if (lote.estado() != Estado.ACTIVA) return null;
                if (!ahora.isBefore(lote.fin())) {
                    cerrar(lote);
                } else if (!ahora.plusSeconds(600).isBefore(lote.fin())) {
                    String destino = lote.ganadorEmail() == null ? lote.tiendaEmail() : lote.ganadorEmail();
                    repo.avisar("vence:" + id, destino, "Subasta por terminar",
                            "Quedan diez minutos o menos para " + lote.titulo() + ".");
                }
                return null;
            });
        }
    }

    private void cerrar(Lote lote) {
        var cerrada = repo.actualizar(lote.cambiar(Estado.CERRADA));
        if (cerrada.ganadorId() == null) {
            repo.avisar("sin-ganador:" + lote.id(), lote.tiendaEmail(), "Subasta cerrada sin ganador",
                    lote.titulo() + " cerró sin pujas.");
            return;
        }
        repo.adjudicar(cerrada);
        repo.actualizar(cerrada.cambiar(Estado.ADJUDICADA));
        repo.avisar("ganada:" + lote.id(), cerrada.ganadorEmail(), "Subasta ganada",
                "Ganaste " + lote.titulo() + ". Puedes confirmar el pago simulado en Mis pujas.");
    }

    private Lote buscar(long id, boolean bloquear) {
        return repo.lote(id, bloquear).orElseThrow(() -> new ReglaSubasta("NO_ENCONTRADO", "No encontramos el lote."));
    }

    private static void exigirAdmin(Actor actor) {
        if (!actor.admin()) throw new ReglaSubasta("PROHIBIDO", "Esta operación es exclusiva de la tienda.");
    }
}
