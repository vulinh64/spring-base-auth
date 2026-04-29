package com.vulinh.configuration;

import module java.base;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.vulinh.data.ServiceCodeError;
import com.vulinh.data.dto.TokenType;
import com.vulinh.data.repository.ClientRepository;
import com.vulinh.exception.ServiceAuthenticationException;
import com.vulinh.service.TokenMinter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  public static final String AS_ADMIN_AUDIENCE = "admin-cli";

  private final ApplicationProperties applicationProperties;

  @Bean
  static PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Order 1 — anonymous access for /login, /refresh, /exchange, JWKS, OIDC discovery, health, docs.
   */
  @Bean
  @Order(1)
  SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
    var security = applicationProperties.security();
    var pathBuilder = PathPatternRequestMatcher.withDefaults();
    var matchers =
        Stream.concat(
                Arrays.stream(security.noAuthUrls()),
                Stream.of(security.jwksPath(), security.discoveryPath()))
            .map(pathBuilder::matcher)
            .toArray(RequestMatcher[]::new);

    return http.securityMatcher(new OrRequestMatcher(matchers))
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a.anyRequest().permitAll())
        .build();
  }

  /**
   * Order 2 — FE-facing endpoints under /accounts/**. Gated by session_token (aud == "session", typ
   * == "session") read from the cookie. Used for /accounts/me and similar.
   */
  @Bean
  @Order(2)
  SecurityFilterChain accountsFilterChain(HttpSecurity http, JwtDecoder sessionJwtDecoder)
      throws Exception {
    var pathBuilder = PathPatternRequestMatcher.withDefaults();
    return http.securityMatcher(pathBuilder.matcher("/accounts/**"))
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a.anyRequest().authenticated())
        .oauth2ResourceServer(
            o ->
                o.bearerTokenResolver(
                        new CookieBearerTokenResolver(
                            applicationProperties.security().sessionTokenCookieName()))
                    .jwt(j -> j.decoder(sessionJwtDecoder)))
        .build();
  }

  /**
   * Order 3 — AS's own admin endpoints. Requires an access_token whose aud == "admin-cli" and typ
   * == "access". Token is sent via the Authorization header (default bearer resolver), since access
   * tokens are returned in /exchange's response body, not as cookies. Same chain pattern any BE
   * service will use to gate its own resource server.
   */
  @Bean
  @Order(3)
  SecurityFilterChain adminFilterChain(HttpSecurity http, JwtDecoder asAdminJwtDecoder)
      throws Exception {
    var pathBuilder = PathPatternRequestMatcher.withDefaults();
    return http.securityMatcher(pathBuilder.matcher("/admin/**"))
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a.anyRequest().authenticated())
        .oauth2ResourceServer(o -> o.jwt(j -> j.decoder(asAdminJwtDecoder)))
        .build();
  }

  /**
   * Order 4 — BE-to-AS internal calls, gated by X-Service-Key (or bypassed in local profile). The
   * filter is constructed inline (not a Spring bean) so it never auto-registers as a global servlet
   * filter and only runs inside this chain.
   */
  @Bean
  @Order(4)
  SecurityFilterChain internalFilterChain(
      HttpSecurity http,
      ClientRepository clientRepository,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver)
      throws Exception {
    var serviceApiKeyFilter =
        new ServiceApiKeyFilter(applicationProperties, clientRepository, handlerExceptionResolver);

    var pathBuilder = PathPatternRequestMatcher.withDefaults();
    return http.securityMatcher(pathBuilder.matcher("/internal/**"))
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a.anyRequest().hasRole("SERVICE"))
        .addFilterBefore(serviceApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  /** Order 5 — catch-all. Default-deny: anything not matched by a higher chain returns 403. */
  @Bean
  @Order(5)
  SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
    return http.securityMatcher(AnyRequestMatcher.INSTANCE)
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a.anyRequest().denyAll())
        .build();
  }

  @Bean
  RouterFunction<ServerResponse> jwksRoute(JWKSource<SecurityContext> jwkSource) {
    return RouterFunctions.route(
        RequestPredicates.GET(applicationProperties.security().jwksPath()),
        _ -> {
          var keys = jwkSource.get(new JWKSelector(new JWKMatcher.Builder().build()), null);
          return ServerResponse.ok()
              .contentType(MediaType.APPLICATION_JSON)
              .body(new JWKSet(keys).toJSONObject(true));
        });
  }

  /**
   * Minimal OIDC discovery document. Let Spring's {@code JwtDecoders.fromIssuerLocation(...)} and
   * {@code spring.security.oauth2.resourceserver.jwt.issuer-uri} bootstrap from this URL alone — BE
   * services don't need to know our JWKS path. We deliberately omit OAuth2-flow endpoints
   * (authorization, token, etc.) since this server is a JWT issuer for trusted FEs, not a
   * standards-compliant OIDC provider.
   */
  @Bean
  RouterFunction<ServerResponse> discoveryRoute() {
    var security = applicationProperties.security();
    var discovery = buildDiscoveryDoc(security);
    return RouterFunctions.route(
        RequestPredicates.GET(security.discoveryPath()),
        _ -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(discovery));
  }

  /**
   * Static so it can be unit-tested without Spring. The shape of this Map is part of the public
   * contract with BE services using {@code JwtDecoders.fromIssuerLocation(...)}; if you change it,
   * update {@code SPRING_BASE_INTEGRATION.md} §3 and let consuming teams know.
   */
  static java.util.Map<String, Object> buildDiscoveryDoc(ApplicationProperties.Security security) {
    var issuer = security.issuerServer();
    return java.util.Map.of(
        "issuer",
        issuer,
        "jwks_uri",
        issuer + security.jwksPath(),
        "id_token_signing_alg_values_supported",
        java.util.List.of("RS256"),
        "subject_types_supported",
        java.util.List.of("public"));
  }

  @Bean
  JWKSource<SecurityContext> jwkSource() throws NoSuchAlgorithmException {
    var keyPair = KeyPairGenerator.getInstance("RSA");
    keyPair.initialize(2048);
    var rsa = keyPair.generateKeyPair();

    var rsaKey =
        new RSAKey.Builder((RSAPublicKey) rsa.getPublic())
            .privateKey((RSAPrivateKey) rsa.getPrivate())
            .keyID(UUID.randomUUID().toString())
            .keyUse(KeyUse.SIGNATURE)
            .algorithm(JWSAlgorithm.RS256)
            .build();

    return new ImmutableJWKSet<>(new JWKSet(rsaKey));
  }

  @Bean
  JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
    return new NimbusJwtEncoder(jwkSource);
  }

  /**
   * General-purpose decoder used by AuthService for /refresh and /exchange (which inspect the
   * decoded JWT manually). Only signature + issuer + timestamps are validated here — typ and aud
   * checks are done by callers.
   */
  @Bean
  JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
    var processor = new DefaultJWTProcessor<>();
    processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource));
    var decoder = new NimbusJwtDecoder(processor);
    decoder.setJwtValidator(
        JwtValidators.createDefaultWithIssuer(applicationProperties.security().issuerServer()));
    return decoder;
  }

  /**
   * Hardened decoder for the AS admin chain — adds aud == "admin-cli" and typ == "access" checks on
   * top of the default validators. Mirrors what BE services will configure for themselves.
   */
  @Bean
  JwtDecoder asAdminJwtDecoder(JWKSource<SecurityContext> jwkSource) {
    var processor = new DefaultJWTProcessor<>();
    processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource));
    var decoder = new NimbusJwtDecoder(processor);
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(applicationProperties.security().issuerServer()),
            new JwtAudValidator(AS_ADMIN_AUDIENCE),
            new JwtTypValidator(TokenType.ACCESS)));
    return decoder;
  }

  /** Decoder for the FE-facing /accounts chain — requires aud == "session" and typ == "session". */
  @Bean
  JwtDecoder sessionJwtDecoder(JWKSource<SecurityContext> jwkSource) {
    var processor = new DefaultJWTProcessor<>();
    processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource));
    var decoder = new NimbusJwtDecoder(processor);
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(applicationProperties.security().issuerServer()),
            new JwtAudValidator(TokenMinter.SESSION_AUDIENCE),
            new JwtTypValidator(TokenType.SESSION)));
    return decoder;
  }

  /**
   * Inner filter class — intentionally not a Spring bean so it doesn't auto-register as a global
   * servlet filter. Constructed by {@link #internalFilterChain} and only wired into the {@code
   * /internal/**} chain. Authentication failures throw {@link ServiceAuthenticationException}; we
   * route them through the configured {@code handlerExceptionResolver} so {@link
   * com.vulinh.exception.GlobalExceptionHandler}'s {@code @ExceptionHandler} fires from the filter
   * layer too.
   */
  static final class ServiceApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Service-Key";
    private static final String LOCAL_BYPASS_PRINCIPAL = "local-debug";

    private final ApplicationProperties applicationProperties;
    private final ClientRepository clientRepository;
    private final HandlerExceptionResolver handlerExceptionResolver;

    ServiceApiKeyFilter(
        ApplicationProperties applicationProperties,
        ClientRepository clientRepository,
        HandlerExceptionResolver handlerExceptionResolver) {
      this.applicationProperties = applicationProperties;
      this.clientRepository = clientRepository;
      this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain chain)
        throws ServletException, IOException {
      try {
        authenticate(request);
        chain.doFilter(request, response);
      } catch (ServiceAuthenticationException ex) {
        handlerExceptionResolver.resolveException(request, response, null, ex);
      }
    }

    private void authenticate(HttpServletRequest request) {
      if (applicationProperties.security().skipServiceKeyVerification()) {
        // Local profile: skip the API key check entirely for quick BE-to-BE debugging.
        SecurityContextHolder.getContext()
            .setAuthentication(serviceAuthentication(LOCAL_BYPASS_PRINCIPAL));
        return;
      }

      var presented = request.getHeader(HEADER_NAME);
      if (presented == null || presented.isBlank()) {
        throw new ServiceAuthenticationException(
            "Missing %s header".formatted(HEADER_NAME), ServiceCodeError.MISSING_SERVICE_KEY);
      }

      var match = clientRepository.findByServiceApiKeyHashAndEnabledIsTrue(sha256Hex(presented));
      if (match.isEmpty()) {
        throw new ServiceAuthenticationException(
            "Invalid %s header".formatted(HEADER_NAME), ServiceCodeError.INVALID_SERVICE_KEY);
      }

      SecurityContextHolder.getContext()
          .setAuthentication(serviceAuthentication(match.get().getClientId()));
    }

    private static UsernamePasswordAuthenticationToken serviceAuthentication(String principal) {
      return new UsernamePasswordAuthenticationToken(
          principal, null, List.of(new SimpleGrantedAuthority("ROLE_SERVICE")));
    }

    private static String sha256Hex(String input) {
      try {
        var md = MessageDigest.getInstance("SHA-256");
        var digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
      } catch (NoSuchAlgorithmException e) {
        throw new IllegalStateException("SHA-256 unavailable", e);
      }
    }
  }
}
