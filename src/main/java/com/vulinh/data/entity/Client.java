package com.vulinh.data.entity;

import module java.base;

import jakarta.persistence.*;
import java.util.ArrayList;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SuppressWarnings("java:S2160")
public class Client extends AbstractAuditableEntity<UUID> {

  @Serial private static final long serialVersionUID = 0L;

  @Id @UuidGenerator UUID id;

  @Column(unique = true, nullable = false)
  String clientId;

  String clientName;

  boolean enabled;

  int accessTokenValiditySeconds;

  int refreshTokenValiditySeconds;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "client_id")
  @Builder.Default
  private List<ClientRole> roles = new ArrayList<>();
}
