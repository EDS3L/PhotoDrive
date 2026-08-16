package pl.photodrive.core.application.port.site;

import java.util.List;
import java.util.Optional;

public interface SiteSlotStorePort {

    Optional<SiteSlotImage> find(SiteSlot slot);

    List<SiteSlotVersion> findVersions();

    void put(SiteSlot slot, byte[] image);

    void delete(SiteSlot slot);
}
