package com.ecommerce.auth.infraestructura.security;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Set;
import java.util.function.Supplier;

@Component
public class PerfilAuth {
    private static final Set<String> RUTAS = Set.of("save", "login", "renovar", "cambiar-clave");
    private final MeterRegistry registro;
    private final boolean activo;

    public PerfilAuth(MeterRegistry registro, @Value("${AUTH_PROFILE_ENABLED:false}") boolean activo) {
        this.registro = registro;
        this.activo = activo;
    }

    public <T> T medir(String fase, Supplier<T> accion) {
        if (!activo || !(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes solicitud)) {
            return accion.get();
        }
        var ruta = solicitud.getRequest().getRequestURI();
        var operacion = ruta.substring(ruta.lastIndexOf('/') + 1);
        if (!RUTAS.contains(operacion)) {
            return accion.get();
        }
        return registro.timer("techstore.auth.fase", "ruta", operacion, "fase", fase).record(accion);
    }
}
