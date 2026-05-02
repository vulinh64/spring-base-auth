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
import com.vulinh.data.dto.TokenType;
import com.vulinh.exception.ServiceAuthenticationException;
import com.vulinh.service.TokenMinter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
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

    return baseStateless(
            http,
            new OrRequestMatcher(
                Stream.concat(
                        Arrays.stream(security.noAuthUrls()),
                        Stream.of(security.jwksPath(), security.discoveryPath()))
                    .map(PathPatternRequestMatcher.withDefaults()::matcher)
                    .toArray(RequestMatcher[]::new)))
        .authorizeHttpRequests(
            authorizeHttpRequestsCustomizer ->
                authorizeHttpRequestsCustomizer.anyRequest().permitAll())
        .build();
  }

  /**
   * Order 2 — FE-facing endpoints under /accounts/**. Gated by session_token (aud == "session", typ
   * == "session") read from the cookie. Used for /accounts/me and similar.
   */
  @Bean
  @Order(2)
  SecurityFilterChain accountsFilterChain(HttpSecurity http, JWKSource<SecurityContext> jwkSource)
      throws Exception {
    var security = applicationProperties.security();

    return baseStateless(http, PathPatternRequestMatcher.withDefaults().matcher("/accounts/**"))
        .authorizeHttpRequests(
            authorizeHttpRequestsCustomizer ->
                authorizeHttpRequestsCustomizer.anyRequest().authenticated())
        .oauth2ResourceServer(
            oauth2ResourceServerCustomizer ->
                oauth2ResourceServerCustomizer
                    .bearerTokenResolver(
                        new CookieBearerTokenResolver(security.sessionTokenCookieName()))
                    .jwt(
                        jwtConfigurer ->
                            jwtConfigurer
                                .decoder(
                                    hardenedJwtDecoder(
                                        jwkSource,
                                        security.issuerServer(),
                                        TokenMinter.SESSION_AUDIENCE,
                                        TokenType.SESSION))
                                .jwtAuthenticationConverter(plainAuthorityConverter())))
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
  SecurityFilterChain adminFilterChain(HttpSecurity http, JWKSource<SecurityContext> jwkSource)
      throws Exception {
    var asAdminJwtDecoder =
        hardenedJwtDecoder(
            jwkSource,
            applicationProperties.security().issuerServer(),
            AS_ADMIN_AUDIENCE,
            TokenType.ACCESS);

    return baseStateless(http, PathPatternRequestMatcher.withDefaults().matcher("/admin/**"))
        .authorizeHttpRequests(a -> a.anyRequest().authenticated())
        .oauth2ResourceServer(
            o ->
                o.jwt(
                    j ->
                        j.decoder(asAdminJwtDecoder)
                            .jwtAuthenticationConverter(plainAuthorityConverter())))
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
      InterServiceAuthenticator interServiceAuthenticator,
      HandlerExceptionResolver handlerExceptionResolver)
      throws Exception {
    var serviceApiKeyFilter =
        new ServiceApiKeyFilter(interServiceAuthenticator, handlerExceptionResolver);

    return baseStateless(http, PathPatternRequestMatcher.withDefaults().matcher("/internal/**"))
        .authorizeHttpRequests(
            authorizeHttpRequestsCustomizer ->
                authorizeHttpRequestsCustomizer.anyRequest().hasAnyAuthority("SERVICE"))
        .addFilterBefore(serviceApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  /** Order 5 — catch-all. Default-deny: anything not matched by a higher chain returns 403. */
  @Bean
  @Order(5)
  SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
    return baseStateless(http, AnyRequestMatcher.INSTANCE)
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
   * Builds a hardened decoder enforcing signature, issuer, timestamps, plus the given aud and typ.
   * Used by the /accounts and /admin chains; BE services will configure their own equivalents.
   */
  /**
   * Project convention — granted authorities are stored literally (no {@code SCOPE_} or {@code
   * ROLE_} prefix). Authorize rules use {@code hasAuthority(...)}, never {@code hasRole(...)}.
   */
  private static JwtAuthenticationConverter plainAuthorityConverter() {
    var authorities = new JwtGrantedAuthoritiesConverter();
    authorities.setAuthorityPrefix("");

    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authorities);
    return converter;
  }

  private static JwtDecoder hardenedJwtDecoder(
      JWKSource<SecurityContext> jwkSource, String issuer, String audience, TokenType typ) {
    var processor = new DefaultJWTProcessor<>();

    processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource));

    var decoder = new NimbusJwtDecoder(processor);

    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer),
            new JwtAudValidator(audience),
            new JwtTypValidator(typ)));

    return decoder;
  }

  /**
   * Static so it can be unit-tested without Spring. The shape of this Map is part of the public
   * contract with BE services using {@code JwtDecoders.fromIssuerLocation(...)}; if you change it,
   * update {@code SPRING_BASE_INTEGRATION.md} §3 and let consuming teams know.
   */
  static Map<String, Object> buildDiscoveryDoc(ApplicationProperties.Security security) {
    var issuer = security.issuerServer();
    return Map.ofEntries(
        Map.entry("issuer", issuer),
        Map.entry("jwks_uri", issuer + security.jwksPath()),
        Map.entry("id_token_signing_alg_values_supported", List.of("RS256")),
        Map.entry("subject_types_supported", List.of("public")));
  }

  private static HttpSecurity baseStateless(HttpSecurity http, RequestMatcher matcher)
      throws Exception {
    return http.securityMatcher(matcher)
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            sessionManagementConfigurer ->
                sessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
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

    private final InterServiceAuthenticator interServiceAuthenticator;
    private final HandlerExceptionResolver handlerExceptionResolver;

    ServiceApiKeyFilter(
        InterServiceAuthenticator interServiceAuthenticator,
        HandlerExceptionResolver handlerExceptionResolver) {
      this.interServiceAuthenticator = interServiceAuthenticator;
      this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain chain)
        throws ServletException, IOException {
      try {
        SecurityContextHolder.getContext()
            .setAuthentication(interServiceAuthenticator.authenticate(request));
        chain.doFilter(request, response);
      } catch (ServiceAuthenticationException ex) {
        handlerExceptionResolver.resolveException(request, response, null, ex);
      }
    }
  }
}
