---
name: search-first
description: Badaj istniejące rozwiązania ZANIM napiszesz kod. Przejdź przez checklistę: czy istnieje w repo? npm/Gradle ecosystem? GitHub? Używaj przed każdym nowym utility, helper, abstrakcją.
origin: ECC
---

# Search-First — Badaj Przed Kodowaniem

Systematyzuje workflow "szukaj istniejących rozwiązań zanim implementujesz".

## Trigger

Używaj tego skilla gdy:

- Zaczynasz nową funkcję która prawdopodobnie ma istniejące rozwiązania
- Dodajesz dependency lub integrację
- User prosi "dodaj X funkcjonalność" i zaraz masz pisać kod
- Przed stworzeniem nowego utility, helper lub abstrakcji

## Checklista Quick Mode

Przed napisaniem dowolnego utility przejdź:

```
0. Czy to już istnieje w repo?
   → szukaj w relevantnych modułach i testach

1. Czy to częsty problem w Java/Spring?
   → Maven Central (dla projektów Gradle i Maven): search.maven.org
   → Spring Boot Starters: start.spring.io

2. Czy to częsty problem w TypeScript/React?
   → npm search dla funkcjonalności
   → Przykłady: react-hook-form (formularze), zod (walidacja),
     date-fns (daty), clsx (klasy CSS)

3. Czy istnieje MCP/narzędzie dla tego w GitHub Copilot?
   → Sprawdź dostępne tools

4. Czy jest referencyjna implementacja na GitHub?
   → Szukaj maintained OSS zanim napiszesz net-new kod
```

## Matrix Decyzyjna

| Sygnał                                       | Akcja                                        |
| -------------------------------------------- | -------------------------------------------- |
| Dokładne dopasowanie, maintained, MIT/Apache | **Adoptuj** — zainstaluj i użyj bezpośrednio |
| Częściowe dopasowanie, dobra podstawa        | **Rozszerz** — zainstaluj + cienki wrapper   |
| Kilka słabych dopasowań                      | **Komponuj** — połącz 2-3 małe pakiety       |
| Nic sensownego nie znaleziono                | **Buduj** — własny kod, ale świadomy badań   |

## Workflow Kompletny

```
1. NEED ANALYSIS
   Zdefiniuj potrzebną funkcjonalność
   Zidentyfikuj ograniczenia języka/frameworka

2. PARALLEL SEARCH
   ┌──────────┐ ┌──────────┐ ┌──────────┐
   │ Gradle/  │ │ GitHub / │ │ Spring   │
   │ npm/PyPI │ │  Web     │ │ Starters │
   └──────────┘ └──────────┘ └──────────┘

3. EVALUATE
   Oceń kandydatów (funkcjonalność, maintainability,
   community, docs, licencja, dependencies)

4. DECIDE
   ┌─────────┐ ┌──────────┐ ┌─────────┐
   │  Adoptuj│ │  Rozszerz│ │  Buduj  │
   └─────────┘ └──────────┘ └─────────┘

5. IMPLEMENT
   Zainstaluj pakiet / Napisz minimalny custom kod
```

## Skróty Wyszukiwania dla Stacku Java Spring DDD + React Vite

### Java Spring Boot

- Walidacja → `spring-boot-starter-validation` (Bean Validation 3)
- Auth → `spring-boot-starter-security` + `spring-security-oauth2-resource-server`
- Retry → `spring-retry` lub `resilience4j`
- Rate limiting → `bucket4j-spring-boot-starter`
- Email → `spring-boot-starter-mail`
- Testy → `spring-boot-starter-test`, `testcontainers`
- Dokumentacja API → `springdoc-openapi`
- Mapowanie → `mapstruct`

### TypeScript React

- Formularze → `react-hook-form` + `zod`
- Server state → `@tanstack/react-query`
- Client state → `zustand`
- Tabele → `@tanstack/react-table`
- Data/czas → `date-fns`
- Klasy CSS → `clsx` + `tailwind-merge`
- Toast → `sonner`
- Modal → `@radix-ui/react-dialog`

### Anti-Patterns

- **Pisanie utility bez sprawdzenia czy istnieje** — najczęstszy błąd
- **Over-customization** — owijanie biblioteki tak mocno że traci korzyści
- **Dependency bloat** — instalowanie masywnego pakietu dla jednej małej funkcji
- **Ignorowanie istniejącego kodu** — w repo może już istnieć helper którego potrzebujesz

## Integracja

- **@planner:** Planner powinien wywołać search-first PRZED Phase 1 (Architecture Review)
- **@ddd-architect:** Sprawdź istniejące biblioteki DDD (np. `aggregateframework`, event sourcing libs) zanim projektujesz własne
