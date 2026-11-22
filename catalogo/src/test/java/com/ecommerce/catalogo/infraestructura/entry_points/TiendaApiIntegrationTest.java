package com.ecommerce.catalogo.infraestructura.entry_points;

import com.ecommerce.catalogo.application.usecase.InformacionTienda;

import com.ecommerce.catalogo.domain.model.Usuario;
import com.ecommerce.catalogo.domain.port.out.NotificacionGateway;
import com.ecommerce.catalogo.domain.port.out.UsuarioGateway;
import com.ecommerce.catalogo.infraestructura.driver_adapters.jpa_repository.carrito.CarritoDataJpaRepository;
import com.ecommerce.catalogo.infraestructura.driver_adapters.jpa_repository.compra.CompraDataJpaRepository;
import com.ecommerce.catalogo.infraestructura.driver_adapters.jpa_repository.producto.ProductoData;
import com.ecommerce.catalogo.infraestructura.driver_adapters.jpa_repository.producto.ProductoDataJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TiendaApiIntegrationTest {

    private static final String SECRET = "test-secret-with-at-least-32-characters-0001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InformacionTienda informacion;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductoDataJpaRepository productoRepository;

    @Autowired
    private CarritoDataJpaRepository carritoRepository;

    @Autowired
    private CompraDataJpaRepository compraRepository;

    @MockitoBean
    private UsuarioGateway usuarioGateway;

    @MockitoBean
    private NotificacionGateway notificacionGateway;

    @BeforeEach
    void limpiarBaseDeDatos() {
        compraRepository.deleteAll();
        carritoRepository.deleteAll();
        productoRepository.deleteAll();
    }

    @Test
    void exploraProductosConFiltrosYValidaPaginacion() throws Exception {
        var monitor = crearProducto("Monitor 100% HD", "99000", 2);
        monitor.setCategoria("Periféricos");
        productoRepository.save(monitor);
        crearProducto("Monitor 1000 HD", "99000", 2);
        mockMvc.perform(get("/api/ecommerce/producto/categorias")).andExpect(status().isOk());
        mockMvc.perform(get("/api/ecommerce/producto/explorar")
                        .param("q", "100%").param("categoria", "Periféricos").param("marca", "Genérica")
                        .param("condicion", "NUEVO").param("minimo", "90000").param("maximo", "100000")
                        .param("orden", "precio-asc").param("tamano", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].nombre").value("Monitor 100% HD"));
        mockMvc.perform(get("/api/ecommerce/producto/explorar").param("tamano", "49"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/ecommerce/producto/explorar").param("orden", "incorrecto"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validaCondicionYGuardaEspecificacionesDeUnUsado() throws Exception {
        String ruta = "/api/ecommerce/producto";
        String admin = bearer(99L, "ADMIN");
        var campos = new java.util.LinkedHashMap<String, Object>(Map.of(
                "nombre", "Teclado", "precio", 59000, "stock", 2, "condicion", "USADO",
                "marca", "Genérica", "modelo", "TKL-87", "especificaciones", Map.of("Teclas", "87")));
        mockMvc.perform(post(ruta).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(campos))).andExpect(status().isBadRequest());
        campos.put("grado", "B");
        campos.put("notaEstado", "Marcas leves. Teclas y conexión USB verificadas.");
        mockMvc.perform(post(ruta).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(campos))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.especificaciones.Teclas").value("87")).andExpect(jsonPath("$.grado").value("B"));
        mockMvc.perform(get(ruta + "/marcas")).andExpect(status().isOk()).andExpect(jsonPath("$[0]").value("Genérica"));
        campos.put("condicion", "NUEVO");
        mockMvc.perform(post(ruta).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(campos))).andExpect(status().isBadRequest());
    }

    @Test
    void carritoResumeCombinaActualizaEliminaYVacia() throws Exception {
        var primero = crearProducto("Teclado", "10.00", 5);
        var segundo = crearProducto("Monitor", "25.00", 1);
        String token = bearer(1L, "USER");
        simularUsuario(1L, "USER");
        for (Long id : new Long[]{primero.getId(), primero.getId(), segundo.getId()}) {
            mockMvc.perform(post("/api/ecommerce/carrito/agregar").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content(agregarItemJson(id, 1)))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/ecommerce/carrito/resumen").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unidades").value(3))
                .andExpect(jsonPath("$.productosDistintos").value(2)).andExpect(jsonPath("$.totalCop").value(45));
        mockMvc.perform(put("/api/ecommerce/carrito/actualizar/{id}", primero.getId())
                        .header("Authorization", token).param("cantidad", "3"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.precioTotal").value(55));
        mockMvc.perform(put("/api/ecommerce/carrito/actualizar/{id}", segundo.getId())
                        .header("Authorization", token).param("cantidad", "2"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/ecommerce/carrito/eliminar/{id}", segundo.getId()).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1));
        mockMvc.perform(delete("/api/ecommerce/carrito/eliminar/{id}", segundo.getId()).header("Authorization", token))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/ecommerce/carrito/vaciar").header("Authorization", token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/ecommerce/carrito/resumen").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.habilitaCompra").value(false));
    }

    @Test
    void cotizacionYEntregaAplicanDatosDeLosProveedores() throws Exception {
        var producto = crearProducto("Cargador", "10000.00", 5);
        informacion.actualizarTasa(new BigDecimal("0.00025"), LocalDate.of(2026, 11, 20));
        mockMvc.perform(get("/api/ecommerce/producto/{id}/precio-usd", producto.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.precioUsd").value(2.50))
                .andExpect(jsonPath("$.fuente").value("Frankfurter"));
        mockMvc.perform(get("/api/ecommerce/producto/entrega").param("diasHabiles", "3"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.diasHabiles").value(3));
        mockMvc.perform(get("/api/ecommerce/producto/entrega").param("diasHabiles", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/ecommerce/producto/entrega").param("diasHabiles", "mañana"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void administraProductosYConsultaHistorial() throws Exception {
        var producto = crearProducto("Nombre inicial", "10.00", 3);
        var admin = bearer(99L, "ADMIN");
        var cliente = bearer(1L, "USER");
        simularUsuario(1L, "USER");
        mockMvc.perform(put("/api/ecommerce/producto/{id}", producto.getId())
                        .header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson("Nombre nuevo", "20.00", 5)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Nombre nuevo"));
        mockMvc.perform(post("/api/ecommerce/carrito/agregar").header("Authorization", cliente)
                        .contentType(MediaType.APPLICATION_JSON).content(agregarItemJson(producto.getId(), 1)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/ecommerce/compras/realizar").header("Authorization", cliente))
                .andExpect(status().isCreated());
        Long id = compraRepository.findAll().get(0).getId();
        mockMvc.perform(get("/api/ecommerce/compras/{id}", id).header("Authorization", cliente))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(20));
        mockMvc.perform(get("/api/ecommerce/compras/{id}", id).header("Authorization", bearer(2L, "USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/ecommerce/compras/usuario/1").header("Authorization", cliente))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        mockMvc.perform(delete("/api/ecommerce/producto/{id}", producto.getId()).header("Authorization", admin))
                .andExpect(status().isNoContent());
    }

    @Test
    void losProductosSonPublicos() throws Exception {
        ProductoData producto = crearProducto("Teclado", "10.00", 5);

        mockMvc.perform(get("/api/ecommerce/producto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(producto.getId()))
                .andExpect(jsonPath("$[0].nombre").value("Teclado"));

        mockMvc.perform(get("/api/ecommerce/producto/{id}", producto.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(5));
    }

    @Test
    void crearProductoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/ecommerce/producto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson("Monitor", "150.00", 5)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearProductoConRolUserDevuelve403() throws Exception {
        mockMvc.perform(post("/api/ecommerce/producto")
                        .header("Authorization", bearer(1L, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson("Monitor", "150.00", 5)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPuedeCrearProducto() throws Exception {
        mockMvc.perform(post("/api/ecommerce/producto")
                        .header("Authorization", bearer(99L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson("Monitor", "150.00", 5)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Monitor"));
    }

    @Test
    void elCarritoRequiereAutenticacion() throws Exception {
        mockMvc.perform(get("/api/ecommerce/carrito"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioAgregaProductoYRealizaCompraDescontandoStock() throws Exception {
        ProductoData producto = crearProducto("Teclado", "10.00", 5);
        simularUsuario(1L, "USER");

        mockMvc.perform(post("/api/ecommerce/carrito/agregar")
                        .header("Authorization", bearer(1L, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(agregarItemJson(producto.getId(), 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].cantidad").value(2))
                .andExpect(jsonPath("$.precioTotal").value(20.00));

        // El carrito no reserva stock.
        assertEquals(5, productoRepository.findById(producto.getId()).orElseThrow().getStock());

        mockMvc.perform(post("/api/ecommerce/compras/realizar")
                        .header("Authorization", bearer(1L, "USER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(20.00))
                .andExpect(jsonPath("$.estado").value("COMPLETADA"));

        assertEquals(3, productoRepository.findById(producto.getId()).orElseThrow().getStock());
        assertEquals(1, compraRepository.count());
        assertEquals(0, carritoRepository.count());
        verify(notificacionGateway).enviarNotificacion(any());

        mockMvc.perform(get("/api/ecommerce/carrito")
                        .header("Authorization", bearer(1L, "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void dosConfirmacionesDelMismoCarritoCreanSoloUnaCompra() throws Exception {
        ProductoData producto = crearProducto("Teclado", "10.00", 3);
        simularUsuario(1L, "USER");
        String token = bearer(1L, "USER");
        mockMvc.perform(post("/api/ecommerce/carrito/agregar")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(agregarItemJson(producto.getId(), 1)))
                .andExpect(status().isOk());

        var ejecutor = java.util.concurrent.Executors.newFixedThreadPool(2);
        var inicio = new java.util.concurrent.CyclicBarrier(2);
        java.util.concurrent.Callable<Integer> confirmar = () -> {
            inicio.await(5, java.util.concurrent.TimeUnit.SECONDS);
            return mockMvc.perform(post("/api/ecommerce/compras/realizar").header("Authorization", token))
                    .andReturn().getResponse().getStatus();
        };
        try {
            var primera = ejecutor.submit(confirmar);
            var segunda = ejecutor.submit(confirmar);
            var estados = java.util.List.of(primera.get(10, java.util.concurrent.TimeUnit.SECONDS),
                    segunda.get(10, java.util.concurrent.TimeUnit.SECONDS)).stream().sorted().toList();
            assertEquals(java.util.List.of(201, 400), estados);
            assertEquals(1, compraRepository.count());
            assertEquals(2, productoRepository.findById(producto.getId()).orElseThrow().getStock());
        } finally {
            ejecutor.shutdownNow();
        }
    }

    @Test
    void checkoutConCarritoVacioDevuelve400() throws Exception {
        simularUsuario(1L, "USER");

        mockMvc.perform(post("/api/ecommerce/compras/realizar")
                        .header("Authorization", bearer(1L, "USER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "No hay items en el carrito para realizar la compra"));
    }

    @Test
    void ultimaUnidadSeAgotaEnCatalogoYEnElCarritoDelOtroComprador() throws Exception {
        ProductoData producto = crearProducto("Teclado", "10.00", 1);
        simularUsuario(1L, "USER");
        simularUsuario(2L, "USER");
        String comprador = bearer(1L, "USER");
        String otroComprador = bearer(2L, "USER");

        for (String token : java.util.List.of(comprador, otroComprador)) {
            mockMvc.perform(post("/api/ecommerce/carrito/agregar")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(agregarItemJson(producto.getId(), 1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[0].stockDisponible").value(1));
        }

        mockMvc.perform(post("/api/ecommerce/compras/realizar").header("Authorization", comprador))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/ecommerce/producto/{id}", producto.getId()))
                .andExpect(jsonPath("$.stock").value(0))
                .andExpect(jsonPath("$.estadoStock").value("AGOTADO"));
        mockMvc.perform(get("/api/ecommerce/producto/explorar").param("q", "Teclado"))
                .andExpect(jsonPath("$.items[0].estadoStock").value("AGOTADO"));
        mockMvc.perform(get("/api/ecommerce/carrito").header("Authorization", otroComprador))
                .andExpect(jsonPath("$.items[0].stockDisponible").value(0));
        mockMvc.perform(get("/api/ecommerce/carrito/resumen").header("Authorization", otroComprador))
                .andExpect(jsonPath("$.habilitaCompra").value(false));

        mockMvc.perform(post("/api/ecommerce/compras/realizar")
                        .header("Authorization", otroComprador))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Stock insuficiente para el producto Teclado"));

        assertEquals(1, compraRepository.count());
        assertEquals(0, productoRepository.findById(producto.getId()).orElseThrow().getStock());
        assertEquals(1, carritoRepository.count());
    }

    @Test
    void usuarioNoPuedeVerComprasDeOtroUsuario() throws Exception {
        mockMvc.perform(get("/api/ecommerce/compras/usuario/{id}", 2L)
                        .header("Authorization", bearer(1L, "USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPuedeVerTodasLasCompras() throws Exception {
        ProductoData producto = crearProducto("Teclado", "10.00", 5);
        simularUsuario(1L, "USER");

        mockMvc.perform(post("/api/ecommerce/carrito/agregar")
                        .header("Authorization", bearer(1L, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(agregarItemJson(producto.getId(), 1)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/ecommerce/compras/realizar")
                        .header("Authorization", bearer(1L, "USER")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/ecommerce/compras")
                        .header("Authorization", bearer(99L, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioId").value(1));
    }

    private ProductoData crearProducto(String nombre, String precio, int stock) {
        return productoRepository.save(new ProductoData(
                null, nombre, "descripcion", new BigDecimal(precio), stock, null));
    }

    private void simularUsuario(Long id, String role) {
        when(usuarioGateway.buscarPorId(id)).thenReturn(new Usuario(
                id, "Usuario " + id, "user" + id + "@example.com", "3001234567", role));
    }

    private String productoJson(String nombre, String precio, int stock) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "nombre", nombre,
                "descripcion", "descripcion",
                "precio", new BigDecimal(precio),
                "stock", stock));
    }

    private String agregarItemJson(Long productoId, int cantidad) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "productoId", productoId,
                "cantidad", cantidad));
    }

    private static String bearer(Long id, String role) {
        Instant now = Instant.now();
        String token = Jwts.builder()
                .subject(String.valueOf(id))
                .claim("email", "user" + id + "@example.com")
                .claim("nombre", "Usuario " + id)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
        return "Bearer " + token;
    }
}
