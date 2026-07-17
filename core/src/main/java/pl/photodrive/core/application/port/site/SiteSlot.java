package pl.photodrive.core.application.port.site;

/**
 * Sloty na pojedyncze zdjęcia strony wizytówki (hero, sekcje „o mnie" itd.).
 *
 * <p>Slot to NIE album: album jest kolekcją prac, slot to dokładnie jeden obrazek
 * w konkretnym miejscu strony publicznej. Wcześniej sloty udawały albumy o magicznych
 * nazwach ({@code home-hero}, {@code about-bio}), co wymuszało na adminie trzy kroki
 * (utwórz album → ustaw publiczny → wgraj) i loterię „które zdjęcie z albumu wisi".
 *
 * <p>Dodanie nowej sekcji strony = dodanie wartości tutaj — panel admina renderuje
 * listę slotów z tego enuma (przez API), więc nowy slot pojawia się tam sam.
 */
public enum SiteSlot {
    HOME_HERO,
    HOME_INTRO,
    HOME_CTA,
    ABOUT_BIO,
    ABOUT_EQUIPMENT,
    // Lewe panele ekranów logowania (strefa klienta / panel zarządzania). Nowe wartości
    // dopisujemy NA KOŃCU — panel renderuje listę w kolejności enuma.
    CLIENT_LOGIN,
    PANEL_LOGIN
}
