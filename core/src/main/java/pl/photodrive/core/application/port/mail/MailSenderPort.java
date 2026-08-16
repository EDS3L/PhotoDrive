package pl.photodrive.core.application.port.mail;


public interface MailSenderPort {
    void send(String toEmail, String subject, String body);

    void send(String toEmail, String subject, String body, String replyToEmail);

    String loadResourceAsString(String classpathLocation);

    String escapeHtml(String html);
}
