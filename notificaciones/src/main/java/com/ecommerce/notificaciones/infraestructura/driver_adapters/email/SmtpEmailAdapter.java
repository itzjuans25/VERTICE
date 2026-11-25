package com.ecommerce.notificaciones.infraestructura.driver_adapters.email;

import com.ecommerce.notificaciones.domain.exception.EnvioNotificacionException;
import com.ecommerce.notificaciones.domain.model.NotificacionEmail;
import com.ecommerce.notificaciones.domain.port.out.EmailGateway;
import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Adaptador de salida para email via SMTP (Mailpit en desarrollo, cualquier
 * servidor SMTP en produccion). Reemplaza AWS SES.
 */
@Slf4j
@Component
public class SmtpEmailAdapter implements EmailGateway {

    private final JavaMailSender mailSender;
    private final String from;
    private final String fromName;
    private boolean configurado = true;

    @Value("${spring.mail.properties.mail.smtp.auth:false}")
    private boolean requiereCredenciales;

    @Value("${spring.mail.username:}")
    private String usuario;

    @Value("${spring.mail.password:}")
    private String clave;

    public SmtpEmailAdapter(JavaMailSender mailSender,
                            @Value("${app.mail.from:no-reply@ecommerce.local}") String from,
                            @Value("${app.mail.from-name:Remata}") String fromName) {
        this.mailSender = mailSender;
        this.from = from;
        this.fromName = fromName;
    }

    @PostConstruct
    void verificarConfiguracion() {
        configurado = !requiereCredenciales || (!usuario.isBlank() && !clave.isBlank()
                && !from.isBlank() && !from.endsWith(".local"));
        if (!configurado) {
            log.warn("SMTP autenticado incompleto: configura MAIL_USERNAME, MAIL_PASSWORD y un MAIL_FROM verificado");
        }
    }

    @Override
    public void enviarEmail(NotificacionEmail notificacion) {
        if (!configurado) {
            throw new EnvioNotificacionException("SMTP autenticado sin credenciales o remitente verificado", null);
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, false, StandardCharsets.UTF_8.name());

            helper.setFrom(new InternetAddress(from, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(notificacion.getDestinatario());
            helper.setSubject(notificacion.getAsunto());
            helper.setText(notificacion.getCuerpo(), notificacion.isHtml());

            mailSender.send(message);
            log.info("Email SMTP enviado a {}", notificacion.getDestinatario());
        } catch (Exception e) {
            throw new EnvioNotificacionException(
                    "No se pudo enviar el email a " + notificacion.getDestinatario(), e);
        }
    }
}
