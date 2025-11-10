package pl.photodrive.core.presentation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.photodrive.core.application.command.album.AddFileCommand;
import pl.photodrive.core.application.command.album.AddFileToClientAlbumCommand;
import pl.photodrive.core.application.command.album.CreateAlbumCommand;
import pl.photodrive.core.application.command.album.CreateAlbumForClientCommand;
import pl.photodrive.core.application.service.AlbumManagementService;
import pl.photodrive.core.domain.model.Album;
import pl.photodrive.core.domain.model.File;
import pl.photodrive.core.presentation.dto.album.AddFileRequest;
import pl.photodrive.core.presentation.dto.album.CreateAlbumRequest;
import pl.photodrive.core.presentation.dto.album.CreateClientAlbumRequest;
import pl.photodrive.core.presentation.dto.album.FileDto;

import java.util.List;
import java.util.UUID;
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

    @PostMapping(path = "/addFile",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileDto> addFile(@RequestPart("file") MultipartFile multipartFile) {
        File file = managementService.addFile(new AddFileCommand(multipartFile));
        return ResponseEntity.ok().body(new FileDto(file.getFileId().value(), file.getFileName().value(), file.getSizeBytes(), file.getContentType()));
    }

    @PostMapping(path = "/{albumName}/addFiles",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<FileDto>> addFilesToAlbum(@PathVariable String albumName,@RequestPart("files") MultipartFile[] multipartFiles) {
        if (multipartFiles == null || multipartFiles.length == 0) {
            return ResponseEntity.badRequest().build();
        }

        List<File> files = managementService.addFilesToClient(new AddFileToClientAlbumCommand(multipartFiles,albumName));

        List<FileDto> results = files.stream()
                .map(file -> new FileDto(
                        file.getFileId().value(),
                        file.getFileName().value(),
                        file.getSizeBytes(),
                        file.getContentType()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

}
