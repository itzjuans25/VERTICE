package com.ecommerce.notificaciones.infraestructura.messaging;

import com.ecommerce.notificaciones.domain.model.MensajeNotificacion;
import com.ecommerce.notificaciones.infraestructura.config.RabbitConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@EnableScheduling
@Slf4j
public class BandejaNotificaciones {
    private static final int MAX_INTENTOS = 5;
    private final JdbcTemplate db;
    private final ObjectMapper json;
    private final RabbitTemplate rabbit;
    private final String exchange;

    public BandejaNotificaciones(JdbcTemplate db, ObjectMapper json, RabbitTemplate rabbit,
                                 @Value("${app.notificaciones.exchange}") String exchange) {
        this.db = db;
        this.json = json;
        this.rabbit = rabbit;
        this.exchange = exchange;
    }

    public String guardar(MensajeNotificacion mensaje) {
        String id = UUID.randomUUID().toString();
        try {
            db.update("INSERT INTO notificaciones_pendientes (id, contenido) VALUES (?, ?)",
                    id, json.writeValueAsString(mensaje));
            return id;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar la notificacion", ex);
        }
    }

    // El bloqueo afecta solo al publicador. Aceptar un aviso nuevo no espera a RabbitMQ.
    @Scheduled(fixedDelay = 100, initialDelayString = "${app.notificaciones.bandeja.initial-delay:2000}")
    @Transactional
    public void publicarSiguiente() {
        var resultados = db.query("""
                SELECT id, contenido, intentos FROM notificaciones_pendientes
                WHERE intentos < ? AND siguiente_intento <= CURRENT_TIMESTAMP
                ORDER BY siguiente_intento, id LIMIT 1 FOR UPDATE SKIP LOCKED
                """, (fila, numero) -> new Pendiente(fila.getString(1), fila.getString(2), fila.getInt(3)), MAX_INTENTOS);
        if (resultados.isEmpty()) {
            return;
        }
        Pendiente pendiente = resultados.get(0);
        try {
            var mensaje = json.readValue(pendiente.contenido(), MensajeNotificacion.class);
            String ruta = switch (mensaje.getTipo()) {
                case EMAIL -> RabbitConfig.ROUTING_EMAIL;
                case SMS -> RabbitConfig.ROUTING_SMS;
                case AMBOS -> RabbitConfig.ROUTING_AMBOS;
            };
            var recibo = new CorrelationData();
            rabbit.convertAndSend(exchange, ruta, mensaje, amqp -> {
                amqp.getMessageProperties().setMessageId(pendiente.id());
                return amqp;
            }, recibo);
            if (recibo.getFuture().get(750, TimeUnit.MILLISECONDS).isAck() && recibo.getReturned() == null) {
                db.update("DELETE FROM notificaciones_pendientes WHERE id = ?", pendiente.id());
                return;
            }
            reintentar(pendiente, "publicacion rechazada o sin ruta");
        } catch (AmqpException | ExecutionException | TimeoutException ex) {
            reintentar(pendiente, ex.getClass().getSimpleName());
        } catch (JsonProcessingException ex) {
            db.update("UPDATE notificaciones_pendientes SET intentos = ? WHERE id = ?", MAX_INTENTOS, pendiente.id());
            log.warn("Aviso {} ilegible, conservado para revision", pendiente.id());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void reintentar(Pendiente pendiente, String motivo) {
        int intento = pendiente.intentos() + 1;
        db.update("UPDATE notificaciones_pendientes SET intentos = ?, siguiente_intento = ? WHERE id = ?",
                intento, Timestamp.from(Instant.now().plusSeconds(1L << intento)), pendiente.id());
        log.warn("Aviso {} sin confirmar en intento {} de {} ({})", pendiente.id(), intento, MAX_INTENTOS, motivo);
    }

    private record Pendiente(String id, String contenido, int intentos) {}
}
