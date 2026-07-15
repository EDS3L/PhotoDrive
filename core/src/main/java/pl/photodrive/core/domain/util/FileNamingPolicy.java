package pl.photodrive.core.domain.util;

import lombok.extern.slf4j.Slf4j;
import pl.photodrive.core.domain.vo.FileName;

import java.util.function.Predicate;

@Slf4j
public final class FileNamingPolicy {

    private FileNamingPolicy() {
    }

    public static FileName makeUnique(FileName original, Predicate<FileName> exists) {
        if (!exists.test(original)) {
            return original;
        }

        String value = original.value();
        int dotIndex = value.lastIndexOf('.');

        String base;
        String ext;

        if (dotIndex == -1) {
            base = value;
            ext = "";
        } else {
            base = value.substring(0, dotIndex);
            ext = value.substring(dotIndex);
        }

        // Format sufiksu MUSI być zgodny z frontem (`suggestNonCollidingName`: `foto_1.jpg`) —
        // inaczej modal kolizji proponuje jedną nazwę, a backend przy pominięciu modala nada inną
        // (B.31). `_N` zamiast ` (N)`, bo spacje i nawiasy są kłopotliwe w URL-ach i wpisach ZIP.
        int counter = 1;
        while (true) {
            String candidateValue = base + "_" + counter + ext;
            FileName candidate = new FileName(candidateValue);

            if (!exists.test(candidate)) {
                return candidate;
            }

            counter++;
        }
    }
}
