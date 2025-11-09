package pl.photodrive.core.presentation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.photodrive.core.application.command.album.CreateAlbumCommand;
import pl.photodrive.core.application.command.album.CreateAlbumForClientCommand;
import pl.photodrive.core.application.port.CurrentUser;
import pl.photodrive.core.application.service.AlbumManagementService;
import pl.photodrive.core.domain.model.Album;
import pl.photodrive.core.presentation.dto.album.CreateAlbumRequest;
import pl.photodrive.core.presentation.dto.album.CreateClientAlbumRequest;

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
        Album album = managementService.createAlbumForClient(new CreateAlbumForClientCommand(request.name(), request.photographEmail()));
        return ResponseEntity.ok().body(album);
    }

}
