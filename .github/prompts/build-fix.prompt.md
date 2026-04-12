---
description: 'Fix Gradle, Spring Boot startup, or TypeScript/Vite build errors.'
argument-hint: 'Wklej komunikat błędu lub zostaw puste żeby zdiagnozować automatycznie'
mode: 'agent'

tools: [read, search, execute]
---

Napraw błąd build: $ARGUMENTS

Użyj agenta `@java-build-resolver` i:

1. Odczytaj pełny komunikat błędu
2. Zidentyfikuj typ: Compilation / Gradle dependency / Spring Bean / JPA / TypeScript
3. Zlokalizuj problematyczny plik
4. Zastosuj fix
5. Uruchom build ponownie i zweryfikuj

Komendy diagnostyczne:

```bash
# Gradle (run from core/)
./gradlew clean build --stacktrace

# Spring Boot (run from core/)
./gradlew bootRun --stacktrace

# TypeScript
cd frontend && npx tsc -b --pretty false
```
