package com.vulinh.data.repository;

import com.vulinh.data.entity.Account;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID>, AccountRepositoryCustom {}
