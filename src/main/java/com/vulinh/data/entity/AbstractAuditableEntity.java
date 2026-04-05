package com.vulinh.data.entity;

import com.vulinh.data.base.AbstractTimestampAuditableEntity;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

import java.io.Serializable;

@Getter
@Setter
@MappedSuperclass
public abstract class AbstractAuditableEntity<T extends Serializable>
    extends AbstractTimestampAuditableEntity<T> {

  @CreatedBy protected String createdBy;

  @LastModifiedBy protected String updatedBy;
}
