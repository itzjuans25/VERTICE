package com.ecommerce.subastas.application.port.out;

import com.ecommerce.subastas.application.port.in.Subastas.*;
import com.ecommerce.subastas.domain.Lote;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public interface RepositorioSubastas {
    <T> T transaccion(Supplier<T> accion);
    Lote insertar(Lote lote);
    Optional<Lote> lote(long id, boolean bloquear);
    Optional<Lote> lotePorCertificado(String codigo);
    Lote actualizar(Lote lote);
    Pagina listar(Filtro filtro);
    Inicio inicio(Instant ahora);
    void registrar(Puja puja);
    List<Puja> historial(long id);
    MisPujas mias(long usuario);
    Optional<Orden> orden(long lote);
    void adjudicar(Lote lote);
    Orden pagar(long id, Instant fecha);
    List<Long> pendientes(Instant ahora);
    void avisar(String clave, Long usuarioId, String destinatario, String asunto, String mensaje);
}
