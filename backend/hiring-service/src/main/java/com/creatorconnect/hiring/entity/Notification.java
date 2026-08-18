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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * An in-app notification delivered to a user when a hiring or review event
 * occurs.
 *
 * <p>Notifications are created synchronously inside the same transaction as
 * the triggering event (apply, accept, reject, withdraw, review). A failure
 * in notification creation is logged and swallowed — the main operation must
 * never roll back because a notification could not be persisted.
 *
 * <p>Design decisions mirror {@link Application}:
 * <ul>
 *   <li><b>UUID primary key</b> — generated at the application level by
 *       Hibernate and stored as {@code CHAR(36)}.</li>
 *   <li><b>JPA auditing</b> — {@code createdAt} / {@code updatedAt} are
 *       filled automatically by {@code JpaAuditingConfig}.</li>
 *   <li><b>Indexed {@code recipient_id}</b> — the single most common query
 *       (list a user's notifications).</li>
 *   <li><b>Indexed {@code recipient_id + is_read}</b> — the unread-count
 *       query.</li>
 * </ul>
 */
@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_recipient_id", columnList = "recipient_id"),
                @Index(name = "idx_notifications_recipient_read", columnList = "recipient_id, is_read")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @Setter(AccessLevel.NONE)
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private UUID id;

    /** The user who should see this notification (matches the JWT {@code userId}). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "recipient_id", nullable = false, length = 36)
    private UUID recipientId;

    @NotNull(message = "Notification type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @NotBlank(message = "Notification title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Size(max = 500, message = "Message must not exceed 500 characters")
    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    /** The ID of the related resource (application, project, or review). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "related_resource_id", length = 36)
    private UUID relatedResourceId;

    /** Discriminator for the related resource: APPLICATION, PROJECT, or REVIEW. */
    @Size(max = 20, message = "Resource type must not exceed 20 characters")
    @Column(name = "related_resource_type", length = 20)
    private String relatedResourceType;

    @CreatedDate
    @Setter(AccessLevel.NONE)
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Setter(AccessLevel.NONE)
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
