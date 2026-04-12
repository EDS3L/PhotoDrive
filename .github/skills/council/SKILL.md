---
name: council
description: Zwołaj cztery głosy dla ambiwalentnych decyzji — Architect, Skeptic, Pragmatist, Critic. Używaj gdy istnieje wiele wiarygodnych ścieżek i potrzebujesz ustrukturyzowanej niezgody zanim wybierzesz.
origin: ECC
---

# Council — Decyzje pod Ambiguity

Zwołaj cztery doradcze głosy dla ambiwalentnych decyzji, tradeoffów i ocen go/no-go. Jest to **podejmowanie decyzji pod niepewnością**, nie code review, planowanie implementacji ani projekt architektury.

## Kiedy Używać

- Decyzja ma wiele wiarygodnych ścieżek i brak oczywistego zwycięzcy
- Potrzebujesz explicitnego surfowania tradeoffów
- Ryzyko anchoring bias z rozmowy jest realne
- Decyzja go/no-go skorzystałaby z adversarialnego challenge

**Przykłady:**

- monorepo vs polyrepo
- ship now vs hold for polish
- feature flag vs full rollout
- REST vs gRPC dla komunikacji serwisów
- CQRS z osobną bazą read vs jeden model
- PostgreSQL vs MongoDB dla nowego modułu

## Kiedy NIE Używać

| Zamiast Council                      | Użyj                    |
| ------------------------------------ | ----------------------- |
| Weryfikacja czy output jest poprawny | `santa-method`          |
| Podział na kroki implementacji       | `@planner`              |
| Projekt architektury systemu         | `@ddd-architect`        |
| Review kodu pod kątem bugów          | `@java-spring-reviewer` |
| Proste pytania faktyczne             | Odpowiedz bezpośrednio  |
| Oczywiste zadania wykonawcze         | Po prostu zrób          |

## Role Głosów

| Głos           | Perspektywa                                                           |
| -------------- | --------------------------------------------------------------------- |
| **Architect**  | poprawność, maintainability, długoterminowe implikacje                |
| **Skeptic**    | kwestionuje założenia, szuka najprostszej wiarygodnej alternatywy     |
| **Pragmatist** | szybkość shipowania, impact dla użytkownika, rzeczywistość operacyjna |
| **Critic**     | edge cases, downside risk, sposoby w jakie plan może się nie udać     |

## Workflow

### 1. Wyodrębnij właściwe pytanie

Zredukuj decyzję do jednego explicitnego pytania:

- Co decydujemy?
- Jakie constraints mają znaczenie?
- Co liczy się jako sukces?

### 2. Zbierz tylko niezbędny kontekst

Coding decision: zbierz relevantne pliki, snippety, metryki — zachowaj kompaktowo.  
Strategiczna decyzja: pomiń snippety repo chyba że materialnie zmieniają odpowiedź.

### 3. Sformułuj pozycję Architekta NAJPIERW

Zanim przeczytasz inne głosy, zapisz:

- Twoją wstępną pozycję
- Trzy najsilniejsze powody dla niej
- Główne ryzyko w preferowanej ścieżce

To zapobiega temu, że synteza jest tylko echem zewnętrznych głosów.

### 4. Uruchom trzy niezależne głosy

Każdy subagent otrzymuje TYLKO:

- Pytanie decyzyjne
- Kompaktowy kontekst
- Jego ścisłą rolę
- **Zero historii konwersacji**

**Prompt template:**

```
Jesteś [ROLA] w czteroosobowej radzie decyzyjnej.

Pytanie: [pytanie decyzyjne]

Kontekst: [tylko relevantne snippety lub constraints]

Odpowiedz:
1. Pozycja — 1-2 zdania
2. Uzasadnienie — 3 zwięzłe bullets
3. Ryzyko — największe ryzyko w Twojej rekomendacji
4. Niespodzianka — jedna rzecz którą inne głosy mogą przeoczyć

Bądź bezpośredni. Bez hedge. Max 300 słów.
```

**Emphasis per rola:**

- **Skeptic:** kwestionuj framing, wytykaj założenia, proponuj najprostszą wiarygodną alternatywę
- **Pragmatist:** optymalizuj pod szybkość, prostotę i realne wykonanie
- **Critic:** surfuj downside risk, edge cases i powody dla których plan może nie zadziałać

### 5. Syntezuj z guardrails

- Nie odrzucaj zewnętrznego głosu bez wyjaśnienia dlaczego
- Jeśli głos zewnętrzny zmienił twoją rekomendację — powiedz to explicitnie
- Zawsze uwzględnij najsilniejszą niezgodę, nawet jeśli ją odrzucasz
- Jeśli dwa głosy ustawiają się przeciwko twojej wstępnej pozycji — traktuj to jako realny sygnał

### 6. Zaprezentuj kompaktowy werdykt

```markdown
## Council: [krótki tytuł decyzji]

**Architect:** [1-2 zdania pozycji]
[1 linia dlaczego]

**Skeptic:** [1-2 zdania pozycji]
[1 linia dlaczego]

**Pragmatist:** [1-2 zdania pozycji]
[1 linia dlaczego]

**Critic:** [1-2 zdania pozycji]
[1 linia dlaczego]

### Werdykt

- **Consensus:** [gdzie się zgadzają]
- **Najsilniejsza niezgoda:** [najważniejsza rozbieżność]
- **Premise check:** [czy Skeptic zakwestionował samo pytanie?]
- **Rekomendacja:** [syntetyczna ścieżka]
```

## Przykład — Java Spring DDD

```
Pytanie: Czy używać CQRS z osobną bazą read model (CQRS + Event Sourcing)
         czy uproszczonego CQRS z jedną bazą danych dla nowego modułu Order?

Architect: Person recommends full CQRS+ES for auditability + eventual consistency
Skeptic:   Questions whether order volume justifies operational complexity
Pragmatist: Simplified CQRS delivers in 2 weeks vs 6 weeks for full ES
Critic:     Full ES without team ES experience = high operational risk

Verdict: Simplified CQRS, jednodbazowy — możliwość migracji do ES gdy volume uzasadni
```

## Anti-Patterns

- Używanie council do code review
- Używanie council gdy zadanie to prosta implementacja
- Podawanie subagentom całej historii konwersacji
- Ukrywanie niezgody w finalnym werdykcie
- Persystowanie każdej decyzji jako notatki niezależnie od ważności
