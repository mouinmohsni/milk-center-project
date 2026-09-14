package org.milkcenter.notificationservice.repository;

import org.milkcenter.notificationservice.model.NotificationRecipient;
import org.milkcenter.notificationservice.model.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, Long> {

    List<NotificationRecipient> findByRecipientUserIdOrderByCreatedAtDesc(Long userId);

    Optional<NotificationRecipient> findByNotificationIdAndRecipientUserId(Long notificationId, Long userId);

    long countByRecipientUserIdAndStatus(Long userId, NotificationStatus status);

    List<NotificationRecipient> findByRecipientUserIdAndStatus(Long userId, NotificationStatus status);

    boolean existsByNotificationIdAndRecipientUserId(Long notificationId, Long userId);
}
