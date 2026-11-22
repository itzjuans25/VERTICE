package com.ecommerce.auth.infraestructura.entry_points;

import com.ecommerce.auth.application.port.in.UsuarioService;
import com.ecommerce.auth.domain.exception.CredencialesInvalidasException;
import com.ecommerce.auth.infraestructura.entry_points.dto.ActualizarUsuarioRequest;
import com.ecommerce.auth.infraestructura.entry_points.dto.LoginRequest;
import com.ecommerce.auth.infraestructura.entry_points.dto.LoginResponse;
import com.ecommerce.auth.infraestructura.entry_points.dto.RegistroUsuarioRequest;
import com.ecommerce.auth.infraestructura.entry_points.dto.UsuarioResponse;
import com.ecommerce.auth.infraestructura.entry_points.mapper.UsuarioDtoMapper;
import com.ecommerce.auth.infraestructura.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ecommerce/usuario")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioDtoMapper mapper;
    private final LimiteLogin limiteLogin;

    @PostMapping("/save")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse save(@Valid @RequestBody RegistroUsuarioRequest request) {
        return mapper.toResponse(usuarioService.registrar(mapper.toDomain(request)));
    }

    @PostMapping("/login")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sesión iniciada",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = LoginResponse.class)))
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        String cuenta = request.email().strip().toLowerCase(java.util.Locale.ROOT);
        String origen = http.getRemoteAddr();
        long admision = limiteLogin.admitir(cuenta, origen);
        if (admision == -1) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(LimiteLogin.VENTANA_SEGUNDOS))
                    .body(java.util.Map.of("message", "Demasiados intentos de inicio de sesión. Intenta en un minuto."));
        }
        boolean credencialesInvalidas = false;
        try {
            return ResponseEntity.ok(mapper.toLoginResponse(usuarioService.login(request.email(), request.password())));
        } catch (CredencialesInvalidasException ex) {
            credencialesInvalidas = true;
            throw ex;
        } finally {
            if (!credencialesInvalidas) {
                limiteLogin.liberar(cuenta, origen, admision);
            }
        }
    }

    @GetMapping("/sesion")
    public SesionResponse sesion(@AuthenticationPrincipal AuthenticatedUser actual) {
        usuarioService.buscarPorId(actual.id());
        return new SesionResponse(actual.id(), actual.role(), "ACTIVA");
    }

    @PostMapping("/renovar")
    public LoginResponse renovar(@AuthenticationPrincipal AuthenticatedUser actual,
                                 @Valid @RequestBody ConfirmacionPassword request) {
        return mapper.toLoginResponse(usuarioService.renovarSesion(actual.id(), request.password()));
    }

    @PostMapping("/cambiar-clave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarClave(@AuthenticationPrincipal AuthenticatedUser actual,
                             @Valid @RequestBody CambioPassword request) {
        usuarioService.cambiarPassword(actual.id(), request.actual(), request.nueva());
    }

    @PostMapping("/evaluar-clave")
    public PoliticaPassword evaluarClave(@Valid @RequestBody EvaluarPassword request) {
        String problema = usuarioService.problemaDePassword(request.password());
        return new PoliticaPassword(problema == null, problema);
    }

    public record ConfirmacionPassword(@NotBlank @Size(max = 72) String password) {}
    public record CambioPassword(@NotBlank @Size(max = 72) String actual,
                                 @NotBlank @Size(max = 72) String nueva) {}
    public record EvaluarPassword(@Size(max = 256) String password) {}
    public record PoliticaPassword(boolean valida, String motivo) {}
    public record SesionResponse(Long usuarioId, String rol, String estado) {}

    @GetMapping("/{id}")
    public UsuarioResponse findById(@PathVariable Long id,
                                    @AuthenticationPrincipal AuthenticatedUser actual) {
        asegurarPropietarioOAdmin(actual, id);
        return mapper.toResponse(usuarioService.buscarPorId(id));
    }

    /**
     * Endpoint interno usado por el servicio de catalogo para validar usuarios.
     * No expone datos personales, solo existencia.
     */
    @GetMapping("/{id}/existe")
    public boolean usuarioExiste(@PathVariable Long id) {
        return usuarioService.existePorId(id);
    }

    @PutMapping("/update")
    public UsuarioResponse update(@Valid @RequestBody ActualizarUsuarioRequest request,
                                  @AuthenticationPrincipal AuthenticatedUser actual) {
        asegurarPropietarioOAdmin(actual, request.id());
        return mapper.toResponse(usuarioService.actualizar(mapper.toDomain(request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUsuario(@PathVariable Long id) {
        usuarioService.eliminarPorId(id);
    }

    private void asegurarPropietarioOAdmin(AuthenticatedUser actual, Long id) {
        if (actual == null) {
            throw new AccessDeniedException("No autenticado");
        }
        boolean esAdmin = "ADMIN".equalsIgnoreCase(actual.role());
        if (!esAdmin && !actual.id().equals(id)) {
            throw new AccessDeniedException("Solo puedes acceder a tus propios datos");
        }
    }
}
