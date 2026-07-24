package pl.photodrive.core.presentation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.photodrive.core.application.command.contact.ContactCommand;
import pl.photodrive.core.application.service.ContactService;
import pl.photodrive.core.presentation.dto.contact.ContactRequest;

/** Publiczny (bez logowania) formularz kontaktowy. Chroniony rate-limitem (anty-spam) w {@code RateLimitFilter}. */
@RestController
@RequestMapping("/api/public/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    public ResponseEntity<Void> contact(@Valid @RequestBody ContactRequest request) {
        ContactCommand command = new ContactCommand(
                request.name(),
                request.email(),
                request.phone(),
                request.sessionType(),
                request.message());
        contactService.handle(command);
        return ResponseEntity.ok().build();
    }
}
