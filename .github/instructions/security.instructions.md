---
description: 'Use for security-sensitive code, auth, authorization, secrets, PII, and API review. Covers OWASP-style web risks for backend and frontend.'
---

# Security Rules — OWASP Top 10

## Zasady Bezwzględne (CRITICAL)

1. **ZERO hardcoded secrets** — tylko przez mechanizm konfiguracji projektu (`@Value`, `@ConfigurationProperties`, env vars, secret manager, vault)
2. **SQL Injection** — tylko JPA/parametryzowane zapytania, nigdy string concatenation
3. **XSS** — nigdy `dangerouslySetInnerHTML` bez sanityzacji, Content-Security-Policy
4. **Autentykacja** — trzymaj się strategii używanej przez system (session, JWT, gateway token); dla przeglądarki preferuj httpOnly cookies, jeśli architektura na to pozwala
5. **Autoryzacja** — `@PreAuthorize` na metodach serwisu, nie tylko na kontrolerach
6. **Walidacja** — `@Valid` + Bean Validation (backend), Zod (frontend)

## Skanowanie

```bash
# Backend — szukaj potencjalnych problemów
rg -n "password|secret|api[._-]?key|token" . -g "*.java" -g "*.kt"
rg -n "createNativeQuery|createQuery.*\+" . -g "*.java" -g "*.kt"  # SQL injection

# Frontend
rg -n "dangerouslySetInnerHTML|eval\(|innerHTML\s*=" . -g "*.ts" -g "*.tsx" -g "*.js" -g "*.jsx"
rg -n "localStorage.*token|sessionStorage.*token|localStorage.*password|sessionStorage.*password" . -g "*.ts" -g "*.tsx" -g "*.js" -g "*.jsx"
```

## Spring Security — Podstawowa Konfiguracja

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)  // stateless API
        .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/**").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
}
```

## JWT — Bezpieczeństwo (jeśli projekt używa JWT)

```java
// SECRET z configu/env, NIE hardcoded
@Value("${security.jwt.secret}")
private String secret;

// Podpisuj algorytmem HS256 minimum, RS256 najlepiej
// Krótki czas życia access tokena (15 min), refresh token oddzielnie
```

## Komunikaty Błędów

```java
// ŹLE — ujawnia szczegóły implementacji
throw new RuntimeException("User with email " + email + " not found in database");

// DOBRZE — ogólny komunikat
throw new AuthenticationException("Nieprawidłowe dane logowania");
```

## Przed Commitem — Checklist

- [ ] Brak hardcoded passwords/keys/tokens
- [ ] Wszystkie pola request DTO mają `@NotBlank`/`@NotNull`
- [ ] SQL — tylko JPA lub `@Param` parametry
- [ ] HTML — brak `dangerouslySetInnerHTML` bez DOMPurify
- [ ] Autoryzacja sprawdzona na każdym endpoincie
- [ ] Lokalne pliki z sekretami są w `.gitignore`
- [ ] Komunikaty błędów nie ujawniają stack trace
