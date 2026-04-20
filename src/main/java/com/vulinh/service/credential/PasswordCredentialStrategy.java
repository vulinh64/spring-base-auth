package com.vulinh.service.credential;

import com.vulinh.data.dto.LoginRequest;
import com.vulinh.data.entity.AccountCredential;
import com.vulinh.data.entity.AccountCredential.CredentialType;
import com.vulinh.service.PasswordVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordCredentialStrategy implements CredentialStrategy {

  private final PasswordVerifier passwordVerifier;

  @Override
  public CredentialType supports() {
    return CredentialType.PASSWORD;
  }

  @Override
  public void verify(LoginRequest request, AccountCredential credential) {
    if (!passwordVerifier.matches(request.password(), credential.getMetadata())) {
      throw new IllegalArgumentException("Invalid credentials");
    }
  }
}
