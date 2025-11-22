package pl.photodrive.core.infrastructure.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import pl.photodrive.core.application.port.mail.MailSenderPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class MailSenderAdapter implements MailSenderPort {

    private final JavaMailSender mailSender;
    private final ResourceLoader resourceLoader;

    @Value("${mail.username}")
    private String username;

    @Override
    public void send(String toEmail, String subject, String body) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    msg,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(new InternetAddress(username, "PhotoDrive"));
            helper.setTo(toEmail);
            helper.setSubject(subject);

            helper.setText(body,true);

            mailSender.send(msg);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String loadResourceAsString(String classpathLocation) {
        String resourcePath = "classpath:" + classpathLocation;
        Resource resource = resourceLoader.getResource(resourcePath);

        if (!resource.exists()) {
            throw new IllegalArgumentException("Resource not found: " + resourcePath);
        }

        try (InputStream is = resource.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read resource: " + resourcePath, e);
        }
    }


    @Override
    public String escapeHtml(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(html.length());
        for (int i = 0; i < html.length(); i++) {
            char c = html.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
