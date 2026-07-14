package pl.photodrive.core.support;

import jakarta.servlet.http.Cookie;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import pl.photodrive.core.application.port.repository.UserRepository;
import pl.photodrive.core.application.port.token.TokenEncoder;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.HashedPassword;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;

/**
 * Zasiew danych i uwierzytelnianie dla testów integracyjnych.
 *
 * <p>Użytkownicy są zapisywani przez <b>prawdziwe</b> {@link UserRepository}, więc każdy
 * test integracyjny mimochodem sprawdza też adapter JPA i ręczne mapowanie encja↔domena.
 */
@Component
public class TestFixtures {

    private static final String COOKIE_NAME = "pd_at";

    private final UserRepository userRepository;
    private final TokenEncoder tokenEncoder;
    private final JdbcTemplate jdbcTemplate;

    // Lombok jest zależnością tylko kodu produkcyjnego — w testach konstruktor piszemy wprost.
    public TestFixtures(UserRepository userRepository, TokenEncoder tokenEncoder, JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.tokenEncoder = tokenEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Czyści bazę między testami. Kolejność DELETE-ów szanuje klucze obce
     * (pliki → albumy → przypisania → użytkownicy).
     */
    public void clearAll() {
        jdbcTemplate.execute("DELETE FROM files");
        jdbcTemplate.execute("DELETE FROM albums");
        jdbcTemplate.execute("DELETE FROM assigned_users");
        jdbcTemplate.execute("DELETE FROM user_roles");
        jdbcTemplate.execute("DELETE FROM passwordTokens");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM platform_watermark");
    }

    public User admin(String email) {
        return persist("Admin", email, Role.ADMIN);
    }

    public User photographer(String email) {
        return persist("Fotograf", email, Role.PHOTOGRAPHER);
    }

    public User client(String email) {
        return persist("Klient", email, Role.CLIENT);
    }

    private User persist(String name, String email, Role role) {
        User user = User.create(name, new Email(email), new HashedPassword("$2a$12$hashed"), role);
        return userRepository.save(user);
    }

    /**
     * Cookie {@code pd_at} z prawdziwym, podpisanym JWT — takie samo, jakie wystawia
     * logowanie. Żądanie przechodzi więc przez pełny łańcuch filtrów (rate limit →
     * origin → JWT), a nie obok niego, jak przy {@code @WithMockUser}.
     */
    public Cookie authCookie(User user) {
        String token = tokenEncoder.createAccessToken(
                user.getId(), user.getRoles(), Instant.now(), Duration.ofMinutes(60));
        return new Cookie(COOKIE_NAME, token);
    }

    /**
     * Prawdziwy JPEG — backend go dekoduje (miniatura 600px, resize, ZIP),
     * więc atrapa bajtów by nie przeszła.
     */
    public static byte[] jpeg(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.DARK_GRAY);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
