package pl.photodrive.core.presentation.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;

@Slf4j
@RestController
@RequestMapping("/api/album")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumManagementService albumService;
    private final TemporaryStoragePort temporaryStorageService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.upload.max-total-size-bytes:2097152000}")
    private long maxTotalSizeBytes;


    @GetMapping("/all")
    public ResponseEntity<List<AlbumDto>> getAllAlbums() {
        return ResponseEntity.ok().body(albumService.getAllAlbums().stream().map(album -> mapAlbum(album,
                false)).toList());
    }

    @GetMapping("/getAllAssignedAlbums")
    public ResponseEntity<List<AlbumDto>> getAllAssignedAlbums() {
        boolean clientView = albumService.isCurrentUserClient();
        return ResponseEntity.ok().body(albumService.getAssignedAlbums().stream().map(album -> mapAlbum(album,
                clientView)).toList());
    }

    @GetMapping("/all/withoutTdd")
    public ResponseEntity<List<AlbumDto>> getAllAlbumsWithoutTTD() {
        return ResponseEntity.ok().body(albumService.getAllAlbumsWithoutTTD().stream().map(album -> mapAlbum(album,
                false)).toList());
    }

    @GetMapping("/allAssignedAlbum/withoutTdd")
    public ResponseEntity<List<AlbumDto>> getAllAssignedAlbumsWithoutTTD() {
        boolean clientView = albumService.isCurrentUserClient();
        return ResponseEntity.ok().body(albumService.getAssignedAlbumsWithoutTTD().stream().map(album -> mapAlbum(album,
                clientView)).toList());
    }

    @GetMapping("{albumId}/file/url/all")
    public ResponseEntity<List<String>> getAllFileUrls(@PathVariable UUID albumId, @RequestParam(required = false) Integer width, @RequestParam(required = false) Integer height, @RequestParam(required = false) boolean showOnlyVisable) {
        GetUrlsCommand cmd = new GetUrlsCommand(albumId, baseUrl, width, height, showOnlyVisable);
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
    public ResponseEntity<Void> renameFile(@PathVariable UUID albumIdUUID, @PathVariable UUID fileIdUUID, @Valid @RequestBody RenameFileRequest request) {
        RenameFileCommand command = new RenameFileCommand(albumIdUUID, fileIdUUID, request.newFileName());

        albumService.renameFile(command);

        return ResponseEntity.noContent().build();

    }

    @PostMapping("/{albumIdUUID}/remove")
    public ResponseEntity<Void> removeFiles(@PathVariable UUID albumIdUUID, @Valid @RequestBody RemoveFilesRequest request) {
        RemoveFileCommand removeFileCommand = new RemoveFileCommand(request.fileIdList(), albumIdUUID);

        albumService.removeFiles(removeFileCommand);

        return ResponseEntity.noContent().build();

    }

    @GetMapping("{albumUUID}/photo/{fileName}")
    public ResponseEntity<Resource> getFile(@PathVariable UUID albumUUID, @PathVariable String fileName, @RequestParam(required = false) Integer width, @RequestParam(required = false) Integer height) {
        GetPhotoPathCommand cmd = new GetPhotoPathCommand(albumUUID,
                fileName,
                width,
                height);
        FileResource fileResponse = albumService.getFilePath(cmd);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(fileResponse.contentType())).header(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"" + fileResponse.resource().getFilename() + "\"").body(fileResponse.resource());
    }


    @PatchMapping("{albumId}/setTtd")
    public ResponseEntity<Void> setTtd(@PathVariable UUID albumId, @Valid @RequestBody SetTtdRequest request) {
        Instant ttdInstant = request.ttd().atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        SetTTDCommand cmd = new SetTTDCommand(albumId, ttdInstant);
        albumService.setTTD(cmd);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("{albumId}/setPublic")
    public ResponseEntity<Void> setPublic(@PathVariable UUID albumId, @RequestParam boolean isPublic) {
        SetAlbumVisibilityCommand cmd = new SetAlbumVisibilityCommand(albumId, isPublic);
        albumService.setAlbumPublic(cmd);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("{albumId}/files/setVisible")
    public ResponseEntity<Void> changeFileVisible(@PathVariable UUID albumId, @RequestParam boolean visible, @Valid @RequestBody ChangeVisibleRequest request) {
        ChangeVisibleCommand cmd = new ChangeVisibleCommand(albumId, request.idList(), visible);
        albumService.changeVisibleStatus(cmd);
        return ResponseEntity.ok().build();
    }

    @PostMapping("{albumId}/files/addWatermark")
    public ResponseEntity<Void> changeWatermarkState(@PathVariable UUID albumId, @RequestParam boolean hasWatermark, @Valid @RequestBody ChangeWatermarkRequest request) {
        ChangeWatermarkCommand cmd = new ChangeWatermarkCommand(albumId, request.filesUUIDList(), hasWatermark);
        albumService.changeWatermarkStatus(cmd);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("{albumId}/album/{targetAlbumId}/swap")
    public ResponseEntity<Void> swapFile(@PathVariable UUID albumId, @PathVariable UUID targetAlbumId, @Valid @RequestBody SwapFileRequest request) {
        SwapFileCommand cmd = new SwapFileCommand(albumId, targetAlbumId, request.fileIdList());
        albumService.swapFile(cmd);
        return ResponseEntity.ok().build();
    }


    private void validateFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files provided");
        }

        long totalSize = files.stream().mapToLong(MultipartFile::getSize).sum();

        if (totalSize > maxTotalSizeBytes) {
            throw new IllegalArgumentException("Total file size exceeds limit: " + maxTotalSizeBytes + " bytes");
        }
    }

    private AlbumDto mapAlbum(Album album, boolean clientView) {
        return new AlbumDto(album.getAlbumId().value(),
                album.getName(),
                clientView ? null : album.getPhotographId(),
                clientView ? null : album.getClientId(),
                album.getTtd(),
                mapFiles(album, clientView),
                clientView || album.getAlbumPath() == null ? null : album.getAlbumPath().value(),
                album.isPublic());
    }

    private List<FileDto> mapFiles(Album album, boolean visibleOnly) {
        if (album.getPhotos() == null || album.getPhotos().isEmpty()) {
            return List.of();
        }
        return album.getPhotos().values().stream()
                .filter(file -> !visibleOnly || file.isVisible())
                .map(file -> new FileDto(
                        file.getFileId().value(),
                        file.getFileName().value(),
                        file.getSizeBytes(),
                        file.getContentType(),
                        file.getUploadedAt(),
                        file.isVisible(),
                        file.isHasWatermark()))
                .toList();
    }
    private void uploadFiles(List<MultipartFile> files, List<FileUpload> fileUploads) {
        for (MultipartFile file : files) {
            try {
                String tempId = temporaryStorageService.saveTemporary(file.getInputStream());
                fileUploads.add(new FileUpload(
                        new FileName(file.getOriginalFilename()),
                        file.getSize(),
                        file.getContentType(),
                        tempId));
            } catch (IOException e) {
                throw new AlbumException("Failed to upload file: " + file.getOriginalFilename());
            }
        }
    }

}
