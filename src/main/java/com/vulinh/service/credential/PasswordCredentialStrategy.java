package com.vulinh.service.credential;

import com.vulinh.annotation.aspect.ExecutionTime;
import com.vulinh.data.dto.LoginRequest;
import com.vulinh.data.entity.AccountCredential;
import com.vulinh.data.entity.AccountCredential.CredentialType;
import com.vulinh.data.repository.AccountCredentialRepository;
import com.vulinh.exception.InvalidCredentialsException;
import com.vulinh.utils.CredentialStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordCredentialStrategy implements CredentialStrategy {

  private final PasswordEncoder passwordEncoder;
  private final AccountCredentialRepository credentialRepository;

  @Override
  public CredentialType supports() {
    return CredentialType.PASSWORD;
  }

  @Override
  @ExecutionTime
  public AccountCredential verify(LoginRequest request) {
    var credential =
        credentialRepository
            .findForLogin(request.username(), CredentialType.PASSWORD)
            .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

    if (!credential.getAccount().isAccountEnabled()) {
      throw new InvalidCredentialsException("Account disabled at login");
    }

    if (!passwordEncoder.matches(request.password(), credential.getMetadata())) {
      throw new InvalidCredentialsException("Password mismatch");
    }

    return credential;
  }
}
