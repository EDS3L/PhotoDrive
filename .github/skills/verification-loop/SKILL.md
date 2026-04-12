---
name: verification-loop
description: Kompleksowa weryfikacja po każdej zmianie kodu — build, typy, lint, testy, bezpieczeństwo, diff. Używaj po każdej funkcji, przed PR, po refaktoryzacji.
origin: ECC
---

# Verification Loop

Kompleksowy system weryfikacji dla sesji kodowania. Uruchamiaj po ukończeniu funkcji, przed PR, po refaktoryzacji.

## Kiedy Używać

- Po zakończeniu funkcji lub istotnej zmiany kodu
- Przed stworzeniem PR
- Po refaktoryzacji
- Co 15 minut podczas długich sesji

## Fazy Weryfikacji

Najpierw wykryj strukturę projektu i używane narzędzia (`build.gradle*`, `pom.xml`, `package.json`, lockfile, workspace config). Uruchamiaj komendy w katalogach modułów, które realnie definiują build, lint i testy.

### Faza 1: Build

**JVM / backend (wybierz build tool projektu):**

```bash
# Gradle wrapper
./gradlew clean compileJava -q

# Maven wrapper
./mvnw -q -DskipTests compile
```

**Frontend / JS (użyj skryptu projektu):**

```bash
npm run build
# lub: pnpm build / yarn build / bun run build
```

Jeśli build nie przechodzi — STOP i napraw zanim przejdziesz dalej.

### Faza 2: Type Check

**TypeScript / frontend:**

```bash
npx tsc -b --pretty false
# lub istniejący skrypt projektu, np. npm run typecheck
```

**Java / JVM:**

```bash
./gradlew compileJava -q
# lub: ./mvnw -q -DskipTests compile
```

Zgłoś wszystkie błędy typów. Napraw krytyczne przed kontynuacją.

### Faza 3: Lint

**Frontend:**

```bash
npm run lint
```

**Backend / JVM:**

Jeśli projekt ma task lintujący (`checkstyle`, `spotlessCheck`, `pmdMain`, `ktlintCheck`, `detekt` lub własny skrypt), uruchom go. Jeśli nie ma jawnego taska lintującego, nie raportuj faila za jego brak; oprzyj backendową weryfikację na kompilacji, testach i review.

### Faza 4: Test Suite

**JVM / backend:**

```bash
./gradlew test jacocoTestReport -q
# lub: ./mvnw test
```

Jeśli projekt ma skonfigurowane coverage (np. JaCoCo, Kover, Surefire/Failsafe + coverage), raportuj je. Jeśli nie, raportuj same wyniki testów.

**Frontend / JS:**

```bash
npx vitest run --coverage
# lub istniejący skrypt projektu, np. npm test -- --coverage
```

Jeśli frontend nie ma jeszcze test infrastructure albo skryptu testowego, odnotuj to jawnie zamiast uruchamiać losowy runner.

Raportuj:

- Łącznie testów: X
- Passed: X
- Failed: X
- Pokrycie: X%

### Faza 5: Security Scan

```bash
# Brak hardcoded secrets
rg -n --glob "!**/node_modules/**" --glob "!**/build/**" --glob "!**/dist/**" "password\s*=\s*\"" .
rg -n --glob "!**/node_modules/**" --glob "!**/build/**" --glob "!**/dist/**" "api[_-]key\s*=" .

# Brak console.log w produkcji
rg -n --glob "!**/node_modules/**" --glob "!**/build/**" --glob "!**/dist/**" "console\.log" .

# SQL injection / native query check
rg -n "createNativeQuery|nativeQuery\s*=" .
```

### Faza 6: Diff Review

```bash
git diff --stat
git diff HEAD~1 --name-only
```

Sprawdź każdy zmieniony plik pod kątem:

- Niezamierzonych zmian
- Brakującej obsługi błędów
- Potencjalnych edge cases

## Format Raportu

```
VERIFICATION REPORT
==================

Build:     [PASS/FAIL]
Types:     [PASS/FAIL] (X błędów)
Lint:      [PASS/FAIL] (X ostrzeżeń)
Tests:     [PASS/FAIL] (X/Y passed, Z% pokrycia)
Security:  [PASS/FAIL] (X issues)
Diff:      [X plików zmienionych]

Overall:   [READY/NOT READY] dla PR

Issues do naprawy:
1. ...
2. ...
```

## Tryb Ciągły

Dla długich sesji uruchamiaj weryfikację co 15 minut lub po każdej głównej zmianie:

- Po ukończeniu każdej funkcji
- Po zakończeniu komponentu
- Przed przejściem do następnego zadania

## Integracja z Innymi Skilami

- **santa-method** — Verification Loop dla sprawdzeń deterministycznych (build, lint, test). Santa dla sprawdzeń semantycznych (dokładność, hallucinations). Uruchom verification-loop NAJPIERW, Santa jako drugie.
- **agent-introspection-debugging** — Po recovery uruchom verification-loop jeśli kod był zmieniany.
