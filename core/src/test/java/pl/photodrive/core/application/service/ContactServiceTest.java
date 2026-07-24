package pl.photodrive.core.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.photodrive.core.application.command.contact.ContactCommand;
import pl.photodrive.core.application.port.mail.MailSenderPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    private static final String RECIPIENT = "studio@photodrive.dev";
    private static final String NOTIFICATION = "templates/email/contact-notification.html";
    private static final String CONFIRMATION = "templates/email/contact-confirmation.html";

    @Mock
    private MailSenderPort mailSenderPort;

    private ContactService service;

    @BeforeEach
    void setUp() {
        service = new ContactService(mailSenderPort, RECIPIENT);
    }

    private ContactCommand command() {
        return new ContactCommand("Jan Kowalski", "jan@example.com", "+48 111 222 333",
                "Fotografia ślubna", "Dzień dobry, chciałbym zapytać o termin.");
    }

    /** escapeHtml mockowany na realne (minimalne) escapowanie — bez tego assercje o treści byłyby fikcją. */
    private void stubEscapeAsRealEscaping() {
        given(mailSenderPort.escapeHtml(anyString())).willAnswer(invocation -> {
            String raw = invocation.getArgument(0);
            return raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        });
    }

    @Test
    @DisplayName("The studio notification goes to the configured recipient and carries the sender as Reply-To, so a plain reply reaches the guest")
    void shouldSendNotificationToRecipientWithSenderAsReplyTo() {
        // Given
        stubEscapeAsRealEscaping();
        given(mailSenderPort.loadResourceAsString(NOTIFICATION)).willReturn("N: {{name}} <{{email}}>");
        given(mailSenderPort.loadResourceAsString(CONFIRMATION)).willReturn("C: {{name}}");

        // When
        service.handle(command());

        // Then
        then(mailSenderPort).should().send(
                eq(RECIPIENT),
                eq("Nowe zapytanie ze strony: Fotografia ślubna"),
                argThat(body -> body.contains("Jan Kowalski") && body.contains("jan@example.com")),
                eq("jan@example.com"));
    }

    @Test
    @DisplayName("Untrusted form input is HTML-escaped before it enters the mail body, so a script tag cannot be injected")
    void shouldEscapeUntrustedInputBeforePuttingItIntoTheMailBody() {
        // Given - a guest sends a malicious message
        stubEscapeAsRealEscaping();
        given(mailSenderPort.loadResourceAsString(NOTIFICATION)).willReturn("{{message}}");
        given(mailSenderPort.loadResourceAsString(CONFIRMATION)).willReturn("{{message}}");
        ContactCommand malicious = new ContactCommand("Jan", "evil@example.com", null,
                "Inne", "<script>alert('xss')</script>");

        // When
        service.handle(malicious);

        // Then - the raw tag never reaches the body; only the escaped form does
        then(mailSenderPort).should().escapeHtml("<script>alert('xss')</script>");
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        then(mailSenderPort).should().send(eq(RECIPIENT), anyString(), body.capture(), eq("evil@example.com"));
        assertThat(body.getValue())
                .contains("&lt;script&gt;")
                .doesNotContain("<script>");
    }

    @Test
    @DisplayName("Newlines in the message become <br> after escaping, so the layout survives without opening an injection hole")
    void shouldConvertMessageNewlinesToLineBreaks() {
        // Given
        stubEscapeAsRealEscaping();
        given(mailSenderPort.loadResourceAsString(NOTIFICATION)).willReturn("{{message}}");
        given(mailSenderPort.loadResourceAsString(CONFIRMATION)).willReturn("{{message}}");
        ContactCommand multiline = new ContactCommand("Jan", "jan@example.com", null,
                "Inne", "Pierwsza linia\nDruga linia");

        // When
        service.handle(multiline);

        // Then
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        then(mailSenderPort).should().send(eq(RECIPIENT), anyString(), body.capture(), anyString());
        assertThat(body.getValue())
                .contains("Pierwsza linia<br>Druga linia")
                .doesNotContain("\n");
    }

    @Test
    @DisplayName("A confirmation is sent back to the guest with the studio as Reply-To")
    void shouldSendConfirmationToGuestWithStudioAsReplyTo() {
        // Given
        stubEscapeAsRealEscaping();
        given(mailSenderPort.loadResourceAsString(NOTIFICATION)).willReturn("N: {{name}}");
        given(mailSenderPort.loadResourceAsString(CONFIRMATION)).willReturn("Dzień dobry {{name}}");

        // When
        service.handle(command());

        // Then
        then(mailSenderPort).should().send(
                eq("jan@example.com"),
                eq("Dziękujemy za wiadomość — PhotoDrive"),
                argThat(body -> body.contains("Jan Kowalski")),
                eq(RECIPIENT));
    }

    @Test
    @DisplayName("A failed confirmation is tolerated, because the notification has already been delivered to the studio")
    void shouldNotFailTheRequestWhenOnlyTheConfirmationCannotBeSent() {
        // Given - the notification template loads, the confirmation one does not
        stubEscapeAsRealEscaping();
        given(mailSenderPort.loadResourceAsString(NOTIFICATION)).willReturn("N: {{message}}");
        given(mailSenderPort.loadResourceAsString(CONFIRMATION)).willThrow(new RuntimeException("template missing"));

        // When / Then - the guest still gets a success; the studio notification went out
        assertThatCode(() -> service.handle(command())).doesNotThrowAnyException();
        then(mailSenderPort).should().send(eq(RECIPIENT), anyString(), anyString(), eq("jan@example.com"));
        then(mailSenderPort).should(never()).send(eq("jan@example.com"), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("A failed studio notification propagates, so the guest is not told the message went through when it did not")
    void shouldPropagateWhenTheStudioNotificationCannotBeSent() {
        // Given - sending the notification to the studio blows up
        stubEscapeAsRealEscaping();
        given(mailSenderPort.loadResourceAsString(NOTIFICATION)).willReturn("N: {{message}}");
        willThrow(new RuntimeException("SMTP down"))
                .given(mailSenderPort).send(eq(RECIPIENT), anyString(), anyString(), anyString());

        // When / Then
        assertThatThrownBy(() -> service.handle(command()))
                .isInstanceOf(RuntimeException.class);
    }
}
