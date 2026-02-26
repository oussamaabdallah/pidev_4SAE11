package org.example.offer.entity;

public enum NotificationType {
    NEW_QUESTION,         // Client a posé une question → notifier le freelancer
    QUESTION_ANSWERED,    // Freelancer a répondu → notifier le client
    NEW_APPLICATION,      // Client a postulé → notifier le freelancer
    APPLICATION_ACCEPTED, // Freelancer a accepté la candidature → notifier le client
    PROGRESS_UPDATE,      // Freelancer a mis à jour la progression → notifier le client
    PROJECT_DELIVERED,    // Freelancer a livré le projet → notifier le client
    PROJECT_VALIDATED     // Client a validé le projet → notifier le freelancer
}
