package com.vulinh.configuration;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final ApplicationProperties applicationProperties;

  @Bean
  static PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  @Order(1)
  SecurityFilterChain authorizationServerFilterChain(HttpSecurity http) throws Exception {
    var authorizationServerConfigurer = OAuth2AuthorizationServerConfigurer.authorizationServer();

    return http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
        .with(authorizationServerConfigurer, Customizer.withDefaults())
        .build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
    return http.securityMatcher("/auth/**", "/swagger-ui/**", "/v3/api-docs/**")
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .build();
  }

  @Bean
  @Order(3)
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .bearerTokenResolver(new CookieBearerTokenResolver())
                    .jwt(Customizer.withDefaults()))
        .build();
  }

  @Bean
  JWKSource<SecurityContext> jwkSource(Ed25519JwtEncoder encoder) {
    return new ImmutableJWKSet<>(new JWKSet(encoder.getEdJwkPair()));
  }

  @Bean
  JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
    var processor = new DefaultJWTProcessor<>();
    processor.setJWSKeySelector(
        new JWSVerificationKeySelector<>(
            Ed25519JwtEncoder.resolveJWSAlgorithm(applicationProperties.security().signingKey()),
            jwkSource));
    return new NimbusJwtDecoder(processor);
  }

  @Bean
  AuthorizationServerSettings authorizationServerSettings() {
    return AuthorizationServerSettings.builder()
        .issuer(applicationProperties.security().issuerServer())
        .build();
  }
}
