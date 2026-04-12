---
name: spring-ddd-patterns
description: Wzorce Spring Boot z Domain-Driven Design — agregaty, encje, value objects, domain events, repozytoria, CQRS, architektura warstwowa. Używaj dla architektury Java Spring DDD.
origin: java-spring-ddd-pro
---

# Spring Boot + DDD Wzorce

Wzorce architektoniczne Spring Boot zgodne z Domain-Driven Design (DDD) dla produkcyjnych, testowalnych serwisów enterprise.

## Kiedy Aktywować

- Projektowanie nowego modułu lub bounded context
- Implementacja agregatu z logiką biznesową
- Tworzenie domain events i ich obsługa
- Konfigurowanie warstwy persistence z JPA
- Implementacja wzorców CQRS (Commands i Queries)
- Tworzenie application services i use case'ów

---

## Struktura Projektu (Hexagonal / Ports & Adapters)

```
src/main/java/{package}/
├── domain/                         ← CZYSTA DOMENA (no Spring, no JPA!)
│   ├── model/
│   │   ├── Order.java              ← Aggregate Root
│   │   ├── OrderLine.java          ← Entity (wewnątrz agregatu)
│   │   ├── OrderId.java            ← Value Object
│   │   ├── Money.java              ← Value Object
│   │   └── OrderStatus.java        ← Enum domenowy
│   ├── event/
│   │   ├── DomainEvent.java        ← Bazowy interfejs
│   │   ├── OrderCreatedEvent.java
│   │   └── OrderConfirmedEvent.java
│   ├── repository/
│   │   └── OrderRepository.java    ← Interface (Port)
│   ├── service/
│   │   └── OrderPricingService.java ← Domain Service
│   └── exception/
│       └── InvalidOrderStateException.java
│
├── application/                    ← WARSTWA APLIKACJI (orkiestracja use case'ów)
│   ├── command/
│   │   ├── CreateOrderCommand.java
│   │   ├── CreateOrderCommandHandler.java
│   │   ├── ConfirmOrderCommand.java
│   │   └── ConfirmOrderCommandHandler.java
│   ├── query/
│   │   ├── GetOrderQuery.java
│   │   ├── GetOrderQueryHandler.java
│   │   └── OrderReadModel.java     ← Interface odczytu
│   └── event/
│       └── OrderCreatedEventHandler.java
│
├── infrastructure/                 ← INFRASTRUKTURA (Spring, JPA, zewnętrzne serwisy)
│   ├── persistence/
│   │   ├── OrderJpaEntity.java     ← JPA @Entity — tutaj adnotacje JPA!
│   │   ├── OrderLineJpaEntity.java
│   │   ├── OrderJpaRepository.java ← extends JpaRepository
│   │   ├── JpaOrderRepository.java ← Implementuje domain OrderRepository
│   │   └── OrderMapper.java        ← Konwertuje domain ↔ JPA
│   ├── messaging/
│   │   └── SpringDomainEventPublisher.java
│   └── config/
│       └── DomainConfig.java
│
└── interfaces/                     ← INTERFEJSY (REST, gRPC, itp.)
    ├── rest/
    │   ├── OrderController.java
    │   ├── CreateOrderRequest.java  ← DTO
    │   └── OrderResponse.java       ← DTO
    └── exception/
        └── GlobalExceptionHandler.java
```

---

## Aggregate Root

```java
// ZASADA: Aggregate Root to jedyna brama do obiektów wewnątrz agregatu
// Brak Spring adnotacji (@Service, @Component) w domenie!
// Brak JPA adnotacji (@Entity, @Table) w domenie!

public class Order {
    private final OrderId id;
    private final CustomerId customerId;
    private OrderStatus status;
    private final List<OrderLine> lines;
    private final List<DomainEvent> domainEvents;

    // Prywatny konstruktor — używaj fabryki
    private Order(OrderId id, CustomerId customerId) {
        this.id = Objects.requireNonNull(id);
        this.customerId = Objects.requireNonNull(customerId);
        this.status = OrderStatus.PENDING;
        this.lines = new ArrayList<>();
        this.domainEvents = new ArrayList<>();
    }

    // Factory method — wyraźna intencja
    public static Order create(CustomerId customerId, List<OrderLineData> lineData) {
        var order = new Order(OrderId.generate(), customerId);
        if (lineData.isEmpty()) {
            throw new IllegalArgumentException("Zamówienie musi mieć co najmniej jedną linię");
        }
        lineData.forEach(data -> order.addLine(data.productId(), data.quantity(), data.price()));
        order.domainEvents.add(new OrderCreatedEvent(order.id, customerId, Instant.now()));
        return order;
    }

    // Metody domenowe — expresyjne zachowanie biznesowe
    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                "Zamówienie " + id + " nie może zostać potwierdzone w stanie " + status
            );
        }
        this.status = OrderStatus.CONFIRMED;
        domainEvents.add(new OrderConfirmedEvent(this.id, Instant.now()));
    }

    public void cancel(CancellationReason reason) {
        if (this.status == OrderStatus.SHIPPED || this.status == OrderStatus.DELIVERED) {
            throw new InvalidOrderStateException("Nie można anulować wysłanego/dostarczonego zamówienia");
        }
        this.status = OrderStatus.CANCELLED;
        domainEvents.add(new OrderCancelledEvent(this.id, reason, Instant.now()));
    }

    // Pobieranie i czyszczenie domenowych eventów
    public List<DomainEvent> pullDomainEvents() {
        var events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    // Tylko gettery, brak setterów publicznych
    public OrderId getId() { return id; }
    public CustomerId getCustomerId() { return customerId; }
    public OrderStatus getStatus() { return status; }
    public List<OrderLine> getLines() { return Collections.unmodifiableList(lines); }

    private void addLine(ProductId productId, int quantity, Money price) {
        this.lines.add(new OrderLine(OrderLineId.generate(), productId, quantity, price));
    }
}
```

---

## Value Object

```java
// ZASADA: Immutable, porównywany przez wartość (nie przez referencję)
// Używaj Java Records dla prostych Value Objects

public record OrderId(UUID value) {
    public OrderId {
        Objects.requireNonNull(value, "OrderId nie może być null");
    }

    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }

    public static OrderId of(String value) {
        return new OrderId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

// Value Object z logiką biznesową
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount, "Kwota nie może być null");
        Objects.requireNonNull(currency, "Waluta nie może być null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Kwota nie może być ujemna: " + amount);
        }
        // Normalizuj do 2 miejsc po przecinku
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }

    private void assertSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new CurrencyMismatchException(this.currency, other.currency);
        }
    }
}
```

---

## Domain Events

```java
// Bazowy interfejs
public interface DomainEvent {
    Instant occurredAt();
}

// Konkretny event — używaj Records
public record OrderCreatedEvent(
    OrderId orderId,
    CustomerId customerId,
    Instant occurredAt
) implements DomainEvent {}

public record OrderConfirmedEvent(
    OrderId orderId,
    Instant occurredAt
) implements DomainEvent {}
```

---

## Repository Interface (w domenie)

```java
// ZASADA: Interface w domenie, implementacja w infrastrukturze
public interface OrderRepository {
    Order findById(OrderId id);                              // Rzuca wyjątek gdy nie znajdzie
    Optional<Order> findOptionalById(OrderId id);
    Page<Order> findByCustomer(CustomerId customerId, Pageable pageable);
    void save(Order order);
    void delete(OrderId id);
}
```

---

## JPA Entity (w infrastrukturze — NIE w domenie)

```java
// Adnotacje JPA SĄ tutaj, w infrastrukturze
@Entity
@Table(name = "orders")
class OrderJpaEntity {
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "customer_id", nullable = false, columnDefinition = "uuid")
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLineJpaEntity> lines = new ArrayList<>();

    @Version
    private Long version;  // Optimistic locking

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    protected OrderJpaEntity() {}  // JPA wymaga
}
```

---

## Repository Implementacja (w infrastrukturze)

```java
@Repository
class JpaOrderRepository implements OrderRepository {

    private final OrderJpaRepository jpaRepository;
    private final OrderMapper mapper;

    JpaOrderRepository(OrderJpaRepository jpaRepository, OrderMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Order findById(OrderId id) {
        return jpaRepository.findById(id.value())
            .map(mapper::toDomain)
            .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Override
    public Optional<Order> findOptionalById(OrderId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void save(Order order) {
        var entity = mapper.toJpa(order);
        jpaRepository.save(entity);
    }
}

// Spring Data JPA interface
interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {
    @Query("SELECT o FROM OrderJpaEntity o WHERE o.customerId = :customerId")
    Page<OrderJpaEntity> findByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);
}
```

---

## Application Service (CQRS — Command Handler)

```java
// Command jest immutable DTO
public record ConfirmOrderCommand(OrderId orderId, UserId confirmedBy) {}

// Handler orkiestruje użycie domeny
@Component
public class ConfirmOrderCommandHandler {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;

    public ConfirmOrderCommandHandler(
        OrderRepository orderRepository,
        DomainEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void handle(ConfirmOrderCommand command) {
        Order order = orderRepository.findById(command.orderId());
        order.confirm();
        orderRepository.save(order);
        eventPublisher.publishAll(order.pullDomainEvents());
    }
}
```

---

## REST Controller (warstwa interfejsów)

```java
@RestController
@RequestMapping("/api/v1/orders")
@Validated
class OrderController {

    private final CreateOrderCommandHandler createOrderHandler;
    private final ConfirmOrderCommandHandler confirmOrderHandler;
    private final GetOrderQueryHandler getOrderHandler;

    // Wstrzykiwanie przez konstruktor (nie @Autowired)
    OrderController(
        CreateOrderCommandHandler createOrderHandler,
        ConfirmOrderCommandHandler confirmOrderHandler,
        GetOrderQueryHandler getOrderHandler
    ) {
        this.createOrderHandler = createOrderHandler;
        this.confirmOrderHandler = confirmOrderHandler;
        this.getOrderHandler = getOrderHandler;
    }

    @PostMapping
    ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        var command = new CreateOrderCommand(
            CustomerId.of(request.customerId()),
            request.lines().stream()
                .map(l -> new OrderLineData(
                    ProductId.of(l.productId()),
                    l.quantity(),
                    Money.of(l.price(), "PLN")
                ))
                .toList()
        );
        OrderId orderId = createOrderHandler.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(OrderResponse.of(orderId));
    }

    @PutMapping("/{orderId}/confirm")
    ResponseEntity<Void> confirmOrder(@PathVariable String orderId) {
        confirmOrderHandler.handle(new ConfirmOrderCommand(OrderId.of(orderId), getCurrentUserId()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{orderId}")
    ResponseEntity<OrderDetailResponse> getOrder(@PathVariable String orderId) {
        OrderDetail detail = getOrderHandler.handle(new GetOrderQuery(OrderId.of(orderId)));
        return ResponseEntity.ok(OrderDetailResponse.from(detail));
    }
}
```

---

## Global Exception Handler

```java
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(OrderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiError.of("ORDER_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    ResponseEntity<ApiError> handleInvalidState(InvalidOrderStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiError.of("INVALID_ORDER_STATE", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
            .body(ApiError.of("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleGeneric(Exception ex) {
        // Loguj z pełnym stack trace
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError.of("INTERNAL_ERROR", "Wystąpił błąd wewnętrzny"));
    }
}

public record ApiError(String code, String message, Instant timestamp) {
    static ApiError of(String code, String message) {
        return new ApiError(code, message, Instant.now());
    }
}
```

---

## Domain Event Publisher (infrastruktura)

```java
// Interface w domenie
public interface DomainEventPublisher {
    void publish(DomainEvent event);
    void publishAll(List<DomainEvent> events);
}

// Implementacja w infrastrukturze z Spring Events
@Component
class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    SpringDomainEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void publish(DomainEvent event) {
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publishAll(List<DomainEvent> events) {
        events.forEach(this::publish);
    }
}

// Event listener
@Component
class OrderConfirmedEventListener {

    private final NotificationService notificationService;

    @EventListener
    @Async  // Asynchroniczna obsługa
    public void on(OrderConfirmedEvent event) {
        notificationService.notifyOrderConfirmed(event.orderId());
    }
}
```

---

## Produkcyjne Defaults

- Preferuj wstrzykiwanie przez konstruktor, unikaj wstrzykiwania przez pole
- Używaj `record` dla Value Objects i DTO
- Włącz `spring.mvc.problemdetails.enabled=true` dla RFC 7807 (Spring Boot 3+)
- Konfiguruj rozmiary pul HikariCP dla obciążenia
- Używaj `@Transactional(readOnly = true)` dla zapytań
- Egzekwuj null-safety przez `@NonNull` i `Optional`
- Niech Aggregate Root zawsze publikuje Domain Events po zmianie stanu
- Keep controllers thin — deleguj do command/query handlers, nic więcej

**Remember**: Domena nie zna infrastruktury. Infrastruktura zna domenę. To jest fundamentalna zasada DDD.
