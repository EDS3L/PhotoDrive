package pl.photodrive.core.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.photodrive.core.application.service.WatermarkManagementService;
import pl.photodrive.core.domain.exception.FileException;
import pl.photodrive.core.presentation.dto.watermark.WatermarkStatusResponse;

import java.io.IOException;

@RestController
@RequestMapping("api/watermark")
@RequiredArgsConstructor
public class WatermarkController {

    private final WatermarkManagementService watermarkService;

    @GetMapping("status")
    public ResponseEntity<WatermarkStatusResponse> getStatus() {
        return ResponseEntity.ok(watermarkService.getWatermark()
                .map(watermark -> new WatermarkStatusResponse(true, watermark.updatedAt()))
                .orElseGet(() -> new WatermarkStatusResponse(false, null)));
    }

    @GetMapping
    public ResponseEntity<byte[]> getImage() {
        return watermarkService.getWatermark()
                .map(watermark -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .body(watermark.image()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> upload(@RequestPart("file") MultipartFile file) {
        try {
            watermarkService.uploadWatermark(file.getBytes());
        } catch (IOException e) {
            throw new FileException("Cannot read uploaded watermark file");
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> delete() {
        watermarkService.deleteWatermark();
        return ResponseEntity.noContent().build();
    }
}
