---
name: blueprint
description: Zamień jednoliniowy cel w krok-po-kroku plan budowy dla projektów multi-sesyjnych i multi-agentowych. Każdy krok ma self-contained context brief — fresh agent może go wykonać cold. Używaj dla EPIC-scope zadań: migracje, nowe moduły DDD, refaktor architektury.
origin: ECC
---

# Blueprint — Generator Planu Budowy

Zamień jednoliniowy cel w krok-po-kroku plan budowy który dowolny agent kodujący może wykonać cold.

## Kiedy Używać

- Podział dużej funkcji na wiele PRów z jasną kolejnością zależności
- Planowanie refaktoru lub migracji obejmującej wiele sesji
- Koordynacja równoległych workstreamów
- Dowolne zadanie gdzie utrata kontekstu między sesjami spowodowałaby przepracowanie

**Nie używaj** dla zadań ukończalnych w jednym PR, mniej niż 3 tool calls, lub gdy user mówi "po prostu zrób to."

## Pipeline 5-Fazowy

### Faza 1: Research

- Sprawdź strukturę projektu, istniejące plany i pliki pamięci
- Identyfikuj istniejące wzorce (packages, architektura warstw, konwencje nazewnicze)
- Waliduj założenia przed projektowaniem

### Faza 2: Design

- Podziel cel na kroki wielkości jednego PR (3-12 typowo)
- Przypisz krawędzie zależności (co blokuje co)
- Zidentyfikuj równoległe vs seryjne kroki
- Przypisz profil wykonawcy per krok (implementacja / architektura / review)
- Zdefiniuj strategię rollback per krok

### Faza 3: Draft

- Napisz self-contained plik Markdown planu
- Każdy krok zawiera:
  - **Context brief** — wszystko co fresh agent potrzebuje (pliki, konwencje, cel)
  - **Task list** — konkretne zadania
  - **Verification commands** — jak sprawdzić czy krok jest ukończony
  - **Exit criteria** — definicja ukończenia

### Faza 4: Review (Adversarial Gate)

- Sprawdź pod kątem: kompletności, poprawności zależności, anti-patterns
- Napraw wszystkie critical findings zanim sfinalizujesz

### Faza 5: Register

- Zapisz plan do `plans/` w repo
- Zaprezentuj licznik kroków i summary równoległości

## Format Krok Planu

````markdown
## Krok N: [Tytuł]

**Execution profile:** Implementacja | Architektura/Design | Review
**Zależy od:** Krok X, Krok Y
**Może być równoległy z:** Krok Z

### Context Brief

[Wszystko co fresh agent potrzebuje do wykonania tego kroku bez czytania poprzednich.
Obejmuje: relevantne pliki, konwencje, architekturę warstw DDD, wzorce nazewnicze]

### Zadania

- [ ] Zadanie 1
- [ ] Zadanie 2

### Komendy Weryfikacji

```bash
cd core && ./gradlew test --tests fully.qualified.ClassName
cd frontend && npm run build
cd frontend && npx vitest run
```
````

### Kryteria Wyjścia

- Wszystkie testy przechodzą
- Brak naruszeń DDD (domain nie importuje Spring/JPA)
- Coverage ≥80%
- [Specificzne dla kroku warunki]

```

## Przykład — Nowy Moduł DDD w Spring Boot

```

Cel: "Implement Order Management bounded context"

Plan:
Krok 1: Domain Layer — Order aggregate root, OrderItem entity, OrderStatus VO
└→ Zależy od: nic

Krok 2: Repository interface + Infrastructure JPA implementation
└→ Zależy od: Krok 1

Krok 3: Application Layer — CreateOrderCommand, PlaceOrderHandler
└→ Zależy od: Krok 1
└→ Równoległe z: Krok 2 (niezależne pliki)

Krok 4: REST Controller, DTOs, mappers
└→ Zależy od: Krok 3

Krok 5: Integration tests + schema migrations
└→ Zależy od: Krok 2, 3, 4

```

## Kluczowe Zasady

- **Cold-start execution** — Każdy krok ma self-contained context brief. Zero potrzeby prior context.
- **Parallel step detection** — Identyfikuj kroki bez shared files lub output dependencies
- **Plan mutation protocol** — Kroki mogą być dzielone, wstawiane, pomijane, reorderowane z audit trail
- **Adversarial review gate** — Każdy plan review przez niezależnego reviewera

## Anti-Patterns

- Kroki z niejasnym exit criteria ("Zaimplementuj cokolwiek jest potrzebne")
- Brakujące zależności (Krok 4 zakłada output Kroku 2 który nie jest zadeklarowany)
- Monolityczne kroki — jeśli krok zajmuje >1 PR, podziel go
- Za dużo kroków dla prostego zadania — jeśli <3 tool calls, nie potrzebujesz blueprint
```
