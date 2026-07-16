package pl.photodrive.core.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.photodrive.core.application.port.site.SiteSlot;
import pl.photodrive.core.application.port.site.SiteSlotVersion;
import pl.photodrive.core.application.service.SiteSlotManagementService;
import pl.photodrive.core.domain.exception.FileException;
import pl.photodrive.core.presentation.dto.site.SiteSlotDto;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Zarządzanie zdjęciami slotów strony wizytówki — tylko ADMIN (pilnuje {@code WebConfig}). */
@RestController
@RequestMapping("api/site/slots")
@RequiredArgsConstructor
public class SiteSlotController {

    private final SiteSlotManagementService slotService;

    /** Wszystkie sloty z enuma, także puste — panel ma pokazywać, co w ogóle da się skonfigurować. */
    @GetMapping
    public ResponseEntity<List<SiteSlotDto>> getSlots() {
        Map<SiteSlot, Instant> configured = slotService.getConfiguredSlots().stream()
                .collect(Collectors.toMap(SiteSlotVersion::slot, SiteSlotVersion::updatedAt));

        List<SiteSlotDto> slots = Arrays.stream(SiteSlot.values())
                .map(slot -> new SiteSlotDto(slot.name(), configured.containsKey(slot), configured.get(slot)))
                .toList();
        return ResponseEntity.ok(slots);
    }

    @PutMapping(value = "/{slot}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> upload(@PathVariable("slot") String slot,
                                       @RequestPart("file") MultipartFile file) {
        try {
            slotService.upload(parseSlot(slot), file.getBytes());
        } catch (IOException e) {
            throw new FileException("Cannot read uploaded slot image");
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{slot}")
    public ResponseEntity<Void> clear(@PathVariable("slot") String slot) {
        slotService.clear(parseSlot(slot));
        return ResponseEntity.noContent().build();
    }

    /**
     * Ręczne parsowanie zamiast {@code @PathVariable SiteSlot}: błąd konwersji enuma leci do
     * generycznego handlera jako 500, a nieznany slot to wina żądania (400), nie serwera.
     */
    static SiteSlot parseSlot(String raw) {
        try {
            return SiteSlot.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown site slot: " + raw);
        }
    }
}
