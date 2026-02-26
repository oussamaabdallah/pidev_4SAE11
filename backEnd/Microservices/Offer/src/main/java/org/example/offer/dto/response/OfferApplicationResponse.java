package org.example.offer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.offer.entity.ApplicationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfferApplicationResponse {

    private Long id;
    private Long offerId;
    private String offerTitle;
    private Long clientId;
    private String message;
    private BigDecimal proposedBudget;
    private String portfolioUrl;
    private String attachmentUrl;
    private Integer estimatedDuration;
    private ApplicationStatus status;
    private String rejectionReason;
    private Boolean isRead;
    private LocalDateTime appliedAt;
    private LocalDateTime respondedAt;
    private LocalDateTime acceptedAt;
    private Boolean canBeModified;
    private String selectedPackage;
    private String selectedExtrasJson;
    private BigDecimal totalAmount;
    /** ID du contrat créé après acceptation (pour accès "Mes projets en cours", mise à jour progression). */
    private Long contractId;
    /** Message d'avertissement (ex: contrat non créé car microservice indisponible). */
    private String warningMessage;
}