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
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserEventHandlerTest {

    @Mock
    private FileStoragePort fileStoragePort;

    @Mock
    private MailSenderPort mailSenderPort;

    @InjectMocks
    private UserEventHandler handler;

    @Test
    @DisplayName("A new photographer gets his own storage folder")
    void shouldCreateFolderForPhotographer() {
        // When
        handler.handlePhotographCreated(new UserCreated(
                UUID.randomUUID(), "foto@photodrive.dev", Set.of(Role.PHOTOGRAPHER)));

        // Then
        then(fileStoragePort).should().createPhotographerFolder("foto@photodrive.dev");
    }

    @Test
    @DisplayName("A new client gets no photographer folder")
    void shouldNotCreateFolderForClient() {
        // When
        handler.handlePhotographCreated(new UserCreated(
                UUID.randomUUID(), "klient@photodrive.dev", Set.of(Role.CLIENT)));

        // Then
        then(fileStoragePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Welcome mail carries the escaped email and the generated start password")
    void shouldSendCredentialsMailWithEscapedValues() {
        // Given
        given(mailSenderPort.escapeHtml("klient@photodrive.dev")).willReturn("klient@photodrive.dev");
        given(mailSenderPort.escapeHtml("Haslo123!")).willReturn("Haslo123!");
        given(mailSenderPort.loadResourceAsString("templates/email/account-created-credentials.html")).willReturn("<p>{{email}} / {{password}}</p>");

        // When
        handler.handleCredentialsNotification(
                new UserCredentialsNotification("klient@photodrive.dev", "Haslo123!"));

        // Then
        then(mailSenderPort).should().escapeHtml("Haslo123!");
        then(mailSenderPort).should().send(eq("klient@photodrive.dev"), anyString(), argThat(body ->
                body.contains("klient@photodrive.dev")
                        && body.contains("Haslo123!")
                        && !body.contains("{{email}}")
                        && !body.contains("{{password}}")));
    }

    @Test
    @DisplayName("Password reset mail carries the authorization code")
    void shouldSendResetTokenMail() {
        // Given
        UUID token = UUID.randomUUID();
        given(mailSenderPort.escapeHtml(token.toString())).willReturn(token.toString());
        given(mailSenderPort.loadResourceAsString("templates/email/password_reset_token.html")).willReturn("<p>{{token}}</p>");

        // When
        handler.handleTokenCreated(new PasswordTokenCreated("klient@photodrive.dev", token));

        // Then
        then(mailSenderPort).should().send(eq("klient@photodrive.dev"), anyString(), argThat(body ->
                body.contains(token.toString()) && !body.contains("{{token}}")));
    }

    @Test
    @DisplayName("Password change is confirmed by mail")
    void shouldSendPasswordChangedMail() {
        // Given
        given(mailSenderPort.loadResourceAsString("templates/email/password_changed.html")).willReturn("<p>zmienione</p>");

        // When
        handler.handleUserRemindPassword(new UserRemindedPassword("klient@photodrive.dev"));

        // Then
        then(mailSenderPort).should().send(eq("klient@photodrive.dev"), anyString(), eq("<p>zmienione</p>"));
    }

    @Test
    @DisplayName("A broken mail server does not undo the operation, because mail is sent after commit")
    void shouldSwallowMailFailures() {
        // Given
        given(mailSenderPort.escapeHtml(anyString())).willReturn("x");
        given(mailSenderPort.loadResourceAsString(anyString())).willThrow(new RuntimeException("SMTP down"));

        // When / Then
        assertThatCode(() -> handler.handleCredentialsNotification(
                new UserCredentialsNotification("klient@photodrive.dev", "Haslo123!")))
                .doesNotThrowAnyException();

        assertThatCode(() -> handler.handleTokenCreated(
                new PasswordTokenCreated("klient@photodrive.dev", UUID.randomUUID())))
                .doesNotThrowAnyException();

        then(mailSenderPort).should(never()).send(anyString(), anyString(), anyString());
    }
}
