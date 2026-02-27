package org.example.offer.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * Fallback Feign : appelé si le microservice Contract est indisponible
 * (timeout, service down, circuit ouvert).
 * Retourne une Map vide → l'appelant interprète l'absence d'"id" comme un échec partiel
 * et affiche un message d'avertissement sans bloquer l'acceptation de la candidature.
 */
@Component
@Slf4j
public class ContractFeignClientFallback implements ContractFeignClient {

    @Override
    public Map<String, Object> createContract(ContractCreateRequest request) {
        log.warn("Contract service unavailable — fallback triggered for offerApplicationId={}",
                request.getOfferApplicationId());
        return Collections.emptyMap();
    }
}
