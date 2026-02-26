package org.example.offer.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client pour le microservice Contract.
 * Création automatique d'un contrat lors de l'acceptation d'une OfferApplication (workflow type Fiverr).
 * Associations via identifiants (offerId, clientId, freelancerId) sans dépendance JPA entre microservices.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContractClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${service.contract.url:http://localhost:8083}")
    private String contractServiceUrl;

    /**
     * Crée un contrat dans le microservice Contract (livraison, validation, paiement gérés par Contract).
     * @return l'ID du contrat créé, ou null si le service est indisponible ou refuse la requête
     */
    public Long createContractFromAcceptedApplication(ContractCreateRequest request) {
        String url = contractServiceUrl + "/api/contracts";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ContractCreateRequest> entity = new HttpEntity<>(request, headers);
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("id")) {
                Object id = body.get("id");
                if (id instanceof Number) {
                    Long contractId = ((Number) id).longValue();
                    log.info("Contract created id={} for offerApplicationId={}, clientId={}, freelancerId={}",
                            contractId, request.getOfferApplicationId(), request.getClientId(), request.getFreelancerId());
                    return contractId;
                }
            }
            log.warn("Contract created but response body missing id: {}", body);
            return null;
        } catch (Exception e) {
            log.warn("Contract creation failed for offerApplicationId={}: {}", request.getOfferApplicationId(), e.getMessage());
            return null;
        }
    }
}
