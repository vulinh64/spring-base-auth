package com.vulinh.service.credential;

import com.vulinh.data.dto.LoginRequest;
import com.vulinh.data.entity.AccountCredential;
import com.vulinh.data.entity.AccountCredential.CredentialType;
import com.vulinh.data.repository.AccountCredentialRepository;
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
  public void verify(LoginRequest request, AccountCredential credential) {
    if (credential.getExpiresAt() != null && Instant.now().isAfter(credential.getExpiresAt())) {
      throw new IllegalArgumentException("OTP expired");
    }
    if (!credential
        .getMetadata()
        .equals(
            HexFormat.of()
                .formatHex(SHA256.digest(request.otp().getBytes(StandardCharsets.UTF_8))))) {
      throw new IllegalArgumentException("Invalid credentials");
    }

    credentialRepository.save(credential.setEnabled(false).setExpiresAt(null));
  }
}
