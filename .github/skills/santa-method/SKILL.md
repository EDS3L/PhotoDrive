---
name: santa-method
description: Wieloagentowa weryfikacja adversarialna — dwóch niezależnych reviewerów musi przejść zanim output trafi do produkcji. Używaj dla kodu produkcyjnego, endpointów bezpieczeństwa, krytycznej logiki biznesowej.
origin: ECC
---

# Santa Method — Weryfikacja Adversarialna

Wieloagentowy framework weryfikacji. Zrób listę, sprawdź dwa razy. Jeśli "niegrzeczne" — napraw aż będzie "grzeczne".

**Kluczowy insight:** agent sprawdzający własny output dzieli te same błędy, luki wiedzowe i systematyczne błędy co agent który go wygenerował. Dwóch niezależnych reviewerów bez wspólnego kontekstu eliminuje ten failure mode.

## Kiedy Używać

- Kod idzie na produkcję bez human review
- Endpointy bezpieczeństwa, autoryzacja, obsługa płatności
- Compliance, regulatory, brand constraints
- Krytyczna logika biznesowa (Aggregate Root, Domain Events)
- Batch generation gdzie spot-checking nie wystarczy

## Kiedy NIE Używać

Wewnętrzne drafty, exploratory research, zadania z deterministyczną weryfikacją (używaj verification-loop dla build/test/lint).

## Architektura 4-Fazowa

```
┌─────────────┐
│  GENERATOR  │  Faza 1: Wygeneruj output
└──────┬──────┘
       │
┌──────▼──────────────────────────┐
│  REVIEWER B ║ REVIEWER C        │  Faza 2: Sprawdź dwa razy
│  (brak wspólnego kontekstu!)    │  Równolegle, ten sam rubric
└──────┬──────────────┬───────────┘
       │              │
┌──────▼──────────────▼───────────┐
│  GATE: B PASS i C PASS → NICE   │  Faza 3: Naughty or Nice
│  Inaczej → NAUGHTY              │  Oba muszą przejść
└──────┬──────────────────────────┘
       │ NAUGHTY
┌──────▼──────────────────────────┐
│  FIX CYCLE (max 3 iteracje)     │  Faza 4: Fix Until Nice
│  Zbierz wszystkie flagi         │
│  Napraw WSZYSTKIE issues        │
│  Re-run OBAJ reviewerzy (fresh)  │
└─────────────────────────────────┘
```

## Rubric dla Kodu Java Spring DDD

Każde kryterium musi mieć obiektywny warunek pass/fail:

| Kryterium       | Warunek PASS                                                      | Sygnał FAIL                            |
| --------------- | ----------------------------------------------------------------- | -------------------------------------- |
| Type safety     | Brak `Object` bez rzutowania w domain, brak raw types             | Unchecked warnings                     |
| Error handling  | Wszystkie exceptional paths obsłużone                             | Brak `throws` lub puste catch blochy   |
| Security        | Brak hardcoded secrets, walidacja wejść, injection prevention     | `@Query` z konkatenacją stringa        |
| DDD correctness | Domena nie importuje Spring/JPA, Aggregate Root kontroluje dostęp | `@Entity` w domain/, publiczne settery |
| Test coverage   | Testy dla nowych ścieżek                                          | Brak testu dla public methods          |

## Rubric dla Kodu TypeScript React

| Kryterium        | Warunek PASS                                    | Sygnał FAIL                            |
| ---------------- | ----------------------------------------------- | -------------------------------------- |
| Type safety      | Brak `any` bez komentarza uzasadniającego       | Niejawne `any`, brak null handling     |
| XSS prevention   | Brak `dangerouslySetInnerHTML` bez sanitization | Interpolacja user input do HTML        |
| State management | Zustand/TanStack Query używane poprawnie        | Direct setState w event handlerach     |
| Validation       | Zod schema na formularzu                        | Brak walidacji przy submit             |
| Accessibility    | ARIA labels, keyboard navigation                | Brak alt na img, brak focus management |

### Pattern A: Sequential Inline z Explicit Context Reset

1. Wygeneruj output normalne
2. **Nowy prompt:** "Jesteś REVIEWER B. Oceń TYLKO ten output według tego rubrica. Twoją rolą jest znajdowanie problemów, nie zatwierdzanie. [OUTPUT] [RUBRIC]"
3. Zapisz wyniki verbatim
4. **Nowy prompt:** "Jesteś REVIEWER C. Oceń TYLKO ten output według tego rubrica. Nie widziałeś oceny REVIEWER B. [OUTPUT] [RUBRIC]"
5. Porównaj obie recenzje, napraw, powtórz

### Pattern B: @agent Delegation

Wywołaj `@java-spring-reviewer` i `@security-reviewer` z tym samym outputem — każdy działa jako niezależny reviewer z własnym kontekstem.

## Format Werdyktu

```json
{
  "verdict": "PASS" | "FAIL",
  "critical_issues": ["konkretny problem — zacytuj dokładnie"],
  "suggestions": ["opcjonalne usprawnienia"],
  "checks": [
    {"criterion": "...", "result": "PASS|FAIL", "detail": "..."}
  ]
}
```

## Warunki Zatrzymania

- Max 3 iteracje fix cycle → eskaluj do człowieka
- Jeśli ten sam issue wraca po fix → problem fundamentalny, nie powierzchniowy
- Każda runda review używa FRESH reviewerów (bez pamięci poprzednich rund)

## Metryki do Śledzenia

- **First-pass rate:** % outputów przechodzących Santa w pierwszej rundzie (cel: >70%)
- **Mean iterations:** średnia rund do NICE (cel: <1.5)
- **Escape rate:** issues znajdowane po wysyłce, które Santa powinna złapać (cel: 0)

## Integracja

- **verification-loop** — uruchom NAJPIERW (deterministyczne checks), Santa POTEM (semantyczne checks)
- **council** — Santa dla weryfikacji kodu, Council dla decyzji architektonicznych
