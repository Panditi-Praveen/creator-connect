package com.creatorconnect.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Core identity record for every CreatorConnect user.
 *
 * <p>One {@code User} row exists per person — whether Creator or Freelancer —
 * and other services reference it by {@link #getId()} (UUID).
 *
 * <p>Design decisions:
 * <ul>
 *   <li><b>UUID primary key</b> — generated at the application level by
 *       Hibernate ({@code GenerationType.UUID}) and stored as {@code CHAR(36)}
 *       for readable values.</li>
 *   <li><b>JPA auditing</b> — {@code createdAt} / {@code updatedAt} are filled
 *       automatically by {@code JpaAuditingConfig}.</li>
 *   <li><b>Lombok</b> — getters/setters, builder, and both constructors are
 *       generated; the no-args constructor is required by JPA.</li>
 * </ul>
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @Setter(AccessLevel.NONE)
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private UUID id;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "provider", nullable = false, length = 20)
    private AuthProvider provider = AuthProvider.LOCAL;

    @Builder.Default
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Builder.Default
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @CreatedDate
    @Setter(AccessLevel.NONE)
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Setter(AccessLevel.NONE)
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
