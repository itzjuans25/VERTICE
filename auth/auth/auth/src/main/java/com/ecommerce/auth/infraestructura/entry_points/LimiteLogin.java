package com.ecommerce.auth.infraestructura.entry_points;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
class LimiteLogin {
    static final int VENTANA_SEGUNDOS = 60;
    private static final int MAXIMO_CUENTA = 10;
    private static final int MAXIMO_ORIGEN = 60;
    private static final int MAXIMO_CLAVES = 10_000;

    private final Map<String, Integer> intentos = new HashMap<>();
    private long ventana;

    synchronized long admitir(String cuenta, String origen) {
        long actual = System.currentTimeMillis() / (VENTANA_SEGUNDOS * 1000L);
        if (actual != ventana) {
            ventana = actual;
            intentos.clear();
        }
        cuenta = "cuenta:" + cuenta;
        origen = "origen:" + origen;
        if (intentos.size() >= MAXIMO_CLAVES
                || intentos.getOrDefault(cuenta, 0) >= MAXIMO_CUENTA
                || intentos.getOrDefault(origen, 0) >= MAXIMO_ORIGEN) {
            return -1;
        }
        intentos.merge(cuenta, 1, Integer::sum);
        intentos.merge(origen, 1, Integer::sum);
        return actual;
    }

    synchronized void liberar(String cuenta, String origen, long admision) {
        if (admision == ventana) {
            intentos.computeIfPresent("cuenta:" + cuenta, (clave, total) -> total == 1 ? null : total - 1);
            intentos.computeIfPresent("origen:" + origen, (clave, total) -> total == 1 ? null : total - 1);
        }
    }
}
