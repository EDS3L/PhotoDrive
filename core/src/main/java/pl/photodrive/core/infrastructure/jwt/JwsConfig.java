package pl.photodrive.core.infrastructure.jwt;

import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.MACSigner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;

@Configuration
public class JwsConfig {

    @Bean
    JWSSigner jwsSigner(@Value("${jwt.secret}") String SECRET_KEY) {
        try {
            byte[] secretBytes = Base64.getDecoder().decode(SECRET_KEY);

            if (secretBytes.length < 32) {
                throw new IllegalArgumentException("JWT secret must be at least 256 bits (32 bytes)");
            }

            return new MACSigner(secretBytes);
        } catch (KeyLengthException e) {
            throw new RuntimeException(e);
        }
    }
}
