package com.ecommerce.catalogo.infraestructura.config;

import com.ecommerce.catalogo.application.port.in.CarritoService;
import com.ecommerce.catalogo.application.port.in.CompraService;
import com.ecommerce.catalogo.application.usecase.ConsultarEdiciones;
import com.ecommerce.catalogo.domain.port.out.EdicionGateway;
import com.ecommerce.catalogo.application.port.in.ProductoService;
import com.ecommerce.catalogo.application.port.out.DespuesDeConfirmar;
import com.ecommerce.catalogo.application.usecase.InformacionTienda;
import com.ecommerce.catalogo.application.usecase.CarritoUseCase;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.ecommerce.catalogo.application.usecase.CompraUseCase;
import com.ecommerce.catalogo.application.usecase.ProductoUseCase;
import com.ecommerce.catalogo.domain.port.out.CarritoGateway;
import com.ecommerce.catalogo.domain.port.out.CompraGateway;
import com.ecommerce.catalogo.domain.port.out.NotificacionGateway;
import com.ecommerce.catalogo.domain.port.out.ProductoGateway;
import com.ecommerce.catalogo.domain.port.out.UsuarioGateway;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public ProductoService productoService(ProductoGateway productoGateway, PlatformTransactionManager gestor) {
        return transaccional(ProductoService.class, new ProductoUseCase(productoGateway), gestor,
                new String[]{"guardarProducto", "actualizarProducto", "eliminarProducto", "descontarStock"},
                "obtenerProducto", "obtenerTodos");
    }

    @Bean
    public CarritoService carritoService(CarritoGateway carritoGateway,
                                         ProductoGateway productoGateway,
                                         UsuarioGateway usuarioGateway, PlatformTransactionManager gestor) {
        return transaccional(CarritoService.class, new CarritoUseCase(carritoGateway, productoGateway, usuarioGateway), gestor,
                new String[]{"agregarProductoAlCarrito", "actualizarCantidadProducto", "eliminarProducto", "vaciarCarrito"},
                "obtenerCarrito");
    }

    @Bean
    public CompraService compraService(CompraGateway compraGateway,
                                       CarritoGateway carritoGateway,
                                       ProductoGateway productoGateway,
                                       UsuarioGateway usuarioGateway,
                                       NotificacionGateway notificacionGateway, DespuesDeConfirmar confirmacion,
                                       EdicionGateway ediciones, PlatformTransactionManager gestor) {
        var caso = new CompraUseCase(compraGateway, carritoGateway, productoGateway, usuarioGateway,
                notificacionGateway, confirmacion, ediciones);
        return transaccional(CompraService.class, caso, gestor, new String[]{"realizarCompra", "cambiarEstado", "pagarSimulado"},
                "obtenerComprasPorUsuario", "obtenerCompraPorId", "obtenerTodasLasCompras");
    }

    @Bean
    ConsultarEdiciones consultarEdiciones(EdicionGateway ediciones, ProductoGateway productos) {
        return new ConsultarEdiciones(ediciones, productos);
    }

    @Bean
    public InformacionTienda informacionTienda() {
        return new InformacionTienda();
    }

    @Bean
    public DespuesDeConfirmar despuesDeConfirmar() {
        return accion -> {
            if (!TransactionSynchronizationManager.isSynchronizationActive()) {
                accion.run();
                return;
            }
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    accion.run();
                }
            });
        };
    }

    private static <T> T transaccional(Class<T> contrato, T caso, PlatformTransactionManager gestor,
                                       String[] escrituras, String... lecturas) {
        var atributos = new NameMatchTransactionAttributeSource();
        var lectura = new RuleBasedTransactionAttribute();
        lectura.setReadOnly(true);
        var escritura = new RuleBasedTransactionAttribute();
        for (String metodo : lecturas) atributos.addTransactionalMethod(metodo, lectura);
        for (String metodo : escrituras) atributos.addTransactionalMethod(metodo, escritura);

        var interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(gestor);
        interceptor.setTransactionAttributeSource(atributos);
        var proxy = new ProxyFactory(caso);
        proxy.addAdvice(interceptor);
        return contrato.cast(proxy.getProxy());
    }
}
