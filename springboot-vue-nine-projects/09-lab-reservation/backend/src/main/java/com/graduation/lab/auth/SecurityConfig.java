package com.graduation.lab.auth;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

  @Bean
  SecurityFilterChain chain(HttpSecurity http, BearerTokenFilter bearer, AuthRateLimitFilter limiter,
                            @Qualifier("cors") CorsConfigurationSource corsConfigurationSource) throws Exception {
    return http
      .csrf(c -> c.disable())
      .cors(c -> c.configurationSource(corsConfigurationSource))
      .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .headers(h -> h
        .frameOptions(f -> f.deny())
        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
        .contentSecurityPolicy(c -> c.policyDirectives("default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
        .httpStrictTransportSecurity(s -> s.includeSubDomains(true).maxAgeInSeconds(31536000)))
      .exceptionHandling(e -> e
        .authenticationEntryPoint((req, res, ex) -> {
          res.setStatus(401); res.setContentType("application/json;charset=UTF-8");
          res.getWriter().write("{\"status\":401,\"message\":\"未认证\"}");
        })
        .accessDeniedHandler((req, res, ex) -> {
          res.setStatus(403); res.setContentType("application/json;charset=UTF-8");
          res.getWriter().write("{\"status\":403,\"message\":\"无权限\"}");
        }))
      .authorizeHttpRequests(a -> a
        .requestMatchers("/api/auth/login", "/api/auth/register", "/actuator/health", "/actuator/health/**", "/error").permitAll()
        .requestMatchers("/actuator/prometheus", "/actuator/info").hasRole("ADMIN")
        .anyRequest().authenticated())
      .addFilterBefore(limiter, UsernamePasswordAuthenticationFilter.class)
      .addFilterBefore(bearer, UsernamePasswordAuthenticationFilter.class)
      .build();
  }

  @Bean
  CorsConfigurationSource cors(@Value("${app.security.allowed-origins}") String origins) {
    var allowed = Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
    if (allowed.isEmpty()) throw new IllegalStateException("app.security.allowed-origins must not be empty");
    var c = new CorsConfiguration();
    c.setAllowedOrigins(allowed);
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    c.setExposedHeaders(List.of("Location"));
    c.setAllowCredentials(false);
    c.setMaxAge(3600L);
    var s = new UrlBasedCorsConfigurationSource();
    s.registerCorsConfiguration("/**", c);
    return s;
  }
}
