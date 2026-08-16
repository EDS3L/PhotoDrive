# PhotoDrive

PhotoDrive to platforma do bezpiecznego zarządzania i dystrybucji sesji fotograficznych. Fotograf zakłada klientom albumy, wgrywa zdjęcia oraz steruje ich widocznością, znakiem wodnym i czasem życia; klient loguje się i pobiera swoje zdjęcia; administrator zarządza użytkownikami i publicznym portfolio.

Jedna aplikacja SPA obsługuje trzy doświadczenia: **stronę publiczną** (wizytówka i portfolio), **strefę klienta** oraz **panel** (admin / fotograf).

Backend zbudowano w oparciu o **Architekturę Heksagonalną (Ports & Adapters)** oraz zasady **Domain-Driven Design (DDD)**.

## 🧱 Monorepo
- `core/` — backend: Spring Boot, REST API, logika domenowa, magazyn plików, baza danych
- `frontend/` — frontend: React + Vite (SPA: strona publiczna + panel admin/fotograf + strefa klienta)
- `docker-compose.yml` / `docker-compose.prod.yml` — uruchomienie lokalne / produkcyjne
- `.github/workflows/` — CI (testy i lint) oraz CD (build obrazów i deploy)

## 🚀 Kluczowe funkcjonalności

System obsługuje trzy główne role użytkowników:

### 📸 Fotograf (PHOTOGRAPHER)
- Zarządzanie albumami klientów (tworzenie, edycja, usuwanie).
- Upload zdjęć (chunkowany, z wykrywaniem kolizji nazw) i pobieranie ZIP.
- Sterowanie **widocznością** zdjęć — nowe zdjęcie w albumie klienta jest domyślnie ukryte, fotograf najpierw kuratoruje, potem udostępnia.
- **Znak wodny** oraz **TTD** (czas życia albumu, po którym scheduler go usuwa).
- Zakładanie kont klientom (hasło startowe generuje serwer i wysyła mailem).

### 👤 Klient (CLIENT)
- Bezpieczny dostęp do **tylko udostępnionych** zdjęć ze swoich albumów po zalogowaniu.
- Pobieranie zdjęć (pojedynczo / jako archiwum ZIP).
- Wymuszona zmiana hasła przy pierwszym logowaniu (egzekwowana po stronie serwera).

### 🛡️ Administrator (ADMIN)
- Zarządzanie kontami i rolami użytkowników.
- Tworzenie albumów administracyjnych i **publikacja portfolio** — każdy publiczny album to zakładka na stronie, z własną etykietą (`displayName`, pełny Unicode) i kolejnością (`displayOrder`).
- **Strona wizytówka** — podmiana zdjęć stałych sekcji strony publicznej (hero, intro, CTA, bio, sprzęt, tła ekranów logowania) bez wdrożenia.
- Zarządzanie globalnym **znakiem wodnym platformy**.
- Automatyczna wysyłka danych dostępowych do nowych użytkowników (szablony e-mail).

### 🌐 Bez logowania
- Strona główna, portfolio (zakładki generowane z publicznych albumów), „o mnie", formularz kontaktowy.
- Publiczne warianty zdjęć są zawsze zmniejszane (cap **2560 px** dłuższego boku) — oryginały nie wychodzą na zewnątrz.

## 🏗️ Architektura i technologie

### Backend (`core/`)
- **Język:** Java 21
- **Framework:** Spring Boot 3.5.7
- **Baza danych:** MySQL 8 (Spring Data JPA / Hibernate)
- **Bezpieczeństwo:** Spring Security + JWT (HS256) w **cookie HttpOnly `pd_at`** + filtr walidacji Origin (ochrona anty-CSRF); hasła BCrypt(12); rate limiting logowania i resetu hasła (per IP, odpowiedź 429 z `Retry-After`)
- **Zdarzenia domenowe:** operacje plikowe przed zatwierdzeniem transakcji (spójność plik ↔ baza), maile po zatwierdzeniu
- **Szablony e-mail:** HTML
- **Build:** Gradle (`./gradlew`)

### Frontend (`frontend/`)
- **React 19 + TypeScript + Vite**, Tailwind CSS, TanStack React Query, Zustand, axios, react-hook-form + zod
- Architektura **feature-based**; serwowany przez **nginx** (proxy `/api` → backend)

### Struktura katalogów backendu (DDD)
- `domain` — czysta logika biznesowa, wolna od frameworków
- `application` — serwisy aplikacyjne, komendy (CQRS), porty, handlery zdarzeń
- `infrastructure` — implementacje techniczne (JPA/MySQL, magazyn plików, mail, JWT, security)
- `presentation` — kontrolery REST API

## ⚙️ Uruchomienie

### Wymagania wstępne
- JDK 21
- MySQL 8
- Node.js 22 (frontend)
- Docker + Docker Compose (uruchomienie zestawu oraz **testy integracyjne** backendu)

### Najszybciej: Docker Compose
```bash
docker compose up --build
```
- Frontend: http://localhost:3000
- Backend:  http://localhost:8080

### Konfiguracja (zmienne środowiskowe)
Backend czyta konfigurację z `core/src/main/resources/application.yml`, które oczekuje zmiennych środowiskowych (np. z pliku `.env` w katalogu głównym — szablon: [`.env.example`](.env.example)):

| Zmienna | Opis |
|---|---|
| `DATASOURCE_URL`, `DATASOURCE_USERNAME`, `DATASOURCE_PASSWORD` | połączenie do MySQL |
| `MAIL_HOST`, `MAIL_USERNAME`, `MAIL_PASSWORD` | serwer SMTP (port 587, STARTTLS) |
| `CONTACT_RECIPIENT` | skrzynka odbierająca zapytania z formularza kontaktowego (domyślnie `MAIL_USERNAME`) |
| `DIR` | katalog magazynu plików (zdjęcia) |
| `STORAGE_TEMP_PATH` | katalog tymczasowy uploadu |
| `TOKEN_SECRETKEY` | sekret JWT — **Base64, min. 256 bitów (32 bajty)** |
| `ORG_MAX_SIZE` | limit pojemności magazynu (GB) |
| `APP_BASE_URL` | bazowy URL aplikacji (np. `https://photodrive.dev`) |
| `CSRF_ALLOWED_ORIGINS` | dozwolone originy dla żądań mutujących (anty-CSRF) |
| `COOKIE_SECURE` | flaga `Secure` na cookie sesji (`true` na produkcji, `false` przy pracy po http) |
| `JWT_ACCESS_TTL_MINUTES` | czas życia sesji JWT w minutach (opcjonalny, default `60`; sliding renewal) |
| `SWAGGER_USER`, `SWAGGER_PASSWORD` | logowanie do Swagger UI |

> Sesja przechowywana jest w **HttpOnly cookie `pd_at`** (token JWT podpisany HS256). Domyślny czas życia to **60 min** (`JWT_ACCESS_TTL_MINUTES`) z **sliding renewal** — cookie odnawia się przy aktywności, gdy tokenowi zostało mniej niż połowa życia.

### Uruchomienie ręczne
Backend:
```bash
cd core
./gradlew bootRun     # wymaga ustawionych zmiennych środowiskowych
./gradlew test        # sam przebieg testów (szybki, lokalny)
./gradlew check       # testy + bramka pokrycia — to samo, co uruchamia CI
```
Frontend:
```bash
cd frontend
npm install
npm run dev           # Vite, proxy /api → http://localhost:8080
npm run lint
npm run test          # sam przebieg testów (szybki, lokalny)
npm run test:coverage # testy + bramka pokrycia — to samo, co uruchamia CI
npm run build         # tsc -b (typuje także testy) + produkcyjny build do dist/
```

Dokumentacja API: **Swagger UI** pod `/swagger-ui` (chronione osobnym logowaniem — `SWAGGER_USER` / `SWAGGER_PASSWORD`).

## 🧪 Testy

| | Backend (`core/`) | Frontend (`frontend/`) |
|---|---|---|
| Stack | JUnit 5 + Mockito + AssertJ + **Testcontainers** | Vitest + React Testing Library + jsdom |
| Przypadki testowe | 683 | 304 |
| Pokrycie linii | 90,8% | 77,9% |
| Bramka | `./gradlew check` (JaCoCo) | `npm run test:coverage` (`thresholds`) |

- Próg **70% jest realnie egzekwowany w obu stackach** — spadek poniżej przerywa build, a więc i deploy.
- Testy integracyjne startują **prawdziwy MySQL 8** w kontenerze, więc `./gradlew check` **wymaga uruchomionego Dockera**. Sprawdzają cały tor HTTP → filtry → domena → baza i dysk, w tym macierz autoryzacji (każdy chroniony endpoint × każda rola).
- Konwencja jest wspólna dla obu stacków: nazwa testu opisuje **regułę biznesową**, w środku bloki `Given` / `When` / `Then` — przebieg testów czyta się jak specyfikacja zachowań.

## 🔄 CI/CD

```
push → GitHub Actions
  ├─ checks (reużywalny workflow)
  │    ├─ Backend  → ./gradlew check
  │    └─ Frontend → npm run lint + npm run test:coverage + npm run build
  └─ [tylko main] build obrazów Docker → Docker Hub → deploy na VPS (Traefik + TLS)
                   ↑ needs: checks — czerwone testy blokują deploy
```

Każdy przebieg na `main` buduje **oba** stacki z bieżącego HEAD-a, dzięki czemu obowiązuje niezmiennik „produkcja = wierzchołek `main`". Dostępny jest też ręczny `workflow_dispatch` (re-deploy bez nowego commita).
