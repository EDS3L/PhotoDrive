package pl.photodrive.core.domain.model;

import pl.photodrive.core.domain.event.user.PasswordTokenCreated;
import pl.photodrive.core.domain.exception.PasswordTokenException;
import pl.photodrive.core.domain.vo.PasswordTokenId;
import pl.photodrive.core.domain.vo.UserId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PasswordToken {

    private PasswordTokenId id;
    private UUID token;
    private Instant expiration;
    private Instant created;
    private final UserId userId;

    private transient final List<Object> domainEvents = new ArrayList<>();


    public PasswordToken(PasswordTokenId id, UUID token, Instant expiration, Instant created, UserId userId) {
        if(token == null) {throw new PasswordTokenException("Token is null");}
        if(expiration == null) {throw new PasswordTokenException("Expiration is null");}
        if(created == null) {throw new PasswordTokenException("Created is null");}
        if(userId == null) {throw new PasswordTokenException("UserId is null");}
        this.id = id;
        this.token = token;
        this.expiration = expiration;
        this.created = created;
        this.userId = userId;
    }

    public static PasswordToken create(UUID token, Instant expiration, Instant created, User user) {
        PasswordToken passwordToken = new PasswordToken(new PasswordTokenId(UUID.randomUUID()), token, expiration, created, user.getId());

        passwordToken.registerEvent(new PasswordTokenCreated(user.getEmail().value(), token));

        return passwordToken;
    }

    public void updateToken(UUID token, String email) {
        if(token== null) {throw new PasswordTokenException("Token is null");}
        if(this.token == token) {throw new PasswordTokenException("Token is the same");}

        this.registerEvent(new PasswordTokenCreated(email, token));
        this.expiration = Instant.now().plusSeconds(900);
        this.token = token;
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

    public UUID getToken() {
        return token;
    }

    public Instant getExpiration() {
        return expiration;
    }

    public Instant getCreated() {
        return created;
    }
}
