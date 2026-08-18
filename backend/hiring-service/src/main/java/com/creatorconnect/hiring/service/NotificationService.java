package com.creatorconnect.hiring.service;

import com.creatorconnect.hiring.dto.response.NotificationResponse;
import com.creatorconnect.hiring.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Use cases for the in-app notification system.
 *
 * <p>Notification creation is fire-and-forget: callers should wrap
 * {@link #create} in a try/catch so a notification failure never rolls back
 * the triggering business operation.
 */
public interface NotificationService {

    /**
     * Persists a new notification. Intended for internal use only (called from
     * the application and review services), not exposed as a REST endpoint.
     *
     * @param recipientId       the user who should receive the notification
     * @param type              the event type
     * @param title             short headline
     * @param message           longer description (may be {@code null})
     * @param relatedResourceId the id of the triggering resource (may be {@code null})
     * @param relatedResourceType "APPLICATION", "PROJECT", or "REVIEW" (may be {@code null})
     */
    void create(UUID recipientId, NotificationType type, String title, String message,
                UUID relatedResourceId, String relatedResourceType);

    /**
     * Returns the user's notifications, newest first, paginated.
     */
    Page<NotificationResponse> list(UUID recipientId, boolean unreadOnly, Pageable pageable);

    /**
     * Returns the user's unread notification count.
     */
    long unreadCount(UUID recipientId);

    /**
     * Marks a single notification as read (only if owned by the user).
     *
     * @return the updated notification, or {@code null} if not found / not owned
     */
    NotificationResponse markAsRead(UUID recipientId, UUID notificationId);

    /**
     * Marks all of the user's unread notifications as read.
     *
     * @return the number of notifications marked
     */
    int markAllAsRead(UUID recipientId);
}
