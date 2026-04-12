---
description: 'Create a detailed implementation plan before coding. Use for complex features or new DDD modules.'
argument-hint: 'Opisz feature lub moduł do zaplanowania'
mode: 'agent'

tools: [read, search]
---

Stwórz szczegółowy plan implementacji dla: $ARGUMENTS

Użyj agenta `@planner` i wykonaj pełny proces planowania:

1. **Analiza wymagań** — zrozum co ma być zbudowane
2. **Modelowanie DDD** — zidentyfikuj Aggregate Root, Value Objects, Domain Events, Bounded Context
3. **Plan TDD** — dla każdego kroku: najpierw test, potem implementacja
4. **Kolejność warstw** — domain → application → infrastructure → interfaces → frontend

Wyjście ma być konkretnym planem z plikami, klasami i zależnościami.
