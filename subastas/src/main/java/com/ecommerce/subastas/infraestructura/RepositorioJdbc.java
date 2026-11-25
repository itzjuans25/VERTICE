package com.ecommerce.subastas.infraestructura;

import com.ecommerce.subastas.application.port.in.Subastas.*;
import com.ecommerce.subastas.application.port.out.RepositorioSubastas;
import com.ecommerce.subastas.domain.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Repository
public class RepositorioJdbc implements RepositorioSubastas {
    private final JdbcClient jdbc;
    private final TransactionTemplate tx;
    private final ObjectMapper json;

    public RepositorioJdbc(JdbcClient jdbc, PlatformTransactionManager gestor, ObjectMapper json) {
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(gestor);
        this.json = json;
    }

    public <T> T transaccion(Supplier<T> accion) { return tx.execute(estado -> accion.get()); }

    public Lote insertar(Lote l) {
        return jdbc.sql("""
                INSERT INTO subasta_lotes(titulo,descripcion,categoria,condicion,atributos,imagenes,
                    precio_inicial,incremento,precio_actual,tienda_email,estado,inicio,fin)
                VALUES (:titulo,:descripcion,:categoria,:condicion,:atributos,:imagenes,
                    :inicial,:incremento,:actual,:tienda,:estado,:inicio,:fin) RETURNING *
                """).param("titulo",l.titulo()).param("descripcion",l.descripcion()).param("categoria",l.categoria().name())
                .param("condicion",l.condicion().name()).param("atributos",serializar(l.atributos()))
                .param("imagenes",serializar(l.imagenes())).param("inicial",l.precioInicial())
                .param("incremento",l.incremento()).param("actual",l.precioActual()).param("tienda",l.tiendaEmail())
                .param("estado",l.estado().name()).param("inicio",fecha(l.inicio())).param("fin",fecha(l.fin()))
                .query(this::mapear).single();
    }

    public Optional<Lote> lote(long id, boolean bloquear) {
        return jdbc.sql("SELECT * FROM subasta_lotes WHERE id=:id" + (bloquear ? " FOR UPDATE" : ""))
                .param("id",id).query(this::mapear).optional();
    }

    public Lote actualizar(Lote l) {
        return jdbc.sql("""
                UPDATE subasta_lotes SET precio_actual=:precio, ganador_id=:ganador, ganador_email=:correo,
                    estado=:estado, fin=:fin, pujas=:pujas, version=version+1
                WHERE id=:id AND version=:version RETURNING *
                """).param("precio",l.precioActual()).param("ganador",l.ganadorId()).param("correo",l.ganadorEmail())
                .param("estado",l.estado().name()).param("fin",fecha(l.fin())).param("pujas",l.pujas())
                .param("id",l.id()).param("version",l.version()).query(this::mapear).optional()
                .orElseThrow(() -> new ReglaSubasta("El lote cambió. Actualiza e intenta otra vez."));
    }

    public Pagina listar(Filtro f) {
        var parametros = new LinkedHashMap<String,Object>();
        var filtro = new StringBuilder(" WHERE estado <> 'CANCELADA'");
        if (f.curados()) filtro.append(" AND jsonb_extract_path_text(CAST(atributos AS jsonb),'Por qué se subasta') IS NOT NULL");
        if (f.texto() != null && !f.texto().isBlank()) {
            filtro.append(" AND (titulo ILIKE :texto ESCAPE '!' OR descripcion ILIKE :texto ESCAPE '!')");
            parametros.put("texto", "%" + f.texto().strip().replace("!","!!").replace("%","!%").replace("_","!_") + "%");
        }
        if (f.categoria() != null) {
            filtro.append(" AND categoria=:categoria");
            parametros.put("categoria",f.categoria().name());
        }
        if (!f.grupo().isEmpty()) {
            filtro.append(" AND categoria IN (:grupo)");
            parametros.put("grupo",f.grupo().stream().map(Enum::name).toList());
        }
        if (f.condicion() != null) {
            filtro.append(" AND condicion=:condicion");
            parametros.put("condicion",f.condicion().name());
        }
        if (f.minimo() != null) {
            filtro.append(" AND precio_actual>=:minimo");
            parametros.put("minimo",f.minimo());
        }
        if (f.maximo() != null) {
            filtro.append(" AND precio_actual<=:maximo");
            parametros.put("maximo",f.maximo());
        }
        long total=jdbc.sql("SELECT count(*) FROM subasta_lotes"+filtro).params(parametros).query(Long.class).single();
        parametros.put("limite",f.tamano());
        parametros.put("desplazamiento",(long) f.pagina()*f.tamano());
        var items=jdbc.sql("SELECT * FROM subasta_lotes"+filtro+
                        " ORDER BY CASE WHEN estado IN ('ACTIVA','PROGRAMADA') THEN 0 ELSE 1 END, fin, id LIMIT :limite OFFSET :desplazamiento")
                .params(parametros).query(this::mapear).list();
        return new Pagina(items,f.pagina(),f.tamano(),total);
    }

    public void registrar(Puja p) {
        jdbc.sql("INSERT INTO subasta_pujas(lote_id,usuario_id,monto,fecha) VALUES (:lote,:usuario,:monto,:fecha)")
                .param("lote",p.loteId()).param("usuario",p.usuarioId()).param("monto",p.monto())
                .param("fecha",fecha(p.fecha())).update();
    }

    @Override
    public Inicio inicio(Instant ahora) {
        var vigentes = " WHERE estado='ACTIVA' AND inicio<=:ahora AND fin>:ahora"
                + " AND jsonb_extract_path_text(CAST(atributos AS jsonb),'Por qué se subasta') IS NOT NULL";
        var fecha = fecha(ahora);
        var lotes = jdbc.sql("SELECT * FROM subasta_lotes" + vigentes + " ORDER BY fin,id LIMIT 8")
                .param("ahora", fecha).query(this::mapear).list();
        var categorias = jdbc.sql("SELECT categoria,count(*) FROM subasta_lotes" + vigentes + " GROUP BY categoria ORDER BY categoria")
                .param("ahora", fecha).query((r,n) -> new CategoriaActiva(Lote.Categoria.valueOf(r.getString(1)), r.getLong(2))).list();
        return new Inicio(lotes,categorias,categorias.stream().mapToLong(CategoriaActiva::total).sum());
    }

    public List<Puja> historial(long id) {
        return jdbc.sql("SELECT * FROM subasta_pujas WHERE lote_id=:id ORDER BY id DESC LIMIT 100")
                .param("id",id).query(puja()).list();
    }

    public MisPujas mias(long usuario) {
        var pujas=jdbc.sql("SELECT * FROM subasta_pujas WHERE usuario_id=:usuario ORDER BY id DESC LIMIT 100")
                .param("usuario",usuario).query(puja()).list();
        var ordenes=jdbc.sql("SELECT * FROM subasta_ordenes WHERE usuario_id=:usuario ORDER BY id DESC LIMIT 100")
                .param("usuario",usuario).query(orden()).list();
        return new MisPujas(pujas,ordenes);
    }

    public Optional<Orden> orden(long lote) {
        return jdbc.sql("SELECT * FROM subasta_ordenes WHERE lote_id=:lote").param("lote",lote).query(orden()).optional();
    }

    public void adjudicar(Lote lote) {
        jdbc.sql("INSERT INTO subasta_ordenes(lote_id,usuario_id,monto) VALUES (:lote,:usuario,:monto)")
                .param("lote",lote.id()).param("usuario",lote.ganadorId()).param("monto",lote.precioActual()).update();
    }

    public Orden pagar(long id, Instant fecha) {
        return jdbc.sql("UPDATE subasta_ordenes SET pagada=:fecha WHERE id=:id RETURNING *")
                .param("fecha",fecha(fecha)).param("id",id).query(orden()).single();
    }

    public List<Long> pendientes(Instant ahora) {
        return jdbc.sql("""
                SELECT id FROM subasta_lotes l
                WHERE (estado='PROGRAMADA' AND inicio<=:ahora) OR (estado='ACTIVA' AND
                    (fin<=:ahora OR (fin<=:pronto AND NOT EXISTS
                        (SELECT 1 FROM subasta_avisos a WHERE a.clave=concat('vence:',l.id)))))
                ORDER BY fin LIMIT 200
                """).param("ahora",fecha(ahora)).param("pronto",fecha(ahora.plusSeconds(600))).query(Long.class).list();
    }

    public void avisar(String clave, Long usuarioId, String destino, String asunto, String mensaje) {
        jdbc.sql("""
                INSERT INTO subasta_avisos(clave,usuario_id,destino,asunto,mensaje) VALUES(:clave,:usuario,:destino,:asunto,:mensaje)
                ON CONFLICT (clave) DO NOTHING
                """).param("clave",clave).param("usuario",usuarioId).param("destino",destino).param("asunto",asunto).param("mensaje",mensaje).update();
    }

    private Lote mapear(ResultSet r,int fila) throws SQLException {
        return new Lote(r.getLong("id"),r.getString("titulo"),r.getString("descripcion"),
                Lote.Categoria.valueOf(r.getString("categoria")),Lote.Condicion.valueOf(r.getString("condicion")),
                deserializar(r.getString("atributos"),new TypeReference<Map<String,String>>() {}),
                deserializar(r.getString("imagenes"),new TypeReference<List<String>>() {}),
                r.getBigDecimal("precio_inicial"),r.getBigDecimal("incremento"),r.getBigDecimal("precio_actual"),
                r.getObject("ganador_id",Long.class),r.getString("ganador_email"),r.getString("tienda_email"),
                Estado.valueOf(r.getString("estado")),instante(r,"inicio"),instante(r,"fin"),
                r.getInt("pujas"),r.getLong("version"));
    }

    private static RowMapper<Puja> puja() {
        return (r,n)-> new Puja(r.getLong("lote_id"),r.getLong("usuario_id"),r.getBigDecimal("monto"),instante(r,"fecha"));
    }

    private static RowMapper<Orden> orden() {
        return (r,n)-> new Orden(r.getLong("id"),r.getLong("lote_id"),r.getLong("usuario_id"),
                r.getBigDecimal("monto"),instante(r,"pagada"));
    }

    static OffsetDateTime fecha(Instant i) { return i.atOffset(ZoneOffset.UTC); }

    static Instant instante(ResultSet r,String columna) throws SQLException {
        var valor=r.getObject(columna,OffsetDateTime.class);
        return valor == null ? null : valor.toInstant();
    }

    private String serializar(Object valor) {
        try { return json.writeValueAsString(valor); } catch (Exception ex) { throw new IllegalStateException("JSON de lote inválido",ex); }
    }

    private <T> T deserializar(String valor,TypeReference<T> tipo) {
        try { return json.readValue(valor,tipo); } catch (Exception ex) { throw new IllegalStateException("JSON de lote inválido",ex); }
    }
}
