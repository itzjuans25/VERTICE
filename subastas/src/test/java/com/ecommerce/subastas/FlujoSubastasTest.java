package com.ecommerce.subastas;

import com.ecommerce.subastas.application.port.in.Subastas;
import com.ecommerce.subastas.application.port.in.Subastas.Actor;
import com.ecommerce.subastas.application.port.in.Subastas.Alta;
import com.ecommerce.subastas.domain.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named="SUBASTAS_IT_JDBC",matches=".+")
@SpringBootTest(properties={"spring.sql.init.data-locations=optional:classpath:sin-semilla.sql",
        "JWT_SECRET=una-frase-determinista-exclusiva-de-las-pruebas", "SUBASTAS_CIERRE_MS=3600000",
        "SUBASTAS_AVISOS_MS=3600000", "spring.rabbitmq.dynamic=false"})
@AutoConfigureMockMvc
class FlujoSubastasTest {
    private static final String JWT = "una-frase-determinista-exclusiva-de-las-pruebas";
    private static final String API = "/api/ecommerce/subastas";
    private static final Actor ADMIN = new Actor(401, "admin@example.test", true);
    private static final Actor ANA = new Actor(402, "ana@example.test", false);
    private static final Actor LUIS = new Actor(403, "luis@example.test", false);

    @Autowired Subastas app;
    @Autowired MockMvc http;
    @Autowired JdbcClient jdbc;
    @Autowired org.springframework.context.ApplicationContext contexto;
    @MockitoBean RabbitTemplate rabbit;

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> System.getenv("SUBASTAS_IT_JDBC"));
        r.add("spring.datasource.username", () -> System.getenv("SUBASTAS_IT_USUARIO"));
        r.add("spring.datasource.password", () -> System.getenv("SUBASTAS_IT_CLAVE"));
    }

    @Test void anonimatoPermisosFiltrosYPago() throws Exception {
        String marca = UUID.randomUUID().toString();
        var ahora = Instant.now();
        var cuerpo = """
                {"titulo":"%s","descripcion":"Cámara de prueba","categoria":"ELECTRONICA","condicion":"USADO",
                "atributos":{"Color":"Negro"},"imagenes":["/subastas/electronica.webp"],"precioInicial":100,"incremento":10,
                "inicio":"%s","fin":"%s"}
                """.formatted(marca, ahora.minusSeconds(2), ahora.plusSeconds(3600));
        http.perform(post(API).contentType("application/json").content(cuerpo)).andExpect(status().isUnauthorized());
        http.perform(post(API).header("Authorization", token(ANA)).contentType("application/json").content(cuerpo)).andExpect(status().isForbidden());
        http.perform(post(API).header("Authorization", token(ADMIN)).contentType("application/json").content(cuerpo))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.titulo").value(marca));
        long id = jdbc.sql("SELECT id FROM subasta_lotes WHERE titulo=:t").param("t",marca).query(Long.class).single();
        http.perform(get(API).param("q",marca).param("categoria","ELECTRONICA").param("condicion","USADO")
                .param("minimo","99").param("maximo","101").param("pagina","0").param("tamano","1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        http.perform(get(API).param("minimo","200").param("maximo","100")).andExpect(status().isConflict());
        http.perform(get(API+"/999999999")).andExpect(status().isNotFound());
        http.perform(get(API+"/mias").header("Authorization","Bearer invalido")).andExpect(status().isUnauthorized());
        http.perform(post(API+"/"+id+"/pujas").header("Authorization",token(ANA)).contentType("application/json").content("{\"monto\":109}"))
                .andExpect(status().isConflict());
        http.perform(post(API+"/"+id+"/pujas").header("Authorization",token(ANA)).contentType("application/json").content("{\"monto\":110}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.voyGanando").value(true));
        assertThatThrownBy(() -> app.pujar(ANA,id,monto(120))).isInstanceOf(ReglaSubasta.class).hasMessageContaining("más alta");
        app.pujar(LUIS,id,monto(120));
        http.perform(get(API+"/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.historial[0].postor").value("Postor 1"))
                .andExpect(jsonPath("$.historial[0].usuarioId").doesNotExist()).andExpect(jsonPath("$.lote.ganadorEmail").doesNotExist());
        app.procesarReloj();
        vencer(id);
        app.procesarReloj();
        http.perform(get(API+"/mias").header("Authorization",token(LUIS))).andExpect(status().isOk())
                .andExpect(jsonPath("$.adjudicaciones[0].loteId").value(id));
        http.perform(post(API+"/"+id+"/pago").header("Authorization",token(ANA))).andExpect(status().isForbidden());
        http.perform(post(API+"/"+id+"/pago").header("Authorization",token(LUIS))).andExpect(status().isOk())
                .andExpect(jsonPath("$.pagada").isNotEmpty());
        assertThat(app.pagar(LUIS,id)).isEqualTo(app.pagar(LUIS,id));
        assertThat(jdbc.sql("SELECT COUNT(*) FROM subasta_avisos WHERE clave LIKE :k OR clave=:g")
                .param("k","superada:"+id+":%").param("g","ganada:"+id).query(Long.class).single()).isEqualTo(2);
    }

    @Test void vitrinasOmitenLotesInternosSinModificarSusFichas() throws Exception {
        var ahora = Instant.now();
        var coleccion = UUID.randomUUID().toString();
        var publico = app.crear(ADMIN, altaCurada("Pieza " + coleccion, Map.of("Por qué se subasta", "Tirada corta"), ahora));
        var interno = app.crear(ADMIN, altaCurada("Registro " + coleccion,
                Map.of("Por qué se subasta", "Recorrido de demostración", "Visibilidad", "Interna"), ahora));
        var historico = app.crear(ADMIN, altaCurada("Ensayo curado · " + coleccion,
                Map.of("Por qué se subasta", "Registro previo"), ahora));
        http.perform(get(API).param("curados", "true").param("q", coleccion))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(publico.id()));
        http.perform(get(API).param("q", coleccion))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(3));
        assertThat(app.detalle(interno.id()).lote().atributos()).containsEntry("Visibilidad", "Interna");
        assertThat(app.detalle(historico.id()).lote().titulo()).startsWith("Ensayo curado");
    }

    private Alta altaCurada(String titulo, Map<String, String> atributos, Instant ahora) {
        return new Alta(titulo, "Objeto de archivo", Lote.Categoria.RELOJES, Lote.Condicion.USADO,
                atributos, List.of("/mercado/vertice-06.webp"), monto(100), monto(10),
                ahora.minusSeconds(2), ahora.plusSeconds(600));
    }

    @Test void accionesCronometroYReintentoDePublicacion() throws Exception {
        var ahora = Instant.now();
        assertThatThrownBy(() -> crear(ahora,ahora)).isInstanceOf(ReglaSubasta.class);
        var programada = crear(ahora.plusSeconds(120),ahora.plusSeconds(800));
        assertThat(programada.estado()).isEqualTo(Estado.PROGRAMADA);
        assertThatThrownBy(() -> app.pujar(ANA,programada.id(),monto(110))).isInstanceOf(ReglaSubasta.class);
        jdbc.sql("UPDATE subasta_lotes SET inicio=CURRENT_TIMESTAMP - interval '1 second' WHERE id=:id").param("id",programada.id()).update();
        app.procesarReloj();
        assertThat(app.detalle(programada.id()).lote().estado()).isEqualTo(Estado.ACTIVA);
        http.perform(post(API+"/"+programada.id()+"/cancelar").header("Authorization",token(ANA))).andExpect(status().isForbidden());
        http.perform(post(API+"/"+programada.id()+"/cancelar").header("Authorization",token(ADMIN))).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
        assertThatThrownBy(() -> app.cancelar(ADMIN,programada.id())).isInstanceOf(ReglaSubasta.class);
        var cerca = crear(ahora.minusSeconds(10),ahora.plusSeconds(80));
        app.procesarReloj();
        var pujada = app.pujar(ANA,cerca.id(),monto(110));
        assertThat(pujada.fin()).isEqualTo(cerca.fin().plusSeconds(120));
        var vacia = crear(ahora.minusSeconds(10),ahora.plusSeconds(1800));
        vencer(vacia.id());
        app.procesarReloj();
        assertThat(app.detalle(vacia.id()).lote().estado()).isEqualTo(Estado.CERRADA);
        assertThatThrownBy(() -> app.pagar(ANA,vacia.id())).isInstanceOf(ReglaSubasta.class);
        assertThatThrownBy(() -> app.cancelar(ADMIN,vacia.id())).isInstanceOf(ReglaSubasta.class);
        assertThatThrownBy(() -> app.detalle(vacia.id()).lote().cambiar(Estado.ADJUDICADA)).isInstanceOf(ReglaSubasta.class);

        var metodo = contexto.getBean("procesosSubastas").getClass().getDeclaredMethod("publicar");
        metodo.setAccessible(true);
        doThrow(new IllegalStateException("fallo de prueba")).when(rabbit).send(anyString(),anyString(),any(),any(CorrelationData.class));
        metodo.invoke(contexto.getBean("procesosSubastas"));
        assertThat(jdbc.sql("SELECT intentos FROM subasta_avisos WHERE clave=:k").param("k","sin-ganador:"+vacia.id())
                .query(Integer.class).single()).isPositive();
        doAnswer(inv -> {
            CorrelationData confirmacion = inv.getArgument(3);
            confirmacion.getFuture().complete(new CorrelationData.Confirm(true,null));
            return null;
        }).when(rabbit).send(anyString(),anyString(),any(),any(CorrelationData.class));
        jdbc.sql("UPDATE subasta_avisos SET reintentar=CURRENT_TIMESTAMP WHERE enviado IS NULL").update();
        metodo.invoke(contexto.getBean("procesosSubastas"));
        assertThat(jdbc.sql("SELECT enviado IS NOT NULL FROM subasta_avisos WHERE clave=:k").param("k","vence:"+cerca.id())
                .query(Boolean.class).single()).isTrue();
    }

    @Test void dosPujasIgualesCompitenPorLaMismaFila() throws Exception {
        var ahora = Instant.now();
        var lote = crear(ahora.minusSeconds(2),ahora.plusSeconds(3600));
        var inicio = new CountDownLatch(1);
        var ejecutor = Executors.newFixedThreadPool(2);
        try {
            var a = ejecutor.submit(() -> competir(ANA,lote.id(),inicio));
            var b = ejecutor.submit(() -> competir(LUIS,lote.id(),inicio));
            inicio.countDown();
            assertThat(List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
            var detalle = app.detalle(lote.id());
            assertThat(detalle.historial()).hasSize(1);
            assertThat(detalle.lote().version()).isEqualTo(1);
            assertThat(detalle.lote().precioActual()).isEqualByComparingTo("110");
        } finally {
            ejecutor.shutdownNow();
        }
    }

    private boolean competir(Actor actor,long id,CountDownLatch inicio) throws InterruptedException {
        inicio.await();
        try {
            app.pujar(actor,id,monto(110));
            return true;
        } catch (ReglaSubasta seAdelantaron) {
            return false;
        }
    }
    private Lote crear(Instant inicio,Instant fin) {
        return app.crear(ADMIN,new Alta("Cámara "+UUID.randomUUID(),"Lote de prueba",Lote.Categoria.ELECTRONICA,
                Lote.Condicion.USADO,Map.of(),List.of("/subastas/electronica.webp"),monto(100),monto(10),inicio,fin));
    }

    private void vencer(long id) {
        jdbc.sql("UPDATE subasta_lotes SET inicio=CURRENT_TIMESTAMP - interval '1 minute', fin=CURRENT_TIMESTAMP - interval '1 second' WHERE id=:id")
                .param("id",id).update();
    }
    private static BigDecimal monto(int valor) { return BigDecimal.valueOf(valor); }
    private static String token(Actor actor) {
        return "Bearer "+Jwts.builder().subject(Long.toString(actor.id())).claim("email",actor.email())
                .claim("role",actor.admin()?"ADMIN":"USER").expiration(java.util.Date.from(Instant.now().plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(JWT.getBytes(StandardCharsets.UTF_8))).compact();
    }
}
