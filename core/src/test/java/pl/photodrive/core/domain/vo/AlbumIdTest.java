package pl.photodrive.core.domain.vo;

import org.junit.jupiter.api.Test;
import pl.photodrive.core.domain.exception.UserException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class AlbumIdTest {

    @Test
    void shouldCreateWithValidUUID() {
        UUID uuid = UUID.randomUUID();
        AlbumId albumId = new AlbumId(uuid);
        assertThat(albumId.value()).isEqualTo(uuid);
    }

    @Test
    void shouldThrowWhenNull() {
        assertThatThrownBy(() -> new AlbumId(null))
                .isInstanceOf(UserException.class);
    }

    @Test
    void shouldGenerateNewUniqueId() {
        AlbumId id1 = AlbumId.newId();
        AlbumId id2 = AlbumId.newId();
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void shouldBeEqualWhenSameUUID() {
        UUID uuid = UUID.randomUUID();
        assertThat(new AlbumId(uuid)).isEqualTo(new AlbumId(uuid));
    }

    @Test
    void shouldHaveSameHashCodeForEqualObjects() {
        UUID uuid = UUID.randomUUID();
        assertThat(new AlbumId(uuid).hashCode()).isEqualTo(new AlbumId(uuid).hashCode());
    }
}
