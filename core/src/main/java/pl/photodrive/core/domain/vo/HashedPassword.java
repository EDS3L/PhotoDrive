package pl.photodrive.core.domain.vo;

public record HashedPassword(String value) {
    public HashedPassword {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Hashed password cannot be null or blank");
        }
    }
}
