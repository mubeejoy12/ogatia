package com.eazicut.api.common.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.Setter;

/**
 * Base class for auditable entities.
 *
 * <p>Provides {@code createdAt} and {@code updatedAt} managed automatically by
 * Spring Data JPA auditing (see {@link org.springframework.data.jpa.repository.config.EnableJpaAuditing @EnableJpaAuditing}
 * on the main application class).
 *
 * <p>Soft-delete is <em>not</em> included here; entities that support soft delete
 * add their own {@code deletedAt} field plus {@link org.hibernate.annotations.SQLDelete SQLDelete}
 * and {@link org.hibernate.annotations.SQLRestriction SQLRestriction} annotations — most
 * reference data (categories, collections) should be hard-referenced instead.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AbstractAuditableEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
