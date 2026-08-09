package com.creatorconnect.hiring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
 * A review left by a creator for a freelancer after working together on a
 * project.
 *
 * <p>The creator is referenced by the {@code creatorId} value carried in the
 * {@code userId} claim of the JWT issued by the Auth Service; the reviewed
 * freelancer and the project are referenced by id only — the Hiring Service
 * does not store (or read) profile or project data, which lives in the Profile
 * and Project Services respectively.
 *
 * <p>Business rules enforced by the service layer:
 * <ul>
 *   <li><b>Creator &amp; owner only</b> — only authenticated {@code CREATOR}s
 *       who own the project may submit a review ({@code creatorId} comes from
 *       the JWT, never the body; ownership is verified against the Project
 *       Service via OpenFeign).</li>
 *   <li><b>Hired first</b> — the freelancer must have an {@code ACCEPTED}
 *       application on the project before they can be reviewed (verified
 *       against the {@code applications} table, which lives in this service).</li>
 *   <li><b>One review per pair</b> — at most one review per
 *       {@code (project, freelancer)} pair, guarded by the service check and
 *       the database-level unique constraint.</li>
 * </ul>
 *
 * <p>Design decisions (mirroring {@link Application}):
 * <ul>
 *   <li><b>UUID primary key</b> — generated at the application level by
 *       Hibernate ({@code GenerationType.UUID}) and stored as {@code CHAR(36)}.</li>
 *   <li><b>{@code projectId} / {@code creatorId} / {@code freelancerId} as
 *       {@code CHAR(36)}</b> — {@code @JdbcTypeCode(SqlTypes.CHAR)} is
 *       mandatory: without it, Hibernate 6 maps a UUID attribute to
 *       {@code BINARY} on MySQL and derived queries can never match the
 *       stored value (same rationale as the Application entity).</li>
 *   <li><b>Unique (projectId, freelancerId)</b> — the database-level guard
 *       against duplicate reviews; the service also checks
 *       {@code existsByProjectIdAndFreelancerId} before inserting.</li>
 *   <li><b>Indexed {@code freelancerId}</b> — the listing query
 *       ({@code GET /reviews/freelancer/{id}}) filters on it.</li>
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
@Table(
        name = "reviews",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reviews_project_freelancer",
                columnNames = {"project_id", "freelancer_id"}
        ),
        indexes = {
                @Index(name = "idx_reviews_freelancer_id", columnList = "freelancer_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @Setter(AccessLevel.NONE)
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private UUID id;

    /**
     * The project the review is about. Stored as a reference only — the
     * project itself (and its owner) lives in the Project Service.
     */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "project_id", nullable = false, updatable = false, length = 36)
    private UUID projectId;

    /**
     * The reviewing creator (matches the {@code userId} JWT claim from the
     * Auth Service).
     */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "creator_id", nullable = false, updatable = false, length = 36)
    private UUID creatorId;

    /**
     * The reviewed freelancer (matches the {@code userId} JWT claim from the
     * Auth Service).
     */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "freelancer_id", nullable = false, updatable = false, length = 36)
    private UUID freelancerId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Size(max = 2000, message = "Review text must not exceed 2000 characters")
    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;

    @CreatedDate
    @Setter(AccessLevel.NONE)
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Setter(AccessLevel.NONE)
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
