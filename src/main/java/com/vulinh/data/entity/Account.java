package com.vulinh.data.entity;

import module java.base;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.UuidGenerator;

// To avoid "user" identifier
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@SuppressWarnings("java:S2160")
public class Account extends AbstractAuditableEntity<UUID> {

  @Serial private static final long serialVersionUID = 0L;

  @Id @UuidGenerator UUID id;

  String username;

  String email;

  String firstName;

  String lastName;

  @Builder.Default
  boolean isEnabled = true;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "account_id")
  @Builder.Default
  private Set<AccountCredential> credentials = new LinkedHashSet<>();

  @ManyToMany
  @JoinTable(
      name = "account_client_role",
      joinColumns = @JoinColumn(name = "account_id"),
      inverseJoinColumns = @JoinColumn(name = "client_role_id"))
  @Builder.Default
  private Set<ClientRole> clientRoles = new LinkedHashSet<>();
}
