package pl.photodrive.core.domain.vo;

import org.junit.jupiter.api.Test;
import pl.photodrive.core.domain.exception.UserException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class FileIdTest {

    @Test
    void shouldCreateWithValidUUID() {
        UUID uuid = UUID.randomUUID();
        FileId fileId = new FileId(uuid);
        assertThat(fileId.value()).isEqualTo(uuid);
    }

    @Test
    void shouldThrowWhenNull() {
        assertThatThrownBy(() -> new FileId(null))
                .isInstanceOf(UserException.class);
    }

    @Test
    void shouldGenerateNewUniqueRawUUID() {
        UUID id1 = FileId.newId();
        UUID id2 = FileId.newId();
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void shouldBeEqualWhenSameUUID() {
        UUID uuid = UUID.randomUUID();
        assertThat(new FileId(uuid)).isEqualTo(new FileId(uuid));
    }

    @Test
    void shouldHaveSameHashCodeForEqualObjects() {
        UUID uuid = UUID.randomUUID();
        assertThat(new FileId(uuid).hashCode()).isEqualTo(new FileId(uuid).hashCode());
    }
}
