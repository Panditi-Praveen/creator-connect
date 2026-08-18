package com.creatorconnect.hiring.service.impl;

import com.creatorconnect.hiring.dto.response.NotificationResponse;
import com.creatorconnect.hiring.entity.Notification;
import com.creatorconnect.hiring.entity.NotificationType;
import com.creatorconnect.hiring.repository.NotificationRepository;
import com.creatorconnect.hiring.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Concrete {@link NotificationService} implementation.
 *
 * <p>Creates notifications synchronously within the same transaction as the
 * triggering event, but wraps the save in a try/catch so a notification
 * failure never rolls back the business operation.
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void create(UUID recipientId, NotificationType type, String title, String message,
                       UUID relatedResourceId, String relatedResourceType) {
        try {
            Notification notification = Notification.builder()
                    .recipientId(recipientId)
                    .type(type)
                    .title(title)
                    .message(message)
                    .relatedResourceId(relatedResourceId)
                    .relatedResourceType(relatedResourceType)
                    .build();
            notificationRepository.save(notification);
        } catch (Exception ex) {
            // Notification failure must never roll back the triggering operation.
            log.warn("Failed to create notification for recipient {}: {}", recipientId, ex.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(UUID recipientId, boolean unreadOnly, Pageable pageable) {
        Page<Notification> page = unreadOnly
                ? notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(recipientId, pageable)
                : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable);
        return page.map(this::toResponse);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public long unreadCount(UUID recipientId) {
        return notificationRepository.countByRecipientIdAndReadFalse(recipientId);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID recipientId, UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .filter(n -> n.getRecipientId().equals(recipientId))
                .map(n -> {
                    n.setRead(true);
                    return toResponse(notificationRepository.save(n));
                })
                .orElse(null);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public int markAllAsRead(UUID recipientId) {
        return notificationRepository.markAllAsRead(recipientId);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .recipientId(n.getRecipientId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .read(n.isRead())
                .relatedResourceId(n.getRelatedResourceId())
                .relatedResourceType(n.getRelatedResourceType())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
