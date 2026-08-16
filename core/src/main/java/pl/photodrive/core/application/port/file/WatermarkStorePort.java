package pl.photodrive.core.application.port.file;

import java.util.Optional;

public interface WatermarkStorePort {

    Optional<PlatformWatermark> get();

    void put(byte[] image);

    void delete();

    boolean exists();
}
