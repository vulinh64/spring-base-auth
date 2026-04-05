package com.vulinh.data.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"client_id", "role_name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientRole extends AbstractAuditableEntity<UUID> {

  @Id @UuidGenerator UUID id;

  @Column(name = "client_id")
  UUID clientId;

  @Column(name = "role_name", nullable = false)
  String roleName;
}
