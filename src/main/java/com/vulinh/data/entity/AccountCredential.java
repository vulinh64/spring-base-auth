package com.vulinh.data.entity;

import module java.base;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "credentialType"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SuppressWarnings("java:S2160")
public class AccountCredential extends AbstractAuditableEntity<UUID> {

  @Serial private static final long serialVersionUID = 0L;

  @Id UUID id;

  @Column(name = "account_id")
  UUID accountId;

  @Enumerated(EnumType.STRING)
  CredentialType credentialType;

  String metadata;

  boolean enabled;

  Instant expiresAt;

  public enum CredentialType {
    PASSWORD,
    OTP;

    public static CredentialType fromGrantType(String grantType) {
      return switch (grantType) {
        case "password" -> PASSWORD;
        case "otp" -> OTP;
        case null, default -> throw new IllegalArgumentException("Unsupported grant type");
      };
    }
  }
}
