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



@Service
@RequiredArgsConstructor
public class AlbumManagementService {

    private final AlbumRepository albumRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    @Transactional
    public Album createAlbumForAdmin(CreateAlbumCommand cmd) {
        AuthenticatedUser authenticatedUser = currentUser.get().orElseThrow(() -> new AuthenticatedUserException("User not found"));
        if(!authenticatedUser.roles().contains(Role.ADMIN)) throw new SecurityException("ADMIN role required");
        User admin = userRepository.findById(authenticatedUser.userId()).orElseThrow(() -> new UserException("User not found!"));

        Album album = Album.createForAdmin(cmd.name(),admin, albumRepository);
        return albumRepository.save(album);
    }

    @Transactional
    public Album createAlbumForClient(CreateAlbumForClientCommand cmd) {
        AuthenticatedUser authenticatedUser = currentUser.get().orElseThrow(() -> new AuthenticatedUserException("User not found"));

        UserId photographId =new UserId(authenticatedUser.userId().value());
        User photograph = userRepository.findById(photographId).orElseThrow(() -> new UserException("Client not found!"));

        Email clientEmail = new Email(cmd.clientEmail());
        User client = userRepository.findByEmail(clientEmail).orElseThrow(() -> new UserException("Client not found!"));

        Album album = Album.createForClient(cmd.name(), photograph, client, albumRepository);

        return albumRepository.save(album);
    }

    @Transactional
    public void createAlbumForPhotographer(CreateAlbumForPhotographer cmd) {
        UserId photographId = new UserId(cmd.photographId());
        User photograph = userRepository.findById(photographId).orElseThrow(() -> new UserException("Client not found!"));

        Album album = Album.createPhotographerRootAlbum(photograph);
        albumRepository.save(album);
    }

}
