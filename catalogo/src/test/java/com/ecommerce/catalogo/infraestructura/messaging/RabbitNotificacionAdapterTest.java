package com.ecommerce.catalogo.infraestructura.messaging;

import com.ecommerce.catalogo.domain.model.MensajeNotificacion;
import com.ecommerce.catalogo.domain.model.NotificacionEmail;
import com.ecommerce.catalogo.domain.model.NotificacionSMS;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RabbitNotificacionAdapterTest {
    @Test
    void enrutaCadaTipoDeNotificacionYSerializaSuContenido() {
        var rabbit = mock(RabbitTemplate.class);
        var adapter = new RabbitNotificacionAdapter(rabbit);
        ReflectionTestUtils.setField(adapter, "exchange", "eventos");
        var email = NotificacionEmail.builder().destinatario("ana@example.test").build();
        var sms = NotificacionSMS.builder().numeroTelefono("+573001234567").build();
        adapter.enviarNotificacion(new MensajeNotificacion(MensajeNotificacion.TipoNotificacion.EMAIL, email, null));
        adapter.enviarNotificacion(new MensajeNotificacion(MensajeNotificacion.TipoNotificacion.SMS, null, sms));
        adapter.enviarNotificacion(new MensajeNotificacion(MensajeNotificacion.TipoNotificacion.AMBOS, email, sms));
        verify(rabbit).convertAndSend(eq("eventos"), eq("notificacion.email"),
                argThat((RabbitNotificacionAdapter.NotificacionEvent evento) -> evento.destinatario().equals("ana@example.test")));
        verify(rabbit).convertAndSend(eq("eventos"), eq("notificacion.sms"),
                argThat((RabbitNotificacionAdapter.NotificacionEvent evento) -> evento.destinatario().equals("+573001234567")));
        verify(rabbit).convertAndSend(eq("eventos"), eq("notificacion.ambos"),
                argThat((RabbitNotificacionAdapter.NotificacionEvent evento) -> evento.email() != null && evento.sms() != null));
        assertThrows(IllegalArgumentException.class, () -> adapter.enviarNotificacion(null));
    }
}
