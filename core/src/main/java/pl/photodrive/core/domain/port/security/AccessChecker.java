package pl.photodrive.core.domain.port.security;

import pl.photodrive.core.application.port.CurrentUser;
import pl.photodrive.core.domain.model.Role;

import java.util.List;


public interface AccessChecker {
    
    void iSCurrentUserHasAccess(CurrentUser currentUser, Role role);
    void isAlbumExists(String albumName);
    void isFilesExistsInAlbum(String albumName, CurrentUser currentUser, List<String> fileList);
}
