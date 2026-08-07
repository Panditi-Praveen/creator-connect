package com.creatorconnect.hiring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A freelancer's application to a posted project.
 *
 * <p>The freelancer is referenced by the {@code freelancerId} value carried
 * in the {@code userId} claim of the JWT issued by the Auth Service; the
 * project is referenced by {@code projectId} only — the Hiring Service does
 * not store (or read) project data, which lives in the Project Service.
 * Project existence and creator project-ownership are deliberately <em>not</em>
 * verified here yet; that cross-service integration is scheduled for Day 6
 * (Hiring Service &harr; Project Service via OpenFeign).
 *
 * <p>Design decisions:
 * <ul>
 *   <li><b>UUID primary key</b> — generated at the application level by
 *       Hibernate ({@code GenerationType.UUID}) and stored as {@code CHAR(36)}.</li>
 *   <li><b>{@code projectId} / {@code freelancerId} as {@code CHAR(36)}</b> —
 *       {@code @JdbcTypeCode(SqlTypes.CHAR)} is mandatory: without it,
 *       Hibernate 6 maps a UUID attribute to {@code BINARY} on MySQL and
 *       derived queries like {@code existsByProjectIdAndFreelancerId} can
 *       never match the stored value (same rationale as the Project Service).</li>
 *   <li><b>Unique (projectId, freelancerId)</b> — the database-level guard
 *       against duplicate applications; the service also checks
 *       {@code existsByProjectIdAndFreelancerId} before inserting.</li>
 *   <li><b>Indexed {@code freelancerId} and {@code projectId}</b> — the two
 *       most common queries ({@code GET /applications/my} and
 *       {@code GET /applications/project/{projectId}}) filter on them.</li>
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
        name = "applications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_applications_project_freelancer",
                columnNames = {"project_id", "freelancer_id"}
        ),
        indexes = {
                @Index(name = "idx_applications_freelancer_id", columnList = "freelancer_id"),
                @Index(name = "idx_applications_project_id", columnList = "project_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Application {

    @Id
    @Setter(AccessLevel.NONE)
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private UUID id;

    /**
     * The project being applied to. Stored as a reference only — the project
     * itself (and its owner) lives in the Project Service.
     */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "project_id", nullable = false, updatable = false, length = 36)
    private UUID projectId;

    /**
     * The applying freelancer (matches the {@code userId} JWT claim from the
     * Auth Service).
     */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "freelancer_id", nullable = false, updatable = false, length = 36)
    private UUID freelancerId;

    @NotBlank(message = "Proposal is required")
    @Size(max = 5000, message = "Proposal must not exceed 5000 characters")
    @Column(name = "proposal", nullable = false, columnDefinition = "TEXT")
    private String proposal;

    @NotNull(message = "Expected budget is required")
    @DecimalMin(value = "0.0", message = "Expected budget must be at least 0")
    @Digits(integer = 10, fraction = 2, message = "Expected budget must have at most 10 integer digits and 2 decimal places")
    @Column(name = "expected_budget", nullable = false, precision = 12, scale = 2)
    private BigDecimal expectedBudget;

    @NotBlank(message = "Estimated duration is required")
    @Size(max = 50, message = "Estimated duration must not exceed 50 characters")
    @Column(name = "estimated_duration", nullable = false, length = 50)
    private String estimatedDuration;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @CreatedDate
    @Setter(AccessLevel.NONE)
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Setter(AccessLevel.NONE)
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
