package pl.photodrive.core.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.photodrive.core.application.service.SiteSlotManagementService;
import pl.photodrive.core.presentation.dto.site.PublicSiteSlotDto;

import java.util.List;

/** Odczyt slotów strony wizytówki — bez logowania (strona publiczna). */
@RestController
@RequestMapping("/api/public/site")
@RequiredArgsConstructor
public class PublicSiteController {

    private final SiteSlotManagementService slotService;

    /** Tylko skonfigurowane sloty; sekcja bez wpisu pokazuje swój placeholder. */
    @GetMapping("/slots")
    public ResponseEntity<List<PublicSiteSlotDto>> getSlots() {
        List<PublicSiteSlotDto> slots = slotService.getConfiguredSlots().stream()
                .map(v -> new PublicSiteSlotDto(v.slot().name(), v.updatedAt().toEpochMilli()))
                .toList();
        // Krótki cache jak listingi albumów: podmiana zdjęcia widoczna na stronie w ~pół minuty.
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=30, must-revalidate")
                .body(slots);
    }

    /**
     * Sam obraz może być cache'owany agresywnie — URL niesie wersję ({@code ?v=updatedAt}),
     * więc podmiana zdjęcia zmienia URL i unieważnia cache bez czekania na wygaśnięcie.
     * (Wzorzec z F.12; parametru {@code v} serwer nie czyta — to wyłącznie cache-buster.)
     */
    @GetMapping("/photo/{slot}")
    public ResponseEntity<byte[]> getPhoto(@PathVariable("slot") String slot) {
        return slotService.getImage(SiteSlotController.parseSlot(slot))
                .map(image -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                        .body(image.image()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
