package pl.photodrive.core.domain.vo;

import pl.photodrive.core.domain.exception.EmailException;

public record Email(String value) {

    public Email {
        validate(value);
    }

    private static void validate(String value) {
        if (value == null || value.isEmpty()) {
            throw new EmailException("Email is empty or null");
            }

            if (!value.matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) {
                throw new EmailException("Invalid email format");
            }
    }
}
