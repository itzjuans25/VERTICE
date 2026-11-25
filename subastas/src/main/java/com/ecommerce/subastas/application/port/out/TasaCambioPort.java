package com.ecommerce.subastas.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface TasaCambioPort {
    record Tasa(BigDecimal valor, LocalDate fecha) {}
    Optional<Tasa> tasa();
}