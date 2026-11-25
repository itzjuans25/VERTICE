package com.ecommerce.notificaciones.application.port.out;

import java.time.Instant;
import java.util.List;

public interface HistorialAvisos {
    record Aviso(long id, String tipo, String asunto, String estado, Instant fecha) {}
    void registrar(long usuarioId, String tipo, String asunto, String estado);
    List<Aviso> listar(long usuarioId, int pagina, int tamano);
}