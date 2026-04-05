package com.vulinh.configuration;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  @Order(1)
  SecurityFilterChain authorizationServerFilterChain(HttpSecurity http) throws Exception {
    http.with(OAuth2AuthorizationServerConfigurer.authorizationServer(), Customizer.withDefaults());
    return http.build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/auth/**", "/swagger-ui/**", "/v3/api-docs/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .bearerTokenResolver(new CookieBearerTokenResolver())
                    .jwt(Customizer.withDefaults()))
        .build();
  }

  @Bean
  AuthorizationServerSettings authorizationServerSettings(
      ApplicationProperties applicationProperties) {
    return AuthorizationServerSettings.builder()
        .issuer(applicationProperties.security().issuerServer())
        .build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  JWKSource<SecurityContext> jwkSource(KeyPair ecKeyPair) {
    var ecKey =
        new ECKey.Builder(Curve.P_256, (ECPublicKey) ecKeyPair.getPublic())
            .privateKey((ECPrivateKey) ecKeyPair.getPrivate())
            .keyID(UUID.randomUUID().toString())
            .algorithm(JWSAlgorithm.ES256)
            .build();

    return new ImmutableJWKSet<>(new JWKSet(ecKey));
  }

  @Bean
  JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
    return new NimbusJwtEncoder(jwkSource);
  }

  @Bean
  JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
    var processor = new DefaultJWTProcessor<>();

    processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.ES256, jwkSource));

    return new NimbusJwtDecoder(processor);
  }

  @Bean
  KeyPair ecKeyPair() throws Exception {
    var generator = KeyPairGenerator.getInstance("EC");
    generator.initialize(Curve.P_256.toECParameterSpec());
    return generator.generateKeyPair();
  }

}
