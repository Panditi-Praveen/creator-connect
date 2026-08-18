package com.creatorconnect.hiring.dto.response;

import com.creatorconnect.hiring.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * API-safe projection of a {@link com.creatorconnect.hiring.entity.Notification}.
 *
 * <p>Carried inside the standard {@link ApiResponse} envelope as {@code data}.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private UUID id;
    private UUID recipientId;
    private NotificationType type;
    private String title;
    private String message;
    private boolean read;
    private UUID relatedResourceId;
    private String relatedResourceType;
    private LocalDateTime createdAt;
}
