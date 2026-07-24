package pl.photodrive.core.application.port.mail;


public interface MailSenderPort {
    void send(String toEmail, String subject, String body);

    /**
     * Jak {@link #send(String, String, String)}, ale ustawia nagłówek Reply-To. Formularz kontaktowy
     * wysyła powiadomienie z adresu platformy, ale odpowiedź ma trafić do gościa — {@code replyToEmail}
     * niesie jego adres. {@code null}/blank = brak nagłówka (zachowanie jak 3-argumentowego wariantu).
     */
    void send(String toEmail, String subject, String body, String replyToEmail);

    String loadResourceAsString(String classpathLocation);

    String escapeHtml(String html);
}
