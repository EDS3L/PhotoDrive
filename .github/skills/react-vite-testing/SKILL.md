---
name: react-vite-testing
description: Testowanie React z Vitest i React Testing Library — unit testy hooków, testy komponentów, mockowanie API i coverage. Używaj dla TDD w nowoczesnym React + Vite.
origin: java-spring-ddd-pro
---

# React + Vite Testowanie

Wzorce testów dla nowoczesnego React (18/19) z Vitest, React Testing Library i TanStack Query.

Ten skill opisuje preferowany wariant dla projektów Vite + Vitest. Jeśli projekt ma już skonfigurowany Jest albo inny runner, zachowaj istniejący stack testowy zamiast mieszać narzędzia.

## Kiedy Aktywować

- Pisanie testów dla hooków React (useQuery, custom hooks)
- Testowanie komponentów (formularze, listy, modalne)
- Mockowanie API calls w testach
- Konfigurowanie test setup z Vitest

---

## Konfiguracja Testów

```typescript
// src/test/setup.ts
import '@testing-library/jest-dom';

// Wyciszenie logów konsoli w testach
beforeEach(() => {
	vi.spyOn(console, 'error').mockImplementation(() => {});
});

afterEach(() => {
	vi.restoreAllMocks();
});
```

```typescript
// vite.config.ts — sekcja test
test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    coverage: {
        provider: 'v8',
        reporter: ['text', 'lcov'],
        exclude: ['src/test/**', '**/*.d.ts', 'src/main.tsx'],
        thresholds: {
            statements: 80,
            branches: 80,
            functions: 80,
            lines: 80,
        },
    },
}
```

Jeśli wybierasz provider `v8`, doinstaluj `@vitest/coverage-v8`.

---

## Test Utilities

```typescript
// src/test/utils.tsx — reużywalne wrappers
import { render, type RenderOptions } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import type { ReactElement } from 'react'

function createTestQueryClient() {
    return new QueryClient({
        defaultOptions: {
            queries: { retry: false, gcTime: 0 },
            mutations: { retry: false },
        },
    })
}

interface RenderWithProvidersOptions extends Omit<RenderOptions, 'wrapper'> {
    initialRouterEntries?: string[]
}

function renderWithProviders(
    ui: ReactElement,
    { initialRouterEntries = ['/'], ...renderOptions }: RenderWithProvidersOptions = {}
) {
    const queryClient = createTestQueryClient()

    function Wrapper({ children }: { children: React.ReactNode }) {
        return (
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={initialRouterEntries}>
                    {children}
                </MemoryRouter>
            </QueryClientProvider>
        )
    }

    return {
        ...render(ui, { wrapper: Wrapper, ...renderOptions }),
        queryClient,
    }
}

export * from '@testing-library/react'
export { renderWithProviders as render }
```

---

## Testy Komponentów

```typescript
// src/features/orders/components/__tests__/OrderCard.test.tsx
import { render, screen } from '@/test/utils'
import userEvent from '@testing-library/user-event'
import { OrderCard } from '../OrderCard'
import { createOrderFixture } from '@/test/fixtures/orderFixtures'

describe('OrderCard', () => {
    it('should display order details', () => {
        const order = createOrderFixture({
            id: 'order-123',
            status: 'PENDING',
            customerId: 'customer-1'
        })

        render(<OrderCard order={order} />)

        expect(screen.getByText('order-123')).toBeInTheDocument()
        expect(screen.getByText(/oczekujące/i)).toBeInTheDocument()
    })

    it('should call onConfirm when confirm button clicked', async () => {
        const user = userEvent.setup()
        const onConfirm = vi.fn()
        const order = createOrderFixture({ status: 'PENDING' })

        render(<OrderCard order={order} onConfirm={onConfirm} />)

        await user.click(screen.getByRole('button', { name: /potwierdź/i }))

        expect(onConfirm).toHaveBeenCalledWith(order.id)
    })

    it('should not show confirm button for non-pending orders', () => {
        const order = createOrderFixture({ status: 'CONFIRMED' })

        render(<OrderCard order={order} onConfirm={vi.fn()} />)

        expect(screen.queryByRole('button', { name: /potwierdź/i })).not.toBeInTheDocument()
    })
})
```

---

## Testy Formularzy

```typescript
// src/features/orders/components/__tests__/CreateOrderForm.test.tsx
import { render, screen, waitFor } from '@/test/utils'
import userEvent from '@testing-library/user-event'
import { CreateOrderForm } from '../CreateOrderForm'
import { vi } from 'vitest'

// Mock API hook
vi.mock('@/features/orders/hooks/useCreateOrder', () => ({
    useCreateOrder: vi.fn(),
}))

import { useCreateOrder } from '@/features/orders/hooks/useCreateOrder'

describe('CreateOrderForm', () => {
    const mockMutate = vi.fn()

    beforeEach(() => {
        vi.mocked(useCreateOrder).mockReturnValue({
            mutateAsync: mockMutate.mockResolvedValue({ id: 'new-order-1' }),
            isPending: false,
            isError: false,
        } as ReturnType<typeof useCreateOrder>)
    })

    it('should show validation error when submitting empty form', async () => {
        const user = userEvent.setup()
        render(<CreateOrderForm />)

        await user.click(screen.getByRole('button', { name: /utwórz zamówienie/i }))

        await waitFor(() => {
            expect(screen.getByText(/pole wymagane/i)).toBeInTheDocument()
        })
    })

    it('should submit form with valid data', async () => {
        const user = userEvent.setup()
        render(<CreateOrderForm />)

        await user.type(screen.getByLabelText(/id klienta/i), 'customer-1')
        await user.click(screen.getByRole('button', { name: /utwórz zamówienie/i }))

        await waitFor(() => {
            expect(mockMutate).toHaveBeenCalledWith(expect.objectContaining({
                customerId: 'customer-1',
            }))
        })
    })

    it('should show error message on mutation failure', async () => {
        vi.mocked(useCreateOrder).mockReturnValue({
            mutateAsync: vi.fn().mockRejectedValue(new Error()),
            isPending: false,
            isError: true,
        } as ReturnType<typeof useCreateOrder>)

        render(<CreateOrderForm />)

        expect(screen.getByText(/błąd tworzenia zamówienia/i)).toBeInTheDocument()
    })
})
```

---

## Testy Hooków (z renderHook)

```typescript
// src/features/orders/hooks/__tests__/useOrders.test.ts
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useOrders } from '../useOrders'
import { ordersApi } from '../../api/ordersApi'
import { createOrdersPageFixture } from '@/test/fixtures/orderFixtures'

// Mock całego modułu API
vi.mock('../../api/ordersApi')

const createWrapper = () => {
    const queryClient = new QueryClient({
        defaultOptions: { queries: { retry: false } }
    })
    return ({ children }: { children: React.ReactNode }) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    )
}

describe('useOrders', () => {
    it('should return orders on successful fetch', async () => {
        const ordersPage = createOrdersPageFixture()
        vi.mocked(ordersApi.getOrders).mockResolvedValue(ordersPage)

        const { result } = renderHook(() => useOrders(), {
            wrapper: createWrapper(),
        })

        await waitFor(() => expect(result.current.isSuccess).toBe(true))

        expect(result.current.data?.content).toHaveLength(ordersPage.content.length)
    })

    it('should return error state on failed fetch', async () => {
        vi.mocked(ordersApi.getOrders).mockRejectedValue(new Error('Network error'))

        const { result } = renderHook(() => useOrders(), {
            wrapper: createWrapper(),
        })

        await waitFor(() => expect(result.current.isError).toBe(true))
    })
})
```

---

## Test Fixtures

```typescript
// src/test/fixtures/orderFixtures.ts
import type { Order, OrdersPage } from '@/features/orders/types/order.types';

let counter = 0;
const nextId = () => `order-${++counter}`;

export const createOrderFixture = (overrides?: Partial<Order>): Order => ({
	id: nextId(),
	customerId: 'customer-1',
	status: 'PENDING',
	lines: [
		{
			id: 'line-1',
			productId: 'product-1',
			quantity: 1,
			price: { amount: 100, currency: 'PLN' },
		},
	],
	createdAt: new Date().toISOString(),
	...overrides,
});

export const createOrdersPageFixture = (overrides?: {
	count?: number;
}): OrdersPage => ({
	content: Array.from({ length: overrides?.count ?? 3 }, () =>
		createOrderFixture(),
	),
	totalElements: overrides?.count ?? 3,
	totalPages: 1,
	size: 20,
	number: 0,
});
```

---

## Mockowanie API z MSW (Mock Service Worker)

```typescript
// src/test/mocks/handlers.ts — dla bardziej zaawansowanych testów integracyjnych
import { http, HttpResponse } from 'msw';
import { createOrderFixture } from '../fixtures/orderFixtures';

export const handlers = [
	http.get('/api/v1/orders', () => {
		return HttpResponse.json({
			content: [createOrderFixture(), createOrderFixture()],
			totalElements: 2,
			totalPages: 1,
		});
	}),

	http.post('/api/v1/orders', () => {
		return HttpResponse.json({ id: 'new-order-1' }, { status: 201 });
	}),

	http.get('/api/v1/orders/:orderId', ({ params }) => {
		return HttpResponse.json(
			createOrderFixture({ id: params.orderId as string }),
		);
	}),
];

// src/test/mocks/server.ts
import { setupServer } from 'msw/node';
import { handlers } from './handlers';

export const server = setupServer(...handlers);

// src/test/setup.ts — dodaj server
import { server } from './mocks/server';

beforeAll(() => server.listen({ onUnhandledRequest: 'warn' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
```

---

## Polecenia Testów

```bash
# Uruchom testy
npx vitest run

# Testy z pokryciem
npx vitest run --coverage

# Tryb watch
npx vitest

# Konkretny plik
npx vitest run OrderCard.test.tsx

# Interaktywny UI testów
npx vitest --ui
```

Jeśli `package.json` nie ma skryptu `test`, używaj bezpośrednio powyższych komend `npx vitest ...`.

---

## Zasady Testowania React

- Używaj `userEvent` zamiast `fireEvent` — symuluje realne interakcje
- Testuj z perspektywy użytkownika (co widzi, nie implementacja)
- Używaj `getByRole` i `getByLabelText` — semantyczne selektory
- Nie testuj szczegółów implementacji (nazwy funkcji, stany wewnętrzne)
- Każdy test powinien być niezależny — bez side effects między testami
- Fixtures z `createXxxFixture()` dla testowalnych danych
- Pokrycie ≥ 80% (mierzone przez Vitest + V8 coverage)
