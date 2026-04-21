package com.vulinh.service.credential;

import com.vulinh.annotation.ExecutionTime;
import com.vulinh.data.dto.LoginRequest;
import com.vulinh.data.entity.AccountCredential;
import com.vulinh.data.entity.AccountCredential.CredentialType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordCredentialStrategy implements CredentialStrategy {

  private final PasswordEncoder passwordEncoder;

  @Override
  public CredentialType supports() {
    return CredentialType.PASSWORD;
  }

  @Override
  @ExecutionTime
  public void verify(LoginRequest request, AccountCredential credential) {
    if (!passwordEncoder.matches(request.password(), credential.getMetadata())) {
      throw new IllegalArgumentException("Invalid credentials");
    }
  }
}
