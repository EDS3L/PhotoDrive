package pl.photodrive.core.domain.vo;

import java.util.regex.Pattern;

public record AlbumPath(String value) {

    private static final Pattern VALID_CHARS_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-_/ .@+]+$");

    private static final Pattern TRAVERSAL_PATTERN = Pattern.compile("(\\.\\./|/\\.\\.)");


    public AlbumPath {
        validate(value);
    }


    private void validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Album path cannot be null or empty");
        }

        if (value.startsWith("/")) {
            throw new IllegalArgumentException("Album path cannot start with '/'");
        }

        if (TRAVERSAL_PATTERN.matcher(value).find()) {
            throw new IllegalArgumentException("Album path cannot contain traversal characters ('../', '/..').");
        }

        if (!VALID_CHARS_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Album path can only contain letters, digits, spaces, '/', '-', '_', '.', '@', and '+'.");
        }

        if (value.endsWith("/")) {
            throw new IllegalArgumentException("Album path cannot end with '/'.");
        }
    }
}
