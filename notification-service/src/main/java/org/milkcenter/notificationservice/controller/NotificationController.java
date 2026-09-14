package org.milkcenter.notificationservice.controller;

import lombok.RequiredArgsConstructor;
import org.milkcenter.notificationservice.dto.NotificationResponse;
import org.milkcenter.notificationservice.security.CurrentUserService;
import org.milkcenter.notificationservice.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/api/notifications") @RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;
    private final CurrentUserService currentUser;

    @GetMapping("/me") public List<NotificationResponse> mine() {

        return service.mine(currentUser.userId());
    }
    @GetMapping("/me/unread-count") public Map<String, Long> unreadCount() {

        return Map.of("count", service.unreadCount(currentUser.userId()));
    }
    @PatchMapping("/{id}/read") public NotificationResponse read(@PathVariable Long id) {
        return service.markRead(id, currentUser.userId());
    }
    @PatchMapping("/me/read-all") public Map<String, Integer> readAll() {

        return Map.of("updated", service.markAllRead(currentUser.userId()));
    }
}
