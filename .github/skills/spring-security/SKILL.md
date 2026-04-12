---
name: spring-security
description: Wzorce Spring Security — JWT, autoryzacja oparta na rolach, OAuth2, bezpieczne endpointy REST. Używaj dla konfiguracji bezpieczeństwa Spring Boot.
origin: java-spring-ddd-pro
---

# Spring Security Wzorce

Konfiguracja bezpieczeństwa dla Spring Boot 3+ z JWT i autoryzacją opartą na rolach.

## Kiedy Aktywować

- Konfigurowanie uwierzytelniania (JWT, session-based, OAuth2)
- Zabezpieczanie endpointów REST z rolami/uprawnieniami
- Implementacja rejestracji i logowania użytkownika
- Konfigurowanie CORS dla SPA (React/Vite)
- Dodawanie rate limiting i ochrony przed atakami

---

## Konfiguracja Spring Security (JWT Stateless)

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Włącza @PreAuthorize
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)  // Stateless API — CSRF nie potrzebne
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/public/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler(new AccessDeniedHandlerImpl())
            )
            .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        // SECURITY: Definiuj allowedOrigins explicite — nigdy "*" na produkcji
        config.setAllowedOrigins(List.of("http://localhost:5173")); // Vite dev server
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

---

## JWT Token Service

```java
@Service
public class JwtService {

    // SECURITY: Secret pobierany z konfiguracji, nigdy hardcoded
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}") // 24h domyślnie
    private long jwtExpirationMs;

    public String generateToken(UserDetails userDetails) {
        return generateToken(Map.of(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
            .claims(extraClaims)
            .subject(userDetails.getUsername())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(getSigningKey(), Jwts.SIG.HS256)
            .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return claimsResolver.apply(claims);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

---

## JWT Authentication Filter

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        try {
            final String username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    var authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException e) {
            // Nieprawidłowy token — nie ustawiamy autentykacji (request zostanie odrzucony jako 401)
            log.debug("Invalid JWT token: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
```

---

## Auth Controller

```java
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }
}

// DTOs
public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, max = 100) String password,
    @NotBlank @Size(max = 50) String name
) {}

public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password
) {}

public record AuthResponse(String token, String refreshToken, String email, List<String> roles) {}
```

---

## Method-Level Security

```java
// Włączone przez @EnableMethodSecurity w SecurityConfig
@Service
public class OrderService {

    @PreAuthorize("hasRole('USER')")
    public Order createOrder(CreateOrderCommand command) { ... }

    @PreAuthorize("hasRole('ADMIN') or @orderSecurityService.isOwner(#orderId, authentication.name)")
    public void cancelOrder(OrderId orderId) { ... }

    @PostFilter("filterObject.customerId == authentication.name or hasRole('ADMIN')")
    public List<Order> getOrders() { ... }
}

// Serwis do sprawdzania własności
@Service
public class OrderSecurityService {
    private final OrderRepository orderRepository;

    public boolean isOwner(String orderId, String username) {
        return orderRepository.findOptionalById(OrderId.of(orderId))
            .map(order -> order.getCustomerId().toString().equals(username))
            .orElse(false);
    }
}
```

---

## Rate Limiting (Bucket4j)

```java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // SECURITY: Używaj getRemoteAddr() z poprawnie skonfigurowanym ForwardedHeaderFilter
    // NIE czytaj X-Forwarded-For bezpośrednio bez weryfikacji proxy
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

        String clientIp = request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(clientIp, k ->
            Bucket.builder()
                .addLimit(Bandwidth.simple(100, Duration.ofMinutes(1)))
                .build()
        );

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Ogranicz tylko endpointy auth
        return !request.getRequestURI().startsWith("/api/v1/auth");
    }
}
```

---

## application.yml Security Config

```yaml
app:
  jwt:
    # SECURITY: Wygeneruj silny secret: openssl rand -base64 64
    secret: ${JWT_SECRET}
    expiration-ms: 86400000  # 24h

spring:
  security:
    headers:
      content-security-policy: "default-src 'self'"
      
server:
  # Wymusz HTTPS w produkcji
  forward-headers-strategy: FRAMEWORK

management:
  endpoints:
    web:
      exposure:
        # Nie eksponuj wszystkiego na produkcji
        include: health,info
  endpoint:
    health:
      show-details: when-authorized
```

---

## Zasady Bezpieczeństwa

- Używaj `BCryptPasswordEncoder(12)` — nigdy MD5, SHA1
- JWT secret z environment variable (`${JWT_SECRET}`) — nigdy w kodzie
- CORS allowedOrigins explicite zdefiniowane — nigdy `"*"` na produkcji
- Rate limiting na endpointach auth
- `@Valid` na wszystkich `@RequestBody`
- Loguj nieudane próby auth (bez hasła/tokenu)
- Actuator endpointy zabezpieczone na produkcji
