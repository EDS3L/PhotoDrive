package pl.photodrive.core.infrastructure.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import pl.photodrive.core.application.port.user.AuthenticatedUser;
import pl.photodrive.core.application.port.user.CurrentUser;
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
        var context = SecurityContextHolder.getContext();
        var auth = context != null ? context.getAuthentication() : null;

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        Object principal = auth.getPrincipal();
        UUID userUuid;

        if (principal instanceof String s) {
            userUuid = UUID.fromString(s);
        } else if (principal instanceof UserDetails userDetails) {
            userUuid = UUID.fromString(userDetails.getUsername());
        } else {
            throw new IllegalStateException("Unsupported principal type: " + principal.getClass());
        }

        var userId = new UserId(userUuid);

        var roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .map(Role::valueOf)
                .collect(Collectors.toUnmodifiableSet());

        return Optional.of(new AuthenticatedUser(userId, roles, Instant.MAX));
    }
}