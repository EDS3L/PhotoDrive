package pl.photodrive.core.infrastructure.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import pl.photodrive.core.infrastructure.jwt.JwtAuthenticationFilter;
import pl.photodrive.core.infrastructure.security.OriginValidationFilter;
import pl.photodrive.core.infrastructure.security.RateLimitFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class WebConfig {

    @Value("${SWAGGER_USER}")
    private String swaggerUser;
    @Value("${SWAGGER_PASSWORD}")
    private String swaggerPassword;

    @Value("${app.csrf.allowed-origins:https://photodrive.dev}")
    private String allowedOrigins;

    /** Ta sama flaga co w {@link OriginValidationFilter} — na prodzie `false` (`application-prod.yml`). */
    @Value("${app.csrf.allow-localhost-origins:true}")
    private boolean allowLocalhostOrigins;


    @Bean
    @Order(1)
    public SecurityFilterChain swaggerFilterChain(HttpSecurity http, BCryptPasswordEncoder passwordEncoder) throws Exception {
        http
                .securityMatcher("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/login", "/logout")
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .formLogin(form -> form
                        .defaultSuccessUrl("/swagger-ui/index.html", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/swagger-ui.html")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .authenticationManager(new ProviderManager(
                        createDaoAuthProvider(passwordEncoder)
                ));

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiFilterChain(HttpSecurity http,
                                              JwtAuthenticationFilter jwt,
                                              OriginValidationFilter originValidationFilter,
                                              RateLimitFilter rateLimitFilter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfig()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/user/add").hasAnyRole("ADMIN", "PHOTOGRAPHER")
                        .requestMatchers("/api/user/all").hasRole("ADMIN")
                        .requestMatchers("/api/user/activeUsers").hasRole("ADMIN")
                        .requestMatchers("/api/user/*/addRole").hasRole("ADMIN")
                        .requestMatchers("/api/user/*/removeRole").hasRole("ADMIN")
                        .requestMatchers("/api/user/*/activateUser").hasRole("ADMIN")
                        .requestMatchers("/api/user/*/deactivateUser").hasRole("ADMIN")
                        .requestMatchers("/api/user/*/assignUsers").hasRole("ADMIN")
                        .requestMatchers("/api/user/*/removeUsers").hasRole("ADMIN")
                        .requestMatchers("/api/album/*/setPublic").hasRole("ADMIN")
                        // Etykieta/kolejność zakładki portfolio — para do setPublic (tylko admin).
                        .requestMatchers("/api/album/*/display").hasRole("ADMIN")
                        // Status watermarku czyta też fotograf (steruje widocznością akcji w UI);
                        // zarządzanie samym logiem (GET/PUT/DELETE /api/watermark) tylko ADMIN.
                        .requestMatchers("/api/watermark/status").hasAnyRole("ADMIN", "PHOTOGRAPHER")
                        .requestMatchers("/api/watermark", "/api/watermark/**").hasRole("ADMIN")
                        // Sloty strony wizytówki: zarządza tylko admin; odczyt publiczny żyje pod /api/public/site.
                        .requestMatchers("/api/site/**").hasRole("ADMIN")
                        .requestMatchers("/api/user/*/changePassword").authenticated()
                        .requestMatchers("/api/user/*/changeEmail").authenticated()
                        .requestMatchers("/api/user/me").authenticated()
                        // Lista klientów należy do fotografa; admin ma osobny endpoint (/{id}/assignedUsers).
                        // Odmowa pada już na filtrze web (obrona w głąb), a nie dopiero w domenie (A13).
                        .requestMatchers("/api/user/getAssignedUsers").hasRole("PHOTOGRAPHER")
                        .requestMatchers("/api/auth/**", "/api/public/**", "/favicon.ico", "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(originValidationFilter, JwtAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, OriginValidationFilter.class)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Unauthorized\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Forbidden\"}");
                        })
                )
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                        .contentTypeOptions(contentTypeOptions -> {})
                        .frameOptions(frameOptions -> frameOptions.deny())
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfig() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        // Localhost jako dozwolony origin tylko poza produkcją — patrz OriginValidationFilter.
        if (allowLocalhostOrigins) {
            configuration.setAllowedOriginPatterns(List.of("http://localhost:*", "http://localhost"));
        }
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With", "content-disposition"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @SuppressWarnings("deprecation")
    private DaoAuthenticationProvider createDaoAuthProvider(BCryptPasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(passwordEncoder);
        provider.setUserDetailsService(new InMemoryUserDetailsManager(
                User.withUsername(swaggerUser)
                        .password(passwordEncoder.encode(swaggerPassword))
                        .roles("SWAGGER_USER")
                        .build()
        ));
        return provider;
    }

}

