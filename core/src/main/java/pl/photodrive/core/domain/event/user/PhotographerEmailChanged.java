package pl.photodrive.core.domain.event.user;

/**
 * Fotograf zmienił swój e-mail. Ścieżka folderu fotografa na dysku wywodzi się z maila
 * ({@code {storage}/{email}/...}), więc folder trzeba przenieść pod nowy mail — inaczej
 * wszystkie odczyty/zapisy zdjęć celują w nieistniejący katalog i fotograf traci dostęp
 * do swoich albumów (B.33). Klienci i admin nie mają folderu per-mail, więc zdarzenie
 * powstaje wyłącznie dla roli PHOTOGRAPHER.
 */
public record PhotographerEmailChanged(String oldEmail, String newEmail) {
}
