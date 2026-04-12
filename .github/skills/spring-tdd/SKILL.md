---
name: spring-tdd
description: TDD z JUnit 5, Mockito i Spring Boot Test slices — wzorce testów dla domeny DDD, kontrolerów i repozytoriów JPA. Używaj dla testowania Spring Boot.
origin: java-spring-ddd-pro
---

# Spring Boot TDD Wzorce

Workflow Test-Driven Development dla Spring Boot z DDD — unit testy, slices testowe, testy integracyjne.

## Kiedy Aktywować

- Pisanie testów dla Aggregate Roots i Value Objects
- Testowanie kontrolerów REST bez pełnego kontekstu Spring
- Testowanie repozytoriów JPA z H2 in-memory
- Pisanie testów integracyjnych dla use case'ów
- Konfigurowanie fixtures domeny dla testów

---

## Unit Test — Aggregate Root

```java
// Zależności: JUnit 5 + AssertJ (brak Mockito dla czystej domeny)
class OrderTest {

    @Test
    void should_create_order_with_pending_status_and_domain_event() {
        // Given
        var customerId = CustomerId.generate();
        var lines = List.of(new OrderLineData(
            ProductId.generate(), 2, Money.of("10.00", "PLN")
        ));

        // When
        var order = Order.create(customerId, lines);

        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getCustomerId()).isEqualTo(customerId);
        assertThat(order.getLines()).hasSize(1);

        var events = order.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(OrderCreatedEvent.class);
        assertThat(((OrderCreatedEvent) events.get(0)).orderId()).isEqualTo(order.getId());
    }

    @Test
    void should_confirm_pending_order_and_emit_event() {
        // Given
        var order = OrderFixture.pendingOrder();

        // When
        order.confirm();

        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        var events = order.pullDomainEvents();
        assertThat(events)
            .hasSize(1)
            .first()
            .isInstanceOf(OrderConfirmedEvent.class);
    }

    @Test
    void should_throw_when_confirming_already_confirmed_order() {
        // Given
        var order = OrderFixture.confirmedOrder();

        // When/Then
        assertThatThrownBy(order::confirm)
            .isInstanceOf(InvalidOrderStateException.class)
            .hasMessageContaining("CONFIRMED");
    }

    @Test
    void should_reject_order_creation_with_empty_lines() {
        // Given
        var customerId = CustomerId.generate();

        // When/Then
        assertThatThrownBy(() -> Order.create(customerId, List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("co najmniej jedną linię");
    }
}
```

---

## Unit Test — Value Object

```java
class MoneyTest {

    @Test
    void should_add_money_with_same_currency() {
        var a = Money.of("10.00", "PLN");
        var b = Money.of("5.50", "PLN");

        var result = a.add(b);

        assertThat(result.amount()).isEqualByComparingTo("15.50");
        assertThat(result.currency().getCurrencyCode()).isEqualTo("PLN");
    }

    @Test
    void should_throw_when_adding_different_currencies() {
        var pln = Money.of("10.00", "PLN");
        var eur = Money.of("5.00", "EUR");

        assertThatThrownBy(() -> pln.add(eur))
            .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    void should_reject_negative_amount() {
        assertThatThrownBy(() -> Money.of("-1.00", "PLN"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_normalize_to_two_decimal_places() {
        var money = Money.of("10.999", "PLN");

        assertThat(money.amount()).isEqualByComparingTo("11.00");
    }
}
```

---

## Unit Test — Command Handler (z Mockito)

```java
@ExtendWith(MockitoExtension.class)
class ConfirmOrderCommandHandlerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private ConfirmOrderCommandHandler handler;

    @Test
    void should_confirm_order_and_publish_event() {
        // Given
        var order = OrderFixture.pendingOrder();
        var command = new ConfirmOrderCommand(order.getId(), UserId.generate());
        given(orderRepository.findById(order.getId())).willReturn(order);

        // When
        handler.handle(command);

        // Then
        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.CONFIRMED));
        verify(eventPublisher).publishAll(argThat(events ->
            events.stream().anyMatch(e -> e instanceof OrderConfirmedEvent)
        ));
    }

    @Test
    void should_throw_when_order_not_found() {
        // Given
        var orderId = OrderId.generate();
        var command = new ConfirmOrderCommand(orderId, UserId.generate());
        given(orderRepository.findById(orderId)).willThrow(new OrderNotFoundException(orderId));

        // When/Then
        assertThatThrownBy(() -> handler.handle(command))
            .isInstanceOf(OrderNotFoundException.class);
        verifyNoInteractions(eventPublisher);
    }
}
```

---

## Controller Test (@WebMvcTest — bez pełnego kontekstu)

```java
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateOrderCommandHandler createOrderHandler;

    @MockitoBean
    private GetOrderQueryHandler getOrderHandler;

    @Test
    void should_return_201_when_order_created() throws Exception {
        // Given
        var orderId = OrderId.generate();
        given(createOrderHandler.handle(any(CreateOrderCommand.class))).willReturn(orderId);

        var request = new CreateOrderRequest("customer-1", List.of(
            new OrderLineRequest("product-1", 2, "10.00")
        ));

        // When/Then
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(orderId.toString()));
    }

    @Test
    void should_return_400_when_request_invalid() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))  // Pusty obiekt — brak wymaganych pól
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void should_return_404_when_order_not_found() throws Exception {
        // Given
        var orderId = OrderId.generate();
        given(getOrderHandler.handle(any())).willThrow(new OrderNotFoundException(orderId));

        // When/Then
        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }
}
```

`@MockitoBean` jest aktualnym odpowiednikiem dawnego `@MockBean` w Spring Framework `6.2+` i Spring Boot `3.4+`. Jeśli trafiasz na starsze materiały z `@MockBean`, traktuj je jako historyczne przykłady.

---

## Repository Test (@DataJpaTest)

```java
@DataJpaTest
@Import({JpaOrderRepository.class, OrderMapper.class})
class JpaOrderRepositoryTest {

    @Autowired
    private JpaOrderRepository repository;

    @Test
    void should_save_and_retrieve_order_by_id() {
        // Given
        var order = OrderFixture.pendingOrder();

        // When
        repository.save(order);
        var found = repository.findById(order.getId());

        // Then
        assertThat(found.getId()).isEqualTo(order.getId());
        assertThat(found.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(found.getLines()).hasSize(order.getLines().size());
    }

    @Test
    void should_throw_when_order_not_found() {
        var nonExistentId = OrderId.generate();

        assertThatThrownBy(() -> repository.findById(nonExistentId))
            .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void should_find_orders_by_customer() {
        // Given
        var customerId = CustomerId.generate();
        var order1 = OrderFixture.pendingOrderForCustomer(customerId);
        var order2 = OrderFixture.pendingOrderForCustomer(customerId);
        repository.save(order1);
        repository.save(order2);

        // When
        var result = repository.findByCustomer(customerId, Pageable.unpaged());

        // Then
        assertThat(result.getTotalElements()).isEqualTo(2);
    }
}
```

---

## Test Fixtures

```java
// Klasa pomocnicza — test fixtures dla domeny
public class OrderFixture {

    public static Order pendingOrder() {
        return Order.create(
            CustomerId.of("test-customer-id"),
            List.of(new OrderLineData(
                ProductId.of("test-product-id"),
                1,
                Money.of("100.00", "PLN")
            ))
        );
    }

    public static Order confirmedOrder() {
        var order = pendingOrder();
        order.confirm();
        order.pullDomainEvents();  // Wyczyść eventy
        return order;
    }

    public static Order pendingOrderForCustomer(CustomerId customerId) {
        return Order.create(
            customerId,
            List.of(new OrderLineData(
                ProductId.generate(),
                1,
                Money.of("50.00", "PLN")
            ))
        );
    }
}
```

---

## Konfiguracja Build Toola (przykład Gradle)

```groovy
dependencies {
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
    testImplementation 'org.awaitility:awaitility'
}

tasks.named('test') {
    useJUnitPlatform()
    finalizedBy jacocoTestReport
}
```

Jeśli projekt używa Mavena, ustaw odpowiedniki w `pom.xml`. Zawsze uruchamiaj testy z katalogu modułu JVM i używaj wrappera obecnego w repo (`./gradlew`, `gradlew.bat`, `./mvnw`, `mvnw.cmd`).

---

## Konwencje Testów

```java
// Nazewnictwo metod — opisz ZACHOWANIE
// DOBRZE
should_confirm_order_and_emit_event()
should_throw_InvalidOrderStateException_when_confirming_cancelled_order()
should_return_201_when_order_created_successfully()

// ŹLE
testConfirmOrder()
confirmOrderTest()
test1()

// Struktura: Given/When/Then (AAA — Arrange/Act/Assert)
@Test
void should_[expected_behavior]_when_[condition]() {
    // Given (Arrange)
    // ...setup...

    // When (Act)
    // ...wywołanie...

    // Then (Assert)
    // ...weryfikacja...
}
```

---

## Zasady TDD dla Spring Boot

- Używaj `@WebMvcTest` + `@MockitoBean` dla kontrolerów — nie `@SpringBootTest`
- Używaj `@DataJpaTest` dla repozytoriów — nie `@SpringBootTest`
- Używaj `@ExtendWith(MockitoExtension.class)` dla unit testów serwisów
- Twórz fixtures domeny w `src/test/java/fixtures/`
- Pokrycie ≥ 80% mierzone narzędziem coverage projektu (najczęściej JaCoCo w JVM)
- Testy domeny NIE powinny mieć żadnych Spring annotacji
