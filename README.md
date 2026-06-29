# PhotoDrive

PhotoDrive to platforma do bezpiecznego zarządzania i dystrybucji sesji fotograficznych. Fotograf zakłada klientom albumy, wgrywa zdjęcia oraz steruje ich widocznością, znakiem wodnym i czasem życia; klient loguje się i pobiera swoje zdjęcia; administrator zarządza użytkownikami i publicznym portfolio.

Backend zbudowano w oparciu o **Architekturę Heksagonalną (Ports & Adapters)** oraz zasady **Domain-Driven Design (DDD)**.

## 🧱 Monorepo
- `core/` — backend: Spring Boot, REST API, logika domenowa, magazyn plików, baza danych
- `frontend/` — frontend: React + Vite (SPA: strona publiczna + panel admin/fotograf + strefa klienta)
- `docker-compose.yml` / `docker-compose.prod.yml` — uruchomienie lokalne / produkcyjne

## 🚀 Kluczowe Funkcjonalności

System obsługuje trzy główne role użytkowników:

### 📸 Fotograf (PHOTOGRAPHER)
- Zarządzanie albumami klientów (tworzenie, edycja, usuwanie).
- Upload zdjęć, sterowanie widocznością, znak wodny, czas wygaśnięcia (TTD).
- Przenoszenie plików między albumami, pobieranie ZIP.

### 👤 Klient (CLIENT)
- Bezpieczny dostęp do **tylko udostępnionych** zdjęć ze swoich albumów po zalogowaniu.
- Pobieranie zdjęć (pojedynczo / jako archiwum ZIP).

### 🛡️ Administrator (ADMIN)
- Zarządzanie kontami i rolami użytkowników.
- Tworzenie albumów administracyjnych i **publikacja portfolio** (albumy publiczne).
- Automatyczna wysyłka danych dostępowych do nowych użytkowników (szablony e-mail).

## 🏗️ Architektura i Technologie

### Backend (`core/`)
- **Język:** Java 21
- **Framework:** Spring Boot 3.5.7
- **Baza danych:** MySQL 8 (Spring Data JPA / Hibernate)
- **Bezpieczeństwo:** Spring Security + JWT (HS256) w **cookie HttpOnly `pd_at`** + filtr walidacji Origin (ochrona anty-CSRF); hasła BCrypt
- **Zdarzenia domenowe:** obsługa operacji na plikach i wysyłki maili (np. `FileAddedToClientAlbum`)
- **Szablony e-mail:** HTML
- **Build:** Gradle (`./gradlew`)

### Frontend (`frontend/`)
- **React 19 + TypeScript + Vite**, Tailwind CSS, TanStack React Query, Zustand, axios
- Serwowany przez **nginx** (proxy `/api` → backend)

### Struktura katalogów backendu (DDD)
- `domain` — czysta logika biznesowa, wolna od frameworków
- `application` — serwisy aplikacyjne, komendy (CQRS), porty, handlery zdarzeń
- `infrastructure` — implementacje techniczne (JPA/MySQL, magazyn plików, mail, JWT)
- `presentation` — kontrolery REST API

## ⚙️ Uruchomienie

### Wymagania wstępne
- JDK 21
- MySQL 8
- Node.js 22 (frontend)
- (opcjonalnie) Docker + Docker Compose

### Najszybciej: Docker Compose
```bash
docker compose up --build
```
- Frontend: http://localhost:3000
- Backend:  http://localhost:8080

### Konfiguracja (zmienne środowiskowe)
Backend czyta konfigurację z `core/src/main/resources/application.yml`, które oczekuje zmiennych środowiskowych (np. z pliku `.env` w katalogu głównym):

| Zmienna | Opis |
|---|---|
| `DATASOURCE_URL`, `DATASOURCE_USERNAME`, `DATASOURCE_PASSWORD` | połączenie do MySQL |
| `MAIL_HOST`, `MAIL_USERNAME`, `MAIL_PASSWORD` | serwer SMTP (port 587, STARTTLS) |
| `DIR` | katalog magazynu plików (zdjęcia) |
| `STORAGE_TEMP_PATH` | katalog tymczasowy uploadu |
| `TOKEN_SECRETKEY` | sekret JWT — **Base64, min. 256 bitów (32 bajty)** |
| `ORG_MAX_SIZE` | limit pojemności magazynu (GB) |
| `APP_BASE_URL` | bazowy URL aplikacji (np. `https://photodrive.dev`) |
| `CSRF_ALLOWED_ORIGINS` | dozwolone originy dla żądań mutujących (anty-CSRF) |
| `COOKIE_SECURE` | flaga `Secure` na cookie sesji (`true` na produkcji) |
| `SWAGGER_USER`, `SWAGGER_PASSWORD` | logowanie do Swagger UI |

> Sesja przechowywana jest w **HttpOnly cookie `pd_at`** (token JWT podpisany HS256). Czas życia tokenu jest krótki i odnawiany przy aktywności — konfigurowany po stronie backendu, nie ma osobnej właściwości `expiration-ms`.

### Uruchomienie ręczne
Backend:
```bash
cd core
./gradlew bootRun     # wymaga ustawionych zmiennych środowiskowych
./gradlew test        # testy + raport pokrycia JaCoCo
```
Frontend:
```bash
cd frontend
npm install
npm run dev           # Vite, proxy /api → http://localhost:8080
npm run build         # produkcyjny build do dist/
```

Dokumentacja API: **Swagger UI** pod `/swagger-ui` (chronione osobnym logowaniem — `SWAGGER_USER` / `SWAGGER_PASSWORD`).
