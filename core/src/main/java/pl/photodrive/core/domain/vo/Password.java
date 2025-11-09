package pl.photodrive.core.domain.vo;

public record Password(String value) {

    public Password {
        validate(value);
    }

    private static void validate(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if (value.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        if (!value.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter.");
        }
        if (!value.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter.");
        }
        if (!value.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("Password must contain at least one digit.");
        }
        if (!value.matches(".*[\\W_].*")) {
            throw new IllegalArgumentException("Password must contain at least one special character.");
        }
    }


    public static String toString(Password password) {
        return password.toString();
    }
}