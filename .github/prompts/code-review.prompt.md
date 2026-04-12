---
description: 'Run a comprehensive code review checking DDD violations, Spring patterns, security, and TypeScript quality.'
argument-hint: 'Ścieżka do pliku lub katalogu do przeglądu (opcjonalnie)'
mode: 'agent'

tools: [read, search, execute]
---

Wykonaj pełny code review dla: $ARGUMENTS

Uruchom odpowiedni przegląd:

**Dla kodu Java/Spring** — sprawdź:

- Naruszenia DDD (JPA w domain, brak Domain Events, publiczne settery na agregacie)
- Spring antypatterns (field injection, @Transactional na kontrolerze)
- JPA problemy (N+1, FetchType.EAGER, brak paginacji)
- Bezpieczeństwo (hardcoded secrets, brak @Valid, SQL injection)

**Dla kodu React/TypeScript** — sprawdź:

- XSS (dangerouslySetInnerHTML bez sanityzacji)
- TypeScript (any, non-null assertions)
- React antypatterns (mutacja state, brak key prop, useEffect loops)
- Wydajność (brak memo, brak lazy loading)

Raport: CRITICAL 🔴 → HIGH 🟠 → MEDIUM 🟡 → OK ✅
