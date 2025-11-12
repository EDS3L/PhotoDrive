package pl.photodrive.core.presentation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pl.photodrive.core.application.command.album.*;
import pl.photodrive.core.application.service.AlbumManagementService;
import pl.photodrive.core.domain.model.Album;
import pl.photodrive.core.domain.model.File;
import pl.photodrive.core.presentation.dto.album.CreateAlbumRequest;
import pl.photodrive.core.presentation.dto.album.CreateClientAlbumRequest;
import pl.photodrive.core.presentation.dto.album.DownloadFilesRequest;
import pl.photodrive.core.presentation.dto.album.FileDto;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/album")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumManagementService managementService;


    @PostMapping("/create/admin")
    public ResponseEntity<Album> createAdminAlbum(@Valid @RequestBody CreateAlbumRequest request) {
        Album album = managementService.createAlbumForAdmin(new CreateAlbumCommand(request.name()));
        return ResponseEntity.ok().body(album);
    }

    @PostMapping("/create/photograph/client")
    public ResponseEntity<Album> createClientAlbum(@Valid @RequestBody CreateClientAlbumRequest request) {
        Album album = managementService.createAlbumForClient(new CreateAlbumForClientCommand(request.name(), request.clientEmail()));
        return ResponseEntity.ok().body(album);
    }

    @PostMapping(path = "/admin/{albumName}/addFiles", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<FileDto>> addFilesToAdminAlbum(@PathVariable String albumName, @RequestPart("files") List<MultipartFile> multipartFiles) {
        List<File> files = managementService.addFilesToAdminAlbum(new AddFileCommand(multipartFiles, albumName));

        List<FileDto> results = files.stream()
                .map(file -> new FileDto(
                        file.getFileId().value(),
                        file.getFileName().value(),
                        file.getSizeBytes(),
                        file.getContentType()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    @PostMapping(path = "/{albumName}/addFiles", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<FileDto>> addFilesToAlbum(@PathVariable String albumName, @RequestPart("files") List<MultipartFile> multipartFiles) {
        if (multipartFiles == null || multipartFiles.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<File> files = managementService.addFilesToClient(new AddFileToClientAlbumCommand(multipartFiles, albumName));

        List<FileDto> results = files.stream()
                .map(file -> new FileDto(
                        file.getFileId().value(),
                        file.getFileName().value(),
                        file.getSizeBytes(),
                        file.getContentType()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    @PostMapping("/{albumName}/download")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable String albumName, @RequestBody @Valid DownloadFilesRequest request) {
        StreamingResponseBody body =
                managementService.downloadFiles(new DownloadFilesCommand(albumName, request.fileList()));

        String zipName = albumName + ".zip";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header("Content-Disposition", "attachment; filename=\"" + zipName + "\"")
                .body(body);
    }

}
