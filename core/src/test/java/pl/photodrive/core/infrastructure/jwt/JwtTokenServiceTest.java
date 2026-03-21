package pl.photodrive.core.infrastructure.jwt;

import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.photodrive.core.application.port.user.AuthenticatedUser;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.vo.UserId;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtTokenServiceTest {

    private JwtTokenService tokenService;
    private final String secret = "a-very-long-secret-key-that-must-be-at-least-256-bits";

    @BeforeEach
    void setUp() throws Exception {
        JWSSigner signer = new MACSigner(secret);
        JWSVerifierProvider verifiers = mock(JWSVerifierProvider.class);
        when(verifiers.forKid(any())).thenReturn(new MACVerifier(secret));
        
        tokenService = new JwtTokenService(signer, verifiers);
    }

    @Test
    void shouldEncodeAndDecodeTokenCorrectly() {
        UserId userId = new UserId(UUID.randomUUID());
        Set<Role> roles = Set.of(Role.PHOTOGRAPHER);
        Instant now = Instant.now();

        String token = tokenService.createAccessToken(userId, roles, now, Duration.ofMinutes(15));
        AuthenticatedUser parsed = tokenService.parse(token);

        assertEquals(userId, parsed.userId());
        assertEquals(roles, parsed.roles());
    }
}
