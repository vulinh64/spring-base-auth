package com.vulinh.data.repository;

import com.vulinh.data.entity.AccountCredential;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountCredentialRepository extends JpaRepository<AccountCredential, UUID> {}
