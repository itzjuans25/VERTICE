package com.ecommerce.notificaciones.infraestructura.messaging;

import com.ecommerce.notificaciones.domain.model.MensajeNotificacion;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BandejaNotificacionesTest {
    private final EmbeddedDatabase database =
            new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
                    .generateUniqueName(true).addScript("schema.sql").build();
    private final JdbcTemplate db = new JdbcTemplate(database);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final BandejaNotificaciones bandeja = new BandejaNotificaciones(db, new ObjectMapper(), rabbit, "eventos");

    @AfterEach
    void cerrar() {
        database.shutdown();
    }

    @Test
    void guardaDuranteLaCaidaYPublicaTrasRecuperarse() {
        bandeja.publicarSiguiente();
        String id = bandeja.guardar(aviso(MensajeNotificacion.TipoNotificacion.EMAIL));
        verifyNoInteractions(rabbit);
        doThrow(new AmqpException("broker caido")).doAnswer(llamada -> {
            CorrelationData recibo = llamada.getArgument(4);
            recibo.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).convertAndSend(eq("eventos"), eq("notificacion.email"), any(MensajeNotificacion.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));
        bandeja.publicarSiguiente();
        assertEquals(1, db.queryForObject("SELECT intentos FROM notificaciones_pendientes WHERE id = ?", Integer.class, id));
        db.update("UPDATE notificaciones_pendientes SET siguiente_intento = CURRENT_TIMESTAMP WHERE id = ?", id);
        bandeja.publicarSiguiente();
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM notificaciones_pendientes", Integer.class));
    }

    @Test
    void conservaElAvisoAgotadoSinVolverAPublicarlo() {
        String id = bandeja.guardar(aviso(MensajeNotificacion.TipoNotificacion.AMBOS));
        db.update("UPDATE notificaciones_pendientes SET intentos = 4 WHERE id = ?", id);
        doAnswer(llamada -> {
            CorrelationData recibo = llamada.getArgument(4);
            recibo.getFuture().complete(new CorrelationData.Confirm(false, "rechazado"));
            return null;
        }).when(rabbit).convertAndSend(eq("eventos"), eq("notificacion.ambos"), any(MensajeNotificacion.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));
        bandeja.publicarSiguiente();
        assertEquals(5, db.queryForObject("SELECT intentos FROM notificaciones_pendientes WHERE id = ?", Integer.class, id));
        bandeja.publicarSiguiente();
        verify(rabbit).convertAndSend(eq("eventos"), eq("notificacion.ambos"), any(MensajeNotificacion.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    private static MensajeNotificacion aviso(MensajeNotificacion.TipoNotificacion tipo) {
        return MensajeNotificacion.builder().tipo(tipo).build();
    }
}
