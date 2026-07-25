package pl.photodrive.core.infrastructure.jpa.adapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.photodrive.core.application.port.repository.AlbumRepository;
import pl.photodrive.core.application.port.repository.PasswordTokenRepository;
import pl.photodrive.core.application.port.repository.PublicAlbumSummary;
import pl.photodrive.core.application.port.repository.UserRepository;
import pl.photodrive.core.domain.model.Album;
import pl.photodrive.core.domain.model.File;
import pl.photodrive.core.domain.model.PasswordToken;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.domain.vo.AlbumPath;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.FileName;
import pl.photodrive.core.domain.vo.UserId;
import pl.photodrive.core.support.IntegrationTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy zapytań repozytoriów na <b>prawdziwym MySQL-u</b>. Test jednostkowy z mockiem
 * repozytorium nigdy nie wykryje, że zapytanie zwraca nie to, co trzeba — a akurat te
 * zapytania są nieodwracalne w skutkach (po jednym z nich scheduler <b>kasuje albumy</b>,
 * a po innym publiczne portfolio pokazuje zdjęcia gościom bez logowania).
 */
class RepositoryAdapterIT extends IntegrationTest {

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordTokenRepository passwordTokenRepository;

    @Test
    @DisplayName("Scheduler's query picks up only albums past their deletion date, never those without one")
    void shouldReturnOnlyExpiredAlbumsWhenSchedulerLooksForAlbumsToDelete() {
        // Given - three albums: expired, still valid, and one with no TTD at all
        User photographer = fixtures.photographer("foto@photodrive.dev");
        User client = fixtures.client("klient@photodrive.dev");
        Instant now = Instant.now();

        Album expired = saveAlbumWithTtd(photographer, client, "Przeterminowany", now.minus(1, ChronoUnit.DAYS));
        saveAlbumWithTtd(photographer, client, "Wazny", now.plus(30, ChronoUnit.DAYS));
        saveAlbumWithTtd(photographer, client, "Bezterminowy", null);

        // When
        List<Album> toDelete = inTransaction(() -> albumRepository.findByTtdBeforeAndTtdIsNotNull(now));

        // Then - a bug here either deletes a client's photos early or keeps them forever
        assertThat(toDelete)
                .extracting(album -> album.getAlbumId().value())
                .containsExactly(expired.getAlbumId().value());
    }

    @Test
    @DisplayName("Public portfolio queries return published albums only")
    void shouldReturnOnlyPublicAlbumsToTheGuestPortfolio() {
        // Given - one album published, one kept private
        User admin = fixtures.admin("admin@photodrive.dev");
        Album published = inTransaction(() -> {
            Album album = Album.createForAdmin("portfolio-sluby", admin);
            album.makePublic(admin);
            return albumRepository.save(album);
        });
        inTransaction(() -> albumRepository.save(Album.createForAdmin("prywatny-material", admin)));

        // When
        List<PublicAlbumSummary> publicAlbums = inTransaction(albumRepository::findAllPublicSummaries);

        // Then - the private album must never reach an anonymous visitor
        assertThat(publicAlbums)
                .extracting(PublicAlbumSummary::name)
                .containsExactly("portfolio-sluby");
        assertThat(inTransaction(() -> albumRepository.findPublicByName("portfolio-sluby"))).isPresent();
        assertThat(inTransaction(() -> albumRepository.findPublicByName("prywatny-material"))).isEmpty();
        assertThat(inTransaction(() -> albumRepository.findPublicByAlbumId(published.getAlbumId()))).isPresent();
    }

    @Test
    @DisplayName("The database-computed photo count covers only visible files, so a hidden photo never inflates a portfolio tab")
    void shouldCountOnlyVisibleFilesInPublicSummaries() {
        // Given - a published album with one visible and one hidden photo; the count is now
        // computed by the COUNT subquery (B.35), so ITS filter is what this test pins down
        User admin = fixtures.admin("admin@photodrive.dev");
        inTransaction(() -> {
            Album album = Album.createForAdmin("licznik-widocznych", admin);
            File visible = File.create(new FileName("widoczne.jpg"), 10L, "image/jpeg");
            File hidden = File.create(new FileName("ukryte.jpg"), 10L, "image/jpeg");
            // Admin-album uploads are visible by default (B.5) - hide one explicitly
            album.addFile(visible);
            album.addFile(hidden);
            album.changeFileVisibleStatus(List.of(hidden.getFileId()), false, admin, admin.getEmail());
            album.makePublic(admin);
            album.pullDomainEvents();
            return albumRepository.save(album);
        });

        // When
        List<PublicAlbumSummary> summaries = inTransaction(albumRepository::findAllPublicSummaries);

        // Then
        PublicAlbumSummary summary = summaries.stream()
                .filter(s -> "licznik-widocznych".equals(s.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Album licznik-widocznych is not in the listing"));
        assertThat(summary.visibleCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Album is reloaded with its photos, so the hand-written mapping loses nothing")
    void shouldRoundTripAlbumWithItsPhotos() {
        // Given - an album whose photo was made visible and watermarked
        User photographer = fixtures.photographer("foto@photodrive.dev");
        User client = fixtures.client("klient@photodrive.dev");
        Album album = inTransaction(() -> {
            Album created = Album.createForClient("Sesja", photographer, client);
            created.addFile(File.create(new FileName("zdjecie.jpg"), 1_024L, "image/jpeg"));
            return albumRepository.save(created);
        });

        // When - the aggregate comes back from MySQL
        Album reloaded = inTransaction(() -> albumRepository.findByAlbumId(album.getAlbumId())).orElseThrow();

        // Then - encja↔domena mapping is written by hand, so every dropped field is a silent bug
        assertThat(reloaded.getName()).isEqualTo(album.getName());
        assertThat(reloaded.getPhotographId()).isEqualTo(photographer.getId().value());
        assertThat(reloaded.getClientId()).isEqualTo(client.getId().value());
        assertThat(reloaded.getAlbumPath().value()).isEqualTo(album.getAlbumPath().value());
        assertThat(reloaded.getPhotos().values())
                .singleElement()
                .satisfies(file -> {
                    assertThat(file.getFileName().value()).isEqualTo("zdjecie.jpg");
                    assertThat(file.getSizeBytes()).isEqualTo(1_024L);
                    assertThat(file.getContentType()).isEqualTo("image/jpeg");
                    // photos arrive hidden — the default the whole client zone relies on
                    assertThat(file.isVisible()).isFalse();
                });
    }

    @Test
    @DisplayName("Photographer's client list survives the round trip to the database")
    void shouldRoundTripAssignedClients() {
        // Given - two clients assigned to a photographer
        User photographer = fixtures.photographer("foto@photodrive.dev");
        User first = fixtures.client("pierwszy@photodrive.dev");
        User second = fixtures.client("drugi@photodrive.dev");
        photographer.assignUsersForSelf(List.of(first.getId(), second.getId()));

        // When
        userRepository.save(photographer);
        User reloaded = inTransaction(() -> userRepository.findById(photographer.getId())).orElseThrow();

        // Then - the assignment lives in a separate table; losing it would cut the photographer
        // off from his own clients
        assertThat(reloaded.getAssignedUsers())
                .extracting(UserId::value)
                .containsExactlyInAnyOrder(first.getId().value(), second.getId().value());
    }

    @Test
    @DisplayName("User is found by e-mail, which is how every login starts")
    void shouldFindUserByEmail() {
        // Given
        User photographer = fixtures.photographer("foto@photodrive.dev");

        // When / Then
        assertThat(inTransaction(() -> userRepository.findByEmail(new Email("foto@photodrive.dev"))))
                .isPresent()
                .get()
                .satisfies(found -> assertThat(found.getId().value()).isEqualTo(photographer.getId().value()));
        assertThat(inTransaction(() -> userRepository.existsByEmail(new Email("foto@photodrive.dev")))).isTrue();
        assertThat(inTransaction(() -> userRepository.existsByEmail(new Email("nikt@photodrive.dev")))).isFalse();
    }

    @Test
    @DisplayName("Album name uniqueness is checked against the database, not just in memory")
    void shouldReportWhetherAlbumNameIsTaken() {
        // Given
        User admin = fixtures.admin("admin@photodrive.dev");
        inTransaction(() -> albumRepository.save(Album.createForAdmin("portfolio-sluby", admin)));

        // When / Then - this check guards the photographer from silently shadowing an album
        assertThat(inTransaction(() -> albumRepository.existsByName("portfolio-sluby"))).isTrue();
        assertThat(inTransaction(() -> albumRepository.existsByName("portfolio-chrzciny"))).isFalse();
    }

    // =======================================================================
    // PasswordTokenRepository — ścieżka resetu hasła na żywej bazie
    // =======================================================================

    @Test
    @DisplayName("A saved reset token is found back by its user and still matches the code that was mailed, which is the whole reset flow in one query")
    void shouldFindSavedResetTokenByUserAndKeepItUsable() {
        // Given - a token stored exactly as the reset flow stores it
        User client = fixtures.client("reset@photodrive.dev");
        UUID rawCode = UUID.randomUUID();
        Instant created = Instant.now();
        inTransaction(() -> passwordTokenRepository.save(
                PasswordToken.create(rawCode, created.plus(15, ChronoUnit.MINUTES), created, client)));

        // When - the lookup the reset endpoint performs
        Optional<PasswordToken> found = inTransaction(() -> passwordTokenRepository.findByUserId(client.getId()));

        // Then - a mock-based test cannot tell whether the user_id query actually matches;
        // here the token comes back out of MySQL and still recognises the mailed code
        assertThat(found).isPresent();
        assertThat(found.get().matches(rawCode)).isTrue();
        assertThat(found.get().getUserId()).isEqualTo(client.getId());
    }

    @Test
    @DisplayName("A user with no reset token reports absence instead of somebody else's token")
    void shouldReportNoResetTokenForUserWithoutOne() {
        // Given - two clients, only one of whom asked for a reset
        User withToken = fixtures.client("z-tokenem@photodrive.dev");
        User withoutToken = fixtures.client("bez-tokenu@photodrive.dev");
        Instant created = Instant.now();
        inTransaction(() -> passwordTokenRepository.save(
                PasswordToken.create(UUID.randomUUID(), created.plus(15, ChronoUnit.MINUTES), created, withToken)));

        // When / Then - a query missing its WHERE would hand the second client the first one's token
        assertThat(inTransaction(() -> passwordTokenRepository.findByUserId(withoutToken.getId()))).isEmpty();
        assertThat(inTransaction(() -> passwordTokenRepository.existsByUserId(withoutToken.getId()))).isFalse();
        assertThat(inTransaction(() -> passwordTokenRepository.existsByUserId(withToken.getId()))).isTrue();
    }

    @Test
    @DisplayName("Deleting a used token really removes the row, so the same authorization code cannot be replayed")
    void shouldDeleteUsedResetToken() {
        // Given
        User client = fixtures.client("zuzyty@photodrive.dev");
        Instant created = Instant.now();
        PasswordToken saved = inTransaction(() -> passwordTokenRepository.save(
                PasswordToken.create(UUID.randomUUID(), created.plus(15, ChronoUnit.MINUTES), created, client)));

        // When - this is what runs after a successful password change
        inTransaction(() -> {
            passwordTokenRepository.delete(saved);
            return null;
        });

        // Then - a delete keyed on the wrong column would leave the code usable forever
        assertThat(inTransaction(() -> passwordTokenRepository.findByUserId(client.getId()))).isEmpty();
    }

    /**
     * Album z dowolnym TTD — także z PRZESZŁYM. Domena słusznie zabrania ustawić datę
     * usunięcia wstecz ({@code setTTD} rzuca „Cannot set TTD before now!"), a album staje
     * się przeterminowany dopiero z upływem czasu. Odtwarzamy go więc tak, jak robi to
     * baza przy odczycie: przez konstruktor.
     */
    private Album saveAlbumWithTtd(User photographer, User client, String name, Instant ttd) {
        return inTransaction(() -> {
            Album album = new Album(
                    AlbumId.newId(),
                    name,
                    photographer.getId().value(),
                    client.getId().value(),
                    ttd,
                    new AlbumPath(photographer.getEmail().value() + "/" + name),
                    false);
            return albumRepository.save(album);
        });
    }
}
