package com.ecommerce.notificaciones.infraestructura.driver_adapters.sms;

import com.ecommerce.notificaciones.domain.model.NotificacionSMS;
import com.ecommerce.notificaciones.domain.port.out.SmsGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "twilio")
public class TwilioSmsAdapter implements SmsGateway {
    private final RestClient cliente;
    private final String cuenta;
    private final String origen;

    public TwilioSmsAdapter(@Value("${TWILIO_ACCOUNT_SID:}") String cuenta,
                            @Value("${TWILIO_AUTH_TOKEN:}") String token,
                            @Value("${TWILIO_FROM:}") String origen) {
        if (!cuenta.matches("AC[0-9a-fA-F]{32}") || token.isBlank() || !origen.matches("\\+[1-9][0-9]{6,14}")) {
            throw new IllegalStateException("Configura TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN y TWILIO_FROM");
        }
        this.cuenta = cuenta;
        this.origen = origen;
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(3000);
        fabrica.setReadTimeout(5000);
        cliente = RestClient.builder().requestFactory(fabrica).baseUrl("https://api.twilio.com")
                .defaultHeaders(headers -> headers.setBasicAuth(cuenta, token)).build();
    }

    @Override
    public void enviarSms(NotificacionSMS sms) {
        var formulario = new LinkedMultiValueMap<String, String>();
        formulario.add("From", origen);
        formulario.add("To", sms.getNumeroTelefono());
        formulario.add("Body", sms.getMensaje());
        cliente.post().uri("/2010-04-01/Accounts/{cuenta}/Messages.json", cuenta)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(formulario)
                .retrieve().toBodilessEntity();
    }
}
