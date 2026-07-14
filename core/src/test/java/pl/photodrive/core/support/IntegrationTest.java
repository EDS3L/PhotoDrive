package pl.photodrive.core.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Baza testów integracyjnych: prawdziwy Spring, prawdziwy MySQL (Testcontainers),
 * prawdziwy dysk (katalog tymczasowy) — w odróżnieniu od testów jednostkowych,
 * gdzie repozytoria i storage są mockami.
 *
 * <p>Sens tej warstwy: testy jednostkowe sprawdzają, czy serwis <em>woła</em> port.
 * Dopiero tutaj sprawdzamy, czy dane naprawdę <em>lądują w bazie</em>, pliki na dysku,
 * a mapowanie encja↔domena (pisane ręcznie) nie gubi po drodze pól. Wcześniej cała ta
 * warstwa — łącznie z {@code AlbumController} — była nietestowana i wykluczona z pomiaru.
 *
 * <p><b>Kontener jest singletonem</b>: startuje raz na całą sesję JVM i celowo NIE jest
 * zatrzymywany (sprząta go Ryuk). Adnotacja {@code @Container} restartowałaby MySQL dla
 * każdej klasy testowej — kilkadziesiąt sekund za każdym razem.
 *
 * <p><b>Wymaga uruchomionego Dockera.</b> Bez niego testy integracyjne nie wystartują
 * (CI: `ubuntu-latest` ma Dockera z pudełka).
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTest {

    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.0"));

    /** Katalog na pliki albumów — jeden na przebieg, kasowany przez OS. */
    private static final Path STORAGE_DIR;
    private static final Path TEMP_DIR;

    static {
        MYSQL.start();
        try {
            STORAGE_DIR = Files.createTempDirectory("photodrive-it-storage");
            TEMP_DIR = Files.createTempDirectory("photodrive-it-temp");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Konfiguracja, którą produkcyjnie wstrzykuje środowisko (`.env`). Testy dostają
     * ten sam `application.yml` co prod — tylko z innymi wartościami, żeby nie było
     * osobnej, „testowej" konfiguracji, która z czasem rozjeżdża się z prawdziwą.
     */
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("DATASOURCE_URL", MYSQL::getJdbcUrl);
        registry.add("DATASOURCE_USERNAME", MYSQL::getUsername);
        registry.add("DATASOURCE_PASSWORD", MYSQL::getPassword);

        registry.add("DIR", () -> STORAGE_DIR.toString());
        registry.add("STORAGE_TEMP_PATH", () -> TEMP_DIR.toString());
        registry.add("ORG_MAX_SIZE", () -> 10_000_000L);

        // Sekret HS256: aplikacja dekoduje go z BASE64 i wymaga min. 256 bitów.
        // Zwykły tekst tu nie przejdzie („Illegal base64 character").
        registry.add("TOKEN_SECRETKEY", () -> "cGhvdG9kcml2ZS1pbnRlZ3JhdGlvbi10ZXN0LXNlY3JldC1rZXktMzJiKw==");

        // Poczta: host jest wymagany do startu kontekstu, ale samo wysyłanie jest zamockowane
        // (mock na `JavaMailSender`, nie na `MailSenderPort` — dzięki temu prawdziwy adapter
        // NADAL ładuje i wypełnia szablony HTML, testujemy więc też tę ścieżkę).
        registry.add("MAIL_HOST", () -> "localhost");
        registry.add("MAIL_USERNAME", () -> "it@photodrive.dev");
        registry.add("MAIL_PASSWORD", () -> "irrelevant");
        // Adapter czyta `mail.username` — produkcyjnie mapuje się na to zmienna środowiskowa
        // MAIL_USERNAME (relaxed binding), ale property z testu nie przechodzi tej konwersji.
        registry.add("mail.username", () -> "it@photodrive.dev");

        registry.add("SWAGGER_USER", () -> "swagger");
        registry.add("SWAGGER_PASSWORD", () -> "swagger");

        // MockMvc nie jest po HTTPS — cookie Secure nigdy by nie doleciało.
        registry.add("COOKIE_SECURE", () -> false);
    }

    /** Jedyna rzecz odcięta od świata: wyjście SMTP. Reszta (baza, dysk, filtry) jest prawdziwa. */
    @MockitoBean
    protected JavaMailSender mailSender;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected TestFixtures fixtures;

    @Autowired
    private TransactionTemplate transactionTemplate;

    /** Katalog, w którym backend naprawdę zapisuje pliki — testy zaglądają tam wprost. */
    protected static Path storageRoot() {
        return STORAGE_DIR;
    }

    /**
     * Odczyt agregatu MUSI iść w transakcji: adapter JPA mapuje encję na model domenowy
     * <b>leniwie</b> (kolekcja zdjęć), więc poza transakcją Hibernate nie ma już sesji
     * i rzuca „failed to lazily initialize a collection". Produkcyjnie problemu nie ma,
     * bo serwisy są {@code @Transactional} — test woła repozytorium wprost, więc musi
     * otworzyć transakcję sam.
     *
     * <p>Świadomie NIE oznaczamy testów {@code @Transactional}: to zamknęłoby cały test
     * w jednej transakcji z rollbackiem, przez co handlery {@code BEFORE_COMMIT} nigdy by
     * się nie wykonały i <b>nie powstałby ani jeden plik na dysku</b> — czyli zniknęłoby
     * dokładnie to, co ten test ma sprawdzać.
     */
    protected <T> T inTransaction(Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }

    @BeforeEach
    void resetDatabase() {
        // Każdy test zaczyna od pustej bazy — inaczej kolejność testów zmienia wynik
        // (np. „album o tej nazwie już istnieje" z poprzedniego przypadku).
        fixtures.clearAll();
    }
}
