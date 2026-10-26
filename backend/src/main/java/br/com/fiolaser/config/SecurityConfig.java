package br.com.fiolaser.config;

import br.com.fiolaser.security.*;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.*;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {
  private final JwtAuthFilter jwt;
  private final GoogleSuccessHandler google;
  private final String frontend;

  public SecurityConfig(
      JwtAuthFilter jwt,
      GoogleSuccessHandler google,
      @Value("${app.cors.frontend-url}") String frontend) {
    this.jwt = jwt;
    this.google = google;
    this.frontend = frontend;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .securityContext(sc -> sc.securityContextRepository(new NullSecurityContextRepository()))
        .requestCache(cache -> cache.disable())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                    new HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED)))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/api/auth/login",
                        "/api/auth/registrar",
                        "/api/auth/google/exchange",
                        "/api/services/**",
                        "/api/bookings/availability/**",
                        "/oauth2/**",
                        "/login/**",
                        "/actuator/health")
                    .permitAll()
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .oauth2Login(
            o ->
                o.successHandler(google)
                    .failureHandler(
                        (req, res, e) -> {
                          var session = req.getSession(false);
                          if (session != null) session.invalidate();
                          res.sendRedirect(frontend + "/login.html?erro=google");
                        }))
        .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  public static PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    var config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(frontend));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setExposedHeaders(List.of("Content-Disposition"));
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
