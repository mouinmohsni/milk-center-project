package org.milkcenter.notificationservice.dto;

import org.milkcenter.notificationservice.model.NotificationRecipient;
import org.milkcenter.notificationservice.model.NotificationStatus;
import org.milkcenter.notificationservice.model.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String eventId,
        NotificationType type,
        String title,
        String message,
        Long recipientUserId,
        String recipientRole,
        Long referenceId,
        NotificationStatus status,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
    public static NotificationResponse from(NotificationRecipient recipient) {
        var notification = recipient.getNotification();
        return new NotificationResponse(
                notification.getId(), notification.getEventId(), notification.getType(),
                notification.getTitle(), notification.getMessage(),
                recipient.getRecipientUserId(), recipient.getRecipientRole(),
                notification.getReferenceId(), recipient.getStatus(),
                notification.getCreatedAt(), recipient.getReadAt()
        );
    }
}
