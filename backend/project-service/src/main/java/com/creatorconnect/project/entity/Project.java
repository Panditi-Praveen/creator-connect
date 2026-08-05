package com.creatorconnect.project.entity;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A creative project posted by a user (the owner).
 *
 * <p>One user may post many projects; the owning user is referenced by the
 * {@code userId} value carried in the {@code userId} claim of the JWT issued
 * by the Auth Service. The Project Service never stores (or reads) user
 * credentials; it only owns the project data below.
 *
 * <p>Design decisions:
 * <ul>
 *   <li><b>UUID primary key</b> — generated at the application level by
 *       Hibernate ({@code GenerationType.UUID}) and stored as {@code CHAR(36)}.</li>
 *   <li><b>{@code userId} as {@code CHAR(36)}</b> — {@code @JdbcTypeCode(SqlTypes.CHAR)}
 *       is mandatory: without it, Hibernate 6 maps a UUID attribute to
 *       {@code BINARY} on MySQL and derived queries like
 *       {@code findByUserId} can never match the stored value. CHAR(36) keeps
 *       writes and reads symmetric (same as {@link #id}).</li>
 *   <li><b>Indexed {@code userId}</b> — the two most common queries
 *       ({@code GET /projects/my} and per-user lookups) filter on it.</li>
 *   <li><b>{@code skillsRequired} as a JSON column</b> — a basic collection
 *       mapped with {@code @JdbcTypeCode(SqlTypes.JSON)}; MySQL stores it as a
 *       native {@code json} array, avoiding a separate join table.</li>
 *   <li><b>JPA auditing</b> — {@code createdAt} / {@code updatedAt} are filled
 *       automatically by {@code JpaAuditingConfig}.</li>
 *   <li><b>Lombok</b> — getters/setters, builder, and both constructors are
 *       generated; the no-args constructor is required by JPA.</li>
 *   <li><b>Bean Validation</b> — column-level constraints mirror the request
 *       DTOs, so Hibernate validates entities on flush even if a future caller
 *       bypasses the REST layer. {@code applicationDeadline} is deliberately
 *       <em>not</em> {@code @Future} here: partial updates must be able to
 *       persist a project whose deadline has already passed; the
 *       {@code @Future} rule lives on the create/update DTOs.</li>
 * </ul>
 */
@Entity
@Table(
        name = "projects",
        indexes = @Index(name = "idx_projects_user_id", columnList = "user_id")
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @Setter(AccessLevel.NONE)
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private UUID id;

    /**
     * Owning user (matches the {@code userId} JWT claim from the Auth Service).
     */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "user_id", nullable = false, updatable = false, length = 36)
    private UUID userId;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category must not exceed 100 characters")
    @Column(name = "category", nullable = false, length = 100)
    private String category;

    /**
     * The skills the project requires (e.g. "After Effects", "Color Grading").
     *
     * <p>Persisted as a native MySQL {@code json} array via
     * {@code @JdbcTypeCode(SqlTypes.JSON)} — no join table, exact round-trip
     * of the list the client sent.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "skills_required", columnDefinition = "json")
    @Builder.Default
    private List<String> skillsRequired = new ArrayList<>();

    @NotNull(message = "Budget is required")
    @DecimalMin(value = "0.0", message = "Budget must be at least 0")
    @Digits(integer = 10, fraction = 2, message = "Budget must have at most 10 integer digits and 2 decimal places")
    @Column(name = "budget", nullable = false, precision = 12, scale = 2)
    private BigDecimal budget;

    @NotBlank(message = "Duration is required")
    @Size(max = 50, message = "Duration must not exceed 50 characters")
    @Column(name = "duration", nullable = false, length = 50)
    private String duration;

    @NotBlank(message = "Experience level is required")
    @Size(max = 50, message = "Experience level must not exceed 50 characters")
    @Column(name = "experience_level", nullable = false, length = 50)
    private String experienceLevel;

    @Size(max = 100, message = "Location must not exceed 100 characters")
    @Column(name = "location", length = 100)
    private String location;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProjectStatus status = ProjectStatus.OPEN;

    @NotNull(message = "Application deadline is required")
    @Column(name = "application_deadline", nullable = false)
    private LocalDate applicationDeadline;

    @CreatedDate
    @Setter(AccessLevel.NONE)
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Setter(AccessLevel.NONE)
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
