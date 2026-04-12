---
name: ddd-domain-modeling
description: Czyste wzorce Domain-Driven Design — Bounded Contexts, Ubiquitous Language, Strategic DDD, Context Maps. Używaj przed implementacją nowego modułu domenowego.
origin: java-spring-ddd-pro
---

# DDD Domain Modeling

Strategiczne i taktyczne wzorce Domain-Driven Design dla złożonych aplikacji enterprise.

## Kiedy Aktywować

- Projektowanie nowego systemu lub modułu
- Identyfikowanie Bounded Contexts i ich granic
- Definiowanie Ubiquitous Language ze stakeholderami
- Modelowanie Aggregates, Entities i Value Objects
- Projektowanie Context Map dla relacji między kontekstami

---

## Strategic DDD — Bounded Contexts

### Co to jest Bounded Context?

Bounded Context to cząstkowe ograniczenie, w obrębie którego model ma określone znaczenie. Ten sam termin (np. "Zamówienie") może oznaczać coś innego w kontekście sprzedaży, wysyłki i finansów.

```
┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐
│   Sales Context     │    │  Shipping Context   │    │  Finance Context    │
│                     │    │                     │    │                     │
│  Order              │    │  Shipment           │    │  Invoice            │
│  - customerId       │    │  - orderId (ref)    │    │  - orderId (ref)    │
│  - items            │    │  - trackingNumber   │    │  - amount           │
│  - totalPrice       │    │  - status           │    │  - taxAmount        │
│  - status           │    │                     │    │                     │
└─────────────────────┘    └─────────────────────┘    └─────────────────────┘
         │ Domain Event: OrderConfirmed                       │
         └────────────────────────────────────────────────────┘
```

### Odkrywanie Bounded Contexts

Pytania do zadania podczas Event Storming:
1. Gdzie zdarzenia biznesowe zmieniają "właściciela"?
2. Gdzie ten sam termin oznacza coś innego?
3. Gdzie różne zespoły mają różne modele tego samego pojęcia?
4. Gdzie są naturalne granice transakcji?

---

## Taktyczne DDD — Budujące Bloki

### Aggregate Design

```
ZASADY AGREGATU:
1. Aggregate Root jest jedyną bramą do obiektów wewnątrz
2. Transakcja nie może obejmować wielu agregatów naraz
3. Agregaty komunikują się przez Domain Events, nie bezpośrednie referencje
4. Agregat powinien być "mały" — modyfikuj tylko to co konieczne
5. Invarianty (niezmienniki) są egzekwowane wewnątrz agregatu
```

Przykład agregatu z inwariante:
```java
public class ShoppingCart {
    private static final int MAX_ITEMS = 50;
    
    private final CartId id;
    private final List<CartItem> items;
    private CartStatus status;

    // INVARIANT: Koszyk nie może mieć > 50 pozycji
    public void addItem(ProductId productId, int quantity) {
        if (status == CartStatus.CHECKED_OUT) {
            throw new InvalidCartStateException("Nie można dodać do zamkniętego koszyka");
        }
        if (items.size() >= MAX_ITEMS) {
            throw new CartCapacityExceededException(MAX_ITEMS);
        }
        
        items.stream()
            .filter(item -> item.getProductId().equals(productId))
            .findFirst()
            .ifPresentOrElse(
                item -> item.increaseQuantity(quantity),
                () -> items.add(new CartItem(productId, quantity))
            );
    }
}
```

---

### Ubiquitous Language

Wspólny język między programistami a ekspertami domenowymi:

```
// ŹLE — techniczne nazwy
void processData(String input, int type, List<Object> items)
void updateStatus(int statusCode)
boolean checkIfValid()

// DOBRZE — domenowe nazwy
void confirmOrder(OrderId orderId)
void applyDiscount(DiscountCode code)
boolean isEligibleForFreeShipping()
void publishDraftArticle(ArticleId articleId)
void activateUserAccount(UserId userId)
```

**Słownik dziedziny (przykład dla e-commerce):**
- `Order` — Zamówienie złożone przez klienta (nie `Cart` ani `Purchase`)
- `Customer` — Zarejestrowany klient z historią zakupów
- `Guest` — Niezarejestrowana osoba
- `Confirm` — Zaakceptować zamówienie do realizacji
- `Fulfill` — Przygotować i wysłać zamówienie

---

### Entity vs Value Object

```
ENTITY:
- Ma tożsamość (ID), która nie zmienia się nawet gdy atrybuty się zmieniają
- Mutowalny (zmienia się w czasie)
- Porównywany przez ID
- Przykłady: Order, Customer, Product

VALUE OBJECT:
- Nie ma własnej tożsamości — jest definiowany przez atrybuty
- Immutable (niezmienny)
- Porównywany przez wartość wszystkich atrybutów
- Przykłady: Money, Address, DateRange, Email
```

Kiedy wybrać Value Object:
```java
// To POWINNO być Value Object, nie String
public class Email {
    private final String value;
    
    public Email(String value) {
        if (value == null || !value.matches("^[^@]+@[^@]+\\.[^@]+$")) {
            throw new InvalidEmailException(value);
        }
        this.value = value.toLowerCase();
    }
}

// To POWINNO być Value Object, nie dwa BigDecimal
public record DateRange(LocalDate from, LocalDate to) {
    public DateRange {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
    }
    
    public boolean contains(LocalDate date) {
        return !date.isBefore(from) && !date.isAfter(to);
    }
    
    public boolean overlaps(DateRange other) {
        return !this.to.isBefore(other.from) && !other.to.isBefore(this.from);
    }
}
```

---

### Domain Events

Domain Events wyrażają fakty które wystąpiły w domenie (czas przeszły):

```java
// Nazewnictwo: [AgregateName][CoSięStało]Event
OrderCreatedEvent       // Zamówienie zostało złożone
OrderConfirmedEvent     // Zamówienie zostało potwierdzone
OrderShippedEvent       // Zamówienie zostało wysłane
PaymentProcessedEvent   // Płatność zostało przetworzona
CustomerRegisteredEvent // Klient został zarejestrowany

// Domain Event jest immutable — zawiera tylko dane (nie zachowanie)
public record OrderShippedEvent(
    OrderId orderId,
    TrackingNumber trackingNumber,
    Instant shippedAt
) implements DomainEvent {}
```

Publikowanie z Aggregate Root:
```java
// Aggregate Root zbiera eventy podczas operacji
public class Order {
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    
    public void ship(TrackingNumber trackingNumber) {
        validateCanBeShipped();
        this.status = OrderStatus.SHIPPED;
        this.trackingNumber = trackingNumber;
        // Event jest dodawany po zmianie stanu
        domainEvents.add(new OrderShippedEvent(this.id, trackingNumber, Instant.now()));
    }
    
    // Application Service pobiera i czyści eventy po zapisie
    public List<DomainEvent> pullDomainEvents() {
        var events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }
}
```

---

### Domain Service

Kiedy używać Domain Service:
- Logika dotyczy wielu agregatów i nie "należy" do żadnego z nich
- Operacja wymaga zewnętrznych danych (np. kursów walut)
- Polityki i reguły które obejmują wiele encji

```java
// Domain Service — NIE jest Spring Bean (@Service) w domenie
// To tylko klasa z logiką domenową
public class OrderTransferService {
    
    public Transfer transfer(Order sourceOrder, Warehouse targetWarehouse) {
        if (!sourceOrder.canBeTransferred()) {
            throw new OrderTransferNotAllowedException(sourceOrder.getId());
        }
        if (!targetWarehouse.hasCapacityFor(sourceOrder.getLines())) {
            throw new InsufficientWarehouseCapacityException();
        }
        
        // Logika obejmuje dwa agregaty
        return Transfer.create(sourceOrder.getId(), targetWarehouse.getId());
    }
}
```

---

## Context Map Wzorce

### Upstream/Downstream Relacje

```
CUSTOMER/SUPPLIER — jeden kontekst zależy od drugiego (kontrolowane API):
  [Sales Context] --downstream--> [Product Catalog Context]

CONFORMIST — downstream akceptuje model upstream bez negocjacji:
  [Reporting] --conforms to--> [Core Domain]

ANTI-CORRUPTION LAYER (ACL) — tłumaczenie między modelami:
  [New System] --ACL--> [Legacy System]
  
SHARED KERNEL — oba konteksty dzielą część modelu:
  [Sales] <-- shared --> [Invoicing] (np. CustomerId)
  
PUBLISHED LANGUAGE — publiczne API z dokumentacją:
  [Payment Provider] --published language--> [Our System]
```

Implementacja ACL:
```java
// Anti-Corruption Layer — tłumaczy model zewnętrzny na domenowy
@Component
public class LegacyOrderAdapter {
    
    private final LegacyOrderClient legacyClient;
    
    // Zwraca domenowy model, nie zewnętrzny
    public Optional<Order> findLegacyOrder(String legacyOrderNumber) {
        return legacyClient.getOrder(legacyOrderNumber)
            .map(this::translateTodomainModel);
    }
    
    private Order translateTodomainModel(LegacyOrderDto legacy) {
        // Tłumaczenie: stary model → nowy model domenowy
        return Order.reconstitute(
            OrderId.of(legacy.getOrderId()),
            CustomerId.of(legacy.getClientCode()),
            mapStatus(legacy.getStatusCode()),
            mapLines(legacy.getLineItems())
        );
    }
    
    private OrderStatus mapStatus(int legacyCode) {
        return switch (legacyCode) {
            case 1 -> OrderStatus.PENDING;
            case 2 -> OrderStatus.CONFIRMED;
            case 3 -> OrderStatus.SHIPPED;
            default -> throw new UnknownLegacyStatusException(legacyCode);
        };
    }
}
```

---

## Event Storming — Odkrywanie Domeny

### Kolory na karteczkach:
- 🟠 **Orange** — Domain Events (Co się stało?)
- 🔵 **Blue** — Commands (Co powoduje event?)
- 🟡 **Yellow** — Actors (Kto wydaje command?)
- 🟢 **Green** — Read Models / Views (Co użytkownik widzi?)
- 🟣 **Purple** — Policies (Gdy X, wtedy Y — automatyczne reakcje)
- 🔴 **Red** — Problemy / Pytania

### Kroki Event Storming:
1. Wypisz wszystkie Domain Events (na orange kartkach)
2. Ułóż je chronologicznie
3. Dodaj Commands które wywołują eventy
4. Dodaj Actors (kto wywołuje command)
5. Zidentyfikuj Aggregates (grupuj events tego samego obiektu)
6. Zidentyfikuj granice Bounded Contexts (naturalne grupowania)
7. Zidentyfikuj Policies (automatyczne reakcje na eventy)

---

## Checklist Modelowania DDD

### Przed implementacją:
- [ ] Ubiquitous Language zdefiniowany i udokumentowany
- [ ] Bounded Contexts zidentyfikowane
- [ ] Aggregate Roots i ich invarianty określone
- [ ] Value Objects wyodrębnione z domeny
- [ ] Domain Events dla każdej ważnej zmiany stanu
- [ ] Context Map narysowana

### Podczas implementacji:
- [ ] Domain nie importuje klas Spring ani JPA
- [ ] Aggregate Root jest jedyną bramą do encji potomnych
- [ ] Value Objects są immutable (brak setterów)
- [ ] Domain Events publikowane po zmianie stanu
- [ ] Repository interfaces w domenie, implementacje w infrastrukturze
- [ ] Application Services orkiestrują — nie zawierają logiki biznesowej

### Code Review DDD:
- [ ] `domain/` nie zawiera `import org.springframework.*`
- [ ] `domain/` nie zawiera `import jakarta.persistence.*`
- [ ] Metody domenowe mają expresyjne nazwy biznesowe
- [ ] Każda zmiana stanu publikuje Domain Event
- [ ] Invarianty są egzekwowane w konstruktorze lub metodach
