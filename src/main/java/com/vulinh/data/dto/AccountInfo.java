package com.vulinh.data.dto;

import com.vulinh.data.entity.Account;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.With;
import org.apache.commons.lang3.StringUtils;

@Builder
@With
public record AccountInfo(
    UUID id, String username, String email, String displayName, List<String> roles) {

  public static AccountInfo from(Account account, List<String> roles) {
    var first = StringUtils.trimToEmpty(account.getFirstName());
    var last = StringUtils.trimToEmpty(account.getLastName());
    var display = StringUtils.trimToNull(StringUtils.normalizeSpace(first + " " + last));

    return AccountInfo.builder()
        .id(account.getId())
        .username(account.getUsername())
        .email(account.getEmail())
        .displayName(display != null ? display : account.getUsername())
        .roles(roles)
        .build();
  }
}
