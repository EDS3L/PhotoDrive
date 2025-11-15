package pl.photodrive.core.presentation.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.photodrive.core.application.command.album.*;
import pl.photodrive.core.application.port.TemporaryStoragePort;
import pl.photodrive.core.application.service.AlbumManagementService;
import pl.photodrive.core.domain.exception.AlbumException;
import pl.photodrive.core.domain.model.Album;
import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.domain.vo.FileId;
import pl.photodrive.core.domain.vo.FileName;
import pl.photodrive.core.presentation.dto.album.*;
import pl.photodrive.core.presentation.dto.file.UploadResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/album")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumManagementService albumService;
    private final TemporaryStoragePort temporaryStorageService;



    @PostMapping("/admin")
    public ResponseEntity<AlbumResponse> createAdminAlbum(@Valid @RequestBody CreateAlbumRequest request) {

        Album album = albumService.createAdminAlbum(new CreateAlbumCommand(request.name(), null));

        return ResponseEntity.ok(AlbumResponse.fromDomain(album));
    }

    @PostMapping("/client/{clientId}/create")
    public ResponseEntity<AlbumResponse> createClientAlbum(@Valid @RequestBody CreateClientAlbumRequest request, @NotNull @PathVariable UUID clientId) {

        CreateAlbumCommand command = new CreateAlbumCommand(request.name(), clientId);

        Album album = albumService.createAlbumForClient(command);

        return ResponseEntity.ok(AlbumResponse.fromDomain(album));
    }

    @PostMapping(path = "/{albumId}/files/admin", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> addFilesToAdminAlbum(@PathVariable UUID albumId, @RequestPart("files") List<MultipartFile> files) {
        log.info("[AlbumController] addFilesToClientAlbum: albumId={}, incoming files={}",
                albumId,
                files.stream().map(MultipartFile::getOriginalFilename).toList());

        return getListResponseEntity(albumId, files);
    }

    @PostMapping(path = "/{albumId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> addFilesToClientAlbum(@PathVariable UUID albumId, @RequestPart("files") List<MultipartFile> files) {

        return getListResponseEntity(albumId, files);
    }

    private ResponseEntity<UploadResponse> getListResponseEntity(@PathVariable UUID albumId, @RequestPart("files") List<MultipartFile> files) {
        validateFiles(files);

        log.info("[AlbumController] Preparing upload to album {}. Files with details={}",
                albumId,
                files.stream()
                        .map(f -> String.format("%s(size=%d, contentType=%s)",
                                f.getOriginalFilename(), f.getSize(), f.getContentType()))
                        .toList());

        List<FileUpload> fileUploads = new ArrayList<>();

        for (MultipartFile mpf : files) {
            try (InputStream is = mpf.getInputStream()) {

                String tempId = temporaryStorageService.saveTemporary(is);

                fileUploads.add(new FileUpload(
                        new FileName(mpf.getOriginalFilename()),
                        mpf.getSize(),
                        mpf.getContentType(),
                        tempId
                ));
            } catch (IOException e) {
                log.error("Failed to save temporary file", e);
                throw new AlbumException("Failed to process file: " + mpf.getOriginalFilename());
            }

        }

        AddFileToAlbumCommand command = new AddFileToAlbumCommand(
                new AlbumId(albumId),
                fileUploads
        );

        List<FileId> addedFileIds = albumService.addFilesToAlbum(command);

        return ResponseEntity.accepted()
                .body(new UploadResponse(
                        addedFileIds.stream()
                                .map(id -> id.value().toString())
                                .toList(),
                        "Files are being processed"
                ));
    }


    @PostMapping("/{albumId}/download")
    public ResponseEntity<byte[]> downloadFilesAsZip(@PathVariable UUID albumId, @Valid @RequestBody DownloadFilesRequest request) {


        DownloadFilesCommand command = new DownloadFilesCommand(request.albumName(),
                request.fileList(),
                new AlbumId(albumId));

        byte[] zipData = albumService.downloadFilesAsZip(command);

        String zipFileName = request.albumName() + ".zip";

        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/zip")).header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + zipFileName + "\"").body(zipData);
    }

    private void validateFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files provided");
        }

        long totalSize = files.stream().mapToLong(MultipartFile::getSize).sum();

        long maxTotalSize = 100L * 1024 * 1024;
        if (totalSize > maxTotalSize) {
            throw new IllegalArgumentException("Total file size exceeds limit: " + maxTotalSize + " bytes");
        }
    }

}
