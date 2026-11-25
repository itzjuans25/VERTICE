package com.ecommerce.subastas.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface ClimaDespachoPort {
    record Observacion(BigDecimal temperatura, int humedad, BigDecimal precipitacion, LocalDateTime horaLocal) {}
    Optional<Observacion> bogota();
}