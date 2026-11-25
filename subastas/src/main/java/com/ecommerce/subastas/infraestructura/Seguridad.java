package com.ecommerce.subastas.infraestructura;

import com.ecommerce.subastas.application.port.in.Subastas.Actor;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Configuration
class Seguridad {
    @Bean
    SecurityFilterChain cadena(HttpSecurity http, @Value("${JWT_SECRET}") String secreto) throws Exception {
        var parser=Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8))).build();
        var filtro=new OncePerRequestFilter() {
            @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain cadena)
                    throws ServletException, IOException {
                String token=req.getHeader("Authorization");
                if (token != null && token.startsWith("Bearer ")) {
                    try {
                        var claims=parser.parseSignedClaims(token.substring(7)).getPayload();
                        var actor=new Actor(Long.parseLong(claims.getSubject()), claims.get("email",String.class),
                                "ADMIN".equals(claims.get("role",String.class)));
                        if (actor.email() != null && !actor.email().isBlank()) {
                            SecurityContextHolder.getContext().setAuthentication(
                                    UsernamePasswordAuthenticationToken.authenticated(actor,null,List.of()));
                        }
                    } catch (io.jsonwebtoken.JwtException | IllegalArgumentException invalido) {
                        SecurityContextHolder.clearContext();
                    }
                }
                cadena.doFilter(req,res);
            }
        };
        return http.csrf(c->c.disable()).cors(Customizer.withDefaults())
                .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e->e.authenticationEntryPoint((req,res,error)->
                        res.sendError(401,"Inicia sesión para continuar")))
                .authorizeHttpRequests(a->a
                        .requestMatchers("/health","/actuator/health","/actuator/health/**","/error","/swagger-ui.html","/swagger-ui/**","/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/ecommerce/subastas/mias").authenticated()
                        .requestMatchers(HttpMethod.GET,"/api/subastas/inicio","/api/subastas/lote/*/precio-usd","/api/subastas/entrega").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/ecommerce/subastas","/api/ecommerce/subastas/*").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(filtro,UsernamePasswordAuthenticationFilter.class).build();
    }

    @Bean CorsConfigurationSource corsConfigurationSource(@Value("${CORS_ALLOWED_ORIGINS:http://localhost:3000}") String origenes) {
        var cfg=new CorsConfiguration();
        cfg.setAllowedOrigins(Arrays.stream(origenes.split(",")).map(String::strip).filter(s->!s.isBlank() && !s.equals("*")).toList());
        cfg.setAllowedMethods(List.of("GET","POST","OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization","Content-Type","Accept"));
        cfg.setMaxAge(3600L);
        var ruta=new UrlBasedCorsConfigurationSource();
        ruta.registerCorsConfiguration("/**",cfg);
        return ruta;
    }
}
