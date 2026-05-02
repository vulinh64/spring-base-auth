package com.vulinh.data.entity;

import module java.base;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@SuppressWarnings("java:S2160")
public class Client extends AbstractAuditableEntity<UUID> {

  @Serial private static final long serialVersionUID = 0L;

  @Id @UuidGenerator UUID id;

  String clientId;

  String clientName;

  boolean enabled;

  int accessTokenValiditySeconds;

  int refreshTokenValiditySeconds;

  String serviceApiKeyHash;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "client_id")
  @Builder.Default
  private List<ClientRole> roles = new ArrayList<>();
}
