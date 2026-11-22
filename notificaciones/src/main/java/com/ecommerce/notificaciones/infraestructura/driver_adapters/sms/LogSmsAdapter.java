package com.ecommerce.notificaciones.infraestructura.driver_adapters.sms;

import com.ecommerce.notificaciones.domain.model.NotificacionSMS;
import com.ecommerce.notificaciones.domain.port.out.SmsGateway;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adaptador SMS por defecto para desarrollo: registra el mensaje en el log.
 * Se activa con {@code SMS_PROVIDER=log} o si no se configura proveedor.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "log", matchIfMissing = true)
public class LogSmsAdapter implements SmsGateway {
    private final JdbcTemplate jdbc;

    @Override
    public void enviarSms(NotificacionSMS notificacion) {
        jdbc.update("INSERT INTO sms_simulado (destinatario, mensaje) VALUES (?, ?)",
                notificacion.getNumeroTelefono(), notificacion.getMensaje());
        log.info("SMS simulado registrado en sms_simulado");
    }
}
