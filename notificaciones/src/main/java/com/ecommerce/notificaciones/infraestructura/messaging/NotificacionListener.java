package com.ecommerce.notificaciones.infraestructura.messaging;

import com.ecommerce.notificaciones.domain.model.MensajeNotificacion;
import com.ecommerce.notificaciones.application.port.in.ProcesarNotificacionService;
import com.ecommerce.notificaciones.application.port.out.HistorialAvisos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada: consume los mensajes publicados por auth y catalogo.
 *
 * <p>Los fallos se propagan deliberadamente para que el contenedor de RabbitMQ
 * aplique la politica de reintentos y, agotados estos, derive a la DLQ.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacionListener {

    private final ProcesarNotificacionService procesarNotificacionService;
    private final HistorialAvisos historial;

    @RabbitListener(queues = "${app.notificaciones.queue:notificaciones.queue}")
    public void escuchar(MensajeNotificacion mensaje) {
        log.info("Notificacion recibida: tipo={}", mensaje == null ? null : mensaje.getTipo());
        try {
            procesarNotificacionService.procesar(mensaje);
            registrar(mensaje, "ENVIADO");
        } catch (RuntimeException fallo) {
            registrar(mensaje, "FALLO_DE_ENVIO");
            throw fallo;
        }
    }

    private void registrar(MensajeNotificacion mensaje, String estado) {
        if (mensaje == null || mensaje.getEmail() == null || mensaje.getEmail().getUsuarioId() == null) return;
        try {
            var correo = mensaje.getEmail();
            historial.registrar(correo.getUsuarioId(), mensaje.getTipo().name(), correo.getAsunto(), estado);
        } catch (RuntimeException falloRegistro) {
            log.warn("No se pudo registrar el resultado del aviso: {}", falloRegistro.getClass().getSimpleName());
        }
    }
}
