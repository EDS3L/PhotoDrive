---
description: 'Use when writing tests, setting up test infrastructure, running TDD workflow, fixing failing tests, or raising coverage across JVM and TypeScript code.'
---

# Testing Rules — TDD

## Workflow (OBOWIĄZKOWY)

1. **RED** — Napisz test który FAILUJE
2. **GREEN** — Napisz MINIMALNĄ implementację żeby przeszedł
3. **REFACTOR** — Popraw design, zachowaj zielone testy

**Nigdy** nie pisz kodu produkcyjnego bez wcześniejszego testu.

Zanim uruchomisz testy lub coverage, sprawdź jak projekt jest zorganizowany: który moduł zawiera kod, jaki wrapper jest dostępny (`gradlew`, `mvnw`) i jaki runner testów ma frontend (`vitest`, `jest`, własny skrypt).

## Pokrycie Minimalne: 80%

- JVM: użyj wrappera i narzędzia coverage z projektu, np. `./gradlew test jacocoTestReport` albo `./mvnw test`
- TypeScript: użyj skryptu projektu albo skonfigurowanego runnera, np. `npm test -- --coverage` albo `npx vitest run --coverage`

Jeśli frontend nie ma jeszcze skryptu `test`, użyj bezpośrednio skonfigurowanego runnera albo najpierw skonfiguruj podstawową infrastrukturę testową.

## Java — Warstwy Testów

### Unit (bez Spring)

```java
@ExtendWith(MockitoExtension.class)
class OrderCommandHandlerTest {
    @Mock OrderRepository repository;
    @InjectMocks OrderCommandHandler handler;

    @Test
    void should_confirm_order_when_status_is_pending() {
        // Given
        Order order = OrderFixture.pendingOrder();
        given(repository.findById(order.getId())).willReturn(Optional.of(order));
        // When
        handler.handle(new ConfirmOrderCommand(order.getId()));
        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(repository).save(order);
    }
}
```

### Integration — Controller (@WebMvcTest)

```java
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(OrderController.class)
class OrderControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean OrderQueryHandler queryHandler;

    @Test
    void should_return_200_with_order_when_order_exists() throws Exception {
        given(queryHandler.handle(any())).willReturn(OrderFixture.confirmedOrderDto());
        mockMvc.perform(get("/api/v1/orders/{id}", "order-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }
}
```

Jeśli projekt jest na Spring Boot `3.4+` / Spring Framework `6.2+`, preferuj `@MockitoBean`. W starszych projektach użyj odpowiednika wspieranego przez ich wersję, np. `@MockBean`.

### Integration — Repository (@DataJpaTest)

```java
@DataJpaTest
class OrderJpaRepositoryTest {
    @Autowired OrderJpaRepository repository;

    @Test
    void should_persist_and_find_order() {
        Order order = OrderFixture.pendingOrder();
        repository.save(OrderMapper.toJpaEntity(order));
        Optional<Order> found = repository.findById(order.getId());
        assertThat(found).isPresent();
    }
}
```

## TypeScript — Warstwy Testów

### Component

```typescript
import { render } from '@/test/utils'

it('should display order details', () => {
    render(<OrderCard order={createOrderFixture()} />)
    expect(screen.getByText(/order-/i)).toBeInTheDocument()
})
```

### Hook

```typescript
const { result } = renderHook(() => useOrders(), { wrapper: createWrapper() });
await waitFor(() => expect(result.current.isSuccess).toBe(true));
```

## Fixtures (Pattern)

```java
// Java
public class OrderFixture {
    public static Order pendingOrder() {
        return Order.create(CustomerId.of("cust-1"), List.of(...));
    }
}
```

```typescript
// TypeScript
export const createOrderFixture = (overrides?: Partial<Order>): Order => ({
    id: `order-${++counter}`,
    status: 'PENDING',
    ...,
    ...overrides,
})
```

## Naming Convention

```
Java:    should_[wynik]_when_[warunek]
TS:      'should [describe behavior] when [condition]'
```
