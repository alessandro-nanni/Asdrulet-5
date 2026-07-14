package com.asdru.asdrulet5.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * There's no login of any kind here — every identity (party leader, member,
 * combatant) is just a string the client generates once and presents on
 * every request, the same way the old dev-only "quick game" tooling worked;
 * see AuthenticatedUser. Authorization is therefore entirely a domain-layer
 * concern (Party/Dungeon/Combat comparing the presented id against the
 * id(s) recorded when the party/turn was set up), not something Spring
 * Security enforces — this filter chain exists only for CORS and CSRF.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final String frontendUrl;

    // Kept as a plain constructor: Lombok's @RequiredArgsConstructor would copy
    // @Value onto the generated constructor parameter (via lombok.config), but
    // @Value stays on the field too, causing Spring to *also* reflectively
    // inject the same final field, which triggers a "final field mutated
    // reflectively" JVM warning (a hard error in future JDKs). Not worth it
    // for one field.
    public SecurityConfig(@Value("${app.frontend-url}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // Plain (non-XOR-masked) handler so the cookie value equals what the
                        // SPA must echo back in the X-XSRF-TOKEN header. See Spring Security's
                        // "CSRF - Integrating with Single Page Applications" guide.
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendUrl));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
