---
description: 'Create a complete React component with TypeScript types, TanStack Query hook, and Vitest tests.'
argument-hint: 'Nazwa komponentu do stworzenia (np. OrderCard, PaymentForm)'
mode: 'agent'

tools: [read, search, edit]
---

Stwórz kompletny komponent React: $ARGUMENTS

Używając wzorców z `.github/skills/react-vite-patterns/SKILL.md` i `.github/skills/react-vite-testing/SKILL.md`, wygeneruj:

1. **`ComponentName.tsx`** — komponent z:
   - TypeScript `interface Props {}`
   - Obsługa stanów: loading, error, empty
   - Dostępność (aria-\* atrybuty, role)
   - Walidacja propsów

2. **`useComponentName.ts`** (jeśli potrzebuje danych) — TanStack Query hook z:
   - Query keys pattern
   - `useQuery` lub `useMutation`
   - Invalidacja cache po mutacji

3. **`ComponentName.test.tsx`** — testy Vitest + RTL:
   - Test happy path (dane wyświetlone)
   - Test loading state
   - Test error state
   - Test interakcji (klik, submit) z `userEvent`

4. **`index.ts`** — barrel export

Umieść pliki w `src/features/{feature}/` lub `src/shared/components/`.
