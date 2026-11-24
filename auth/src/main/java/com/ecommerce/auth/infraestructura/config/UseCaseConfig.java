package com.ecommerce.auth.infraestructura.config;

import com.ecommerce.auth.domain.port.out.EncrypterGateway;
import com.ecommerce.auth.domain.port.out.NotificacionGateway;
import com.ecommerce.auth.domain.port.out.TokenGateway;
import com.ecommerce.auth.domain.port.out.UsuarioGateway;
import com.ecommerce.auth.application.port.in.UsuarioService;
import com.ecommerce.auth.application.usecase.UsuarioUseCase;
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
    public UsuarioService usuarioService(UsuarioGateway usuarioGateway,
                                         EncrypterGateway encrypterGateway,
                                         NotificacionGateway notificacionGateway,
                                         TokenGateway tokenGateway,
                                         PlatformTransactionManager gestor) {
        var caso = new UsuarioUseCase(usuarioGateway, encrypterGateway, notificacionGateway, tokenGateway);
        return transaccional(UsuarioService.class, caso, gestor, new String[]{"registrar", "actualizar", "eliminarPorId"},
                "buscarPorId", "existePorId");
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
