package com.bidforge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

// Browsers block JavaScript on origin A (http://localhost:4200 (the Angular dev server) from calling an API on origin B (http://localhost:8080)
// unless origin B explicitly allows it
// Postman/curl are not browsers and ignore CORS entirely
// SecurityConfig .cors(Customizer.withDefaults())} picks this bean up automatically.
// Allowed origins are configurable per environment via bidforge.cors.allowed-origins property
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${bidforge.cors.allowed-origins:http://localhost:4200}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // We use header JWTs, not cookies, so credentials stay disabled
        config.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
