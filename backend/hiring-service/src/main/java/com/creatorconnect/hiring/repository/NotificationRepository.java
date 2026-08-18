package com.creatorconnect.hiring.repository;

import com.creatorconnect.hiring.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link Notification} entity.
 *
 * <p>Provides:
 * <ul>
 *   <li>{@link #findByRecipientId} — paginated listing for {@code GET /notifications}.</li>
 *   <li>{@link #countByRecipientIdAndReadFalse} — unread badge count.</li>
 *   <li>{@link #markAllAsRead} — bulk mark-all-read.</li>
 * </ul>
 */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Returns the user's notifications (newest first), paginated.
     *
     * @param recipientId the user's id (from the JWT)
     * @param pageable    paging/sorting (default: createdAt DESC)
     * @return the requested page
     */
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    /**
     * Returns only unread notifications for the user.
     */
    Page<Notification> findByRecipientIdAndReadFalseOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    /**
     * Counts the user's unread notifications.
     *
     * @param recipientId the user's id
     * @return the unread count
     */
    long countByRecipientIdAndReadFalse(UUID recipientId);

    /**
     * Marks every unread notification for the user as read.
     *
     * @param recipientId the user's id
     * @return the number of rows updated
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipientId = :recipientId AND n.read = false")
    int markAllAsRead(@Param("recipientId") UUID recipientId);
}
