---
description: 'Design DDD domain model — aggregates, value objects, domain events, bounded contexts, ubiquitous language.'
argument-hint: 'Opisz koncept domenowy lub feature do zaprojektowania'
mode: 'agent'

tools: [read, search]
---

Zaprojektuj model domeny DDD dla: $ARGUMENTS

Użyj agenta `@ddd-architect` i wykonaj:

1. **Ubiquitous Language** — zdefiniuj słownik pojęć domenowych
2. **Agregaty** — zidentyfikuj Aggregate Root i granice agregatu
3. **Value Objects** — co powinno być VO (immutable, bez identity)?
4. **Domain Events** — jakie zdarzenia biznesowe wystąpią?
5. **Bounded Context** — granice i relacje z innymi kontekstami

Wyjście:

- Szkielet kodu Java z Aggregate Root, Value Objects, Domain Events
- Diagram bounded contexts (ASCII/text)
- Checklist naruszeń DDD do weryfikacji
