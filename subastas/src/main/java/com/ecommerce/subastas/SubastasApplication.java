package com.ecommerce.subastas;

import com.ecommerce.subastas.application.port.in.Subastas;
import com.ecommerce.subastas.application.port.out.RepositorioSubastas;
import com.ecommerce.subastas.application.port.out.CalendarioFestivosPort;
import com.ecommerce.subastas.application.port.out.TasaCambioPort;
import com.ecommerce.subastas.application.usecase.GestionSubastas;
import com.ecommerce.subastas.application.usecase.ReferenciasSubastas;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Clock;
import java.time.ZoneId;

@SpringBootApplication(exclude = org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
@io.swagger.v3.oas.annotations.OpenAPIDefinition(info = @io.swagger.v3.oas.annotations.info.Info(title = "Remata | Subastas", version = "1"))
@SecurityScheme(name="jwt", type=SecuritySchemeType.HTTP, scheme="bearer", bearerFormat="JWT")
public class SubastasApplication {
    public static void main(String[] args) { SpringApplication.run(SubastasApplication.class,args); }
    @Bean Clock reloj() { return Clock.systemUTC(); }
    @Bean Subastas subastas(RepositorioSubastas repo, Clock reloj) { return new GestionSubastas(repo,reloj); }
    @Bean ReferenciasSubastas referencias(TasaCambioPort divisas, CalendarioFestivosPort calendario, Clock reloj) {
        return new ReferenciasSubastas(divisas,calendario,reloj.withZone(ZoneId.of("America/Bogota")));
    }
    @Bean ThreadPoolTaskScheduler taskScheduler() { return ejecutor("procesos-subastas-"); }
    @Bean ThreadPoolTaskScheduler proveedoresScheduler() { return ejecutor("referencias-subastas-"); }

    private ThreadPoolTaskScheduler ejecutor(String prefijo) {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix(prefijo);
        return scheduler;
    }
}
