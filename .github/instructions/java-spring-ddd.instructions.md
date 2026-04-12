---
description: 'Use when writing, reviewing, or modifying Java code — Spring Boot controllers, services, domain aggregates, repositories, JPA entities, DTOs. Covers DDD layer rules, Spring idioms, JPA best practices.'
applyTo: '**/*.java'
---

# Java + Spring Boot + DDD Rules

Traktuj poniższe warstwy jako role architektoniczne. Jeśli projekt używa innych nazw pakietów albo nie jest rozłożony dokładnie na `domain/application/infrastructure/interfaces`, zachowaj te same granice semantyczne zamiast wymuszać nazwy folderów.

## DDD Warstwy — Co Wolno Gdzie

| Warstwa           | Dozwolone                                   | ZABRONIONE                                           |
| ----------------- | ------------------------------------------- | ---------------------------------------------------- |
| `domain/`         | Czyste Java klasy, Records, interfaces      | `@Entity`, `@Service`, `@Repository`, Spring imports |
| `application/`    | `@Service`, `@Transactional`, Spring Events | JPA annotations, HTTP-specific code                  |
| `infrastructure/` | `@Entity`, `@Repository`, JPA annotations   | Logika biznesowa                                     |
| `interfaces/`     | `@RestController`, `@RequestBody`, DTOs     | Logika biznesowa, @Transactional                     |

## Aggregate Root — Wzorzec

```java
public class Order {
    private final OrderId id;
    private OrderStatus status;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    public static Order create(CustomerId customerId, List<OrderLineData> lines) {
        Order order = new Order(OrderId.generate(), customerId);
        // ... dodaj linie
        order.domainEvents.add(new OrderCreatedEvent(order.id, customerId));
        return order;
    }

    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new InvalidOrderStateException("...");
        }
        this.status = OrderStatus.CONFIRMED;
        domainEvents.add(new OrderConfirmedEvent(this.id));
    }

    // BRAK publicznych setterów!
}
```

## Value Object — Record

```java
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount);
        if (amount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Kwota ujemna");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }
}
```

## Spring Boot Zasady

- **Constructor injection** — nigdy `@Autowired` na polach
- **`@Transactional`** tylko na serwisach aplikacyjnych, nie na kontrolerach
- **`@Transactional(readOnly = true)`** na wszystkich metodach tylko-odczytu
- **`@Valid`** na każdym `@RequestBody`
- **`@RestControllerAdvice`** dla centralnej obsługi błędów

## JPA (tylko w warstwie persistence/infrastructure, nigdy w domenie)

- Brak `FetchType.EAGER` na kolekcjach
- `Page<T>` + `Pageable` na listach
- `@Version` na agregatach (optimistic locking)
- `JOIN FETCH` lub `@EntityGraph` zamiast N+1

## Java 21+ Idiomy

- `record` dla Value Objects, DTOs, Domain Events
- `Optional<T>` zamiast `null` z serwisów
- `switch` expressions zamiast switch statements
- Sealed interfaces dla zamkniętych hierarchii

## Konwencje Testów

```java
// Naming: should_[expected]_when_[condition]
@Test
void should_throw_InvalidOrderStateException_when_confirming_already_confirmed_order() {}

// Struktura AAA
void test() {
    // Given
    Order order = OrderFixture.confirmedOrder();
    // When / Then
    assertThatThrownBy(() -> order.confirm())
        .isInstanceOf(InvalidOrderStateException.class);
}
```
