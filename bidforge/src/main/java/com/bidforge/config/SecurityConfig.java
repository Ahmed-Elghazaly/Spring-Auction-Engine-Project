package com.bidforge.config;

import com.bidforge.security.JwtAuthFilter;
import com.bidforge.security.RestAccessDeniedHandler;
import com.bidforge.security.RestAuthEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


// @EnableMethodSecurity additionally activates method-level checks
// @PreAuthorize is used as a second safety layer on admin services

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RestAuthEntryPoint authEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          RestAuthEntryPoint authEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authEntryPoint = authEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    // The chain of security filters every HTTP request passes through, and the authorization rules evaluated at the end
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF os for HTML/session apps, Our API is stateless with no cookies and no sessions
                // we only use the JWT
                .csrf(csrf -> csrf.disable())

                // we don't create an HTTP session bec every request authenticates itself from scratch via its JWT
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))

                // URL level authorization rules, first match wins so specific rules come before general ones
                .authorizeHttpRequests(auth -> auth
                        // Registration and login are open by definition
                        .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                        // user's own data endpoints must come before the general GET
                        .requestMatchers("/api/auctions/mine", "/api/auctions/won").authenticated()
                        // Browsing auctions is public, visitors can look before creating an account
                        // but all write operations on auctions still require a JWT
                        .requestMatchers(HttpMethod.GET, "/api/auctions/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Everything else just requires being logged in.
                        .anyRequest().authenticated())

                // Run our JWT filter before Spring's own username/password filter so the SecurityContext is populated in time
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    // The object that actually verifies username and password at login
    // Spring assembles it from our CustomUserDetailsService which loads the account and the PasswordEncoder which compares hashes
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
