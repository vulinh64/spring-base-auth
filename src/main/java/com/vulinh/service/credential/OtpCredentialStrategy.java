package com.vulinh.service.credential;

import com.vulinh.data.dto.LoginRequest;
import com.vulinh.data.entity.AccountCredential;
import com.vulinh.data.entity.AccountCredential.CredentialType;
import com.vulinh.data.repository.AccountCredentialRepository;
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
    if (!credential.getMetadata().equals(hashOtp(request.otp()))) {
      throw new IllegalArgumentException("Invalid credentials");
    }
    credential.setEnabled(false);
    credentialRepository.save(credential);
  }

  private static String hashOtp(String otp) {
    try {
      var hash = MessageDigest.getInstance("SHA-256").digest(otp.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
