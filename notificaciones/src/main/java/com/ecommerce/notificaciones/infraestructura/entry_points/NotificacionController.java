package com.ecommerce.notificaciones.infraestructura.entry_points;

import com.ecommerce.notificaciones.application.usecase.ProcesarNotificacionUseCase;
import com.ecommerce.notificaciones.domain.exception.DatosInvalidosException;
import com.ecommerce.notificaciones.domain.model.MensajeNotificacion;
import com.ecommerce.notificaciones.domain.model.NotificacionEmail;
import com.ecommerce.notificaciones.infraestructura.messaging.BandejaNotificaciones;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ecommerce/notificaciones")
public class NotificacionController {
    private final BandejaNotificaciones bandeja;
    private final RabbitProperties rabbitProperties;
    private final String clave;

    public NotificacionController(BandejaNotificaciones bandeja, RabbitProperties rabbitProperties,
            @Value("${app.notificaciones.clave-api:}") String clave) {
        this.bandeja = bandeja;
        this.rabbitProperties = rabbitProperties;
        this.clave = clave;
    }

    @PostMapping("/validar")
    public Validacion validar(@RequestBody MensajeNotificacion mensaje) {
        return new Validacion(true, ProcesarNotificacionUseCase.validar(mensaje));
    }

    @PostMapping("/previsualizar/bienvenida")
    public NotificacionEmail bienvenida(@RequestBody Bienvenida solicitud) {
        return vista("Bienvenido a VÉRTICE", "Hola " + nombre(solicitud.nombre()) + ". Tu cuenta está lista.");
    }

    @PostMapping("/previsualizar/compra")
    public NotificacionEmail compra(@RequestBody Compra solicitud) {
        if (solicitud.orden() < 1 || solicitud.totalCop() == null || solicitud.totalCop().signum() <= 0) {
            throw new DatosInvalidosException("Indica un numero de orden y un total positivo");
        }
        return vista("Confirmación de compra #" + solicitud.orden(),
                "Hola " + nombre(solicitud.nombre()) + ". Total de tu compra: " + solicitud.totalCop() + " COP.");
    }

    @GetMapping("/plan-reintentos")
    public PlanReintentos planReintentos() {
        var retry = rabbitProperties.getListener().getSimple().getRetry();
        int intentos = retry.isEnabled() ? retry.getMaxAttempts() : 1;
        List<Long> esperas = new ArrayList<>();
        long espera = retry.getInitialInterval().toMillis();
        for (int intento = 1; intento < intentos; intento++) {
            esperas.add(espera);
            espera = Math.min((long) (espera * retry.getMultiplier()), retry.getMaxInterval().toMillis());
        }
        return new PlanReintentos(intentos, esperas, "DLQ");
    }

    @PostMapping("/encolar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> encolar(@RequestHeader(value = "X-Notification-Key", defaultValue = "") String recibida,
                                      @RequestBody MensajeNotificacion mensaje) {
        if (clave.isBlank() || !MessageDigest.isEqual(clave.getBytes(StandardCharsets.UTF_8),
                recibida.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Clave de notificaciones requerida");
        }
        ProcesarNotificacionUseCase.validar(mensaje);
        return Map.of("id", bandeja.guardar(mensaje), "estado", "PENDIENTE");
    }

    @ExceptionHandler(DatosInvalidosException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> datosInvalidos(DatosInvalidosException ex) {
        return Map.of("message", ex.getMessage());
    }

    private static NotificacionEmail vista(String asunto, String cuerpo) {
        return NotificacionEmail.builder().asunto(asunto).cuerpo(cuerpo).html(false).build();
    }

    private static String nombre(String valor) {
        if (valor == null || valor.isBlank() || valor.length() > 100) {
            throw new DatosInvalidosException("El nombre debe contener entre 1 y 100 caracteres");
        }
        return valor.strip();
    }

    public record Bienvenida(String nombre) {}
    public record Compra(String nombre, long orden, BigDecimal totalCop) {}
    public record Validacion(boolean valida, List<String> canales) {}
    public record PlanReintentos(int intentos, List<Long> esperasMs, String destinoSiAgota) {}
}
