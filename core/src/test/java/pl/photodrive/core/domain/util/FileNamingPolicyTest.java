package pl.photodrive.core.domain.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.photodrive.core.domain.vo.FileName;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FileNamingPolicyTest {

    @Test
    @DisplayName("A free name is used as-is, so an upload without a collision keeps its filename")
    void shouldKeepNameWhenItIsNotTaken() {
        // Given
        Set<String> taken = Set.of("inne.jpg");

        // When
        FileName result = FileNamingPolicy.makeUnique(new FileName("foto.jpg"),
                candidate -> taken.contains(candidate.value()));

        // Then
        assertThat(result.value()).isEqualTo("foto.jpg");
    }

    @Test
    @DisplayName("A taken name gets the _N suffix, the SAME format the front proposes, so the two never disagree")
    void shouldAppendUnderscoreSuffixMatchingTheFrontendFormat() {
        // Given - the plain name is already used in the album
        Set<String> taken = Set.of("foto.jpg");

        // When
        FileName result = FileNamingPolicy.makeUnique(new FileName("foto.jpg"),
                candidate -> taken.contains(candidate.value()));

        // Then - "_1", not " (1)": spaces and parentheses are painful in URLs and ZIP entries (B.31)
        assertThat(result.value()).isEqualTo("foto_1.jpg");
    }

    @Test
    @DisplayName("The suffix counts up until it finds a gap, so two collisions do not resolve to the same name")
    void shouldIncrementSuffixUntilFree() {
        // Given - both the plain name and _1 are taken
        Set<String> taken = Set.of("foto.jpg", "foto_1.jpg");

        // When
        FileName result = FileNamingPolicy.makeUnique(new FileName("foto.jpg"),
                candidate -> taken.contains(candidate.value()));

        // Then
        assertThat(result.value()).isEqualTo("foto_2.jpg");
    }

    @Test
    @DisplayName("A name without an extension still gets a suffix, so extensionless files collide safely")
    void shouldSuffixNameWithoutExtension() {
        // Given
        Set<String> taken = Set.of("raw");

        // When
        FileName result = FileNamingPolicy.makeUnique(new FileName("raw"),
                candidate -> taken.contains(candidate.value()));

        // Then
        assertThat(result.value()).isEqualTo("raw_1");
    }
}
