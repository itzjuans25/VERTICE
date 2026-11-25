package com.ecommerce.notificaciones.infraestructura.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
class CorsAvisos implements WebMvcConfigurer {
    private final String[] origenes;

    CorsAvisos(@Value("${CORS_ALLOWED_ORIGINS:http://localhost:3000}") String origenes) {
        this.origenes = Arrays.stream(origenes.split(",")).map(String::strip)
                .filter(s -> !s.isEmpty() && !s.equals("*")).toArray(String[]::new);
    }

    @Override public void addCorsMappings(CorsRegistry registro) {
        for (String ruta : new String[]{"/api/notificaciones/mis-avisos","/health"}) {
            registro.addMapping(ruta).allowedOrigins(origenes).allowedMethods("GET").allowedHeaders("Authorization");
        }
    }
}