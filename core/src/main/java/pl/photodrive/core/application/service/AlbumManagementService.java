package pl.photodrive.core.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pl.photodrive.core.application.command.album.*;
import pl.photodrive.core.application.exception.AuthenticatedUserException;
import pl.photodrive.core.application.exception.SecurityException;
import pl.photodrive.core.application.port.AuthenticatedUser;
import pl.photodrive.core.application.port.CurrentUser;
import pl.photodrive.core.domain.exception.AlbumException;
import pl.photodrive.core.domain.exception.UserException;
import pl.photodrive.core.domain.model.Album;
import pl.photodrive.core.domain.model.File;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.port.AlbumSaver;
import pl.photodrive.core.domain.port.FileUniquenessChecker;
import pl.photodrive.core.domain.port.StoragePort;
import pl.photodrive.core.domain.port.repository.AlbumRepository;
import pl.photodrive.core.domain.port.repository.UserRepository;
import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.FileName;
import pl.photodrive.core.domain.vo.UserId;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class AlbumManagementService {

    private final AlbumRepository albumRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final FileUniquenessChecker fileUniquenessChecker;
    private final AlbumSaver albumSaver;

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

    @Transactional
    public File addFile(AddFileCommand cmd) {
        var user = currentUser.get().orElseThrow(() -> new AuthenticatedUserException("User not found"));
        log.info("Photograph ID: {}", user.userId().value());
        Album album = albumRepository.findByPhotographId(user.userId().value());
        MultipartFile multipartFile = cmd.multipartFile();
        File file = File.create(new FileName(multipartFile.getOriginalFilename()), multipartFile.getSize(), multipartFile.getContentType(), fileUniquenessChecker);

        album.addFile(file);
        albumRepository.save(album);

        return file;
    }

    @Transactional
    public List<File> addFilesToClient(AddFileToClientAlbumCommand cmd) {
        AuthenticatedUser authenticatedUser = currentUser.get().orElseThrow(() -> new AuthenticatedUserException("User not found"));

        UserId photographId =new UserId(authenticatedUser.userId().value());
        User photograph = userRepository.findById(photographId).orElseThrow(() -> new UserException("Client not found!"));
        Album album = albumRepository.findByName(cmd.albumName()).orElseThrow(() -> new AlbumException("Album not found!"));
        MultipartFile[] multipartFiles = cmd.multipartFiles();


        return album.addFiles(multipartFiles, photograph.getEmail().value(), albumSaver,fileUniquenessChecker);
    }

}
