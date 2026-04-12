---
name: react-vite-patterns
description: Wzorce React z Vite — architektura feature-based, komponenty, hooki, routing, zarządzanie stanem, formularze i styling. Używaj dla architektury frontendu React.
origin: java-spring-ddd-pro
---

# React + Vite Wzorce

Nowoczesne wzorce frontend dla React z Vite. Traktuj konkretne wersje i biblioteki jako zależne od projektu: najpierw sprawdź, czy aplikacja używa React 18/19, React Router 6/7, Tailwind 3/4, TanStack Query, Zustand albo innych odpowiedników.

Najpierw zachowaj styl już obecny w projekcie. Jeśli repo ma ustalone wzorce routera, state management albo konfiguracji CSS, rozszerzaj je zamiast mieszać równoległe podejścia.

## Kiedy Aktywować

- Tworzenie nowego projektu React z Vite
- Projektowanie architektury feature-based
- Implementacja zarządzania stanem (server state i client state)
- Konfigurowanie routingu z React Router 6/7 albo wzorcem już przyjętym w projekcie
- Tworzenie formularzy z walidacją (React Hook Form + Zod)

---

## Konfiguracja Projektu (Vite + TypeScript)

```bash
# Utwórz projekt
npm create vite@latest my-app -- --template react-ts
cd my-app
npm install

# Zależności produkcyjne
npm install react-router-dom @tanstack/react-query zustand axios
npm install react-hook-form @hookform/resolvers zod
npm install clsx tailwind-merge lucide-react framer-motion

# Tailwind CSS 3/4
npm install -D tailwindcss @tailwindcss/vite

# Testy (opcjonalnie, ale zalecane)
npm install -D vitest @vitest/coverage-v8 @testing-library/react @testing-library/user-event
npm install -D @testing-library/jest-dom jsdom @types/node
```

W nowszych projektach Tailwind 4 bywa CSS-first: dla prostych aplikacji konfiguracja może siedzieć bezpośrednio w `src/index.css` przez `@import 'tailwindcss'` i `@theme`. W starszych albo bardziej rozbudowanych projektach `tailwind.config.js` lub `tailwind.config.ts` nadal jest poprawnym wyborem. Nie migruj stylu konfiguracji bez wyraźnej potrzeby.

---

## Struktura Projektu (Feature-Based — DDD aligned)

```
src/
├── features/                          ← Każdy feature = jeden Bounded Context
│   ├── orders/                        ← Feature: Zamówienia
│   │   ├── components/
│   │   │   ├── OrderList.tsx
│   │   │   ├── OrderCard.tsx
│   │   │   └── CreateOrderForm.tsx
│   │   ├── hooks/
│   │   │   ├── useOrders.ts           ← TanStack Query hooks
│   │   │   └── useCreateOrder.ts
│   │   ├── api/
│   │   │   └── ordersApi.ts           ← Axios calls
│   │   ├── types/
│   │   │   └── order.types.ts         ← TypeScript interfaces
│   │   ├── utils/
│   │   │   └── orderUtils.ts
│   │   └── index.ts                   ← Publiczne API featura
│   └── auth/
│       ├── components/
│       ├── hooks/
│       ├── api/
│       └── index.ts
├── shared/
│   ├── components/
│   │   ├── ui/                        ← Primitive UI (Button, Input, Card)
│   │   └── layout/                    ← Layouty strony
│   ├── hooks/
│   │   ├── useDebounce.ts
│   │   └── useLocalStorage.ts
│   ├── api/
│   │   └── apiClient.ts               ← Skonfigurowany Axios
│   └── types/
│       └── common.types.ts
├── app/
│   ├── router/
│   │   └── AppRoutes.tsx              ← React Router konfiguracja
│   ├── store/
│   │   └── authStore.ts               ← Zustand auth store
│   ├── providers/
│   │   └── AppProviders.tsx           ← BrowserRouter + QueryClient providers
│   └── App.tsx
├── main.tsx
└── vite-env.d.ts
```

---

## API Client (Axios)

```typescript
// src/shared/api/apiClient.ts
import axios from 'axios';

const apiClient = axios.create({
	baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1',
	timeout: 10_000,
	headers: {
		'Content-Type': 'application/json',
	},
});

// Request interceptor — dodaj JWT token
apiClient.interceptors.request.use((config) => {
	const token = useAuthStore.getState().token;
	if (token) {
		config.headers.Authorization = `Bearer ${token}`;
	}
	return config;
});

// Response interceptor — obsługa 401
apiClient.interceptors.response.use(
	(response) => response,
	(error) => {
		if (error.response?.status === 401) {
			useAuthStore.getState().logout();
			window.location.href = '/login';
		}
		return Promise.reject(error);
	},
);

export default apiClient;
```

---

## Feature API Layer

```typescript
// src/features/orders/api/ordersApi.ts
import apiClient from '@/shared/api/apiClient';
import type {
	Order,
	CreateOrderRequest,
	OrdersPage,
} from '../types/order.types';

export const ordersApi = {
	getOrders: async (page = 0, size = 20): Promise<OrdersPage> => {
		const { data } = await apiClient.get<OrdersPage>('/orders', {
			params: { page, size },
		});
		return data;
	},

	getOrderById: async (orderId: string): Promise<Order> => {
		const { data } = await apiClient.get<Order>(`/orders/${orderId}`);
		return data;
	},

	createOrder: async (request: CreateOrderRequest): Promise<{ id: string }> => {
		const { data } = await apiClient.post<{ id: string }>('/orders', request);
		return data;
	},

	confirmOrder: async (orderId: string): Promise<void> => {
		await apiClient.put(`/orders/${orderId}/confirm`);
	},

	cancelOrder: async (orderId: string, reason: string): Promise<void> => {
		await apiClient.delete(`/orders/${orderId}`, { data: { reason } });
	},
};
```

---

## TanStack Query Hooks

```typescript
// src/features/orders/hooks/useOrders.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ordersApi } from '../api/ordersApi';
import type { CreateOrderRequest } from '../types/order.types';

// Query keys — centralne zarządzanie
export const orderQueryKeys = {
	all: ['orders'] as const,
	lists: () => [...orderQueryKeys.all, 'list'] as const,
	list: (page: number) => [...orderQueryKeys.lists(), page] as const,
	detail: (id: string) => [...orderQueryKeys.all, 'detail', id] as const,
};

// Pobieranie listy zamówień
export function useOrders(page = 0) {
	return useQuery({
		queryKey: orderQueryKeys.list(page),
		queryFn: () => ordersApi.getOrders(page),
		staleTime: 1000 * 60, // 1 minuta
	});
}

// Pobieranie szczegółów zamówienia
export function useOrder(orderId: string) {
	return useQuery({
		queryKey: orderQueryKeys.detail(orderId),
		queryFn: () => ordersApi.getOrderById(orderId),
		enabled: !!orderId,
	});
}

// Tworzenie zamówienia
export function useCreateOrder() {
	const queryClient = useQueryClient();

	return useMutation({
		mutationFn: (request: CreateOrderRequest) => ordersApi.createOrder(request),
		onSuccess: () => {
			// Unieważnij cache listy zamówień
			queryClient.invalidateQueries({ queryKey: orderQueryKeys.lists() });
		},
	});
}

// Potwierdzanie zamówienia
export function useConfirmOrder() {
	const queryClient = useQueryClient();

	return useMutation({
		mutationFn: (orderId: string) => ordersApi.confirmOrder(orderId),
		onSuccess: (_, orderId) => {
			// Zaktualizuj cache konkretnego zamówienia
			queryClient.invalidateQueries({
				queryKey: orderQueryKeys.detail(orderId),
			});
			queryClient.invalidateQueries({ queryKey: orderQueryKeys.lists() });
		},
	});
}
```

---

## Zustand Store (Stan Klienta)

```typescript
// src/app/store/authStore.ts
import { create } from 'zustand';

interface AuthState {
	isAuthenticated: boolean;
	email: string | null;
	setAuthenticated: (authenticated: boolean, email?: string) => void;
	clear: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
	isAuthenticated: false,
	email: null,
	setAuthenticated: (authenticated, email) =>
		set({ isAuthenticated: authenticated, email: email ?? null }),
	clear: () => set({ isAuthenticated: false, email: null }),
}));
```

Persistuj tylko niesekretne preferencje UI. Access token trzymaj w httpOnly cookie albo w pamięci procesu, nie w localStorage.

---

## React Router 6/7 — Component Routes i Data Routers

```typescript
// src/app/router/AppRoutes.tsx
import { Routes, Route, Navigate, Outlet } from 'react-router-dom'
import { useAuthStore } from '@/app/store/authStore'

function ProtectedRoute() {
    const isAuthenticated = useAuthStore((state) => state.isAuthenticated)
    return isAuthenticated ? <Outlet /> : <Navigate to="/panel-login" replace />
}

export function AppRouter() {
    return (
        <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/panel-login" element={<PanelLoginPage />} />

            <Route element={<ProtectedRoute />}>
                <Route path="/admin" element={<AdminDashboard />} />
                <Route path="/photographer" element={<PhotographerDashboard />} />
            </Route>

            <Route path="*" element={<NotFoundPage />} />
        </Routes>
    )
}
```

W nowoczesnym React Router nadal możesz używać `BrowserRouter` + `Routes`. Data routery (`createBrowserRouter`) wybieraj wtedy, gdy naprawdę potrzebujesz `loader`, `action`, deferred data albo centralnego modelu route modules. Jeśli projekt ma już przyjęty jeden styl, trzymaj się go konsekwentnie.

---

## Formularze (React Hook Form + Zod)

```typescript
// src/features/orders/components/CreateOrderForm.tsx
import { useForm, useFieldArray } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useCreateOrder } from '../hooks/useCreateOrder'
import { useNavigate } from 'react-router-dom'

// Schemat walidacji Zod — source of truth dla typów
const orderLineSchema = z.object({
    productId: z.string().min(1, 'Wybierz produkt'),
    quantity: z.number()
        .int('Ilość musi być liczbą całkowitą')
        .positive('Ilość musi być większa od 0')
        .max(100, 'Maksymalnie 100 sztuk'),
    price: z.number().positive('Cena musi być większa od 0'),
})

const createOrderSchema = z.object({
    customerId: z.string().min(1, 'Pole wymagane'),
    lines: z.array(orderLineSchema).min(1, 'Dodaj co najmniej jedną pozycję'),
})

type CreateOrderFormData = z.infer<typeof createOrderSchema>

export function CreateOrderForm() {
    const navigate = useNavigate()
    const createOrder = useCreateOrder()

    const { register, control, handleSubmit, formState: { errors, isSubmitting } } =
        useForm<CreateOrderFormData>({
            resolver: zodResolver(createOrderSchema),
            defaultValues: {
                customerId: '',
                lines: [{ productId: '', quantity: 1, price: 0 }],
            }
        })

    const { fields, append, remove } = useFieldArray({ control, name: 'lines' })

    const onSubmit = async (data: CreateOrderFormData) => {
        const result = await createOrder.mutateAsync(data)
        navigate(`/orders/${result.id}`)
    }

    return (
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div>
                <label htmlFor="customerId">ID Klienta</label>
                <input
                    id="customerId"
                    {...register('customerId')}
                    aria-describedby={errors.customerId ? 'customerId-error' : undefined}
                />
                {errors.customerId && (
                    <p id="customerId-error" role="alert">{errors.customerId.message}</p>
                )}
            </div>

            {fields.map((field, index) => (
                <div key={field.id}>
                    <input {...register(`lines.${index}.productId`)} placeholder="ID Produktu" />
                    <input
                        type="number"
                        {...register(`lines.${index}.quantity`, { valueAsNumber: true })}
                        placeholder="Ilość"
                    />
                    {errors.lines?.[index]?.quantity && (
                        <p role="alert">{errors.lines[index].quantity?.message}</p>
                    )}
                    <button type="button" onClick={() => remove(index)}>Usuń</button>
                </div>
            ))}

            <button type="button" onClick={() => append({ productId: '', quantity: 1, price: 0 })}>
                Dodaj pozycję
            </button>

            {createOrder.isError && (
                <p role="alert" className="text-red-500">
                    Błąd tworzenia zamówienia. Spróbuj ponownie.
                </p>
            )}

            <button type="submit" disabled={isSubmitting || createOrder.isPending}>
                {createOrder.isPending ? 'Tworzenie...' : 'Utwórz zamówienie'}
            </button>
        </form>
    )
}
```

---

## React 18/19 — Narzędzia warte użycia

- `startTransition` dla niskopriorytetowych aktualizacji UI, np. filtrowania dużych list albo nawigacji nieblokującej inputu
- `useDeferredValue` gdy kosztowny rendering listy powinien nadążać za wpisywaniem bez przycinania UI
- `useEffectEvent` tylko wtedy, gdy projekt jest na React 19 i ten wzorzec jest już dopuszczalny w zespole

---

## Custom Hooks (persistuj tylko UI preferences)

`useLocalStorage` jest akceptowalny dla niesekretnych preferencji interfejsu. Nie używaj go do auth, tokenów ani danych wrażliwych.

```typescript
// src/shared/hooks/useDebounce.ts
import { useState, useEffect } from 'react';

export function useDebounce<T>(value: T, delay: number): T {
	const [debouncedValue, setDebouncedValue] = useState<T>(value);

	useEffect(() => {
		const timer = setTimeout(() => setDebouncedValue(value), delay);
		return () => clearTimeout(timer);
	}, [value, delay]);

	return debouncedValue;
}

// src/shared/hooks/useLocalStorage.ts
export function useLocalStorage<T>(key: string, initialValue: T) {
	const [storedValue, setStoredValue] = useState<T>(() => {
		try {
			const item = window.localStorage.getItem(key);
			return item ? JSON.parse(item) : initialValue;
		} catch {
			return initialValue;
		}
	});

	const setValue = (value: T | ((prev: T) => T)) => {
		try {
			const valueToStore =
				value instanceof Function ? value(storedValue) : value;
			setStoredValue(valueToStore);
			window.localStorage.setItem(key, JSON.stringify(valueToStore));
		} catch (error) {
			console.error(`Error saving to localStorage key "${key}":`, error);
		}
	};

	return [storedValue, setValue] as const;
}
```

---

## App Providers

```typescript
// src/app/providers/AppProviders.tsx
import type { ReactNode } from 'react'
import { BrowserRouter } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { queryClient } from '@/lib/queryClient'

interface AppProvidersProps {
    children: ReactNode
}

export function AppProviders({ children }: AppProvidersProps) {
    return (
        <QueryClientProvider client={queryClient}>
            <BrowserRouter>{children}</BrowserRouter>
        </QueryClientProvider>
    )
}
```

---

## vite.config.ts

```typescript
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import path from 'path';

export default defineConfig({
	plugins: [react(), tailwindcss()],
	resolve: {
		alias: {
			'@': path.resolve(__dirname, './src'),
		},
	},
	server: {
		proxy: {
			'/api': {
				target: 'http://localhost:8080',
				changeOrigin: true,
			},
		},
	},
});
```

---

## Zasady

- Feature-based struktura — każdy feature jest niezależnym modułem
- Eksportuj tylko przez `index.ts` (barrel exports)
- Dane serwera w TanStack Query — nigdy w `useState`
- Stan klienta (UI, auth) w Zustand
- Routing: zachowaj styl już używany w projekcie; component routes są dobre dla prostych SPA, data router gdy `loader`/`action` daje realną wartość
- Tailwind 3/4: wybierz CSS-first albo `tailwind.config.*` zgodnie z wersją i istniejącą konfiguracją projektu
- Każda walidacja formularza przez Zod schema
- `VITE_` prefix dla wszystkich zmiennych środowiskowych
- Nie przechowuj access tokenów w localStorage. Preferuj httpOnly cookies lub in-memory auth state
