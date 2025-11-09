package pl.photodrive.core.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.photodrive.core.application.port.CurrentUser;
import pl.photodrive.core.application.service.AlbumManagementService;

@RestController
@RequestMapping("/api/album")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumManagementService managementService;
    private final CurrentUser currentUser;

}
