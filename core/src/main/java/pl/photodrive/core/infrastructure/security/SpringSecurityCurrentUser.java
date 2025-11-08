package pl.photodrive.core.infrastructure.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import pl.photodrive.core.application.port.AuthenticatedUser;
import pl.photodrive.core.application.port.CurrentUser;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.vo.UserId;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
class SpringSecurityCurrentUser implements CurrentUser {

    @Override
    public Optional<AuthenticatedUser> get() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) return Optional.empty();

        var userId = new UserId(UUID.fromString((String) auth.getPrincipal()));

        var roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .map(Role::valueOf)
                .collect(Collectors.toSet());

        return Optional.of(new AuthenticatedUser(userId, roles, Instant.EPOCH));
    }
}