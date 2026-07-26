package pl.photodrive.core.application.command.auth;

/**
 * Kod autoryzacji przychodzi jako SUROWY tekst, mimo że w domenie jest UUID-em.
 *
 * <p>Powód: użytkownik przepisuje go z maila ręcznie, więc literówka jest normalnym
 * przebiegiem, a nie awarią. Gdy pole miało typ {@code UUID}, Jackson wywracał się na
 * deserializacji, zanim żądanie dotarło do serwisu — a to dawało **500** i komunikat inny
 * niż przy zwykłym złym kodzie, czyli łamało regułę „jednolita odpowiedź dla KAŻDEJ
 * porażki resetu" (B.14). Parsowanie należy do {@code AuthManagerService}, bo tam żyją
 * pozostałe warunki tej reguły.
 */
public record RemindPasswordCommand(String email, String token, String newPassword) {
}
