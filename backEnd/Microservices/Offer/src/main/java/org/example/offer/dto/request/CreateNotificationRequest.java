package org.example.offer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Requête pour créer une notification (ex: mise à jour progression, livraison, validation client).
 * Permet au frontend ou à un autre service d'envoyer une notification sans modifier le microservice Offer en profondeur.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationRequest {

    @NotNull(message = "recipientUserId is required")
    private Long recipientUserId;

    @NotBlank(message = "type is required")
    private String type; // APPLICATION_ACCEPTED, PROGRESS_UPDATE, PROJECT_DELIVERED, PROJECT_VALIDATED, etc.

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "message is required")
    private String message;

    private Long offerId;
    private Long questionId;
}
