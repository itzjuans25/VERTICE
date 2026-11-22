package com.ecommerce.notificaciones.infraestructura.entry_points;

import com.ecommerce.notificaciones.domain.model.MensajeNotificacion;
import org.junit.jupiter.api.Test;
import com.ecommerce.notificaciones.infraestructura.messaging.BandejaNotificaciones;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NotificacionControllerTest {
    private static final String BASE = "/api/ecommerce/notificaciones";
    private static final String MENSAJE = "{\"tipo\":\"EMAIL\",\"email\":{\"destinatario\":\"ana@example.test\",\"asunto\":\"Hola\",\"cuerpo\":\"Texto\"}}";
    private final BandejaNotificaciones bandeja = mock(BandejaNotificaciones.class);
    private final RabbitProperties propiedades = new RabbitProperties();
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new NotificacionController(bandeja, propiedades, "clave-de-prueba")).build();

    @Test
    void validaYPrevisualizaSinEnviar() throws Exception {
        mvc.perform(post(BASE + "/validar").contentType(MediaType.APPLICATION_JSON).content(MENSAJE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.canales[0]").value("EMAIL"));
        mvc.perform(post(BASE + "/validar").contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"SMS\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(BASE + "/previsualizar/bienvenida").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\" Ana \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cuerpo").value("Hola Ana. Tu cuenta está lista."));
        mvc.perform(post(BASE + "/previsualizar/bienvenida").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\"}")).andExpect(status().isBadRequest());
        mvc.perform(post(BASE + "/previsualizar/compra").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"orden\":2,\"totalCop\":2500}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.asunto").value("Confirmación de compra #2"));
        mvc.perform(post(BASE + "/previsualizar/compra").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"orden\":0}")).andExpect(status().isBadRequest());
        verifyNoInteractions(bandeja);
    }

    @Test
    void calculaLasEsperasLimitadasPorConfiguracion() throws Exception {
        var retry = propiedades.getListener().getSimple().getRetry();
        retry.setEnabled(true);
        retry.setMaxAttempts(4);
        retry.setInitialInterval(Duration.ofSeconds(1));
        retry.setMaxInterval(Duration.ofSeconds(3));
        retry.setMultiplier(2);
        mvc.perform(get(BASE + "/plan-reintentos")).andExpect(status().isOk())
                .andExpect(jsonPath("$.esperasMs[0]").value(1000))
                .andExpect(jsonPath("$.esperasMs[1]").value(2000))
                .andExpect(jsonPath("$.esperasMs[2]").value(3000));
    }

    @Test
    void exigeClaveYConfirmaQueElAvisoQuedoGuardado() throws Exception {
        mvc.perform(post(BASE + "/encolar").contentType(MediaType.APPLICATION_JSON).content(MENSAJE))
                .andExpect(status().isForbidden());
        verifyNoInteractions(bandeja);
        when(bandeja.guardar(any(MensajeNotificacion.class))).thenReturn("aviso-1");
        mvc.perform(post(BASE + "/encolar").header("X-Notification-Key", "clave-de-prueba")
                        .contentType(MediaType.APPLICATION_JSON).content(MENSAJE))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.id").value("aviso-1"));
        verify(bandeja).guardar(any(MensajeNotificacion.class));
    }
}
