package pl.photodrive.core.infrastructure.jwt;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.vo.UserId;
import pl.photodrive.core.infrastructure.exception.InvalidTokenException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private static final byte[] SECRET = "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8);

    @Test
    @DisplayName("Issued access token can be parsed back into its claims")
    void shouldCreateAndParseAccessToken() throws Exception {
        // Given
        JWSSigner signer = new MACSigner(SECRET);
        JwtTokenService service = new JwtTokenService(signer, new JWSVerifierProviderImpl(signer));
        UUID userUuid = UUID.randomUUID();
        Instant now = Instant.now();

        String token = service.createAccessToken(new UserId(userUuid), Set.of(Role.ADMIN), now, Duration.ofMinutes(15), false);

        // When
        var authenticated = service.parse(token);

        // Then
        assertThat(authenticated.userId().value()).isEqualTo(userUuid);
        assertThat(authenticated.roles()).containsExactly(Role.ADMIN);
        assertThat(authenticated.mustChangePassword()).isFalse();
        assertThat(authenticated.expiresAt())
                .isBetween(now.plus(Duration.ofMinutes(15)).minusSeconds(1),
                        now.plus(Duration.ofMinutes(15)).plusSeconds(1));
    }

    @Test
    @DisplayName("The forced-change flag survives a token round-trip, so the filter can lock a starting-password session (B.20)")
    void shouldRoundTripMustChangePasswordFlag() throws Exception {
        // Given
        JWSSigner signer = new MACSigner(SECRET);
        JwtTokenService service = new JwtTokenService(signer, new JWSVerifierProviderImpl(signer));

        // When - a token minted for a user that must change the password
        String token = service.createAccessToken(new UserId(UUID.randomUUID()), Set.of(Role.CLIENT),
                Instant.now(), Duration.ofMinutes(15), true);

        // Then
        assertThat(service.parse(token).mustChangePassword()).isTrue();
    }

    @Test
    @DisplayName("A token minted before B.20 (no flag claim) parses as not requiring a change, so existing sessions are not locked out")
    void shouldTreatTokenWithoutFlagClaimAsNoChangeRequired() throws Exception {
        // Given - a valid token that simply lacks the mcp claim (issued before the feature)
        JWSSigner signer = new MACSigner(SECRET);
        JwtTokenService service = new JwtTokenService(signer, new JWSVerifierProviderImpl(signer));
        String legacyToken = signToken(signer, "CeVeMe", "key-2025-11");

        // When / Then
        assertThat(service.parse(legacyToken).mustChangePassword()).isFalse();
    }

    @Test
    @DisplayName("Token signed with an unknown key id is rejected")
    void shouldRejectTokenWithUnknownKeyId() throws Exception {
        // Given
        JWSSigner signer = new MACSigner(SECRET);
        JwtTokenService service = new JwtTokenService(signer, new JWSVerifierProviderImpl(signer));
        String token = signToken(signer, "CeVeMe", "old-key");

        // When / Then
        assertThatThrownBy(() -> service.parse(token))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Unknown key");
    }

    @Test
    @DisplayName("Token without the expected issuer is rejected")
    void shouldRejectTokenWithoutIssuer() throws Exception {
        // Given
        JWSSigner signer = new MACSigner(SECRET);
        JwtTokenService service = new JwtTokenService(signer, new JWSVerifierProviderImpl(signer));
        String token = signToken(signer, null, "key-2025-11");

        // When / Then
        assertThatThrownBy(() -> service.parse(token))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Invalid issuer");
    }

    private String signToken(JWSSigner signer, String issuer, String keyId) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(UUID.randomUUID().toString())
                .claim("roles", List.of(Role.ADMIN.name()))
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(Duration.ofMinutes(15))))
                .build();
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
                .type(JOSEObjectType.JWT)
                .keyID(keyId)
                .build();
        SignedJWT jwt = new SignedJWT(header, claims);
        jwt.sign(signer);
        return jwt.serialize();
    }
}
