package pl.photodrive.core.infrastructure.jwt;

import com.nimbusds.jose.*;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import pl.photodrive.core.application.port.AuthenticatedUser;
import pl.photodrive.core.application.port.TokenDecoder;
import pl.photodrive.core.application.port.TokenEncoder;
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

    private final JWSSigner signer;
    private final JWSVerifierProvider verifiers;


    @Override
    public AuthenticatedUser parse(String rawJwt) {
       try {
           var signed = SignedJWT.parse(rawJwt);
           var kid = signed.getHeader().getKeyID();
           var verifier = verifiers.forKid(kid);
           if(!signed.verify(verifier)) throw new InvalidTokenException("Signature");

           var claims = signed.getJWTClaimsSet();
           var now = Instant.now();
           var experience = claims.getExpirationTime().toInstant();

           if(now.isAfter(experience)) throw new ExpiredTokenException("Token is expired!");

           var userId = new UserId(UUID.fromString(claims.getSubject()));

           Set<Role> roles = ((List<?>) claims.getClaim("roles")).stream()
                   .map(Object::toString)
                   .map(Role::valueOf)
                   .collect(Collectors.toSet());

           return new AuthenticatedUser(userId, roles,experience);
       } catch (JOSEException | ParseException e) {
           throw new RuntimeException(e);
       }

    }

    @Override
    public String createAccessToken(UserId userId, Set<Role> roles, Instant now, Duration ttl) {
        String issuer = "CeVeMe";
        var claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(userId.value().toString())
                .claim("roles", roles.stream().map(Enum::name).toList())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(ttl)))
                .build();

        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.HS256)
                .type(JOSEObjectType.JWT)
                .keyID("key-2025-11")
                .build();

        var signed = new SignedJWT(jwsHeader,claims);

        try {
            signed.sign(signer);
        } catch (JOSEException e) {
            throw new IllegalStateException("Signing failed", e);
        }
        return signed.serialize();
    }


}


