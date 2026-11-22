package com.ecommerce.auth.infraestructura.driver_adapters.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface UsuarioDataJpaRepository extends JpaRepository<UsuarioData, Long> {

    Optional<UsuarioData> findByEmail(String email);

    Optional<UsuarioData> findByEmailIgnoreCase(String email);

    @Transactional
    @Modifying
    @Query("update UsuarioData u set u.password = :nuevo where u.id = :id and u.password = :anterior")
    int cambiarPassword(Long id, String anterior, String nuevo);
}
