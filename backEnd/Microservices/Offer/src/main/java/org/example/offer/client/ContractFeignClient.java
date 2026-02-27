package org.example.offer.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Client Feign pour le microservice Contract.
 *
 * - name = "CONTRACT" : nom Eureka du service Contract (spring.application.name=CONTRACT).
 *   Feign interroge Eureka pour obtenir l'adresse réelle (ex: localhost:8083).
 *   Si Eureka est indisponible, le fallback est l'URL directe dans application.properties.
 *
 * - path = "/api/contracts" : préfixe commun à tous les endpoints de ce client.
 *
 * Feign génère automatiquement l'implémentation HTTP à partir de cette interface.
 * Aucun code RestTemplate, aucune gestion manuelle des headers.
 */
@FeignClient(
        name = "CONTRACT",
        path = "/api/contracts",
        fallback = ContractFeignClientFallback.class
)
public interface ContractFeignClient {

    /**
     * Crée un contrat dans le microservice Contract.
     * POST http://CONTRACT/api/contracts
     *
     * @param request données du contrat (clientId, freelancerId, amount, dates…)
     * @return Map contenant au minimum { "id": <Long> } ou vide si erreur
     */
    @PostMapping
    Map<String, Object> createContract(@RequestBody ContractCreateRequest request);
}
