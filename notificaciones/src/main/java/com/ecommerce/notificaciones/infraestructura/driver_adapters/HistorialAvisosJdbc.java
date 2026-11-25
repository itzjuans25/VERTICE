package com.ecommerce.notificaciones.infraestructura.driver_adapters;

import com.ecommerce.notificaciones.application.port.out.HistorialAvisos;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
class HistorialAvisosJdbc implements HistorialAvisos {
    private final JdbcClient jdbc;

    HistorialAvisosJdbc(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override public void registrar(long usuarioId, String tipo, String asunto, String estado) {
        jdbc.sql("INSERT INTO notificacion_avisos(usuario_id,tipo,asunto,estado) VALUES (:usuario,:tipo,:asunto,:estado)")
                .param("usuario",usuarioId).param("tipo",tipo)
                .param("asunto",asunto == null ? "Aviso" : asunto.substring(0,Math.min(asunto.length(),254)))
                .param("estado",estado).update();
    }

    @Override public List<Aviso> listar(long usuarioId, int pagina, int tamano) {
        return jdbc.sql("""
                SELECT id,tipo,asunto,estado,fecha FROM notificacion_avisos
                WHERE usuario_id=:usuario ORDER BY id DESC LIMIT :limite OFFSET :desplazamiento
                """).param("usuario",usuarioId).param("limite",tamano + 1).param("desplazamiento",(long) pagina * tamano)
                .query((r,n) -> new Aviso(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),
                        r.getTimestamp(5).toInstant())).list();
    }

}