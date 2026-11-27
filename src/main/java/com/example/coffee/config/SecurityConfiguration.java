package com.example.coffee.config;

import com.example.coffee.api.response.ApiError;
import com.example.coffee.common.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  InMemoryUserDetailsManager ownerUserDetailsService(
      @Value("${shop.owner.username}") String username,
      @Value("${shop.owner.password-hash}") String passwordHash) {
    if (username.trim().isEmpty()
        || !passwordHash.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}")) {
      throw new IllegalStateException(
          "Configure OWNER_USERNAME and a BCrypt OWNER_PASSWORD_HASH, or explicitly use the demo profile.");
    }
    return new InMemoryUserDetailsManager(
        User.withUsername(username).password(passwordHash).roles("OWNER").build());
  }

  @Bean
  @Order(1)
  SecurityFilterChain ownerSecurity(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
    // Owner routes validate Basic credentials against BCrypt, then check OWNER; no login endpoint
    // is needed.
    AuthenticationEntryPoint unauthorized =
        (request, response, exception) -> {
          response.setHeader("WWW-Authenticate", "Basic realm=\"coffee-owner\"");
          response.setStatus(ErrorCode.UNAUTHORIZED.getHttpStatus());
          response.setContentType("application/json");
          objectMapper.writeValue(response.getOutputStream(), new ApiError(ErrorCode.UNAUTHORIZED));
        };
    return http.antMatcher("/api/v1/owner/**")
        .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("OWNER"))
        .httpBasic(basic -> basic.authenticationEntryPoint(unauthorized))
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(unauthorized)
                    .accessDeniedHandler(
                        (req, res, e) -> {
                          res.setStatus(ErrorCode.FORBIDDEN.getHttpStatus());
                          res.setContentType("application/json");
                          objectMapper.writeValue(
                              res.getOutputStream(), new ApiError(ErrorCode.FORBIDDEN));
                        }))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain customerSecurity(HttpSecurity http) throws Exception {
    // No HTTP Basic filter on public endpoints, so owner credentials are never required there.
    return http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .build();
  }
}
