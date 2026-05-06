package com.vulinh.service.credential;

import com.vulinh.data.dto.LoginRequest;
import com.vulinh.data.entity.AccountCredential;
import com.vulinh.data.entity.AccountCredential.CredentialType;
import com.vulinh.data.repository.AccountCredentialRepository;
import com.vulinh.exception.InvalidCredentialsException;
import com.vulinh.utils.CredentialStrategy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OtpCredentialStrategy implements CredentialStrategy {

  private static final MessageDigest SHA256;

  static {
    try {
      SHA256 = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private final AccountCredentialRepository credentialRepository;

  @Override
  public CredentialType supports() {
    return CredentialType.OTP;
  }

  @Override
  public AccountCredential verify(LoginRequest request) {
    var credential =
        credentialRepository
            .findForLogin(request.username(), CredentialType.OTP)
            .orElseThrow(() -> new InvalidCredentialsException("OTP credential not found"));

    if (!credential.getAccount().isAccountEnabled()) {
      throw new InvalidCredentialsException("Account disabled at login");
    }

    if (credential.getExpiresAt() != null && Instant.now().isAfter(credential.getExpiresAt())) {
      throw new InvalidCredentialsException("OTP expired");
    }
    if (!credential
        .getMetadata()
        .equals(
            HexFormat.of()
                .formatHex(SHA256.digest(request.otp().getBytes(StandardCharsets.UTF_8))))) {
      throw new InvalidCredentialsException("OTP mismatch");
    }

    return credentialRepository.save(credential.setEnabled(false).setExpiresAt(null));
  }
}
