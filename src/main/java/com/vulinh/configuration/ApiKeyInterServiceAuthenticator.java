package com.vulinh.configuration;

import module java.base;

import com.vulinh.data.ServiceCodeError;
import com.vulinh.data.predicate.ClientPredicate;
import com.vulinh.data.repository.ClientRepository;
import com.vulinh.exception.ServiceAuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
@RequiredArgsConstructor
public class ApiKeyInterServiceAuthenticator implements InterServiceAuthenticator {

  private static final String HEADER_NAME = "X-Service-Key";

  private final ClientRepository clientRepository;

  @Override
  public Authentication authenticate(HttpServletRequest request) {
    var presented = request.getHeader(HEADER_NAME);

    if (presented == null || presented.isBlank()) {
      throw new ServiceAuthenticationException(
          "Missing %s header".formatted(HEADER_NAME), ServiceCodeError.MISSING_SERVICE_KEY);
    }

    var match =
        clientRepository
            .findOne(new ClientPredicate().byServiceApiKeyHash(sha256Hex(presented)).toPredicate())
            .orElseThrow(
                () ->
                    new ServiceAuthenticationException(
                        "Invalid %s header".formatted(HEADER_NAME),
                        ServiceCodeError.INVALID_SERVICE_KEY));

    return new UsernamePasswordAuthenticationToken(
        String.valueOf(match.getId()), null, List.of(new SimpleGrantedAuthority(BE_TO_BE_ROLE)));
  }

  private static String sha256Hex(String input) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new ExceptionInInitializerError(e);
    }
  }
}
