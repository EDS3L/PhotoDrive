package pl.photodrive.core.application.port.file;

import java.io.IOException;
import java.io.InputStream;

public interface TemporaryStoragePort {
    String saveTemporary(InputStream data) throws IOException;

    InputStream getFile(String tempId) throws IOException;

    boolean exists(String tempId);

    void delete(String tempId);
}