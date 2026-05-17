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
public class ClientRole extends AbstractAuditableEntity<UUID> {

  @Serial private static final long serialVersionUID = 0L;

  @Id @UuidGenerator UUID id;

  @Column(name = "client_id")
  UUID clientId;

  @Column(name = "role_name", nullable = false)
  String roleName;
}
