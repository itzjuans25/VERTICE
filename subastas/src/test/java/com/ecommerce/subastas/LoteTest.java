package com.ecommerce.subastas;

import com.ecommerce.subastas.domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class LoteTest {
    private static final Instant AHORA = Instant.parse("2026-11-24T13:00:00Z");

    @Test void aceptaMinimoExtiendeEnElLimiteYConservaElPlazoNormal() {
        var normal = activo(AHORA.plusSeconds(121));
        var corto = activo(AHORA.plusSeconds(120));
        assertThat(normal.ofertar(1,"ana@example.test",BigDecimal.valueOf(110),AHORA).fin()).isEqualTo(normal.fin());
        var extendido = corto.ofertar(1,"ana@example.test",BigDecimal.valueOf(110),AHORA);
        assertThat(extendido.fin()).isEqualTo(corto.fin().plusSeconds(120));
        assertThat(extendido.pujas()).isEqualTo(1);
        assertThat(extendido.ganadorId()).isEqualTo(1);
    }

    @Test void rechazaPujasBajasVencidasOContraUnoMismo() {
        var lote = activo(AHORA.plusSeconds(300));
        assertThatThrownBy(() -> lote.ofertar(1,"ana@example.test",BigDecimal.valueOf(109),AHORA))
                .isInstanceOf(ReglaSubasta.class).hasMessageContaining("mínima");
        assertThatThrownBy(() -> lote.ofertar(1,"ana@example.test",BigDecimal.valueOf(110),lote.fin()))
                .isInstanceOf(ReglaSubasta.class).hasMessageContaining("no está activa");
        var lider = lote.ofertar(1,"ana@example.test",BigDecimal.valueOf(110),AHORA);
        assertThatThrownBy(() -> lider.ofertar(1,"ana@example.test",BigDecimal.valueOf(120),AHORA))
                .isInstanceOf(ReglaSubasta.class).hasMessageContaining("más alta");
    }

    @Test void adjudicaSoloDesdeCerradaYConGanador() {
        var lote = activo(AHORA.plusSeconds(300));
        assertThatThrownBy(() -> lote.cambiar(Estado.ADJUDICADA)).isInstanceOf(ReglaSubasta.class);
        assertThatThrownBy(() -> lote.cambiar(Estado.CERRADA).cambiar(Estado.ADJUDICADA)).isInstanceOf(ReglaSubasta.class);
        assertThat(lote.ofertar(1,"ana@example.test",BigDecimal.valueOf(110),AHORA)
                .cambiar(Estado.CERRADA).cambiar(Estado.ADJUDICADA).estado()).isEqualTo(Estado.ADJUDICADA);
    }

    private static Lote activo(Instant fin) {
        return new Lote(1,"Reloj","Lote de prueba",Lote.Categoria.RELOJES,Lote.Condicion.NUEVO,
                Map.of(),List.of("/subastas/relojes.webp"),BigDecimal.valueOf(100),BigDecimal.TEN,
                BigDecimal.valueOf(100),null,null,"tienda@example.test",Estado.ACTIVA,AHORA.minusSeconds(1),fin,0,0);
    }
}
