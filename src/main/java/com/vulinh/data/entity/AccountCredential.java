package com.vulinh.data.entity;

import module java.base;

import com.vulinh.data.ServiceCodeError;
import com.vulinh.exception.ApplicationValidationException;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "credentialType"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@SuppressWarnings("java:S2160")
public class AccountCredential extends AbstractAuditableEntity<UUID> {

  @Serial private static final long serialVersionUID = 0L;

  @Id UUID id;

  @Column(name = "account_id")
  UUID accountId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", insertable = false, updatable = false)
  @ToString.Exclude
  Account account;

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
        case null, default ->
            throw new ApplicationValidationException(
                "Unsupported grant_type: %s".formatted(grantType),
                ServiceCodeError.UNSUPPORTED_GRANT_TYPE,
                grantType);
      };
    }
  }
}
