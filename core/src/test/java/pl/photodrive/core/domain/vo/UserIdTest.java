package pl.photodrive.core.domain.vo;

import org.junit.jupiter.api.Test;
import pl.photodrive.core.domain.exception.UserException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class UserIdTest {

    @Test
    void shouldCreateWithValidUUID() {
        UUID uuid = UUID.randomUUID();
        UserId userId = new UserId(uuid);
        assertThat(userId.value()).isEqualTo(uuid);
    }

    @Test
    void shouldThrowWhenNull() {
        assertThatThrownBy(() -> new UserId(null))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    void shouldGenerateNewUniqueId() {
        UserId id1 = UserId.newId();
        UserId id2 = UserId.newId();
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void shouldBeEqualWhenSameUUID() {
        UUID uuid = UUID.randomUUID();
        assertThat(new UserId(uuid)).isEqualTo(new UserId(uuid));
    }

    @Test
    void shouldHaveSameHashCodeForEqualObjects() {
        UUID uuid = UUID.randomUUID();
        assertThat(new UserId(uuid).hashCode()).isEqualTo(new UserId(uuid).hashCode());
    }
}
