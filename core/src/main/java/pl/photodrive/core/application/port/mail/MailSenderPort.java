package pl.photodrive.core.application.port.mail;


public interface MailSenderPort {
    void send(String toEmail, String subject, String body);

    String loadResourceAsString(String classpathLocation);

    String escapeHtml(String html);
}