package com.asdru.asdrulet5.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

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
        RequestMatcher apiOrWsMatcher = new OrRequestMatcher(
                PathPatternRequestMatcher.pathPattern("/api/**"),
                PathPatternRequestMatcher.pathPattern("/ws")
        );

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
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/me").permitAll()
                        // Read-only state and the realtime channel carry no secrets beyond
                        // what a party code already grants access to, and both need to work
                        // for the dev-only "Quick game" tool's synthetic, session-less
                        // players (see PartyDevSessionController / PartyDevController /
                        // CombatDevController) — those write endpoints are still safe
                        // because they 404 unless app.dev-tools.enabled is set.
                        .requestMatchers(HttpMethod.GET, "/api/parties/*", "/api/parties/*/combat",
                                "/api/parties/*/dungeon", "/api/classes/**")
                        .permitAll()
                        .requestMatchers("/ws").permitAll()
                        .requestMatchers("/api/parties/dev", "/api/parties/dev/**", "/api/parties/*/dev/**",
                                "/api/parties/*/combat/dev/**", "/api/parties/*/dungeon/dev/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        apiOrWsMatcher
                ))
                .oauth2Login(oauth2 -> oauth2.defaultSuccessUrl(frontendUrl, true));

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
