package pl.photodrive.core.application.port.user;

import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.vo.UserId;

import java.time.Instant;
import java.util.Set;

/**
 * @param mustChangePassword flaga z tokenu (claim): użytkownik loguje się hasłem startowym
 *                           i musi je zmienić. Filtr blokuje wtedy wszystko poza zmianą hasła
 *                           i odczytem własnego profilu (B.20). Poza ścieżką tokenu (np. kontekst
 *                           bieżącego usera dla autoryzacji) nie jest śledzona i bywa {@code false}.
 */
public record AuthenticatedUser(UserId userId, Set<Role> roles, Instant expiresAt, boolean mustChangePassword) {
}
