package pl.photodrive.core.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.photodrive.core.application.command.album.CreateAlbumCommand;
import pl.photodrive.core.application.command.album.CreateAlbumForClientCommand;
import pl.photodrive.core.application.command.album.CreateAlbumForPhotographer;
import pl.photodrive.core.application.exception.AuthenticatedUserException;
import pl.photodrive.core.application.exception.SecurityException;
import pl.photodrive.core.application.port.AuthenticatedUser;
import pl.photodrive.core.application.port.CurrentUser;
import pl.photodrive.core.domain.exception.UserException;
import pl.photodrive.core.domain.model.Album;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.port.StoragePort;
import pl.photodrive.core.domain.port.repository.AlbumRepository;
import pl.photodrive.core.domain.port.repository.UserRepository;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.UserId;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AlbumManagementService {

    private final AlbumRepository albumRepository;
    private final UserRepository userRepository;
    private final StoragePort storagePort;
    private final CurrentUser currentUser;

    @Transactional
    public Album createAlbum(CreateAlbumCommand cmd) {
        AuthenticatedUser authenticatedUser = currentUser.get().orElseThrow(() -> new AuthenticatedUserException("User not found"));
        if(!authenticatedUser.roles().contains(Role.ADMIN)) throw new SecurityException("ADMIN role required");
        User user = userRepository.findById(authenticatedUser.userId()).orElseThrow(() -> new UserException("User not found!"));
        String albumName = createAlbumName(cmd.name(), user.getEmail().value());

        Album album = Album.create(albumName,authenticatedUser.userId().value(), null, null);
        albumRepository.save(album);
        storagePort.createAdminAlbumDir(albumName, user.getEmail().value());
        return album;
    }

    @Transactional
    public Album createAlbumForClient(CreateAlbumForClientCommand cmd) {
        AuthenticatedUser authenticatedUser = currentUser.get().orElseThrow(() -> new AuthenticatedUserException("User not found"));

        if(!authenticatedUser.roles().contains(Role.PHOTOGRAPHER) || !authenticatedUser.roles()
                .contains(Role.ADMIN)) throw new SecurityException("ADMIN/PHOTOGRAPHER role required");

        UserId photographId =new UserId(authenticatedUser.userId().value());
        User photograph = userRepository.findById(photographId).orElseThrow(() -> new UserException("Client not found!"));

        Email clientEmail = new Email(cmd.clientEmail());
        User client = userRepository.findByEmail(clientEmail).orElseThrow(() -> new UserException("Client not found!"));

        String albumName = createAlbumName(cmd.name(), client.getEmail().value());

        Album album = Album.create(albumName,authenticatedUser.userId().value(), client.getId().value(), null);
        albumRepository.save(album);
        storagePort.createClientAlbumDir(albumName, photograph.getEmail().value());
        return album;
    }

    @Transactional
    public void createAlbumForPhotographer(CreateAlbumForPhotographer cmd) {
        AuthenticatedUser authenticatedUser = currentUser.get().orElseThrow(() -> new AuthenticatedUserException("User not found"));

        if(!authenticatedUser.roles().contains(Role.ADMIN)) throw new SecurityException("ADMIN role required");

        UserId photographId = new UserId(cmd.photographId());
        User photograph = userRepository.findById(photographId).orElseThrow(() -> new UserException("Client not found!"));


        Album album = Album.create(photograph.getEmail().value(),photographId.value(),null, null);
        albumRepository.save(album);
        storagePort.createFolderForPhotograph(photograph.getEmail().value());
    }


    private String createAlbumName(String albumName, String email) {
        return albumName + "_" + email + "_" + LocalDate.now();
    }
}
