package com.beautytextile.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          UserDetailsService userDetailsService,
                          PasswordEncoder passwordEncoder) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(c -> c.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // ---- Angular static files (served by Spring Boot) ----
                .requestMatchers("/", "/index.html", "/*.js", "/*.css", "/*.ico",
                                 "/assets/**", "/*.json", "/*.txt").permitAll()
                // ---- Public (customer) endpoints ----
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/auth/users/**").authenticated()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/images/**", "/error").permitAll()
                .requestMatchers("/health", "/actuator/**").permitAll()
                .requestMatchers("/api/files/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/orders", "/api/orders/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/orders/*").permitAll()
                .requestMatchers(HttpMethod.PATCH, "/api/orders/*/fulfillment").authenticated()
                .requestMatchers("/api/payment/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/offers/**").permitAll()
                // ---- Admin-only endpoints ----
                .requestMatchers("/api/admin/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/offers/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/offers/**").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/offers/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/offers/**").authenticated()
                .requestMatchers("/api/billing/**").authenticated()
                .requestMatchers("/api/returns/**").authenticated()
                .requestMatchers("/api/inventory/**").authenticated()
                .requestMatchers("/api/reports/**").authenticated()
                // Reviews — public read/submit, admin management authenticated
                .requestMatchers(HttpMethod.GET,  "/api/reviews/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/reviews").permitAll()
                .requestMatchers("/api/reviews/admin/**").authenticated()
                // Settings — public GET, admin PUT
                .requestMatchers(HttpMethod.GET, "/api/settings/**").permitAll()
                .requestMatchers(HttpMethod.PUT, "/api/settings/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/products/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/products/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/categories/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/orders").authenticated()
                .anyRequest().permitAll()   // SPA routes (/, /products, /admin/login etc.)
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Use patterns so any localhost port works in dev (e.g. ng serve on :51907)
        config.setAllowedOriginPatterns(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
