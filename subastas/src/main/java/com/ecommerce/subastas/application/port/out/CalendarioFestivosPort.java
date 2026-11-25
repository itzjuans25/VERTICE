package com.ecommerce.subastas.application.port.out;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

public interface CalendarioFestivosPort {
    Optional<Set<LocalDate>> festivos(int anno);
}