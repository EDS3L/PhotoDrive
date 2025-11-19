# PhotoDrive Core

PhotoDrive to profesjonalna platforma typu open-source dedykowana studiom fotograficznym i dużym firmom. System umożliwia bezpieczne zarządzanie sesjami zdjęciowymi, ich dystrybucję do klientów oraz integrację z zewnętrznymi systemami poprzez API.

Projekt został zbudowany w oparciu o **Architekturę Heksagonalną (Ports & Adapters)** oraz zasady **Domain-Driven Design (DDD)**, co zapewnia wysoką skalowalność i łatwość utrzymania.

## 🚀 Kluczowe Funkcjonalności

System obsługuje trzy główne role użytkowników, z dedykowanymi zestawami funkcji:

### 📸 Dla Fotografa (Photographer)
*   **Zarządzanie Sesjami (Albumy):** Pełny cykl życia sesji zdjęciowej (tworzenie, edycja, usuwanie albumów).
*   **Upload Plików:** Wydajny mechanizm przesyłania zdjęć do dedykowanych folderów/galerii.
*   **Organizacja:** Strukturyzowanie plików wewnątrz albumów.

### 👤 Dla Klienta (Client)
*   **Dostęp do Zdjęć:** Bezpieczne pobieranie zdjęć z wykupionych sesji po autoryzacji.
*   **Integracja API:** Możliwość podłączenia galerii do własnej strony internetowej za pomocą tokenów dostępowych.

### 🛡️ Dla Administratora (Admin)
*   **Zarządzanie Użytkownikami:** Tworzenie i edycja kont Fotografów oraz Klientów.
*   **Bezpieczeństwo:** Nadawanie uprawnień i ról systemowych.
*   **Powiadomienia:** Automatyczna wysyłka danych dostępowych do nowych użytkowników (szablony e-mail).

## 🏗️ Architektura i Technologie

Projekt wykorzystuje nowoczesny stos technologiczny Java:

*   **Język:** Java 17+
*   **Framework:** Spring Boot 3.x
*   **Baza Danych:** Relacyjna (JPA/Hibernate) - konfiguracja w `application.yml`.
*   **Bezpieczeństwo:** Spring Security + JWT (JSON Web Tokens) do bezstanowej autoryzacji.
*   **Przetwarzanie Asynchroniczne:** System zdarzeń (Events) do obsługi operacji na plikach (np. `FileAddedToClientAlbum`).
*   **Szablony:** Thymeleaf (do generowania wiadomości e-mail).

### Struktura Katalogów (DDD)
*   `domain` - Czysta logika biznesowa, wolna od frameworków.
*   `application` - Serwisy aplikacyjne i obsługa komend (CQRS).
*   `infrastructure` - Implementacje techniczne (baza danych, pliki, maile, JWT).
*   `presentation` - Kontrolery REST API.

## ⚙️ Instalacja i Uruchomienie

### Wymagania wstępne
*   JDK 17 lub nowsze
*   Maven lub Gradle
*   Baza danych (np. PostgreSQL lub H2 dla testów)

### Konfiguracja
Przed uruchomieniem należy skonfigurować plik `src/main/resources/application.yml`.

#### 1. Baza Danych
Ustaw parametry połączenia do bazy danych:
```yaml
spring:
  datasource:
    url:
    username: twoj_user
    password: twoje_haslo
```

#### 2. Konfiguracja JWT (Klucze API)
Aby API działało poprawnie i generowało bezpieczne tokeny dla klientów, należy wygenerować silny klucz sekretny (HMAC-SHA) i umieścić go w konfiguracji:

```yaml
jwt:
  secret: "BARDZO_DLUGI_I_SKOMPLIKOWANY_CIAG_ZNAKOW_DLA_BEZPIECZENSTWA"
  expiration-ms: 86400000 # Czas życia tokena (np. 24h)
```
*Jest to kluczowe dla modułu `infrastructure/jwt`, który odpowiada za autoryzację zapytań API.*

#### 3. Magazyn Plików
Skonfiguruj ścieżkę, gdzie fizycznie będą zapisywane zdjęcia:
```yaml
storage:
  location: "/var/photodrive/data"
```

Aplikacja domyślnie wystartuje na porcie `8080`.
