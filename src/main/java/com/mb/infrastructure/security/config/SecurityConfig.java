package com.mb.infrastructure.security.config;

import com.mb.common.constant.ApiEndpoint;
import com.mb.infrastructure.security.filter.JwtAuthenticationFilter;
import com.mb.modules.auth.role.enums.RoleName;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * SecurityConfig class is responsible for configuring the security settings of the application. It
 * enables web security and method security to protect the application from unauthorized access.
 *
 * @author rohit.kavthekar
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
  private final CorsConfigurationSource corsConfigurationSource;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) {

    http
        // CSRF disabled for JWT-based stateless REST API
        .csrf(AbstractHttpConfigurer::disable)

        // Disable form login since this is a REST API
        .formLogin(AbstractHttpConfigurer::disable)

        // Disable HTTP Basic authentication
        .httpBasic(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(ex -> ex.authenticationEntryPoint(customAuthenticationEntryPoint))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(
            requests ->
                requests
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers("/actuator/health", "/actuator/info")
                    .permitAll()
                    // PoC-ONLY: public virtual thread demo endpoints. Remove together with
                    // modules/virtualthread before using this boilerplate in a real project.
                    .requestMatchers(ApiEndpoint.VIRTUAL_THREADS_PUBLIC_PATTERN)
                    .permitAll()
                    .requestMatchers("/actuator/**")
                    .hasRole(RoleName.ADMIN.name())
                    .anyRequest()
                    .authenticated());

    return http.build();
  }
}
