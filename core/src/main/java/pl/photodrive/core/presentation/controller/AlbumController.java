package pl.photodrive.core.presentation.controller;

import jakarta.servlet.ServletContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.photodrive.core.application.command.album.*;
import pl.photodrive.core.application.command.file.RemoveFileCommand;
import pl.photodrive.core.application.command.file.RenameFileCommand;
import pl.photodrive.core.application.port.file.TemporaryStoragePort;
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
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final ServletContext servletContext;

    private final Path fileStorageLocation = Paths.get("/app/photodrive/").toAbsolutePath().normalize();

    @DeleteMapping("/{albumId}/delete")
    public ResponseEntity<Void> deletePhotographAlbum(@PathVariable UUID albumId) {

        RemoveAlbumCommand command = new RemoveAlbumCommand(albumId);

        albumService.deleteAlbum(command);

        return ResponseEntity.noContent().build();
    }


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

    @PostMapping(path = "upload/{albumId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> addFilesToClientAlbum(@PathVariable UUID albumId, @RequestPart("files") List<MultipartFile> files) {
        validateFiles(files);

        List<FileUpload> fileUploads = new ArrayList<>();

        uploadFiles(files, fileUploads);

        AddFileToAlbumCommand command = new AddFileToAlbumCommand(albumId, fileUploads);
        List<FileId> addedFileIds = albumService.addFilesToAlbum(command);

        return ResponseEntity.accepted().body(new UploadResponse(addedFileIds.stream().map(id -> id.value().toString()).toList(),
                "Files are being processed"));
    }

    @PostMapping("/{albumId}/download")
    public ResponseEntity<byte[]> downloadFilesAsZip(@PathVariable UUID albumId, @Valid @RequestBody DownloadFilesRequest request) {
        DownloadFilesCommand command = new DownloadFilesCommand(request.fileList(), albumId);

        byte[] zipData = albumService.downloadFilesAsZip(command);

        String zipFileName = albumId + ".zip";

        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/zip")).header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + zipFileName + "\"").body(zipData);
    }

    @PutMapping("/{albumIdUUID}/rename/{fileIdUUID}")
    public ResponseEntity<Void> renameFile(@PathVariable UUID albumIdUUID, @PathVariable UUID fileIdUUID, @RequestBody RenameFileRequest request) {
        RenameFileCommand command = new RenameFileCommand(albumIdUUID, fileIdUUID, request.newFileName());

        albumService.renameFile(command);

        return ResponseEntity.noContent().build();

    }

    @PostMapping("/{albumIdUUID}/remove/{fileIdUUID}")
    public ResponseEntity<Void> removeFile(@PathVariable UUID albumIdUUID, @PathVariable UUID fileIdUUID) {
        RemoveFileCommand removeFileCommand = new RemoveFileCommand(fileIdUUID,albumIdUUID);

        albumService.removeFile(removeFileCommand);

        return ResponseEntity.noContent().build();

    }

    @GetMapping("{albumUUID}/photo/{fileName}")
    public ResponseEntity<Resource> getFile(@PathVariable UUID albumUUID, @PathVariable String fileName) {
        GetPhotoPathCommand cmd = new GetPhotoPathCommand(albumUUID);
        try {
            Path targetPath = this.fileStorageLocation.resolve(albumService.getFilePath(cmd)).resolve(fileName).normalize();

            Resource resource = new UrlResource(targetPath.toUri());

            if (resource.exists() && resource.isReadable()) {

                String contentType = servletContext.getMimeType(resource.getFile().getAbsolutePath());

                if (contentType == null) {
                    if (fileName.toLowerCase().endsWith(".png")) {
                        contentType = "image/png";
                    } else if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
                        contentType = "image/jpeg";
                    } else {
                        contentType = "application/octet-stream";
                    }
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }


        } catch (IOException e) {
            throw new AlbumException("Can't read file: " + e.getMessage());
        }
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

    private void uploadFiles(List<MultipartFile> files, List<FileUpload> fileUploads) {
        for (MultipartFile mpf : files) {
            try (InputStream is = mpf.getInputStream()) {

                String tempId = temporaryStorageService.saveTemporary(is);

                fileUploads.add(new FileUpload(new FileName(mpf.getOriginalFilename()),
                        mpf.getSize(),
                        mpf.getContentType(),
                        tempId));
            } catch (IOException e) {
                log.error("Failed to save temporary file", e);
                throw new AlbumException("Failed to process file: " + mpf.getOriginalFilename());
            }

        }

    }

}
