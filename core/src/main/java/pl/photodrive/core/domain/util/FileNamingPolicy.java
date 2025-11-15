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

        int counter = 1;
        while (true) {
            String candidateValue = base + " (" + counter + ")" + ext;
            FileName candidate = new FileName(candidateValue);

            if (!exists.test(candidate)) {
                return candidate;
            }

            counter++;
        }
    }
}
