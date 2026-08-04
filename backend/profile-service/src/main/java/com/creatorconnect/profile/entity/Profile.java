package com.creatorconnect.profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
 * The creator/freelancer profile record.
 *
 * <p>One {@code Profile} row exists per user and is referenced by the user's
 * {@code UUID} identity — the same value carried in the {@code userId} claim of
 * the JWT issued by the Auth Service. The Profile Service never stores (or
 * reads) user credentials; it only owns the public professional data below.
 *
 * <p>Design decisions:
 * <ul>
 *   <li><b>UUID primary key</b> — generated at the application level by
 *       Hibernate ({@code GenerationType.UUID}) and stored as {@code CHAR(36)}.</li>
 *   <li><b>Unique {@code userId}</b> — a user may have exactly one profile;
 *       the database enforces it and the service rejects duplicates with
 *       {@code 409 CONFLICT}.</li>
 *   <li><b>JPA auditing</b> — {@code createdAt} / {@code updatedAt} are filled
 *       automatically by {@code JpaAuditingConfig}.</li>
 *   <li><b>Lombok</b> — getters/setters, builder, and both constructors are
 *       generated; the no-args constructor is required by JPA.</li>
 *   <li><b>Bean Validation</b> — column-level constraints mirror the request
 *       DTOs, so Hibernate validates entities on flush even if a future caller
 *       bypasses the REST layer.</li>
 * </ul>
 */
@Entity
@Table(name = "profiles")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Profile {

    @Id
    @Setter(AccessLevel.NONE)
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private UUID id;

    /**
     * Owning user (matches the {@code userId} JWT claim from the Auth Service).
     *
     * <p>{@code @JdbcTypeCode(SqlTypes.CHAR)} is mandatory here: without it,
     * Hibernate 6 maps a UUID attribute to {@code BINARY} on MySQL, so
     * {@code ddl-auto} created the column as {@code binary(36)} and bound the
     * UUID as 16 raw bytes on insert. Reads ({@code findByUserId}) then could
     * never match the stored value and every read/update/delete returned 404
     * even though create succeeded. CHAR(36) keeps the value a readable UUID
     * string and makes writes and reads symmetric (same as {@link #id}).
     */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "user_id", nullable = false, unique = true, updatable = false, length = 36)
    private UUID userId;

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Size(max = 150, message = "Headline must not exceed 150 characters")
    @Column(name = "headline", length = 150)
    private String headline;

    @Size(max = 2000, message = "Bio must not exceed 2000 characters")
    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Size(max = 500, message = "Profile image URL must not exceed 500 characters")
    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Size(max = 100, message = "Location must not exceed 100 characters")
    @Column(name = "location", length = 100)
    private String location;

    @Size(max = 300, message = "Website must not exceed 300 characters")
    @Column(name = "website", length = 300)
    private String website;

    @Size(max = 300, message = "LinkedIn URL must not exceed 300 characters")
    @Column(name = "linkedin", length = 300)
    private String linkedin;

    @Size(max = 300, message = "GitHub URL must not exceed 300 characters")
    @Column(name = "github", length = 300)
    private String github;

    @Size(max = 1000, message = "Skills must not exceed 1000 characters")
    @Column(name = "skills", length = 1000)
    private String skills;

    @Min(value = 0, message = "Experience must be at least 0")
    @Max(value = 100, message = "Experience must not exceed 100")
    @Column(name = "experience")
    private Integer experience;

    @Builder.Default
    @Column(name = "available_for_hire", nullable = false)
    private Boolean availableForHire = true;

    @CreatedDate
    @Setter(AccessLevel.NONE)
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Setter(AccessLevel.NONE)
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
