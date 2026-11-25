package com.ecommerce.subastas.infraestructura;

import com.ecommerce.subastas.application.port.in.Subastas;
import com.ecommerce.subastas.application.port.in.Subastas.*;
import com.ecommerce.subastas.domain.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/ecommerce/subastas")
class SubastasController {
    private final Subastas subastas;

    SubastasController(Subastas subastas) { this.subastas=subastas; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED) @SecurityRequirement(name="jwt")
    Publico crear(@AuthenticationPrincipal Actor actor,@Valid @RequestBody Formulario form) {
        return Publico.de(subastas.crear(actor,new Alta(form.titulo().strip(),form.descripcion().strip(),
                form.categoria(),form.condicion(),form.atributos(),form.imagenes(),form.precioInicial(),
                form.incremento(),form.inicio(),form.fin())),actor);
    }

    @GetMapping
    PaginaPublica listar(@AuthenticationPrincipal Actor actor,
                         @RequestParam(required=false) @Size(max=100) String q,
                         @RequestParam(required=false) Lote.Categoria categoria,
                         @RequestParam(required=false) @Size(max=12) List<Lote.Categoria> grupo,
                         @RequestParam(defaultValue="false") boolean curados,
                         @RequestParam(required=false) Lote.Condicion condicion,
                         @RequestParam(required=false) @DecimalMin("0") BigDecimal minimo,
                         @RequestParam(required=false) @DecimalMin("0") BigDecimal maximo,
                         @RequestParam(defaultValue="0") @Min(0) int pagina,
                         @RequestParam(defaultValue="12") @Min(1) @Max(48) int tamano) {
        var resultado=subastas.listar(new Filtro(q,categoria,condicion,minimo,maximo,pagina,tamano,grupo == null ? List.of() : grupo,curados));
        return new PaginaPublica(resultado.items().stream().map(l->Publico.de(l,actor)).toList(),pagina,tamano,resultado.total());
    }

    @GetMapping("/{id}")
    DetallePublico detalle(@AuthenticationPrincipal Actor actor,@PathVariable long id) {
        var d=subastas.detalle(id);
        var alias=new LinkedHashMap<Long,String>();
        var pujas=d.historial().stream().map(p->new PujaPublica(alias.computeIfAbsent(p.usuarioId(),
                clave->"Postor "+(alias.size()+1)),p.monto(),p.fecha(),actor != null && p.usuarioId()==actor.id())).toList();
        var orden=d.orden()!=null && actor!=null && d.orden().usuarioId()==actor.id() ? d.orden() : null;
        return new DetallePublico(Publico.de(d.lote(),actor),pujas,orden);
    }

    @PostMapping("/{id}/pujas") @ResponseStatus(HttpStatus.CREATED) @SecurityRequirement(name="jwt")
    Publico pujar(@AuthenticationPrincipal Actor actor,@PathVariable long id,@Valid @RequestBody Oferta oferta) {
        return Publico.de(subastas.pujar(actor,id,oferta.monto()),actor);
    }

    @GetMapping("/mias") @SecurityRequirement(name="jwt")
    MisPujas mias(@AuthenticationPrincipal Actor actor) { return subastas.mias(actor); }

    @PostMapping("/{id}/cancelar") @SecurityRequirement(name="jwt")
    Publico cancelar(@AuthenticationPrincipal Actor actor,@PathVariable long id) {
        return Publico.de(subastas.cancelar(actor,id),actor);
    }

    @PostMapping("/{id}/pago") @SecurityRequirement(name="jwt")
    Orden pagar(@AuthenticationPrincipal Actor actor,@PathVariable long id) { return subastas.pagar(actor,id); }

    @ExceptionHandler(ReglaSubasta.class)
    ResponseEntity<Map<String,String>> regla(ReglaSubasta ex) {
        var estado=switch(ex.codigo()) {
            case "NO_ENCONTRADO" -> HttpStatus.NOT_FOUND;
            case "PROHIBIDO" -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.CONFLICT;
        };
        return ResponseEntity.status(estado).body(Map.of("message",ex.getMessage()));
    }

    record Formulario(@NotBlank @Size(max=150) String titulo,@NotBlank @Size(max=4000) String descripcion,
                      @NotNull Lote.Categoria categoria,@NotNull Lote.Condicion condicion,
                      @NotNull @Size(max=12) Map<@NotBlank @Size(max=40) String,@NotBlank @Size(max=100) String> atributos,
                      @NotNull @Size(min=1,max=5) List<@NotBlank @Pattern(regexp="/(?:subastas|mercado)/[a-zA-Z0-9_-]+\\.(webp|png|jpg|svg)") String> imagenes,
                      @NotNull @Digits(integer=13,fraction=2) @DecimalMin("1") BigDecimal precioInicial,
                      @NotNull @Digits(integer=13,fraction=2) @DecimalMin("1") BigDecimal incremento,
                      @NotNull Instant inicio,@NotNull Instant fin) {}
    record Oferta(@NotNull @Digits(integer=13,fraction=2) @DecimalMin("1") BigDecimal monto) {}
    record PujaPublica(String postor,BigDecimal monto,Instant fecha,boolean mia) {}
    record DetallePublico(Publico lote,List<PujaPublica> historial,Orden adjudicacion) {}
    record PaginaPublica(List<Publico> items,int pagina,int tamano,long total) {}

    record Publico(long id,String titulo,String descripcion,Lote.Categoria categoria,Lote.Condicion condicion,
                   Map<String,String> atributos,List<String> imagenes,BigDecimal precioInicial,BigDecimal incremento,
                   BigDecimal precioActual,BigDecimal pujaMinima,Estado estado,Instant inicio,Instant fin,
                   int pujas,long version,boolean voyGanando) {
        static Publico de(Lote l,Actor actor) {
            return new Publico(l.id(),l.titulo(),l.descripcion(),l.categoria(),l.condicion(),l.atributos(),l.imagenes(),
                    l.precioInicial(),l.incremento(),l.precioActual(),l.pujaMinima(),l.estado(),l.inicio(),l.fin(),
                    l.pujas(),l.version(),actor!=null && Objects.equals(actor.id(),l.ganadorId()));
        }
    }
}
