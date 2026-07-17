package com.ledgerai.auth.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Security configuration (SECURITY §4, §11, §15; ADR-001, ADR-018).
 *
 * <p>
 * A single stateless filter validates the Bearer access token on protected
 * requests; the public
 * endpoints are exactly registration, login, refresh, and the operational
 * health check. Access tokens
 * are signed/validated with a symmetric key supplied only via configuration.
 * CORS allows credentials
 * for the configured trusted origins so the refresh cookie can flow; CSRF is
 * not token-based here —
 * the only cookie is the SameSite refresh cookie (ADR-018), and all other
 * endpoints are Bearer-only.
 *
 * <p>
 * The filter chain establishes <em>authentication</em> only, and denies by default
 * ({@code anyRequest().authenticated()}). <em>Authorization</em> is ownership-based
 * and is enforced in the service layer via
 * {@link com.ledgerai.common.security.OwnershipGuard} — never from URL structure
 * (ARCHITECTURE §7.1, §9.2; SECURITY §5). There is intentionally no role or
 * permission model here: LedgerAI's access control is ownership, and RBAC is an
 * explicitly future concern (SECURITY §5, §19, §20).
 */
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {
    
    private final AuthProperties properties;
    private final HandlerExceptionResolver handlerExceptionResolver;
    
    public SecurityConfig(AuthProperties properties,
                          @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.properties = properties;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                                               .requestMatchers(HttpMethod.POST,
                                                   "/api/v1/auth/register",
                                                   "/api/v1/auth/login",
                                                   "/api/v1/auth/refresh")
                                               .permitAll()
                                               .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                                               .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())))
            // Failures raised inside the filter chain never reach @RestControllerAdvice on their own, so
            // they would answer with an empty body. Routing them through the resolver renders them as the
            // same RFC 7807 documents as everything else (API_SPEC §2.12) — one error model, no exceptions.
            .exceptionHandling(exceptions -> exceptions
                                                 .authenticationEntryPoint((request, response, ex) ->
                                                                               handlerExceptionResolver.resolveException(request, response, null, ex))
                                                 .accessDeniedHandler((request, response, ex) ->
                                                                          handlerExceptionResolver.resolveException(request, response, null, ex)));
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey()));
    }
    
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey()).macAlgorithm(MacAlgorithm.HS256).build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.cors().allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
    
    private SecretKey secretKey() {
        byte[] keyBytes = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }
}
