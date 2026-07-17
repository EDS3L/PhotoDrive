package pl.photodrive.core.domain.vo;

import pl.photodrive.core.domain.exception.FileException;

import java.util.Set;

public record FileName(String value) {

    public static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png");

    private static final Set<String> FORBIDDEN_EXTENSIONS = Set.of(".exe",
            ".bat",
            ".sh",
            ".cmd",
            ".com",
            ".msi",
            ".scr",
            ".jar",
            ".ps1",
            ".vb",
            ".vbs");

    private static final Set<String> RESERVED_WINDOWS_NAMES = Set.of("CON",
            "PRN",
            "AUX",
            "NUL",
            "COM1",
            "COM2",
            "COM3",
            "COM4",
            "COM5",
            "COM6",
            "COM7",
            "COM8",
            "COM9",
            "LPT1",
            "LPT2",
            "LPT3",
            "LPT4",
            "LPT5",
            "LPT6",
            "LPT7",
            "LPT8",
            "LPT9");

    // Konstruktor (używany też przy odtwarzaniu z DB/FS): walidacja strukturalna + zakaz
    // plików wykonywalnych. NIE egzekwuje białej listy formatów.
    public FileName {
        validate(value);
    }

    // Utworzenie NOWEGO pliku (upload) — dodatkowo egzekwuje białą listę formatów
    // (tylko zdjęcia jpg/jpeg/png). Odtwarzanie z magazynu jej nie przechodzi.
    public static FileName of(String value) {
        FileName fileName = new FileName(value);
        validateAllowedFormat(value);
        return fileName;
    }

    private static void validate(String value) {
        validateNotEmpty(value);
        validateIllegalCharacters(value);
        validateReservedNames(value);
        validateNotForbidden(value);
    }

    private static void validateNotEmpty(String value) {
        if (value == null || value.isEmpty()) {
            throw new FileException("File name is empty or null");
        }

        if (value.length() > 255) {
            throw new FileException("File name is too long (max 255 characters)");
        }

        if (value.endsWith(" ") || value.endsWith(".")) {
            throw new FileException("File name cannot end with space or dot");
        }
    }

    private static void validateIllegalCharacters(String value) {
        if (value.matches(".*[\\\\/:*?\"<>|#%&{}$!'@+`~=,;\\[\\]].*")) {
            throw new FileException("File name contains invalid characters");
        }

        if (value.indexOf('\0') >= 0) {
            throw new FileException("File name contains invalid character: null");
        }
    }

    private static void validateReservedNames(String value) {
        String nameWithoutExtension = value.contains(".") ? value.substring(0, value.lastIndexOf('.')) : value;
        if (RESERVED_WINDOWS_NAMES.contains(nameWithoutExtension.toUpperCase())) {
            throw new FileException("Reserved file name");
        }
    }

    private static void validateNotForbidden(String value) {
        String lower = value.toLowerCase();
        if (FORBIDDEN_EXTENSIONS.stream().anyMatch(lower::endsWith)) {
            throw new FileException("Executable files are not allowed");
        }
    }

    private static void validateAllowedFormat(String value) {
        String lower = value.toLowerCase();
        if (ALLOWED_EXTENSIONS.stream().noneMatch(lower::endsWith)) {
            throw new FileException("Invalid or unsupported file format");
        }
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileName(String value1))) return false;
        return java.util.Objects.equals(value, value1);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(value);
    }
}
