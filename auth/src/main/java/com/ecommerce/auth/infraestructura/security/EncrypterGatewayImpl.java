package com.ecommerce.auth.infraestructura.security;

import com.ecommerce.auth.domain.port.out.EncrypterGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EncrypterGatewayImpl implements EncrypterGateway {

    private final PasswordEncoder passwordEncoder;
    private final LimiteHash limiteHash;

    @Override
    public String encrypt(String password) {
        return limiteHash.ejecutar(() -> passwordEncoder.encode(password));
    }

    @Override
    public boolean checkPass(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || encodedPassword.isBlank()) {
            return false;
        }
        return limiteHash.ejecutar(() -> passwordEncoder.matches(rawPassword, encodedPassword));
    }
}
