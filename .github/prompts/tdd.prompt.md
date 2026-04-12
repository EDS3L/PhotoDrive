---
description: 'Guide through TDD workflow for a class or feature. Generates failing test first, then minimal implementation.'
argument-hint: 'Nazwa klasy lub funkcji do implementacji przez TDD'
mode: 'agent'

tools: [read, search, edit]
---

Przeprowadź TDD workflow dla: $ARGUMENTS

Wykonaj workflow RED → GREEN → REFACTOR:

### 1. RED — Napisz failujący test

- Dla Java: `@Test` z JUnit 5, nazwij `should_[wynik]_when_[warunek]`
- Dla React: `it('should ...')` z Vitest + RTL
- Test ma FAILOWAĆ zanim napiszesz implementację

### 2. GREEN — Minimalna implementacja

- Napisz tylko tyle kodu ile potrzeba żeby test przeszedł
- Nie optymalizuj, nie dodawaj dodatkowych funkcji

### 3. REFACTOR

- Popraw design nie zmieniając zachowania
- Zweryfikuj pokrycie ≥ 80%

Pamiętaj:

- Domain tests bez Spring (czyste unit testy)
- `@WebMvcTest` dla kontrolerów
- `@DataJpaTest` dla repozytoriów
- Fixtures zamiast inline danych testowych
