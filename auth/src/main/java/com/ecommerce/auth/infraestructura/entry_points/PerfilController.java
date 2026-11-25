package com.ecommerce.auth.infraestructura.entry_points;

import com.ecommerce.auth.application.port.in.UsuarioService;
import com.ecommerce.auth.domain.model.Usuario;
import com.ecommerce.auth.infraestructura.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/perfil")
class PerfilController {
    private final UsuarioService usuarios;

    @GetMapping
    @Operation(summary = "Consultar el perfil de la sesión")
    Perfil consultar(@AuthenticationPrincipal AuthenticatedUser actual) {
        return Perfil.de(usuarios.buscarPorId(actual.id()));
    }

    @PutMapping
    @Operation(summary = "Actualizar nombre y teléfono de la sesión")
    Perfil editar(@AuthenticationPrincipal AuthenticatedUser actual, @Valid @RequestBody Edicion edicion) {
        Usuario cambios = new Usuario();
        cambios.setId(actual.id());
        cambios.setNombre(edicion.nombre());
        cambios.setTelefono(edicion.telefono());
        return Perfil.de(usuarios.actualizar(cambios));
    }

    record Edicion(@NotBlank @Size(max=100) String nombre,
                   @Size(max=30) @Pattern(regexp="[+0-9() -]*") String telefono) {}
    record Perfil(Long id, String nombre, String telefono, String email) {
        static Perfil de(Usuario u) { return new Perfil(u.getId(),u.getNombre(),u.getTelefono(),u.getEmail()); }
    }
}