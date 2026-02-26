package org.example.offer.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.offer.dto.request.CreateNotificationRequest;
import org.example.offer.dto.response.NotificationResponse;
import org.example.offer.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Créer une notification (pour progression, livraison, validation client).
     * POST /api/notifications
     * Body: { "recipientUserId": 1, "type": "PROGRESS_UPDATE", "title": "...", "message": "...", "offerId": null, "questionId": null }
     * Types: APPLICATION_ACCEPTED, PROGRESS_UPDATE, PROJECT_DELIVERED, PROJECT_VALIDATED, NEW_QUESTION, QUESTION_ANSWERED, NEW_APPLICATION
     */
    @PostMapping
    public ResponseEntity<Void> createNotification(@Valid @RequestBody CreateNotificationRequest request) {
        notificationService.createFromRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Récupérer les notifications pour un utilisateur (à l'ouverture du dashboard / page).
     * GET /api/notifications?recipientUserId=1&limit=50
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @RequestParam Long recipientUserId,
            @RequestParam(defaultValue = "50") int limit) {
        List<NotificationResponse> list = notificationService.getByRecipientUserId(recipientUserId, limit);
        return ResponseEntity.ok(list);
    }

    /**
     * Nombre de notifications non lues (pour badge).
     * GET /api/notifications/unread-count?recipientUserId=1
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@RequestParam Long recipientUserId) {
        long count = notificationService.countUnreadByRecipientUserId(recipientUserId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Marquer une notification comme lue.
     * PATCH /api/notifications/{id}/read?userId=1
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, @RequestParam Long userId) {
        notificationService.markAsRead(id, userId);
        return ResponseEntity.noContent().build();
    }
}
