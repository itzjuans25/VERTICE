package com.ecommerce.subastas.application.port.in;

import com.ecommerce.subastas.domain.Lote;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface Subastas {
    record Actor(long id, String email, boolean admin) {}
    record Alta(String titulo, String descripcion, Lote.Categoria categoria, Lote.Condicion condicion,
                Map<String,String> atributos, List<String> imagenes, BigDecimal precioInicial,
                BigDecimal incremento, Instant inicio, Instant fin) {}
    record Filtro(String texto, Lote.Categoria categoria, Lote.Condicion condicion,
                  BigDecimal minimo, BigDecimal maximo, int pagina, int tamano, List<Lote.Categoria> grupo, boolean curados,
                  Instant activasAl) {
        public Filtro(String texto, Lote.Categoria categoria, Lote.Condicion condicion,
                      BigDecimal minimo, BigDecimal maximo, int pagina, int tamano) {
            this(texto,categoria,condicion,minimo,maximo,pagina,tamano,List.of(),false,null);
        }
    }
    record Pagina(List<Lote> items, int pagina, int tamano, long total) {}
    record Puja(long loteId, long usuarioId, BigDecimal monto, Instant fecha) {}
    record Orden(long id, long loteId, long usuarioId, BigDecimal monto, Instant pagada) {}
    record Detalle(Lote lote, List<Puja> historial, Orden orden) {}
    record MisPujas(List<Puja> pujas, List<Orden> adjudicaciones) {}
    record CategoriaActiva(Lote.Categoria categoria, long total) {}
    record Inicio(List<Lote> lotes, List<CategoriaActiva> categorias, long total) {}
    record Certificado(String codigo, long loteId, String titulo, String edicion, String estado, Instant fechaPagoSimulado) {}

    Lote crear(Actor actor, Alta alta);
    Pagina listar(Filtro filtro);
    Detalle detalle(long id);
    BigDecimal precioActual(long id);
    Optional<Certificado> verificar(String codigo);
    Lote pujar(Actor actor, long id, BigDecimal monto);
    MisPujas mias(Actor actor);
    Inicio inicio();
    Lote cancelar(Actor actor, long id);
    Orden pagar(Actor actor, long id);
    void procesarReloj();
}
