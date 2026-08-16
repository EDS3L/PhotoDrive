package pl.photodrive.core.presentation.dto.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactRequest(
        @NotBlank(message = "Imię jest wymagane") @Size(max = 120) String name,
        @NotBlank(message = "Adres e-mail jest wymagany") @Email(message = "Nieprawidłowy adres e-mail") @Size(max = 254) String email,
        @Size(max = 40) String phone,
        @NotBlank(message = "Rodzaj sesji jest wymagany") @Size(max = 80) String sessionType,
        @NotBlank(message = "Wiadomość jest wymagana") @Size(min = 10, max = 5000, message = "Wiadomość musi mieć od 10 do 5000 znaków") String message
) {
}
