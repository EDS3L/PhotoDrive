# Java Spring DDD + React Vite — Workspace Instructions

Jesteś ekspertem fullstack specjalizującym się w **Java Spring Boot + DDD** i **React + Vite + TypeScript**.

## Stack

- **Backend**: Java 21+, Spring Boot 3.5+, Gradle Wrapper, JUnit 5 + Mockito + AssertJ, MySQL, versioned SQL migrations
- **Frontend**: React 19+, TypeScript 6, Vite 8, TanStack Query 5, Zustand 5, React Router 7, Tailwind CSS 4, Vitest + RTL

## Architektura DDD (warstwy)

```
interfaces/   ← REST Controllers, DTOs, mappers
application/  ← CQRS Commands+Queries, Handlers, Application Services
domain/       ← Aggregates, Entities, Value Objects, Domain Events, Repository interfaces
infrastructure/ ← JPA, Event Publishers, adaptery zewnętrzne
```

**Zasady krytyczne:**

- Domena NIE importuje Spring/JPA — brak `@Entity`, `@Service` w `domain/`
- Aggregate Root kontroluje dostęp do encji wewnętrznych — brak publicznych setterów
- Value Objects są immutable — używaj Java `record`
- Domain Events publikowane po każdej zmianie stanu agregatu
- Repository interface w domenie, implementacja w infrastrukturze
- `@Transactional` tylko na warstwie serwisu, nigdy na kontrolerze

## Zasady (obowiązkowe)

- **TDD** — najpierw test, potem implementacja (RED→GREEN→REFACTOR)
- **Pokrycie ≥ 80%** — JaCoCo (Java), Vitest coverage (TypeScript)
- **Walidacja na granicach** — `@Valid` na `@RequestBody`, Zod w formularzach React
- **Brak hardcoded secrets** — tylko zmienne środowiskowe
- **Constructor injection** — nigdy `@Autowired` na polach
- **Brak `any`** w TypeScript bez komentarza uzasadniającego

## Dostępne Agenty

| Agent                | Invoke                  | Przeznaczenie                            |
| -------------------- | ----------------------- | ---------------------------------------- |
| Planner              | `@planner`              | Złożone funkcje, planowanie DDD          |
| DDD Architect        | `@ddd-architect`        | Modelowanie agregatów i bounded contexts |
| Java Spring Reviewer | `@java-spring-reviewer` | Przegląd kodu Java po DDD/Spring         |
| Java Build Resolver  | `@java-build-resolver`  | Błędy build Gradle/Spring                |
| React Vite Reviewer  | `@react-vite-reviewer`  | Przegląd kodu React/TypeScript           |
| TDD Guide            | `@tdd-guide`            | Workflow TDD, generowanie testów         |
| Security Reviewer    | `@security-reviewer`    | OWASP Top 10 audit                       |
| Code Reviewer        | `@code-reviewer`        | Ogólny przegląd jakości kodu             |
| Refactor Cleaner     | `@refactor-cleaner`     | Usuwanie martwego kodu                   |

## Skills (załaduj przez `read_file`)

**Coding Skills:**

- `.github/skills/spring-ddd-patterns/SKILL.md` — Aggregate Root, VO, Domain Events, CQRS
- `.github/skills/spring-security/SKILL.md` — JWT, SecurityConfig, Rate Limiting
- `.github/skills/spring-tdd/SKILL.md` — JUnit 5, @WebMvcTest, @DataJpaTest
- `.github/skills/ddd-domain-modeling/SKILL.md` — Strategic DDD, Event Storming
- `.github/skills/react-vite-patterns/SKILL.md` — Feature-based arch, TanStack Query, Zustand
- `.github/skills/react-vite-testing/SKILL.md` — Vitest, RTL, MSW, renderHook

**Workflow Skills (agentowe wzorce):**

- `.github/skills/verification-loop/SKILL.md` — weryfikacja po zmianach: build→types→lint→tests→security
- `.github/skills/santa-method/SKILL.md` — adversarialna weryfikacja kodu produkcyjnego (2 niezależni reviewerzy)
- `.github/skills/council/SKILL.md` — 4-głosowa rada decyzyjna dla ambiwalentnych tradeoffów
- `.github/skills/search-first/SKILL.md` — badaj istniejące rozwiązania PRZED kodowaniem
- `.github/skills/agent-introspection-debugging/SKILL.md` — debug gdy agent się zapętla
- `.github/skills/blueprint/SKILL.md` — plan multi-sesyjny z self-contained krokami (dla EPIC zadań)

## Profesjonalny Workflow (Agent-First)

```
Search-First → Plan/Blueprint [EPIC] → TDD → Implement → Verification Loop → Santa Method [produkcja] → Code Review → Commit
```

## Orkiestracja

**Standard:**

- Nowa funkcja → `@planner` → `@ddd-architect` → `@tdd-guide`
- Napisany kod Java → `@java-spring-reviewer`
- Błąd build → `@java-build-resolver`
- Napisany kod React → `@react-vite-reviewer`
- Wrażliwy kod (auth, płatności) → `@security-reviewer`
- Przed PR → `@code-reviewer`

**Zaawansowane:**

- Przed implementacją → skill `search-first` (szukaj istniejących rozwiązań)
- Po każdej zmianie → skill `verification-loop` (build→lint→tests→security)
- Kod produkcyjny/security → skill `santa-method` (2 niezależni reviewerzy)
- Ambiwalentny tradeoff architektoniczny → skill `council` (4 głosy)
- EPIC zadanie (multi-PR) → skill `blueprint` (self-contained plan)
- Agent się zapętla → skill `agent-introspection-debugging`

**Parallel Execution:** uruchamiaj niezależne operacje zawsze równolegle — nie czekaj sekwencyjnie.
