package com.ecommerce.subastas.application.port.in;

import com.ecommerce.subastas.domain.Lote;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface Subastas {
    record Actor(long id, String email, boolean admin) {}
    record Alta(String titulo, String descripcion, Lote.Categoria categoria, Lote.Condicion condicion,
                Map<String,String> atributos, List<String> imagenes, BigDecimal precioInicial,
                BigDecimal incremento, Instant inicio, Instant fin) {}
    record Filtro(String texto, Lote.Categoria categoria, Lote.Condicion condicion,
                  BigDecimal minimo, BigDecimal maximo, int pagina, int tamano, List<Lote.Categoria> grupo, boolean curados) {
        public Filtro(String texto, Lote.Categoria categoria, Lote.Condicion condicion,
                      BigDecimal minimo, BigDecimal maximo, int pagina, int tamano) {
            this(texto,categoria,condicion,minimo,maximo,pagina,tamano,List.of(),false);
        }
    }
    record Pagina(List<Lote> items, int pagina, int tamano, long total) {}
    record Puja(long loteId, long usuarioId, BigDecimal monto, Instant fecha) {}
    record Orden(long id, long loteId, long usuarioId, BigDecimal monto, Instant pagada) {}
    record Detalle(Lote lote, List<Puja> historial, Orden orden) {}
    record MisPujas(List<Puja> pujas, List<Orden> adjudicaciones) {}
    record CategoriaActiva(Lote.Categoria categoria, long total) {}
    record Inicio(List<Lote> lotes, List<CategoriaActiva> categorias, long total) {}

    Lote crear(Actor actor, Alta alta);
    Pagina listar(Filtro filtro);
    Detalle detalle(long id);
    Lote pujar(Actor actor, long id, BigDecimal monto);
    MisPujas mias(Actor actor);
    Inicio inicio();
    Lote cancelar(Actor actor, long id);
    Orden pagar(Actor actor, long id);
    void procesarReloj();
}
