package pl.photodrive.core.infrastructure.jwt;

import com.nimbusds.jose.*;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.photodrive.core.application.port.token.TokenDecoder;
import pl.photodrive.core.application.port.token.TokenEncoder;
import pl.photodrive.core.application.port.user.AuthenticatedUser;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.vo.UserId;
import pl.photodrive.core.infrastructure.exception.ExpiredTokenException;
import pl.photodrive.core.infrastructure.exception.InvalidTokenException;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtTokenService implements TokenEncoder, TokenDecoder {

    private static final String ISSUER = "CeVeMe";
    private static final String KEY_ID = "key-2025-11";

    private final JWSSigner signer;
    private final JWSVerifierProvider verifiers;


    @Override
    public AuthenticatedUser parse(String rawJwt) {
        try {
            var signed = SignedJWT.parse(rawJwt);
            var header = signed.getHeader();
            if (!JWSAlgorithm.HS256.equals(header.getAlgorithm())) {
                throw new InvalidTokenException("Unsupported algorithm");
            }

            var kid = header.getKeyID();
            if (!KEY_ID.equals(kid)) {
                throw new InvalidTokenException("Unknown key");
            }

            var verifier = verifiers.forKid(kid);
            if (!signed.verify(verifier)) throw new InvalidTokenException("Signature");

            var claims = signed.getJWTClaimsSet();
            var now = Instant.now();
            if (!ISSUER.equals(claims.getIssuer())) {
                throw new InvalidTokenException("Invalid issuer");
            }

            if (claims.getExpirationTime() == null) {
                throw new InvalidTokenException("Missing expiration");
            }

            if (claims.getIssueTime() == null || claims.getIssueTime().toInstant().isAfter(now.plusSeconds(60))) {
                throw new InvalidTokenException("Invalid issue time");
            }

            var experience = claims.getExpirationTime().toInstant();

            if (now.isAfter(experience)) throw new ExpiredTokenException("Token is expired!");

            if (claims.getSubject() == null || claims.getSubject().isBlank()) {
                throw new InvalidTokenException("Missing subject");
            }

            var userId = new UserId(UUID.fromString(claims.getSubject()));

            Object rawRoles = claims.getClaim("roles");
            if (!(rawRoles instanceof List<?> rolesClaim) || rolesClaim.isEmpty()) {
                throw new InvalidTokenException("Missing roles");
            }

            Set<Role> roles = rolesClaim.stream().map(Object::toString).map(Role::valueOf).collect(Collectors.toSet());

            return new AuthenticatedUser(userId, roles, experience);
        } catch (ExpiredTokenException | InvalidTokenException e) {
            throw e;
        } catch (JOSEException | ParseException e) {
            throw new InvalidTokenException("Malformed token");
        } catch (RuntimeException e) {
            throw new InvalidTokenException("Invalid token claims");
        }

    }

    @Override
    public String createAccessToken(UserId userId, Set<Role> roles, Instant now, Duration ttl) {
        var claims = new JWTClaimsSet.Builder().issuer(ISSUER).subject(userId.value().toString()).claim("roles",
                roles.stream().map(Enum::name).toList()).issueTime(Date.from(now)).expirationTime(Date.from(now.plus(ttl))).build();

        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).keyID(KEY_ID).build();

        var signed = new SignedJWT(jwsHeader, claims);

        try {
            signed.sign(signer);
        } catch (JOSEException e) {
            throw new IllegalStateException("Signing failed", e);
        }
        return signed.serialize();
    }


}


