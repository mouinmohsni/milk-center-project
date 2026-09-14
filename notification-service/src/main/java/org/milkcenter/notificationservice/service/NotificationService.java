package org.milkcenter.notificationservice.service;

import lombok.RequiredArgsConstructor;
import org.milkcenter.notificationservice.dto.NotificationResponse;
import org.milkcenter.notificationservice.model.*;
import org.milkcenter.notificationservice.repository.NotificationRecipientRepository;
import org.milkcenter.notificationservice.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository recipientRepository;

    @Transactional
    public Notification createForUsers(String eventId, NotificationType type, String title,
                                       String message, Long referenceId,
                                       Collection<Long> userIds, String role) {
        Notification notification = notificationRepository.findByEventId(eventId)
                .orElseGet(() -> notificationRepository.save(Notification.builder()
                        .eventId(eventId).type(type).title(title).message(message)
                        .referenceId(referenceId).build()));

        if (userIds != null) {
            for (Long userId : userIds) {
                if (userId == null || recipientRepository.existsByNotificationIdAndRecipientUserId(notification.getId(), userId)) {
                    continue;
                }
                notification.addRecipient(NotificationRecipient.builder()
                        .recipientUserId(userId).recipientRole(role)
                        .status(NotificationStatus.UNREAD).build());
            }
            notificationRepository.save(notification);
        }
        return notification;
    }

    @Transactional
    public Notification createForUser(String eventId, NotificationType type, String title,
                                      String message, Long referenceId, Long userId, String role) {
        return createForUsers(eventId, type, title, message, referenceId, List.of(userId), role);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> mine(Long userId) {
        return recipientRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId)
                .stream().map(NotificationResponse::from).toList();
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId, Long userId) {
        NotificationRecipient recipient = recipientRepository
                .findByNotificationIdAndRecipientUserId(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification introuvable"));
        recipient.setStatus(NotificationStatus.READ);
        recipient.setReadAt(LocalDateTime.now());
        return NotificationResponse.from(recipientRepository.save(recipient));
    }

    @Transactional
    public int markAllRead(Long userId) {
        List<NotificationRecipient> recipients = recipientRepository
                .findByRecipientUserIdAndStatus(userId, NotificationStatus.UNREAD);
        recipients.forEach(recipient -> {
            recipient.setStatus(NotificationStatus.READ);
            recipient.setReadAt(LocalDateTime.now());
        });
        recipientRepository.saveAll(recipients);
        return recipients.size();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return recipientRepository.countByRecipientUserIdAndStatus(userId, NotificationStatus.UNREAD);
    }
}
