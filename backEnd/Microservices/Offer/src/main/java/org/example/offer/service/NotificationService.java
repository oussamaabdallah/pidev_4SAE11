package org.example.offer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.offer.dto.response.NotificationResponse;
import org.example.offer.entity.Notification;
import org.example.offer.entity.NotificationType;
import org.example.offer.exception.ResourceNotFoundException;
import org.example.offer.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    private static final int DEFAULT_PAGE_SIZE = 50;

    @Transactional(readOnly = true)
    public List<NotificationResponse> getByRecipientUserId(Long recipientUserId, int limit) {
        List<Notification> list = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(
                recipientUserId, PageRequest.of(0, limit > 0 ? limit : DEFAULT_PAGE_SIZE));
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countUnreadByRecipientUserId(Long recipientUserId) {
        return notificationRepository.countByRecipientUserIdAndReadFalse(recipientUserId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
        if (!n.getRecipientUserId().equals(userId)) {
            return;
        }
        n.setRead(true);
        notificationRepository.save(n);
    }

    /** S'exécute dans une transaction séparée pour ne pas faire échouer l'appelant (ex: applyToOffer) si l'insert échoue (ex: colonne type trop courte). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createNotification(Long recipientUserId, NotificationType type, String title, String message, Long offerId, Long questionId) {
        Notification n = new Notification();
        n.setRecipientUserId(recipientUserId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setOfferId(offerId);
        n.setQuestionId(questionId);
        n.setRead(false);
        notificationRepository.save(n);
        log.info("Notification created for user {}: {}", recipientUserId, type);
    }

    /**
     * Créer une notification à partir d'une requête (API POST).
     * Utilisé pour notifier le client à chaque mise à jour progression, livraison, ou validation.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createFromRequest(org.example.offer.dto.request.CreateNotificationRequest request) {
        NotificationType type;
        try {
            type = NotificationType.valueOf(request.getType().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid notification type: " + request.getType() + ". Allowed: " + java.util.Arrays.toString(NotificationType.values()));
        }
        createNotification(
                request.getRecipientUserId(),
                type,
                request.getTitle(),
                request.getMessage(),
                request.getOfferId(),
                request.getQuestionId()
        );
    }

    private NotificationResponse toResponse(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.setId(n.getId());
        r.setType(n.getType());
        r.setTitle(n.getTitle());
        r.setMessage(n.getMessage());
        r.setOfferId(n.getOfferId());
        r.setQuestionId(n.getQuestionId());
        r.setRead(n.isRead());
        r.setCreatedAt(n.getCreatedAt());
        return r;
    }
}
