package com.ecommerce.subastas.infraestructura;

import com.ecommerce.subastas.application.port.in.Subastas;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
class ProcesosSubastas {
    private static final Logger LOG=LoggerFactory.getLogger(ProcesosSubastas.class);
    private final Subastas subastas;
    private final RepositorioJdbc repo;
    private final JdbcClient jdbc;
    private final RabbitTemplate rabbit;
    private final ObjectMapper json;
    private final String exchange;

    ProcesosSubastas(Subastas subastas,RepositorioJdbc repo,JdbcClient jdbc,RabbitTemplate rabbit,
                     ObjectMapper json,@Value("${NOTIFICACIONES_EXCHANGE:notificaciones.exchange}") String exchange) {
        this.subastas=subastas;
        this.repo=repo;
        this.jdbc=jdbc;
        this.rabbit=rabbit;
        this.json=json;
        this.exchange=exchange;
    }

    @Bean TopicExchange exchangeSubastas() { return new TopicExchange(exchange,true,false); }

    @Scheduled(fixedDelayString="${SUBASTAS_CIERRE_MS:30000}")
    void revisarReloj() { subastas.procesarReloj(); }

    @Scheduled(fixedDelayString="${SUBASTAS_AVISOS_MS:4000}")
    void publicar() {
        for (int i=0;i<10;i++) {
            Boolean enviado=repo.transaccion(()->jdbc.sql("""
                    SELECT clave,destino,asunto,mensaje,intentos FROM subasta_avisos
                    WHERE enviado IS NULL AND reintentar<=CURRENT_TIMESTAMP
                    ORDER BY reintentar,clave LIMIT 1 FOR UPDATE SKIP LOCKED
                    """).query((r,f)->new Aviso(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getInt(5)))
                    .optional().map(this::enviar).orElse(false));
            if (!Boolean.TRUE.equals(enviado)) return;
        }
    }

    private boolean enviar(Aviso a) {
        try {
            var correo=Map.of("destinatario",a.destino(),"asunto",a.asunto(),"cuerpo",a.mensaje(),"html",false);
            var carga=json.writeValueAsBytes(Map.of("tipo","EMAIL","email",correo));
            var mensaje=MessageBuilder.withBody(carga).setContentType("application/json")
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT).setMessageId(a.clave()).build();
            var confirmacion=new CorrelationData(a.clave());
            rabbit.send(exchange,"notificacion.email",mensaje,confirmacion);
            boolean ack=confirmacion.getFuture().get(3,TimeUnit.SECONDS).isAck();
            if (!ack || confirmacion.getReturned()!=null) throw new IllegalStateException("RabbitMQ no confirmó la ruta");
            jdbc.sql("UPDATE subasta_avisos SET enviado=CURRENT_TIMESTAMP WHERE clave=:id").param("id",a.clave()).update();
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            LOG.warn("Aviso pendiente {}, intento {}",a.clave(),a.intentos()+1);
            long espera=Math.min(300,5L << Math.min(a.intentos(),6));
            jdbc.sql("UPDATE subasta_avisos SET intentos=intentos+1,reintentar=:fecha WHERE clave=:id")
                    .param("fecha",RepositorioJdbc.fecha(Instant.now().plusSeconds(espera))).param("id",a.clave()).update();
        }
        return true;
    }

    private record Aviso(String clave,String destino,String asunto,String mensaje,int intentos) {}
}
