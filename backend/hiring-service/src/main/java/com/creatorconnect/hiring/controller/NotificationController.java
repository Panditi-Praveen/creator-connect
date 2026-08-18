package com.creatorconnect.hiring.controller;

import com.creatorconnect.hiring.dto.response.ApiResponse;
import com.creatorconnect.hiring.dto.response.NotificationResponse;
import com.creatorconnect.hiring.security.HiringPrincipal;
import com.creatorconnect.hiring.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for the in-app notification system.
 *
 * <p>Base path: {@code /notifications}. All endpoints require a valid JWT.
 * A user may only access their own notifications — the {@code recipientId}
 * is derived from the JWT, never from the request.
 *
 * <p>Routed through the Gateway at {@code /hiring/notifications/**} (the
 * Gateway rewrites the {@code /hiring} prefix before forwarding).
 */
@RestController
@RequestMapping("/notifications")
@Tag(name = "Notification", description = "In-app notifications — list, mark read, unread count")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Returns the authenticated user's notifications (newest first, paginated).
     *
     * @param unreadOnly if {@code true}, return only unread notifications
     * @param pageable   paging/sorting (default: createdAt DESC, size 20)
     */
    @GetMapping
    @Operation(summary = "List notifications", description = "Returns the authenticated user's notifications, newest first. Use ?unreadOnly=true to filter unread only.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notifications retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @PageableDefault(sort = {"createdAt"}, direction = Sort.Direction.DESC, size = 20) Pageable pageable,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        HiringPrincipal principal = (HiringPrincipal) authentication.getPrincipal();
        Page<NotificationResponse> notifications = notificationService.list(principal.userId(), unreadOnly, pageable);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "Notifications retrieved successfully",
                notifications, httpRequest.getRequestURI()
        ));
    }

    /**
     * Returns the count of unread notifications for the authenticated user.
     */
    @GetMapping("/unread-count")
    @Operation(summary = "Unread count", description = "Returns the number of unread notifications for the authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Count retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<ApiResponse<Long>> unreadCount(
            Authentication authentication,
            HttpServletRequest httpRequest) {

        HiringPrincipal principal = (HiringPrincipal) authentication.getPrincipal();
        long count = notificationService.unreadCount(principal.userId());
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "Unread count retrieved",
                count, httpRequest.getRequestURI()
        ));
    }

    /**
     * Marks a single notification as read (only if owned by the caller).
     */
    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read", description = "Marks a single notification as read. Only the notification's recipient may perform this action.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notification marked as read"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Notification not found or not owned by caller")
    })
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable UUID id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        HiringPrincipal principal = (HiringPrincipal) authentication.getPrincipal();
        NotificationResponse updated = notificationService.markAsRead(principal.userId(), id);
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.success(
                            HttpStatus.NOT_FOUND.value(), "Notification not found",
                            null, httpRequest.getRequestURI()
                    ));
        }
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "Notification marked as read",
                updated, httpRequest.getRequestURI()
        ));
    }

    /**
     * Marks all of the caller's unread notifications as read.
     */
    @PatchMapping("/read-all")
    @Operation(summary = "Mark all as read", description = "Marks every unread notification for the authenticated user as read.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All notifications marked as read"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(
            Authentication authentication,
            HttpServletRequest httpRequest) {

        HiringPrincipal principal = (HiringPrincipal) authentication.getPrincipal();
        int count = notificationService.markAllAsRead(principal.userId());
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "All notifications marked as read",
                count, httpRequest.getRequestURI()
        ));
    }
}
