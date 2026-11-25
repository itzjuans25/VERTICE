package com.ecommerce.catalogo.application.usecase;

import com.ecommerce.catalogo.application.port.in.CompraService;
import com.ecommerce.catalogo.application.port.out.DespuesDeConfirmar;
import com.ecommerce.catalogo.domain.exception.CarritoVacioException;
import com.ecommerce.catalogo.domain.exception.CompraNoEncontradaException;
import com.ecommerce.catalogo.domain.exception.DatosInvalidosException;
import com.ecommerce.catalogo.domain.exception.StockInsuficienteException;
import com.ecommerce.catalogo.domain.exception.UsuarioNoEncontradoException;
import com.ecommerce.catalogo.domain.model.Carrito;
import com.ecommerce.catalogo.domain.model.Compra;
import com.ecommerce.catalogo.domain.model.ItemCarrito;
import com.ecommerce.catalogo.domain.model.MensajeNotificacion;
import com.ecommerce.catalogo.domain.model.NotificacionEmail;
import com.ecommerce.catalogo.domain.model.NotificacionSMS;
import com.ecommerce.catalogo.domain.model.Usuario;
import com.ecommerce.catalogo.domain.port.out.CarritoGateway;
import com.ecommerce.catalogo.domain.port.out.CompraGateway;
import com.ecommerce.catalogo.domain.port.out.EdicionGateway;
import com.ecommerce.catalogo.domain.port.out.NotificacionGateway;
import com.ecommerce.catalogo.domain.port.out.ProductoGateway;
import com.ecommerce.catalogo.domain.port.out.UsuarioGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class CompraUseCase implements CompraService {

    private final CompraGateway compraGateway;
    private final CarritoGateway carritoGateway;
    private final ProductoGateway productoGateway;
    private final UsuarioGateway usuarioGateway;
    private final NotificacionGateway notificacionGateway;
    private final DespuesDeConfirmar despuesDeConfirmar;
    private final EdicionGateway ediciones;

    @Override
    public Compra realizarCompra(Long usuarioId) {
        Usuario usuario = usuarioGateway.buscarPorId(usuarioId);
        if (usuario == null) {
            throw new UsuarioNoEncontradoException("Usuario no encontrado con ID: " + usuarioId);
        }

        Carrito carrito = carritoGateway.buscarPorUsuarioIdParaCompra(usuarioId);
        if (carrito == null || carrito.getItems() == null || carrito.getItems().isEmpty()) {
            throw new CarritoVacioException(
                    "No hay items en el carrito para realizar la compra");
        }

        // Descuento atomico: si un item no tiene stock, la transaccion revierte todo.
        var itemsPorProducto = carrito.getItems().stream()
                .sorted(Comparator.comparing(ItemCarrito::getProductoId)).toList();
        for (ItemCarrito item : itemsPorProducto) {
            ediciones.bloquear(item.getProductoId()).ifPresent(edicion ->
                    edicion.validarCompra(item.getCantidad(),
                            compraGateway.unidadesCompradas(usuarioId, item.getProductoId()), Instant.now()));
            if (!productoGateway.descontarStock(item.getProductoId(), item.getCantidad())) {
                throw new StockInsuficienteException(
                        "Stock insuficiente para el producto " + item.getNombreProducto());
            }
        }

        Compra compra = construirCompra(usuarioId, carrito);
        Compra compraGuardada = compraGateway.guardar(compra);

        // El carrito ya no se necesita: el stock se desconto en el checkout.
        carritoGateway.eliminarPorUsuarioId(usuarioId);

        despuesDeConfirmar.ejecutar(() -> enviarNotificacionCompra(compraGuardada, usuario));
        return compraGuardada;
    }

    @Override
    public List<Compra> obtenerComprasPorUsuario(Long usuarioId) {
        if (usuarioGateway.buscarPorId(usuarioId) == null) {
            throw new UsuarioNoEncontradoException(
                    "Usuario no encontrado con ID: " + usuarioId);
        }
        return compraGateway.buscarPorUsuarioId(usuarioId);
    }

    @Override
    public Compra obtenerCompraPorId(Long compraId) {
        Compra compra = compraGateway.buscarPorId(compraId);
        if (compra == null) {
            throw new CompraNoEncontradaException(compraId);
        }
        return compra;
    }

    @Override
    public List<Compra> obtenerTodasLasCompras() {
        return compraGateway.obtenerTodas();
    }

    private static Compra construirCompra(Long usuarioId, Carrito carrito) {
        Compra compra = new Compra();
        compra.setUsuarioId(usuarioId);
        compra.setFechaCompra(LocalDateTime.now());
        compra.setEstado("COMPLETADA");
        compra.setRequierePagoSimulado(true);

        List<ItemCarrito> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (ItemCarrito item : carrito.getItems()) {
            ItemCarrito copia = new ItemCarrito();
            copia.setProductoId(item.getProductoId());
            copia.setNombreProducto(item.getNombreProducto());
            copia.setPrecioUnitario(item.getPrecioUnitario());
            copia.setCantidad(item.getCantidad());
            BigDecimal subtotal = item.getPrecioUnitario()
                    .multiply(BigDecimal.valueOf(item.getCantidad()));
            copia.setSubtotal(subtotal);
            copia.setImagenUrl(item.getImagenUrl());
            items.add(copia);
            total = total.add(subtotal);
        }

        compra.setItems(items);
        compra.setTotal(total);
        return compra;
    }

    @Override
    public Compra pagarSimulado(Long id, Long propietario) {
        Compra compra = obtenerCompraPorId(id);
        exigirPropietario(compra, propietario);
        if (compra.getPagoSimuladoEn() != null) return compra;
        if (!Boolean.TRUE.equals(compra.getRequierePagoSimulado()) || "CANCELADA".equals(compra.getEstado())) {
            throw new DatosInvalidosException("La compra no admite pago simulado");
        }
        boolean primeraConfirmacion = compraGateway.registrarPagoSimulado(id, propietario, Instant.now());
        Compra confirmada = obtenerCompraPorId(id);
        if (confirmada.getPagoSimuladoEn() == null) {
            throw new DatosInvalidosException("La compra no admite pago simulado");
        }
        if (primeraConfirmacion) {
            for (ItemCarrito item : compra.getItems()) {
                ediciones.porProducto(item.getProductoId()).ifPresent(edicion ->
                        compraGateway.guardarCertificado(id, new CompraGateway.Certificado(
                                "VT-C-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase(Locale.ROOT),
                                edicion.productoId(), item.getNombreProducto(), edicion.serie(), confirmada.getPagoSimuladoEn())));
            }
            despuesDeConfirmar.ejecutar(() -> avisarPago(confirmada, propietario));
        }
        return confirmada;
    }

    @Override
    public List<CompraGateway.Certificado> certificados(Long id, Long propietario) {
        exigirPropietario(obtenerCompraPorId(id), propietario);
        return compraGateway.certificados(id);
    }

    private static void exigirPropietario(Compra compra, Long propietario) {
        if (!compra.getUsuarioId().equals(propietario)) throw new CompraNoEncontradaException(compra.getId());
    }

    private void avisarPago(Compra compra, Long propietario) {
        try {
            Usuario usuario = usuarioGateway.buscarPorId(propietario);
            if (usuario == null) return;
            String referencia = "SIM-COMPRA-" + compra.getId();
            notificacionGateway.enviarNotificacion(MensajeNotificacion.builder()
                    .tipo(MensajeNotificacion.TipoNotificacion.EMAIL)
                    .email(NotificacionEmail.builder().usuarioId(propietario).destinatario(usuario.getEmail())
                            .asunto("Pago simulado registrado · " + referencia)
                            .cuerpo("Registramos " + compra.getTotal() + " COP de demostración. Consulta el comprobante en tu actividad.")
                            .build()).build());
        } catch (RuntimeException e) {
            log.warn("No se pudo publicar el pago simulado de la compra {}", compra.getId());
        }
    }

    @Override
    public Compra cambiarEstado(Long id, String estado) {
        Compra compra = obtenerCompraPorId(id);
        String anterior = compra.getEstado();
        if (anterior.equals(estado)) {
            return compra;
        }
        if (!(("COMPLETADA".equals(anterior) && "ENVIADA".equals(estado))
                || ("ENVIADA".equals(anterior) && "ENTREGADA".equals(estado)))) {
            throw new com.ecommerce.catalogo.domain.exception.DatosInvalidosException("Transicion de pedido invalida");
        }
        Usuario usuario = usuarioGateway.buscarPorId(compra.getUsuarioId());
        if (!compraGateway.cambiarEstado(id, anterior, estado)) {
            throw new com.ecommerce.catalogo.domain.exception.DatosInvalidosException("El estado cambio en otra solicitud");
        }
        compra.setEstado(estado);
        despuesDeConfirmar.ejecutar(() -> notificarEstado(compra, usuario));
        return compra;
    }

    private void notificarEstado(Compra compra, Usuario usuario) {
        if (usuario == null) { return; }
        String asunto = "Estado del pedido #" + compra.getId() + " · VÉRTICE";
        String texto = "Pedido #" + compra.getId() + ": " + compra.getEstado();
        var mensaje = MensajeNotificacion.builder()
                .tipo(MensajeNotificacion.TipoNotificacion.EMAIL)
                .email(NotificacionEmail.builder().usuarioId(usuario.getId()).destinatario(usuario.getEmail()).asunto(asunto).cuerpo(texto).build())
                .build();
        if (usuario.getTelefono() != null && !usuario.getTelefono().isBlank()) {
            mensaje.setTipo(MensajeNotificacion.TipoNotificacion.AMBOS);
            mensaje.setSms(NotificacionSMS.builder().numeroTelefono(usuario.getTelefono()).mensaje(texto).build());
        }
        try {
            notificacionGateway.enviarNotificacion(mensaje);
        } catch (RuntimeException ex) {
            log.warn("No se pudo publicar el cambio de estado del pedido {}", compra.getId(), ex);
        }
    }

    private void enviarNotificacionCompra(Compra compra, Usuario usuario) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            NotificacionEmail email = NotificacionEmail.builder()
                    .usuarioId(usuario.getId()).destinatario(usuario.getEmail())
                    .asunto("Confirmacion de Compra #" + compra.getId() + " · VÉRTICE")
                    .cuerpo("<h1>Compra confirmada</h1>"
                            + "<p>Hola " + usuario.getNombre() + ",</p>"
                            + "<p>Tu compra ha sido procesada exitosamente.</p>"
                            + "<p><strong>Numero de orden:</strong> " + compra.getId() + "</p>"
                            + "<p><strong>Fecha:</strong> "
                            + compra.getFechaCompra().format(formatter) + "</p>"
                            + "<p><strong>Total:</strong> $" + compra.getTotal() + "</p>"
                            + "<p>Completa el pago simulado en la actividad de tu cuenta. Demostración académica.</p>")
                    .html(true)
                    .build();

            NotificacionSMS sms = null;
            if (usuario.getTelefono() != null && !usuario.getTelefono().isBlank()) {
                String productos = resumenProductos(compra.getItems());
                sms = NotificacionSMS.builder()
                        .numeroTelefono(usuario.getTelefono())
                        .mensaje("Hola " + usuario.getNombre() + "! Tu compra #" + compra.getId()
                                + " ha sido confirmada. Productos: " + productos
                                + ". Total: $" + compra.getTotal()
                                + ". Gracias por tu preferencia!")
                        .build();
            }

            MensajeNotificacion mensaje = MensajeNotificacion.builder()
                    .tipo(sms != null
                            ? MensajeNotificacion.TipoNotificacion.AMBOS
                            : MensajeNotificacion.TipoNotificacion.EMAIL)
                    .email(email)
                    .sms(sms)
                    .build();

            notificacionGateway.enviarNotificacion(mensaje);
        } catch (Exception e) {
            log.warn("No se pudo encolar la notificacion de la compra {}: {}",
                    compra.getId(), e.getMessage());
        }
    }

    private static String resumenProductos(List<ItemCarrito> items) {
        StringBuilder texto = new StringBuilder();
        int maxProductos = Math.min(3, items.size());
        for (int i = 0; i < maxProductos; i++) {
            texto.append(items.get(i).getNombreProducto());
            if (i < maxProductos - 1) {
                texto.append(", ");
            }
        }
        if (items.size() > maxProductos) {
            texto.append(" y ").append(items.size() - maxProductos).append(" mas");
        }
        return texto.toString();
    }
}
