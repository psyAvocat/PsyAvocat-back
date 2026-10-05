package com.psyavocat.controller;

import com.psyavocat.dto.notification.NotificationResponseDTO;
import com.psyavocat.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponseDTO>> getMesNotifications() {
        return ResponseEntity.ok(notificationService.getMesNotifications());
    }

    @PatchMapping("/{id}/lu")
    public ResponseEntity<Void> markAsRead(@PathVariable String id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/lu-tout")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable String id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }
}
