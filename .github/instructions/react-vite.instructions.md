---
description: 'Use when working with React/Vite TypeScript code. Covers component structure, state, routing, forms, and TypeScript strictness.'
applyTo: '**/*.{tsx,ts,jsx}'
---

# React + Vite + TypeScript Rules

Najpierw zachowaj konwencje już przyjęte w projekcie. Używaj tych zasad jako domyślnych wzorców dla nowoczesnych aplikacji React + Vite, ale nie mieszaj równoległych podejść tylko dlatego, że istnieje nowsza alternatywa.

## Struktura Projektu (Feature-Based)

```
src/
├── features/{feature}/
│   ├── components/    ← komponenty featura
│   ├── hooks/         ← useQuery/useMutation hooks
│   ├── api/           ← axios calls
│   ├── types/         ← TypeScript interfaces
│   └── index.ts       ← barrel export
├── shared/            ← reużywalne komponenty/hooki
└── app/               ← router, store, providers
```

## State Management

| Typ Danych            | Narzędzie                                          |
| --------------------- | -------------------------------------------------- |
| Dane z serwera        | TanStack Query lub istniejąca warstwa server-state |
| Stan globalny klienta | Zustand, Context lub istniejący store              |
| Stan lokalny UI       | `useState`                                         |
| Formularz             | React Hook Form + Zod lub inny schema-based stack  |

**Nie przechowuj** długowiecznych danych z API w przypadkowym `useState` jeśli projekt ma już dedykowaną warstwę pobierania i cache.

## Routing / Styling / React APIs

- Jeśli projekt używa React Router, zachowaj istniejący styl routingu; component routes są dobrym wyborem dla prostych SPA, a data routers wtedy, gdy naprawdę potrzebujesz `loader` / `action`
- Jeśli projekt używa Tailwinda, dopasuj się do jego wersji i stylu konfiguracji; Tailwind 4 często jest CSS-first, ale `tailwind.config.*` nadal może być poprawne w starszych lub bardziej złożonych aplikacjach
- Dla kosztownych interakcji rozważ `startTransition` i `useDeferredValue`; `useEffectEvent` stosuj tylko wtedy, gdy wersja React i standard zespołu to wspierają

## Immutability (CRITICAL)

```typescript
// ŹLE
items.push(newItem);
setItems(items);

// DOBRZE
setItems((prev) => [...prev, newItem]);
```

## TypeScript

- Preferuj `unknown` zamiast `any`; jeśli `any` jest konieczne, ogranicz jego zasięg i uzasadnij to komentarzem
- Zawsze typuj propsy komponentów (`interface Props {}`)
- Zod schema jako source of truth: `type Order = z.infer<typeof orderSchema>`
- Explicit return types dla hooków (`useOrders(): UseQueryResult<...>`)

## Formularze — React Hook Form + Zod

```typescript
const schema = z.object({
	customerId: z.string().min(1, 'Wymagane'),
	quantity: z.number().int().positive().max(100),
});
type FormData = z.infer<typeof schema>;

const {
	register,
	handleSubmit,
	formState: { errors },
} = useForm<FormData>({
	resolver: zodResolver(schema),
});
```

## TanStack Query — Query Keys

```typescript
export const orderQueryKeys = {
	all: ['orders'] as const,
	lists: () => [...orderQueryKeys.all, 'list'] as const,
	detail: (id: string) => [...orderQueryKeys.all, 'detail', id] as const,
};
```

## Bezpieczeństwo

- **Nigdy** `dangerouslySetInnerHTML` bez `DOMPurify.sanitize()`
- Tokeny: preferuj httpOnly cookies, nie localStorage
- Używaj mechanizmu env toolchaina projektu; w Vite będzie to zwykle `VITE_`, ale niezależnie od narzędzia **nie** przechowuj tam sekretów backend
- Route guards dla chronionych stron

## Vite

```typescript
// vite.config.ts — przykładowy proxy i pluginy
plugins: [react(), tailwindcss()], // tailwindcss() jeśli projekt używa pluginu Tailwind 4

server: {
    proxy: {
        '/api': { target: 'http://localhost:8080', changeOrigin: true }
    }
}
```
