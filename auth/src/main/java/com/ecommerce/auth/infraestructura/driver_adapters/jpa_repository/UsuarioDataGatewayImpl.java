package com.ecommerce.auth.infraestructura.driver_adapters.jpa_repository;

import com.ecommerce.auth.domain.exception.UsuarioNoEncontradoException;
import com.ecommerce.auth.domain.model.Usuario;
import com.ecommerce.auth.domain.port.out.UsuarioGateway;
import com.ecommerce.auth.infraestructura.mapper.MapperUsuario;
import com.ecommerce.auth.infraestructura.security.PerfilAuth;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UsuarioDataGatewayImpl implements UsuarioGateway {

    private final MapperUsuario mapperUsuario;
    private final UsuarioDataJpaRepository repository;
    private final PerfilAuth perfil;

    @Override
    public Usuario guardar(Usuario usuario) {
        UsuarioData data = mapperUsuario.toUsuarioData(usuario);
        return perfil.medir("escritura", () -> mapperUsuario.toUsuario(repository.save(data)));
    }

    @Override
    public void eliminarPorID(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Usuario buscarPorID(Long id) {
        return perfil.medir("consulta", () -> repository.findById(id)
                .map(mapperUsuario::toUsuario)
                .orElse(null));
    }

    @Override
    public Usuario actualizarUsario(Usuario usuario) {
        UsuarioData existente = repository.findById(usuario.getId())
                .orElseThrow(() -> new UsuarioNoEncontradoException(usuario.getId()));

        existente.setNombre(usuario.getNombre());
        existente.setEmail(usuario.getEmail());
        existente.setPassword(usuario.getPassword());
        existente.setTelefono(usuario.getTelefono());
        existente.setRole(usuario.getRole());
        existente.setEdad(usuario.getEdad());

        return mapperUsuario.toUsuario(repository.save(existente));
    }

    @Override
    public boolean cambiarPassword(Long id, String hashAnterior, String hashNuevo) {
        return perfil.medir("escritura", () -> repository.cambiarPassword(id, hashAnterior, hashNuevo) == 1);
    }

    @Override
    public Usuario buscarPorEmail(String email) {
        return perfil.medir("consulta", () -> repository.findByEmailIgnoreCase(email)
                .map(mapperUsuario::toUsuario)
                .orElse(null));
    }
}
