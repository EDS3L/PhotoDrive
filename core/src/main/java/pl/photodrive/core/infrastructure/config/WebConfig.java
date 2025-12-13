package pl.photodrive.core.infrastructure.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import pl.photodrive.core.infrastructure.jwt.JwtAuthenticationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
public class WebConfig {

    @Value("${SWAGGER.IP.ONE}")
    private String allowedIPOne;
    @Value("${SWAGGER.IP.TWO}")
    private String allowedIPTwo;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwt) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfig())).csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(
                auth -> auth.requestMatchers("/api/user/**").hasAnyRole("ADMIN", "PHOTOGRAPHER").requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html").access((authentication, object) -> {
                            String ipAddress = object.getRequest().getRemoteAddr();
                            boolean isAllowed = ipAddress.equals(allowedIPOne) || ipAddress.equals(allowedIPTwo);
                            return new AuthorizationDecision(isAllowed);
                }).requestMatchers("/api/auth/**").permitAll().anyRequest().authenticated()).addFilterBefore(
                jwt,
                UsernamePasswordAuthenticationFilter.class).exceptionHandling(e -> e.authenticationEntryPoint((request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
        }).accessDeniedHandler((request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Forbidden\"}");
        })).headers(headers -> headers.httpStrictTransportSecurity(HeadersConfigurer.HstsConfig::disable));


        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfig() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost", "https://photodrive.dev"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "content-disposition"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}

