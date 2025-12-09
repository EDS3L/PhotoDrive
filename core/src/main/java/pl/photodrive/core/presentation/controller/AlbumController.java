package pl.photodrive.core.presentation.controller;

import jakarta.servlet.ServletContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.photodrive.core.application.command.album.*;
import pl.photodrive.core.application.command.file.ChangeVisibleCommand;
import pl.photodrive.core.application.command.file.FileResource;
import pl.photodrive.core.application.command.file.RemoveFileCommand;
import pl.photodrive.core.application.command.file.RenameFileCommand;
import pl.photodrive.core.application.port.file.TemporaryStoragePort;
import pl.photodrive.core.application.service.AlbumManagementService;
import pl.photodrive.core.domain.exception.AlbumException;
import pl.photodrive.core.domain.model.Album;
import pl.photodrive.core.domain.vo.FileId;
import pl.photodrive.core.domain.vo.FileName;
import pl.photodrive.core.presentation.dto.album.*;
import pl.photodrive.core.presentation.dto.file.RemoveFilesRequest;
import pl.photodrive.core.presentation.dto.file.UploadResponse;
import pl.photodrive.core.presentation.dto.file.UploadResponseFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@RestController
@RequestMapping("/api/album")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumManagementService albumService;
    private final TemporaryStoragePort temporaryStorageService;
    private final ServletContext servletContext;

    private final Path fileStorageLocation = Paths.get("/app/photodrive/").toAbsolutePath().normalize();


    @GetMapping("/all")
    public ResponseEntity<List<AlbumDto>> getAllAlbums() {
        return ResponseEntity.ok().body(albumService.getAllAlbums().stream().map(album -> new AlbumDto(album.getAlbumId().value(),
                album.getName(),
                album.getPhotographId(),
                album.getClientId(),
                album.getTtd(),
                album.getPhotos(),
                album.getAlbumPath())).toList());
    }

    @GetMapping("/getAllAssignedAlbums")
    public ResponseEntity<List<AlbumDto>> getAllAssignedAlbums() {
        return ResponseEntity.ok().body(albumService.getAssignedAlbums().stream().map(album -> new AlbumDto(album.getAlbumId().value(),
                album.getName(),
                album.getPhotographId(),
                album.getClientId(),
                album.getTtd(),
                album.getPhotos(),
                album.getAlbumPath())).toList());
    }

    @GetMapping("/all/withoutTdd")
    public ResponseEntity<List<AlbumDto>> getAllAlbumsWithoutTTD() {
        return ResponseEntity.ok().body(albumService.getAllAlbumsWithoutTTD().stream().map(album -> new AlbumDto(album.getAlbumId().value(),
                album.getName(),
                album.getPhotographId(),
                album.getClientId(),
                album.getTtd(),
                album.getPhotos(),
                album.getAlbumPath())).toList());
    }

    @GetMapping("/allAssignedAlbum/withoutTdd")
    public ResponseEntity<List<AlbumDto>> getAllAssignedAlbumsWithoutTTD() {
        return ResponseEntity.ok().body(albumService.getAssignedAlbumsWithoutTTD().stream().map(album -> new AlbumDto(
                album.getAlbumId().value(),
                album.getName(),
                album.getPhotographId(),
                album.getClientId(),
                album.getTtd(),
                album.getPhotos(),
                album.getAlbumPath())).toList());
    }

    @GetMapping("{albumId}/file/url/all")
    public ResponseEntity<List<String>> getAllFileUrls(@PathVariable UUID albumId, @RequestParam(required = false) Integer width, @RequestParam(required = false) Integer height, @RequestParam(required = false) boolean showOnlyVisable) {
        GetUrlsCommand cmd = new GetUrlsCommand(albumId, "http://localhost:8080", width, height, showOnlyVisable);
        return ResponseEntity.ok().body(albumService.getAllUrlsFromAlbum(cmd));
    }

    @DeleteMapping("/{albumId}/delete")
    public ResponseEntity<Void> deletePhotographAlbum(@PathVariable UUID albumId) {

        RemoveAlbumCommand command = new RemoveAlbumCommand(albumId);

        albumService.deleteAlbum(command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/create")
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

        List<UploadResponseFile> responseFiles = IntStream.range(0, addedFileIds.size())
                .mapToObj(index -> new UploadResponseFile(addedFileIds.get(index).value().toString(),files.get(index).getOriginalFilename())).toList();

        return ResponseEntity.accepted().body(new UploadResponse(responseFiles, "Files are being processed"));
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

    @PostMapping("/{albumIdUUID}/remove")
    public ResponseEntity<Void> removeFiles(@PathVariable UUID albumIdUUID, @RequestBody RemoveFilesRequest request) {
        RemoveFileCommand removeFileCommand = new RemoveFileCommand(request.fileIdList(), albumIdUUID);

        albumService.removeFiles(removeFileCommand);

        return ResponseEntity.noContent().build();

    }

    @GetMapping("{albumUUID}/photo/{fileName}")
    public ResponseEntity<Resource> getFile(@PathVariable UUID albumUUID, @PathVariable String fileName, @RequestParam(required = false) Integer width, @RequestParam(required = false) Integer height) {
        GetPhotoPathCommand cmd = new GetPhotoPathCommand(albumUUID,
                fileName,
                fileStorageLocation,
                servletContext,
                width,
                height);
        FileResource fileResponse = albumService.getFilePath(cmd);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(fileResponse.contentType())).header(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"" + fileResponse.resource().getFilename() + "\"").body(fileResponse.resource());
    }


    @PatchMapping("{albumId}/setTtd")
    public ResponseEntity<Void> setTtd(@PathVariable UUID albumId, @RequestParam Instant ttd) {
        SetTTDCommand cmd = new SetTTDCommand(albumId, ttd);
        albumService.setTTD(cmd);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("{albumId}/files/setVisible")
    public ResponseEntity<Void> changeFileVisible(@PathVariable UUID albumId, @RequestParam boolean visible, @RequestBody ChangeVisibleRequest request) {
        ChangeVisibleCommand cmd = new ChangeVisibleCommand(albumId, request.idList(), visible);
        albumService.changeVisibleStatus(cmd);
        return ResponseEntity.ok().build();
    }

    @PostMapping("{albumId}/files/addWatermark")
    public ResponseEntity<Void> changeWatermarkState(@PathVariable UUID albumId, @RequestParam boolean hasWatermark, @RequestBody ChangeWatermarkRequest request) {
        ChangeWatermarkCommand cmd = new ChangeWatermarkCommand(albumId, request.filesUUIDList(), hasWatermark);
        albumService.changeWatermarkStatus(cmd);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("{albumId}/album/{targetAlbumId}/swap")
    public ResponseEntity<Void> swapFile(@PathVariable UUID albumId, @PathVariable UUID targetAlbumId, @RequestBody SwapFileRequest request) {
        SwapFileCommand cmd = new SwapFileCommand(albumId, targetAlbumId, request.fileIdList());
        albumService.swapFile(cmd);
        return ResponseEntity.ok().build();
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
