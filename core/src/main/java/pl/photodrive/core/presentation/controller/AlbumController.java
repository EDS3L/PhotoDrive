package pl.photodrive.core.presentation.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pl.photodrive.core.application.command.album.*;
import pl.photodrive.core.application.port.CurrentUser;
import pl.photodrive.core.application.service.AlbumManagementService;
import pl.photodrive.core.domain.model.Album;
import pl.photodrive.core.domain.model.File;
import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.presentation.dto.album.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/album")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumManagementService albumService;


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
    public ResponseEntity<List<FileResponse>> addFilesToAdminAlbum(@PathVariable UUID albumId, @RequestPart("files") List<MultipartFile> files) {

        return getListResponseEntity(albumId, files);
    }

    @PostMapping(path = "/{albumId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<FileResponse>> addFilesToClientAlbum(@PathVariable UUID albumId, @RequestPart("files") List<MultipartFile> files) {

        return getListResponseEntity(albumId, files);
    }

    private ResponseEntity<List<FileResponse>> getListResponseEntity(@PathVariable UUID albumId, @RequestPart("files") List<MultipartFile> files) {
        validateFiles(files);

        List<File> addedFiles = albumService.addFilesToAlbum(new AlbumId(albumId), files);

        List<FileResponse> response = addedFiles.stream().map(file -> new FileResponse(file.getFileId().value().toString(),
                file.getFileName().value(),
                file.getSizeBytes(),
                file.getContentType())).toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{albumId}/download")
    public ResponseEntity<byte[]> downloadFilesAsZip(@PathVariable UUID albumId, @Valid @RequestBody DownloadFilesRequest request, @AuthenticationPrincipal CurrentUser currentUserDetails) {


        DownloadFilesCommand command = new DownloadFilesCommand(request.albumName(),
                request.fileList(),
                new AlbumId(albumId));

        byte[] zipData = albumService.downloadFilesAsZip(command, currentUserDetails);

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
