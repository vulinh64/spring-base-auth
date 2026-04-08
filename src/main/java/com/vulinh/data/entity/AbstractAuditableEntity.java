package com.vulinh.data.entity;

import module java.base;

import com.vulinh.data.base.AbstractTimestampAuditableEntity;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

@Getter
@Setter
@MappedSuperclass
@SuppressWarnings("java:S2160")
public abstract class AbstractAuditableEntity<T extends Serializable>
    extends AbstractTimestampAuditableEntity<T> {

  @Serial private static final long serialVersionUID = 0L;

  @CreatedBy protected String createdBy;

  @LastModifiedBy protected String updatedBy;
}
