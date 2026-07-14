package pl.photodrive.core.application.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.photodrive.core.application.event.UserCredentialsNotification;
import pl.photodrive.core.application.port.file.FileStoragePort;
import pl.photodrive.core.application.port.mail.MailSenderPort;
import pl.photodrive.core.domain.event.user.PasswordTokenCreated;
import pl.photodrive.core.domain.event.user.UserCreated;
import pl.photodrive.core.domain.event.user.UserRemindedPassword;
import pl.photodrive.core.domain.model.Role;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEventHandlerTest {

    @Mock
    private FileStoragePort fileStoragePort;

    @Mock
    private MailSenderPort mailSenderPort;

    @InjectMocks
    private UserEventHandler handler;

    @Test
    @DisplayName("Nowy fotograf dostaje własny folder w storage")
    void shouldCreateFolderForPhotographer() {
        handler.handlePhotographCreated(new UserCreated(
                UUID.randomUUID(), "foto@photodrive.dev", Set.of(Role.PHOTOGRAPHER)));

        verify(fileStoragePort).createPhotographerFolder("foto@photodrive.dev");
    }

    @Test
    @DisplayName("Klient NIE dostaje folderu fotografa")
    void shouldNotCreateFolderForClient() {
        handler.handlePhotographCreated(new UserCreated(
                UUID.randomUUID(), "klient@photodrive.dev", Set.of(Role.CLIENT)));

        verifyNoInteractions(fileStoragePort);
    }

    @Test
    @DisplayName("Mail powitalny podstawia email i hasło startowe, po uprzednim escape'owaniu")
    void shouldSendCredentialsMailWithEscapedValues() {
        when(mailSenderPort.escapeHtml("klient@photodrive.dev")).thenReturn("klient@photodrive.dev");
        when(mailSenderPort.escapeHtml("Haslo123!")).thenReturn("Haslo123!");
        when(mailSenderPort.loadResourceAsString("templates/email/account-created-credentials.html"))
                .thenReturn("<p>{{email}} / {{password}}</p>");

        handler.handleCredentialsNotification(
                new UserCredentialsNotification("klient@photodrive.dev", "Haslo123!"));

        verify(mailSenderPort).escapeHtml("Haslo123!");
        verify(mailSenderPort).send(eq("klient@photodrive.dev"), anyString(), argThat(body ->
                body.contains("klient@photodrive.dev")
                        && body.contains("Haslo123!")
                        && !body.contains("{{email}}")
                        && !body.contains("{{password}}")));
    }

    @Test
    @DisplayName("Mail z kodem autoryzacji podstawia token")
    void shouldSendResetTokenMail() {
        UUID token = UUID.randomUUID();
        when(mailSenderPort.escapeHtml(token.toString())).thenReturn(token.toString());
        when(mailSenderPort.loadResourceAsString("templates/email/password_reset_token.html"))
                .thenReturn("<p>{{token}}</p>");

        handler.handleTokenCreated(new PasswordTokenCreated("klient@photodrive.dev", token));

        verify(mailSenderPort).send(eq("klient@photodrive.dev"), anyString(), argThat(body ->
                body.contains(token.toString()) && !body.contains("{{token}}")));
    }

    @Test
    @DisplayName("Zmiana hasła wysyła potwierdzenie")
    void shouldSendPasswordChangedMail() {
        when(mailSenderPort.loadResourceAsString("templates/email/password_changed.html"))
                .thenReturn("<p>zmienione</p>");

        handler.handleUserRemindPassword(new UserRemindedPassword("klient@photodrive.dev"));

        verify(mailSenderPort).send(eq("klient@photodrive.dev"), anyString(), eq("<p>zmienione</p>"));
    }

    @Test
    @DisplayName("Padnięcie poczty NIE wywraca operacji — mail leci AFTER_COMMIT, konto już istnieje")
    void shouldSwallowMailFailures() {
        when(mailSenderPort.escapeHtml(anyString())).thenReturn("x");
        when(mailSenderPort.loadResourceAsString(anyString())).thenThrow(new RuntimeException("SMTP down"));

        assertThatCode(() -> handler.handleCredentialsNotification(
                new UserCredentialsNotification("klient@photodrive.dev", "Haslo123!")))
                .doesNotThrowAnyException();

        assertThatCode(() -> handler.handleTokenCreated(
                new PasswordTokenCreated("klient@photodrive.dev", UUID.randomUUID())))
                .doesNotThrowAnyException();

        verify(mailSenderPort, never()).send(anyString(), anyString(), anyString());
    }
}
