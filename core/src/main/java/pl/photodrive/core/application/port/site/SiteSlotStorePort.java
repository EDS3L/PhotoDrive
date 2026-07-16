package pl.photodrive.core.application.port.site;

import java.util.List;
import java.util.Optional;

/**
 * Przechowywanie zdjęć slotów strony wizytówki. Obrazy żyją w bazie danych
 * (jak logo watermarku, {@code WatermarkStorePort}) — celowo NIE na dysku:
 * brak ścieżek wywodzonych z nazw (klasa problemów B.32 tu nie istnieje),
 * backup razem z dumpem bazy, zero ręcznej konfiguracji na serwerze.
 */
public interface SiteSlotStorePort {

    Optional<SiteSlotImage> find(SiteSlot slot);

    /** Wersje wszystkich skonfigurowanych slotów — bez BLOB-ów (to jest na gorącej ścieżce strony publicznej). */
    List<SiteSlotVersion> findVersions();

    void put(SiteSlot slot, byte[] image);

    void delete(SiteSlot slot);
}
