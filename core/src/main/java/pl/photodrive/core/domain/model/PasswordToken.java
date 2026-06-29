package pl.photodrive.core.domain.model;

import pl.photodrive.core.domain.event.user.PasswordTokenCreated;
import pl.photodrive.core.domain.exception.PasswordTokenException;
import pl.photodrive.core.domain.vo.PasswordTokenId;
import pl.photodrive.core.domain.vo.UserId;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

public class PasswordToken {

    private final PasswordTokenId id;
    private String tokenHash;
    private Instant expiration;
    private final Instant created;
    private final UserId userId;

    private transient final List<Object> domainEvents = new ArrayList<>();


    public PasswordToken(PasswordTokenId id, String tokenHash, Instant expiration, Instant created, UserId userId) {
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new PasswordTokenException("Token hash is null or blank");
        }
        if (expiration == null) {
            throw new PasswordTokenException("Expiration is null");
        }
        if (created == null) {
            throw new PasswordTokenException("Created is null");
        }
        if (userId == null) {
            throw new PasswordTokenException("UserId is null");
        }
        this.id = id;
        this.tokenHash = tokenHash;
        this.expiration = expiration;
        this.created = created;
        this.userId = userId;
    }

    public static PasswordToken create(UUID token, Instant expiration, Instant created, User user) {
        PasswordToken passwordToken = new PasswordToken(new PasswordTokenId(UUID.randomUUID()),
                hash(token),
                expiration,
                created,
                user.getId());

        passwordToken.registerEvent(new PasswordTokenCreated(user.getEmail().value(), token));

        return passwordToken;
    }

    public void updateToken(UUID token, String email) {
        if (token == null) {
            throw new PasswordTokenException("Token is null");
        }
        String newTokenHash = hash(token);
        if (this.tokenHash.equals(newTokenHash)) {
            throw new PasswordTokenException("Token is the same");
        }

        this.registerEvent(new PasswordTokenCreated(email, token));
        this.expiration = Instant.now().plusSeconds(900);
        this.tokenHash = newTokenHash;
    }

    public boolean matches(UUID rawToken) {
        if (rawToken == null) {
            return false;
        }
        return tokenHash.equals(hash(rawToken));
    }

    private static String hash(UUID token) {
        if (token == null) {
            throw new PasswordTokenException("Token is null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private void registerEvent(Object event) {
        this.domainEvents.add(event);
    }

    public List<Object> pullDomainEvents() {
        List<Object> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return Collections.unmodifiableList(events);
    }


    public UserId getUserId() {
        return userId;
    }

    public PasswordTokenId getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiration() {
        return expiration;
    }

    public Instant getCreated() {
        return created;
    }
}
