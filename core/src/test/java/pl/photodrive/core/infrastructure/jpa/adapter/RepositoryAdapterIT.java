package pl.photodrive.core.infrastructure.jpa.adapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.photodrive.core.application.port.repository.AlbumRepository;
import pl.photodrive.core.application.port.repository.UserRepository;
import pl.photodrive.core.domain.model.Album;
import pl.photodrive.core.domain.model.File;
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
        List<Album> publicAlbums = inTransaction(albumRepository::findAllPublic);

        // Then - the private album must never reach an anonymous visitor
        assertThat(publicAlbums)
                .extracting(Album::getName)
                .containsExactly("portfolio-sluby");
        assertThat(inTransaction(() -> albumRepository.findPublicByName("portfolio-sluby"))).isPresent();
        assertThat(inTransaction(() -> albumRepository.findPublicByName("prywatny-material"))).isEmpty();
        assertThat(inTransaction(() -> albumRepository.findPublicByAlbumId(published.getAlbumId()))).isPresent();
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
