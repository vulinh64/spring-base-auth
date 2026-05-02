package com.vulinh.data.repository;

import com.vulinh.data.entity.Account;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;

public interface AccountRepository extends BaseRepository<Account, UUID> {

  @Query(
      """
      select cr.roleName from Account a
      join a.clientRoles cr
      join Client c on c.id = cr.clientId
      where a.id = :accountId
        and (c.clientId = :clientId or str(c.id) = :clientId)
      """)
  List<String> findRoleNames(UUID accountId, String clientId);
}
