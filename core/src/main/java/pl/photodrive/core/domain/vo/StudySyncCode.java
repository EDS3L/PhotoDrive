package pl.photodrive.core.domain.vo;

import pl.photodrive.core.domain.exception.StudyProgressException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

public record StudySyncCode(String value) {

    private static final Pattern FORMAT = Pattern.compile("[A-Za-z0-9_-]{22}");

    public StudySyncCode {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new StudyProgressException("Invalid sync code");
        }
    }

    public String hashed() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    @Override
    public String toString() {
        return "StudySyncCode[***]";
    }
}
