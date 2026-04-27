package com.vulinh.utils;

import com.vulinh.data.dto.LoginRequest;
import com.vulinh.data.entity.AccountCredential;
import com.vulinh.data.entity.AccountCredential.CredentialType;

public interface CredentialStrategy {

  CredentialType supports();

  AccountCredential verify(LoginRequest request);
}
