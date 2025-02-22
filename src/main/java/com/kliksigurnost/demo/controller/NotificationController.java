package com.kliksigurnost.demo.controller;

import com.kliksigurnost.demo.exception.NotificationNotFoundException;
import com.kliksigurnost.demo.exception.UnauthorizedAccessException;
import com.kliksigurnost.demo.model.Notification;
import com.kliksigurnost.demo.service.CloudflareNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final CloudflareNotificationService cloudflareNotificationService;

    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications() {
        log.info("Fetching all notifications for the current user");
        return ResponseEntity.ok(cloudflareNotificationService.getNotificationsByUser());
    }

    @GetMapping("/unseen")
    public ResponseEntity<List<Notification>> getUnseenNotifications() {
        log.info("Fetching unseen notifications for the current user");
        return ResponseEntity.ok(cloudflareNotificationService.getUnseenNotificationsByUser());
    }

    @GetMapping("/unseenCount")
    public ResponseEntity<Integer> getUnseenNotificationCount() {
        log.info("Fetching unseen notification count for the current user");
        return ResponseEntity.ok(cloudflareNotificationService.getUnseenNotificationCountByUser());
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<String> deleteNotification(@PathVariable Integer notificationId) {
        log.info("Deleting notification with ID: {}", notificationId);
        try {
            cloudflareNotificationService.deleteNotification(notificationId);
            return ResponseEntity.ok("Notification deleted successfully");
        } catch (NotificationNotFoundException e) {
            log.warn("Notification not found: {}", notificationId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (UnauthorizedAccessException e) {
            log.warn("Unauthorized access to delete notification: {}", notificationId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}