package pl.photodrive.core.application.port.file;

import java.util.Optional;

/**
 * Przechowywanie JEDNEGO globalnego znaku wodnego platformy (PNG wgrywany przez admina).
 * Logo żyje w bazie danych (singleton) — celowo NIE na dysku: brak folderów, o których
 * baza nie wie, brak ręcznej konfiguracji na serwerze, backup razem z dumpem bazy.
 */
public interface WatermarkStorePort {

    Optional<PlatformWatermark> get();

    void put(byte[] image);

    void delete();

    boolean exists();
}
